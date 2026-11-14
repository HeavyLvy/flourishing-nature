package io.github.heavylvy.flourishingnature.neoforge.nest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.heavylvy.flourishingnature.core.nest.Clutch;
import io.github.heavylvy.flourishingnature.core.nest.NestEgg;
import io.github.heavylvy.flourishingnature.core.nest.NestOwner;
import io.github.heavylvy.flourishingnature.core.nest.NestRules;
import io.github.heavylvy.flourishingnature.core.nest.NestSettings;
import io.github.heavylvy.flourishingnature.neoforge.boundary.NestWorld;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A nest's eggs, owner, and when a hen was last around. Every second it checks for a hen sitting
 * on it (which warms fertile eggs), whether its owner is still about, and whether an abandoned
 * wild nest has fallen apart.
 */
public final class NestBlockEntity extends BlockEntity {
    /** Work happens once a second; incubation counts the whole second when a hen is sitting. */
    static final int CHECK_INTERVAL = 20;

    private static final Codec<NestEgg> EGG_CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.BOOL.fieldOf("fertile").forGetter(egg -> egg instanceof NestEgg.Fertile),
                    Codec.LONG.optionalFieldOf("warmth", 0L).forGetter(NestBlockEntity::warmth))
            .apply(i, (fertile, warmth) -> fertile ? new NestEgg.Fertile(warmth) : new NestEgg.Plain()));

    private Clutch clutch = Clutch.EMPTY;
    private NestOwner owner = new NestOwner.Unowned();
    private long lastHenSeen;

    public NestBlockEntity(BlockPos pos, BlockState state) {
        super(NestRegistry.NEST_ENTITY.get(), pos, state);
    }

    private static long warmth(NestEgg egg) {
        return switch (egg) {
            case NestEgg.Plain plain -> 0L;
            case NestEgg.Fertile fertile -> fertile.warmth();
        };
    }

    public Clutch clutch() {
        return clutch;
    }

    public NestOwner owner() {
        return owner;
    }

    /** Crafted nests never decay; wild ones, built by hens, do once abandoned. */
    public boolean isWild() {
        return getBlockState().getValue(NestBlock.WILD);
    }

    public long lastHenSeen() {
        return lastHenSeen;
    }

    /** Test hook: pretend the last hen was here at {@code tick}, to skip days of waiting. */
    public void setLastHenSeen(long tick) {
        lastHenSeen = tick;
        setChanged();
    }

    /** Adds an egg; false if the nest is full. Public so tests can stock a nest. */
    public boolean addEgg(NestEgg egg) {
        Optional<Clutch> more = clutch.with(egg);
        more.ifPresent(this::setClutch);
        return more.isPresent();
    }

    /** A player takes the newest egg. */
    Optional<NestEgg> takeEgg() {
        Optional<Clutch.Taken> taken = clutch.takeNewest();
        taken.ifPresent(t -> setClutch(t.rest()));
        return taken.map(Clutch.Taken::egg);
    }

    void claim(UUID hen, long now) {
        owner = new NestOwner.Hen(hen);
        lastHenSeen = now;
        setChanged();
    }

    /** The owner died or moved on: the nest is free to claim. */
    void release() {
        owner = new NestOwner.Unowned();
        setChanged();
    }

    private void setClutch(Clutch next) {
        clutch = next;
        setChanged();
        BlockState state = getBlockState();
        BlockState shown = state.setValue(NestBlock.EGGS, next.size()).setValue(NestBlock.FERTILE, next.hasFertile());
        if (!shown.equals(state)) {
            NestWorld.level(this).ifPresent(level -> level.setBlock(worldPosition, shown, Block.UPDATE_CLIENTS));
        }
    }

    /** The adult chickens standing in the nest right now. */
    List<Chicken> sitters(Level level) {
        return level.getEntitiesOfClass(Chicken.class, new net.minecraft.world.phys.AABB(worldPosition),
                hen -> hen.isAlive() && !hen.isBaby() && hen.blockPosition().equals(worldPosition));
    }

    void serverTick(ServerLevel level) {
        if ((level.getGameTime() + worldPosition.hashCode()) % CHECK_INTERVAL != 0) {
            return;
        }
        check(level, CHECK_INTERVAL);
    }

    /** One check, as if {@code elapsed} ticks passed. Public so tests can run one now. */
    public void check(ServerLevel level, long elapsed) {
        NestSettings settings = NestConfig.settings();
        long now = level.getGameTime();
        NestIndex.of(level).add(worldPosition);
        List<Chicken> sitters = sitters(level);
        boolean ownerAround = owner instanceof NestOwner.Hen hen && NestWorld.entity(level, hen.id()).isPresent();
        if (!sitters.isEmpty() || ownerAround) {
            lastHenSeen = now;
            setChanged();
        }
        if (!sitters.isEmpty() && clutch.hasFertile()) {
            Clutch.Incubation incubation = clutch.incubate(elapsed, settings.incubationTicks());
            setClutch(incubation.rest());
            for (int i = 0; i < incubation.hatched(); i++) {
                hatch(level, sitters.getFirst());
            }
        }
        if (isWild() && NestRules.decays(false, lastHenSeen, now, settings)) {
            fallApart(level);
        }
    }

    private void hatch(ServerLevel level, Chicken mother) {
        NestWorld.newChicken(level).ifPresent(chick -> {
            chick.setAge(AgeableMob.BABY_START_AGE);
            chick.snapTo(worldPosition.getX() + 0.5, worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5, 0.0F, 0.0F);
            mother.getExistingData(HerdAttachments.HERD).ifPresent(herd -> chick.setData(HerdAttachments.HERD, herd));
            level.addFreshEntity(chick);
        });
    }

    /** An abandoned wild nest crumbles into a few sticks (its eggs drop too). */
    private void fallApart(ServerLevel level) {
        level.removeBlock(worldPosition, false);
        Block.popResource(level, worldPosition, new ItemStack(Items.STICK, NestRules.DECAY_STICKS));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        NestWorld.level(this).ifPresent(level -> {
            for (int i = 0; i < clutch.size(); i++) {
                Block.popResource(level, pos, new ItemStack(Items.EGG));
            }
        });
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("eggs", EGG_CODEC.listOf(), clutch.eggs());
        if (owner instanceof NestOwner.Hen hen) {
            output.store("owner", UUIDUtil.CODEC, hen.id());
        }
        output.putLong("last_hen_seen", lastHenSeen);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        clutch = new Clutch(input.read("eggs", EGG_CODEC.listOf()).orElse(List.of()).stream().limit(Clutch.MAX).toList());
        owner = input.read("owner", UUIDUtil.CODEC).<NestOwner>map(NestOwner.Hen::new).orElse(new NestOwner.Unowned());
        lastHenSeen = input.getLongOr("last_hen_seen", 0L);
    }
}

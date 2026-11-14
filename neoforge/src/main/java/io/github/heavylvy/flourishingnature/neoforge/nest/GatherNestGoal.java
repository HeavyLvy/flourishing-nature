package io.github.heavylvy.flourishingnature.neoforge.nest;

import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.nest.NestRules;
import io.github.heavylvy.flourishingnature.core.nest.NestSettings;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.NestWorld;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * A hen with no nest to claim builds one: she picks up sticks, grass, leaf litter and wheat lying
 * around (or pulls up grass and leaf litter growing nearby), carrying each piece in her beak, and
 * once she has enough she lays the nest down near her herd's home, under cover if she can.
 */
final class GatherNestGoal extends Goal {
    static final int PRIORITY = 3;
    private static final int SEARCH_INTERVAL = 40;
    private static final int GIVE_UP_TICKS = 400;
    private static final double PICK_REACH = 1.5;
    /** How far from the herd's home she looks for a nest spot. */
    private static final int SITE_RADIUS = 5;
    /** Nests keep this far apart. */
    private static final int NEST_SPACING = 2;
    private static final int SCAN_HEIGHT = 2;

    /** What she is heading for. */
    private sealed interface Target {
        record Item(ItemEntity item) implements Target {}

        record Plant(BlockPos pos) implements Target {}

        record Site(BlockPos pos) implements Target {}
    }

    private final Chicken hen;
    private Target target = new Target.Site(BlockPos.ZERO);
    private int cooldown;
    private int ticks;

    GatherNestGoal(Chicken hen) {
        this.hen = hen;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private boolean needsNest() {
        return !hen.isBaby() && hen.level() instanceof ServerLevel && HenNests.nest(hen).isEmpty();
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        cooldown = adjustedTickDelay(SEARCH_INTERVAL);
        if (!needsNest()) {
            return false;
        }
        NestSettings settings = NestConfig.settings();
        Optional<Target> next = HenNests.buildPoints(hen).enoughFor(settings) ? site() : material(settings);
        next.ifPresent(found -> target = found);
        return next.isPresent();
    }

    @Override
    public void start() {
        ticks = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < GIVE_UP_TICKS && needsNest() && switch (target) {
            case Target.Item(ItemEntity item) -> item.isAlive();
            case Target.Plant(BlockPos pos) -> pullPoints(hen.level().getBlockState(pos)) > 0;
            case Target.Site site -> true;
        };
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        switch (target) {
            case Target.Item(ItemEntity item) -> {
                if (NestWalk.reach(hen, item.getX(), item.getY(), item.getZ(), PICK_REACH)) {
                    pickUp(item);
                }
            }
            case Target.Plant(BlockPos pos) -> {
                if (NestWalk.reach(hen, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, PICK_REACH)) {
                    pull(pos);
                }
            }
            case Target.Site(BlockPos pos) -> {
                if (NestWalk.reach(hen, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, PICK_REACH)) {
                    build(pos);
                }
            }
        }
    }

    @Override
    public void stop() {
        hen.getNavigation().stop();
        ticks = GIVE_UP_TICKS;
    }

    private void pickUp(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        int points = itemPoints(stack);
        if (points == 0 || !entity.isAlive()) {
            return;
        }
        ItemStack piece = stack.split(1);
        if (stack.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(stack);
        }
        carry(piece, points);
    }

    private void pull(BlockPos pos) {
        Level level = hen.level();
        BlockState state = level.getBlockState(pos);
        int points = pullPoints(state);
        if (points == 0 || !NestWorld.mayEdit(level, hen, pos)) {
            return;
        }
        ItemStack piece = new ItemStack(state.getBlock().asItem());
        level.destroyBlock(pos, false);
        carry(piece, points);
    }

    /** She holds the latest piece in her beak; the points add up out of sight. */
    private void carry(ItemStack piece, int points) {
        HenNests.addBuildPoints(hen, points);
        hen.setItemSlot(EquipmentSlot.MAINHAND, piece);
        hen.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        hen.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 1.6F);
        hen.getNavigation().stop();
        ticks = GIVE_UP_TICKS;
    }

    private void build(BlockPos pos) {
        Level level = hen.level();
        if (!buildable(level, pos)) {
            return;
        }
        level.setBlockAndUpdate(pos, NestRegistry.NEST.get().defaultBlockState().setValue(NestBlock.WILD, true));
        level.getBlockEntity(pos, NestRegistry.NEST_ENTITY.get()).ifPresent(nest -> {
            NestIndex.of(level).add(pos);
            HenNests.assign(hen, nest);
        });
        hen.playSound(SoundEvents.GRASS_PLACE, 0.8F, 1.0F);
        ticks = GIVE_UP_TICKS;
    }

    static int itemPoints(ItemStack stack) {
        if (stack.is(NestRegistry.LARGE_MATERIAL)) {
            return 2;
        }
        return stack.is(NestRegistry.SMALL_MATERIAL) ? 1 : 0;
    }

    private static int pullPoints(BlockState state) {
        if (state.is(NestRegistry.LARGE_MATERIAL_BLOCKS)) {
            return 2;
        }
        return state.is(NestRegistry.SMALL_MATERIAL_BLOCKS) ? 1 : 0;
    }

    /** The nearest dropped item or growing plant she can use. */
    private Optional<Target> material(NestSettings settings) {
        int r = settings.gatherRadius();
        Optional<Target> item = hen.level().getEntitiesOfClass(ItemEntity.class, hen.getBoundingBox().inflate(r, SCAN_HEIGHT, r),
                        entity -> entity.isAlive() && itemPoints(entity.getItem()) > 0)
                .stream()
                .min(Comparator.comparingDouble(hen::distanceToSqr))
                .map(Target.Item::new);
        if (item.isPresent()) {
            return item;
        }
        Level level = hen.level();
        BlockPos origin = hen.blockPosition();
        Optional<BlockPos> nearest = Optional.empty();
        double best = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -SCAN_HEIGHT, -r), origin.offset(r, SCAN_HEIGHT, r))) {
            double d = pos.distSqr(origin);
            if (d < best && pullPoints(level.getBlockState(pos)) > 0 && NestWorld.mayEdit(level, hen, pos)) {
                best = d;
                nearest = Optional.of(lowerHalf(level, pos.immutable()));
            }
        }
        return nearest.map(Target.Plant::new);
    }

    /** Tall grass is pulled from its bottom half, which takes the top with it. */
    private static BlockPos lowerHalf(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(DoublePlantBlock.HALF) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    /** The best free spot near her herd's home (or near her, if she has no herd yet). */
    private Optional<Target> site() {
        Level level = hen.level();
        Position home = home();
        BlockPos center = BlockPos.containing(home.x(), home.y(), home.z());
        List<NestRules.Site> sites = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-SITE_RADIUS, -3, -SITE_RADIUS), center.offset(SITE_RADIUS, 3, SITE_RADIUS))) {
            if (buildable(level, pos)) {
                sites.add(new NestRules.Site(new Position(pos.getX(), pos.getY(), pos.getZ()), !level.canSeeSky(pos)));
            }
        }
        return NestRules.bestSite(sites, home)
                .map(site -> new Target.Site(BlockPos.containing(site.at().x(), site.at().y(), site.at().z())));
    }

    private Position home() {
        Optional<Position> anchor = hen.level() instanceof ServerLevel server
                ? hen.getExistingData(HerdAttachments.HERD).flatMap(herd -> HerdManager.homeOf(server, herd)).map(HerdHome::anchor)
                : Optional.empty();
        return anchor.orElseGet(() -> EntityFacts.position(hen));
    }

    /** Solid ground below, room to sit, dry, no nest close by, and not inside a lit base. */
    private boolean buildable(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState here = level.getBlockState(pos);
        return (here.isAir() || (here.canBeReplaced() && !here.is(NestRegistry.NEST.get())))
                && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && NestWorld.mayEdit(level, hen, pos)
                && noNestNear(level, pos);
    }

    private static boolean noNestNear(Level level, BlockPos pos) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-NEST_SPACING, -1, -NEST_SPACING), pos.offset(NEST_SPACING, 1, NEST_SPACING))) {
            if (level.getBlockState(near).is(NestRegistry.NEST.get())) {
                return false;
            }
        }
        return true;
    }
}

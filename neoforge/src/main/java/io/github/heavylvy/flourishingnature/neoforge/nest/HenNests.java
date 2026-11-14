package io.github.heavylvy.flourishingnature.neoforge.nest;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.nest.BuildPoints;
import io.github.heavylvy.flourishingnature.core.nest.Clutch;
import io.github.heavylvy.flourishingnature.core.nest.LayProgress;
import io.github.heavylvy.flourishingnature.core.nest.NestEgg;
import io.github.heavylvy.flourishingnature.core.nest.NestOwner;
import io.github.heavylvy.flourishingnature.core.nest.NestSettings;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingAttachments;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.ItemStack;

/** A hen's side of nesting: which nest is hers, claiming one, laying, and her egg clock. */
public final class HenNests {
    private HenNests() {}

    /** How often the hen's nest bookkeeping runs, in ticks. */
    static final int UPDATE_INTERVAL = 20;

    /** Her nest, if she has one and it is still there and still hers. */
    public static Optional<NestBlockEntity> nest(Chicken hen) {
        return hen.getExistingData(NestRegistry.HEN_NEST)
                .flatMap(pos -> hen.level().getBlockEntity(pos, NestRegistry.NEST_ENTITY.get()))
                .filter(nest -> nest.owner().isOwnedBy(hen.getUUID()));
    }

    /** Makes {@code nest} hers. Public so tests can hand a hen a nest. */
    public static void assign(Chicken hen, NestBlockEntity nest) {
        nest(hen).filter(old -> old != nest).ifPresent(NestBlockEntity::release);
        nest.claim(hen.getUUID(), hen.level().getGameTime());
        hen.setData(NestRegistry.HEN_NEST, nest.getBlockPos());
        hen.removeData(NestRegistry.BUILD_POINTS);
        dropBeakMaterial(hen);
    }

    /** Fertile eggs she carries from breeding, waiting to be laid. */
    public static int fertileEggs(Chicken hen) {
        return hen.getExistingData(NestRegistry.FERTILE_EGGS).orElse(0);
    }

    static void addFertileEgg(Chicken hen) {
        hen.setData(NestRegistry.FERTILE_EGGS, Math.min(Clutch.MAX, fertileEggs(hen) + 1));
    }

    public static BuildPoints buildPoints(Chicken hen) {
        return new BuildPoints(hen.getExistingData(NestRegistry.BUILD_POINTS).orElse(0));
    }

    static void addBuildPoints(Chicken hen, int points) {
        hen.setData(NestRegistry.BUILD_POINTS, buildPoints(hen).plus(points).value());
    }

    public static LayProgress layProgress(Chicken hen) {
        return new LayProgress(hen.getExistingData(NestRegistry.LAY_PROGRESS).orElse(0.0));
    }

    /** Test hook: her next egg is due now, skipping hours of waiting. */
    public static void makeEggDue(Chicken hen) {
        hen.setData(NestRegistry.LAY_PROGRESS, (double) NestConfig.settings().layIntervalTicks());
    }

    /** She has an egg to lay: a fertile one from breeding, or an ordinary one that's due. */
    static boolean hasEggToLay(Chicken hen) {
        return fertileEggs(hen) > 0 || layProgress(hen).ready(NestConfig.settings());
    }

    /** Lays her waiting egg into her nest. False if she has no nest, or it is full. */
    static boolean layInto(Chicken hen, NestBlockEntity nest) {
        boolean fertile = fertileEggs(hen) > 0;
        if (!hasEggToLay(hen) || !nest.addEgg(fertile ? NestEgg.Fertile.fresh() : new NestEgg.Plain())) {
            return false;
        }
        if (fertile) {
            hen.setData(NestRegistry.FERTILE_EGGS, fertileEggs(hen) - 1);
        } else {
            hen.removeData(NestRegistry.LAY_PROGRESS);
        }
        hen.playSound(SoundEvents.CHICKEN_EGG, 1.0F, 0.9F + hen.getRandom().nextFloat() * 0.2F);
        return true;
    }

    /** The owner died: her nest is free again. */
    static void onDeath(Chicken hen) {
        nest(hen).ifPresent(NestBlockEntity::release);
    }

    /**
     * Once a second: forget a nest that is gone, claim one if she has none, and move her egg clock
     * on at her current wellbeing and hunger.
     */
    static void update(Chicken hen) {
        if (hen.isBaby()) {
            return;
        }
        NestSettings settings = NestConfig.settings();
        if (hen.getExistingData(NestRegistry.HEN_NEST).isPresent() && nest(hen).isEmpty()) {
            hen.removeData(NestRegistry.HEN_NEST);
        }
        if (nest(hen).isEmpty()) {
            claimNearest(hen, settings);
        }
        hen.setData(NestRegistry.LAY_PROGRESS,
                layProgress(hen).advance(UPDATE_INTERVAL, mood(hen), hunger(hen), settings).ticks());
    }

    private static void claimNearest(Chicken hen, NestSettings settings) {
        BlockPos at = hen.blockPosition();
        NestIndex.of(hen.level()).near(hen.level(), at, settings.claimRadius()).stream()
                .filter(nest -> nest.owner() instanceof NestOwner.Unowned)
                .min(Comparator.comparingDouble(nest -> nest.getBlockPos().distSqr(at)))
                .ifPresent(nest -> assign(hen, nest));
    }

    /** Whatever she was carrying for a nest she no longer needs to build. */
    static void dropBeakMaterial(Chicken hen) {
        ItemStack held = hen.getMainHandItem();
        if (held.is(NestRegistry.SMALL_MATERIAL) || held.is(NestRegistry.LARGE_MATERIAL)) {
            hen.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    private static Mood mood(Chicken hen) {
        return hen.getExistingData(WellbeingAttachments.STATE).map(state -> state.wellbeing().mood()).orElse(Mood.CONTENT);
    }

    private static HungerBand hunger(Chicken hen) {
        return FullnessApi.band(hen);
    }
}

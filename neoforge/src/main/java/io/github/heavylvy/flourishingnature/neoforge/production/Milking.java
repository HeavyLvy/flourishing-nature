package io.github.heavylvy.flourishingnature.neoforge.production;

import io.github.heavylvy.flourishingnature.core.production.MilkCharges;
import io.github.heavylvy.flourishingnature.core.production.MilkSettings;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Milk charges. Cows and goats milked with a bucket, and mooshrooms given a bowl, spend one charge;
 * with none left the animal shakes its head and complains instead.
 *
 * <p>The client cannot know an animal's charges, so it never predicts the milking: it only swings
 * the arm, and the server's inventory change arrives a moment later. Bucket milking is done here
 * on the server (vanilla plays that sound only on the milker's client); mooshroom stew is left to
 * vanilla once a charge is paid, so suspicious stew keeps working.
 */
public final class Milking {
    private Milking() {}

    private static final int SHAKE_TICKS = 16;

    private enum Kind {
        BUCKET,
        STEW
    }

    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!ProductionConfig.milk() || !(event.getTarget() instanceof Animal animal) || !AnimalCondition.tracked(animal)) {
            return;
        }
        Optional<Kind> kind = kind(animal, event.getItemStack());
        if (kind.isEmpty() || animal.isBaby()) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (!(animal.level() instanceof ServerLevel level)) {
            return;
        }
        if (!takeCharge(animal)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            refuse(level, animal);
            return;
        }
        switch (kind.get()) {
            case BUCKET -> {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                fillBucket(level, event.getEntity(), event.getHand(), animal);
            }
            case STEW -> {
                // Vanilla's mooshroom bowl branch runs next and plays its own sound for everyone.
            }
        }
    }

    private static Optional<Kind> kind(Animal animal, ItemStack held) {
        if (animal instanceof MushroomCow && held.is(Items.BOWL)) {
            return Optional.of(Kind.STEW);
        }
        if ((animal instanceof AbstractCow || animal instanceof Goat) && held.is(Items.BUCKET)) {
            return Optional.of(Kind.BUCKET);
        }
        return Optional.empty();
    }

    private static void fillBucket(ServerLevel level, Player player, InteractionHand hand, Animal animal) {
        ItemStack held = player.getItemInHand(hand);
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, Items.MILK_BUCKET.getDefaultInstance()));
        level.playSound(null, animal, milkSound(animal), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static SoundEvent milkSound(Animal animal) {
        if (animal instanceof Goat goat) {
            return goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_MILK : SoundEvents.GOAT_MILK;
        }
        return SoundEvents.COW_MILK;
    }

    /** Charges now, refilled at the animal's current product rate. */
    public static MilkCharges charges(Animal animal) {
        return animal.getData(ProductionAttachments.MILK)
                .now(animal.level().getGameTime(), AnimalCondition.of(animal).productRate(), ProductionConfig.milkSettings());
    }

    /** Sets the charges outright: test hook and debug tool. */
    public static void setCharges(Animal animal, MilkCharges charges) {
        animal.setData(ProductionAttachments.MILK, charges);
    }

    private static boolean takeCharge(Animal animal) {
        MilkSettings settings = ProductionConfig.milkSettings();
        Optional<MilkCharges> taken = animal.getData(ProductionAttachments.MILK)
                .take(animal.level().getGameTime(), AnimalCondition.of(animal).productRate(), settings);
        taken.ifPresent(charges -> animal.setData(ProductionAttachments.MILK, charges));
        return taken.isPresent();
    }

    /** A complaining call, a puff of smoke and a head shake over the next second. */
    private static void refuse(ServerLevel level, Animal animal) {
        animal.playAmbientSound();
        level.playSound(null, animal, SoundEvents.DISPENSER_FAIL, SoundSource.NEUTRAL, 0.6F, 1.4F);
        level.sendParticles(ParticleTypes.SMOKE, animal.getX(), animal.getEyeY(), animal.getZ(), 4, 0.2, 0.1, 0.2, 0.01);
        animal.setData(ProductionAttachments.SHAKE_UNTIL, level.getGameTime() + SHAKE_TICKS);
    }

    /** Turns the head left and right while a refusal shake lasts. */
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal animal) || animal.level().isClientSide()) {
            return;
        }
        animal.getExistingData(ProductionAttachments.SHAKE_UNTIL).ifPresent(until -> {
            long left = until - animal.level().getGameTime();
            if (left <= 0) {
                animal.removeData(ProductionAttachments.SHAKE_UNTIL);
                return;
            }
            float swing = (float) Math.sin(left * Math.PI / 4.0) * 35.0F;
            animal.setYHeadRot(animal.yBodyRot + swing);
        });
    }
}

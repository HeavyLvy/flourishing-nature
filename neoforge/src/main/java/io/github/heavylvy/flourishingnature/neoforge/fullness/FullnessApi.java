package io.github.heavylvy.flourishingnature.neoforge.fullness;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.fullness.FullnessMeter;
import io.github.heavylvy.flourishingnature.core.fullness.FullnessRates;
import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import java.util.function.Supplier;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The one way to read or change an animal's fullness. Every animal has a meter; one that never had
 * one starts at {@link FullnessMeter#newborn}. Drains lazily, so reading is cheap and nothing ticks.
 */
public final class FullnessApi {
    private FullnessApi() {}

    private static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModIdentity.MOD_ID);

    private static final MapCodec<FullnessMeter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.doubleRange(0.0, 1.0).fieldOf("fullness").forGetter(m -> m.fullness().value()),
                    Codec.LONG.fieldOf("at").forGetter(FullnessMeter::atTick))
            .apply(i, (f, at) -> new FullnessMeter(new Fullness(f), at)));

    private static final Supplier<AttachmentType<FullnessMeter>> METER = REGISTER.register(
            "fullness",
            () -> AttachmentType.builder(holder -> FullnessMeter.newborn(
                            holder instanceof Animal a ? a.level().getGameTime() : 0L))
                    .serialize(CODEC)
                    .build());

    public static void register(IEventBus modBus, IEventBus gameBus) {
        REGISTER.register(modBus);
        gameBus.addListener(FullnessApi::onHandFeed);
    }

    public static Fullness fullness(Animal animal) {
        return animal.getData(METER).now(animal.level().getGameTime(), drain(animal));
    }

    public static HungerBand band(Animal animal) {
        return fullness(animal).band();
    }

    /** The animal ate {@code amount} (see {@link FullnessRates} for typical values). */
    public static void eat(Animal animal, double amount) {
        long now = animal.level().getGameTime();
        animal.setData(METER, animal.getData(METER).eat(amount, now, drain(animal)));
    }

    /** Sets fullness outright, for a wolf pack sharing a kill, tests and debug tools. */
    public static void set(Animal animal, Fullness value) {
        animal.setData(METER, FullnessMeter.at(value, animal.level().getGameTime()));
    }

    private static double drain(Animal animal) {
        return FullnessRates.drainPerTick(EntityFacts.species(animal));
    }

    /** Hand-feeding an animal its food: a starving animal recovers in a few bites. */
    private static void onHandFeed(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() && event.getTarget() instanceof Animal animal
                && animal.isFood(event.getItemStack())) {
            eat(animal, FullnessRates.HAND_FED);
        }
    }
}

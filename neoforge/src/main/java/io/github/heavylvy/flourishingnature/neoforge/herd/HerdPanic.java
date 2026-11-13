package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.BuiltInProfiles;
import io.github.heavylvy.flourishingnature.core.herd.HerdAlarm;
import io.github.heavylvy.flourishingnature.core.herd.HerdMember;
import io.github.heavylvy.flourishingnature.core.herd.HerdingSettings;
import io.github.heavylvy.flourishingnature.core.herd.Neighbor;
import io.github.heavylvy.flourishingnature.core.herd.PanicAlarm;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.FleeTargets;
import java.util.Optional;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Spreads panic from one frightened animal to its herd mates. */
final class HerdPanic {
    private HerdPanic() {}

    /** How long a herd mate keeps fleeing after the alarm, in ticks. */
    static final int ALARM_TICKS = 100;

    /** Pack hunters fight back instead of scattering, so they never spread or receive alarms. */
    static boolean panics(Animal animal) {
        return !(animal instanceof Wolf);
    }

    /** A herd animal took damage that makes vanilla animals panic: warn its herd. */
    static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof Animal victim)
                || !event.getSource().is(DamageTypeTags.PANIC_CAUSES)) {
            return;
        }
        Optional<Entity> attacker = FleeTargets.attacker(event.getSource());
        raise(victim, attacker.map(EntityFacts::position).orElse(EntityFacts.position(victim)),
                attacker.flatMap(Hunters::chaseSpeed).orElse(Double.POSITIVE_INFINITY), HerdingSettings.DEFAULT);
    }

    /** {@code frightened} saw or felt a danger at {@code danger}: alarm every herd mate near it. */
    static void raise(Animal frightened, Position danger, double chaserSpeed, HerdingSettings settings) {
        if (!panics(frightened)) {
            return;
        }
        BuiltInProfiles.forSpecies(EntityFacts.species(frightened)).ifPresent(profile -> HerdLookup.preference(frightened)
                .ifPresent(preference -> {
                    HerdMember self = new HerdMember(profile, preference, EntityFacts.position(frightened));
                    PanicAlarm alarm = new PanicAlarm(danger, frightened.level().getGameTime() + ALARM_TICKS, chaserSpeed);
                    for (Animal mate : HerdLookup.herdAnimalsNear(frightened, settings.alarmRadius())) {
                        if (!panics(mate)) {
                            continue;
                        }
                        HerdLookup.preference(mate).ifPresent(matePreference -> {
                            Neighbor listener = new Neighbor(
                                    EntityFacts.species(mate), matePreference, EntityFacts.position(mate));
                            if (HerdAlarm.spreadsTo(self, listener, settings)) {
                                mate.setData(HerdAttachments.ALARM, alarm);
                            }
                        });
                    }
                }));
    }
}

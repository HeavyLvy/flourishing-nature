package io.github.heavylvy.flourishingnature.neoforge.herd;

import com.mojang.brigadier.Command;
import io.github.heavylvy.flourishingnature.core.herd.BuiltInProfiles;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.core.herd.HerdingSettings;
import io.github.heavylvy.flourishingnature.core.herd.PreferenceRoller;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesProfile;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McRandomness;
import java.util.Optional;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;

/** Wires herding onto vanilla animals. */
public final class HerdEvents {
    private HerdEvents() {}

    public static void register(IEventBus modBus, IEventBus gameBus) {
        HerdAttachments.REGISTER.register(modBus);
        gameBus.addListener(HerdEvents::onJoin);
        gameBus.addListener(HerdEvents::onBabySpawn);
        gameBus.addListener(HerdEvents::onRegisterCommands);
    }

    /**
     * Every time a supported animal enters a level (spawned or loaded), give it a preference if it
     * has none yet and add the herding goals. Goals are not saved, so this runs on every load.
     */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Animal animal)) {
            return;
        }
        BuiltInProfiles.forSpecies(EntityFacts.species(animal)).ifPresent(profile -> {
            if (HerdLookup.preference(animal).isEmpty()) {
                HerdPreference rolled = PreferenceRoller.roll(profile.preference(), new McRandomness(animal.getRandom()));
                animal.setData(HerdAttachments.PREFERENCE, rolled);
            }
            HerdBrain brain = new HerdBrain(animal, profile, HerdingSettings.DEFAULT);
            animal.goalSelector.addGoal(SeekHerdGoal.PRIORITY, new SeekHerdGoal(animal, brain));
            animal.goalSelector.addGoal(HerdCohesionGoal.PRIORITY, new HerdCohesionGoal(animal, brain));
        });
    }

    /** Babies inherit a preference centred on their parents', so herds keep their character. */
    private static void onBabySpawn(BabyEntitySpawnEvent event) {
        Optional<AgeableMob> child = EntityFacts.child(event);
        child.ifPresent(baby -> {
            Optional<SpeciesProfile> profile = BuiltInProfiles.forSpecies(EntityFacts.species(baby));
            Optional<HerdPreference> a = parentPreference(event.getParentA());
            Optional<HerdPreference> b = parentPreference(event.getParentB());
            profile.ifPresent(p -> a.ifPresent(pa -> b.ifPresent(pb -> baby.setData(
                    HerdAttachments.PREFERENCE,
                    PreferenceRoller.inherit(p.preference(), pa, pb, new McRandomness(baby.getRandom()))))));
        });
    }

    private static Optional<HerdPreference> parentPreference(Mob parent) {
        return parent instanceof Animal animal ? HerdLookup.preference(animal) : Optional.empty();
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("flourishing")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("herd_debug").executes(context -> {
                    boolean on = HerdDebug.toggle();
                    context.getSource().sendSuccess(
                            () -> Component.literal("Herd debug name tags " + (on ? "on" : "off")), true);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}

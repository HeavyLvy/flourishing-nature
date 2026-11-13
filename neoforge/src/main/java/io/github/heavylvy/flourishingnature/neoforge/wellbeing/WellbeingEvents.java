package io.github.heavylvy.flourishingnature.neoforge.wellbeing;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.github.heavylvy.flourishingnature.core.herbivore.Diets;
import io.github.heavylvy.flourishingnature.core.herd.BuiltInProfiles;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import io.github.heavylvy.flourishingnature.core.wellbeing.Need;
import io.github.heavylvy.flourishingnature.core.wellbeing.Surroundings;
import io.github.heavylvy.flourishingnature.core.wellbeing.WellbeingProfile;
import io.github.heavylvy.flourishingnature.core.wellbeing.WellbeingSettings;
import io.github.heavylvy.flourishingnature.core.wellbeing.WellbeingState;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.herbivore.Forage;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdSocial;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Runs wellbeing checks on herd animals and records hand-feeding. */
public final class WellbeingEvents {
    private WellbeingEvents() {}

    private static final WellbeingSettings SETTINGS = WellbeingSettings.DEFAULT;
    private static final WellbeingProfile PROFILE = WellbeingProfile.grazer();
    private static final double INSPECT_RANGE = 6.0;

    public static void register(IEventBus modBus, IEventBus gameBus) {
        WellbeingAttachments.REGISTER.register(modBus);
        gameBus.addListener(WellbeingEvents::onTick);
        gameBus.addListener(WellbeingEvents::onInteract);
        gameBus.addListener(WellbeingEvents::onRegisterCommands);
    }

    /** Every {@link WellbeingSettings#checkInterval()} ticks, staggered by entity id. */
    private static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal animal) || animal.level().isClientSide()) {
            return;
        }
        long now = animal.level().getGameTime();
        if (Math.floorMod(now + animal.getId(), SETTINGS.checkInterval()) != 0) {
            return;
        }
        check(animal, now);
    }

    /** Runs one wellbeing check now. Public so gametests can force one. */
    public static void check(Animal animal, long now) {
        SpeciesId species = EntityFacts.species(animal);
        if (BuiltInProfiles.forSpecies(species).isEmpty()) {
            return;
        }
        HerdSocial.preference(animal).ifPresent(preference -> {
            Surroundings seen = sense(animal, species, preference, now);
            WellbeingState next = animal.getExistingData(WellbeingAttachments.STATE)
                    .map(state -> state.next(seen, PROFILE, SETTINGS))
                    .orElseGet(() -> WellbeingState.first(seen, PROFILE, SETTINGS));
            animal.setData(WellbeingAttachments.STATE, next);
        });
    }

    private static Surroundings sense(Animal animal, SpeciesId species, HerdPreference preference, long now) {
        long sinceMeal = animal.getExistingData(WellbeingAttachments.LAST_MEAL)
                .map(tick -> Math.max(0L, now - tick))
                .orElse(Long.MAX_VALUE / 2);
        return new Surroundings(
                WorldSenses.biomeFit(animal, species),
                WorldSenses.skyNearby(animal),
                WorldSenses.precipitating(animal),
                WorldSenses.roofNearby(animal),
                FullnessApi.fullness(animal),
                Diets.forSpecies(species).isPresent(),
                Diets.forSpecies(species)
                        .map(diet -> Forage.ediblePlants(animal.level(), animal.position(), diet, WorldSenses.FOOD_REACH))
                        .orElse(0),
                sinceMeal,
                WorldSenses.mobsCloseBy(animal),
                HerdSocial.socialCount(animal).orElse(0.0),
                preference);
    }

    /** Feeding an animal its food by hand fills its food need. */
    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof Animal animal)) {
            return;
        }
        if (animal.isFood(event.getItemStack())) {
            animal.setData(WellbeingAttachments.LAST_MEAL, animal.level().getGameTime());
        }
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("flourishing")
                .then(Commands.literal("inspect").executes(WellbeingEvents::inspect)));
    }

    /** Prints the nearest animal's wellbeing breakdown: the teaching tool until the crook exists. */
    private static int inspect(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<Animal> nearest = source.getLevel()
                .getEntitiesOfClass(Animal.class, AABB.ofSize(source.getPosition(), INSPECT_RANGE * 2, INSPECT_RANGE * 2, INSPECT_RANGE * 2),
                        a -> a.getExistingData(WellbeingAttachments.STATE).isPresent())
                .stream()
                .min(Comparator.comparingDouble(a -> a.distanceToSqr(source.getPosition())));
        if (nearest.isEmpty()) {
            source.sendFailure(Component.literal("No animal with wellbeing within " + (int) INSPECT_RANGE + " blocks"));
            return 0;
        }
        Animal animal = nearest.get();
        animal.getExistingData(WellbeingAttachments.STATE).ifPresent(state -> {
            StringBuilder text = new StringBuilder(String.format(Locale.ROOT, "%s: wellbeing %.0f (%s)",
                    animal.getName().getString(), state.wellbeing().value(),
                    state.wellbeing().mood().name().toLowerCase(Locale.ROOT)));
            for (Need need : Need.values()) {
                text.append(String.format(Locale.ROOT, "\n  %s %.2f", need.name().toLowerCase(Locale.ROOT), state.needs().of(need).value()));
            }
            String message = text.toString();
            source.sendSuccess(() -> Component.literal(message), false);
        });
        return Command.SINGLE_SUCCESS;
    }
}

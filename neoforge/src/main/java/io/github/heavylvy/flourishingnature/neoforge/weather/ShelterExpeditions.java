package io.github.heavylvy.flourishingnature.neoforge.weather;

import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import io.github.heavylvy.flourishingnature.core.random.Randomness;
import io.github.heavylvy.flourishingnature.core.weather.Expedition;
import io.github.heavylvy.flourishingnature.core.weather.WeatherTolerance;
import io.github.heavylvy.flourishingnature.neoforge.boundary.ReachableTerrain;
import io.github.heavylvy.flourishingnature.neoforge.boundary.Weather;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * Moves the home of a herd that is caught in the rain with no cover within reach, a leg every
 * {@link #LEG_INTERVAL} ticks, until cover is close enough for members to shelter on their own.
 * One per level, owned by the herd manager. Not saved.
 */
public final class ShelterExpeditions {
    static final int LEG_INTERVAL = 200;
    private static final int COVER_REACH = 14;
    private static final int SCOUT_REACH = 8;

    private final Map<HerdId, Expedition> active = new HashMap<>();
    private final Map<HerdId, Long> nextLeg = new HashMap<>();

    /**
     * Returns the herd's home, moved one leg if it needs to find cover. {@code species} is the
     * herd's lead species; {@code member} is any herd member, used to check legs are walkable.
     */
    public HerdHome update(ServerLevel level, HerdId herd, SpeciesId species, Mob member, HerdHome home, Randomness random) {
        BlockPos anchor = BlockPos.containing(home.anchor().x(), home.anchor().y(), home.anchor().z());
        boolean needsCover = WeatherTolerance.seeksShelter(species)
                && Weather.precipitating(level, anchor)
                && Weather.nearestShelter(level, anchor, COVER_REACH).isEmpty();
        if (!needsCover) {
            active.remove(herd);
            nextLeg.remove(herd);
            return home;
        }
        long now = level.getGameTime();
        if (now < nextLeg.getOrDefault(herd, 0L)) {
            return home;
        }
        nextLeg.put(herd, now + LEG_INTERVAL);
        Expedition expedition = active.computeIfAbsent(herd, id -> Expedition.start(random));
        Optional<Expedition.Leg> leg = expedition.step(
                home.anchor(),
                scout -> Weather.nearestShelter(level, BlockPos.containing(scout.x(), scout.y(), scout.z()), SCOUT_REACH)
                        .map(p -> new Position(p.getX() + 0.5, p.getY(), p.getZ() + 0.5)),
                new ReachableTerrain(member));
        return leg.map(l -> {
                    active.put(herd, l.next());
                    return new HerdHome(l.anchor(), home.roamer(), home.nextMove());
                })
                .orElse(home);
    }

    public void forget(HerdId herd) {
        active.remove(herd);
        nextLeg.remove(herd);
    }
}

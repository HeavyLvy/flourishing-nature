package io.github.heavylvy.flourishingnature.core.herd;

import io.github.heavylvy.flourishingnature.core.random.Randomness;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Herd-level decisions. Herds are real groups with an id and a home, like households: the
 * household decides who moves in, two small households next door combine, and when a house gets
 * too full the unhappy members move out together rather than one at a time.
 *
 * <p>One call looks at every loaded herd animal and returns everyone's herd and every herd's home:
 *
 * <ol>
 *   <li>An animal too far from its herd's home has lost the herd and leaves it.
 *   <li>Nearby herds merge when nobody in the combined herd would feel crowded. Herds well under
 *       their wanted size look further for a partner.
 *   <li>A member feels crowded when the herd is bigger than {@code crowdFactor} times its own wish.
 *       Two or more crowded members split off together and set up a new home nearby. A single
 *       crowded member moves to a roomier compatible herd if there is one, and otherwise puts up
 *       with it. Nobody storms off alone, so there is no leave-get-lonely-come-back loop.
 *   <li>Animals without a herd join the best nearby herd that has room, or found their own.
 *   <li>Homes stay on dry, open ground. Roaming herds move their home every minute or two;
 *       settled herds shuffle a few blocks now and then.
 * </ol>
 */
public final class HerdPlanner {
    private HerdPlanner() {}

    private static final int PASTURE_ATTEMPTS = 8;

    public static HerdPlan plan(
            List<HerdAnimal> animals,
            Map<HerdId, HerdHome> previousHomes,
            long now,
            Randomness random,
            Terrain terrain,
            Supplier<HerdId> newIds,
            HerdPlanSettings s) {
        Map<HerdId, List<HerdAnimal>> herds = new LinkedHashMap<>();
        List<HerdAnimal> unassigned = new ArrayList<>();
        for (HerdAnimal animal : animals) {
            switch (animal.membership()) {
                case Membership.InHerd in -> herds.computeIfAbsent(in.herd(), id -> new ArrayList<>()).add(animal);
                case Membership.Unassigned none -> unassigned.add(animal);
            }
        }

        Map<HerdId, HerdHome> homes = new HashMap<>();
        for (Map.Entry<HerdId, List<HerdAnimal>> herd : herds.entrySet()) {
            HerdHome home = Optional.ofNullable(previousHomes.get(herd.getKey()))
                    .orElseGet(() -> newHome(herd.getKey(), centroid(herd.getValue()), now, random, terrain, s));
            homes.put(herd.getKey(), home);
        }

        // 1. Lost animals leave their herd.
        for (Map.Entry<HerdId, List<HerdAnimal>> herd : herds.entrySet()) {
            Position anchor = anchorOf(homes, herd.getKey());
            herd.getValue().removeIf(a -> {
                boolean lost = a.position().distanceTo(anchor) > s.leashRadius();
                if (lost) {
                    unassigned.add(a);
                }
                return lost;
            });
        }
        herds.values().removeIf(List::isEmpty);

        // 2. Merge nearby herds.
        boolean merged = true;
        while (merged) {
            merged = false;
            List<HerdId> ids = new ArrayList<>(herds.keySet());
            search:
            for (int i = 0; i < ids.size(); i++) {
                for (int j = i + 1; j < ids.size(); j++) {
                    List<HerdAnimal> a = members(herds, ids.get(i));
                    List<HerdAnimal> b = members(herds, ids.get(j));
                    if (a.isEmpty() || b.isEmpty()) {
                        continue;
                    }
                    if (canMerge(a, anchorOf(homes, ids.get(i)), b, anchorOf(homes, ids.get(j)), s)) {
                        HerdId keep = a.size() >= b.size() ? ids.get(i) : ids.get(j);
                        HerdId drop = keep.equals(ids.get(i)) ? ids.get(j) : ids.get(i);
                        members(herds, keep).addAll(members(herds, drop));
                        herds.remove(drop);
                        merged = true;
                        break search;
                    }
                }
            }
        }

        // 3. Crowding: split off together, or move to a roomier herd.
        for (HerdId id : new ArrayList<>(herds.keySet())) {
            List<HerdAnimal> members = members(herds, id);
            if (members.isEmpty()) {
                continue;
            }
            List<HerdAnimal> crowded = crowdedMembers(members, members.size(), s);
            if (crowded.size() >= 2) {
                crowded.sort(Comparator.comparingInt(a -> a.preference().value()));
                members.removeAll(crowded);
                HerdId splinter = newIds.get();
                Position away = awayPoint(anchorOf(homes, id), s.splitDistance(), random);
                homes.put(splinter, newHome(splinter, away, now, random, terrain, s));
                herds.put(splinter, new ArrayList<>(crowded));
            } else if (crowded.size() == 1) {
                HerdAnimal mover = crowded.getFirst();
                bestHerdFor(mover, herds, homes, s.transferRadius(), other -> !other.equals(id), s).ifPresent(target -> {
                    members.remove(mover);
                    members(herds, target).add(mover);
                });
            }
        }

        // 4. Animals without a herd join one or found one.
        for (HerdAnimal animal : unassigned) {
            Optional<HerdId> join = bestHerdFor(animal, herds, homes, s.joinRadius(), any -> true, s);
            if (join.isPresent()) {
                members(herds, join.get()).add(animal);
            } else {
                HerdId founded = newIds.get();
                homes.put(founded, newHome(founded, animal.position(), now, random, terrain, s));
                herds.put(founded, new ArrayList<>(List.of(animal)));
            }
        }

        // 5. Homes: keep them on good ground and move them on schedule.
        Map<Integer, HerdId> assignments = new HashMap<>();
        Map<HerdId, HerdHome> liveHomes = new HashMap<>();
        for (Map.Entry<HerdId, List<HerdAnimal>> herd : herds.entrySet()) {
            if (herd.getValue().isEmpty()) {
                continue;
            }
            for (HerdAnimal a : herd.getValue()) {
                assignments.put(a.handle(), herd.getKey());
            }
            HerdHome home = Optional.ofNullable(homes.get(herd.getKey()))
                    .orElseGet(() -> newHome(herd.getKey(), centroid(herd.getValue()), now, random, terrain, s));
            liveHomes.put(herd.getKey(), maintain(home, now, random, terrain, s));
        }
        spreadApart(herds, liveHomes, terrain, s);
        return new HerdPlan(assignments, liveHomes);
    }

    /**
     * Herds of species that get along push their homes apart, a block per pass, until they are
     * {@code herdSpacing} apart: like neighbouring families who each want their own patch of
     * pasture. So when a big herd splits, the pieces slowly walk away from each other. Herds still
     * well under the size they want are left alone, so they can find each other and merge.
     */
    private static void spreadApart(
            Map<HerdId, List<HerdAnimal>> herds, Map<HerdId, HerdHome> homes, Terrain terrain, HerdPlanSettings s) {
        List<HerdId> ids = new ArrayList<>(homes.keySet());
        ids.sort(Comparator.comparing(id -> id.value().toString()));
        Map<HerdId, double[]> push = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                HerdId a = ids.get(i);
                HerdId b = ids.get(j);
                Position pa = anchorOf(homes, a);
                Position pb = anchorOf(homes, b);
                double dx = pa.x() - pb.x();
                double dz = pa.z() - pb.z();
                double distance = Math.sqrt(dx * dx + dz * dz);
                List<HerdAnimal> ma = members(herds, a);
                List<HerdAnimal> mb = members(herds, b);
                if (distance >= s.herdSpacing() || wantsMore(ma) || wantsMore(mb) || !compatible(lead(ma), lead(mb), s)) {
                    continue;
                }
                if (distance < 1.0e-6) {
                    dx = 1.0;
                    dz = 0.0;
                    distance = 1.0;
                }
                double ux = dx / distance;
                double uz = dz / distance;
                push.computeIfAbsent(a, id -> new double[2]);
                push.computeIfAbsent(b, id -> new double[2]);
                push.get(a)[0] += ux;
                push.get(a)[1] += uz;
                push.get(b)[0] -= ux;
                push.get(b)[1] -= uz;
            }
        }
        for (Map.Entry<HerdId, double[]> entry : push.entrySet()) {
            double[] v = entry.getValue();
            double length = Math.sqrt(v[0] * v[0] + v[1] * v[1]);
            if (length < 1.0e-6) {
                continue;
            }
            HerdHome home = Optional.ofNullable(homes.get(entry.getKey())).orElseThrow();
            Position from = home.anchor();
            double heading = Math.atan2(v[1], v[0]);
            // Straight away if possible; otherwise sidestep around water, cliffs or walls.
            for (double turn : new double[] {0, Math.PI / 4, -Math.PI / 4, Math.PI / 2, -Math.PI / 2}) {
                Position wanted = new Position(
                        from.x() + Math.cos(heading + turn) * s.spacingStep(),
                        from.y(),
                        from.z() + Math.sin(heading + turn) * s.spacingStep());
                Optional<Position> ground = terrain.pasture(wanted);
                if (ground.isPresent()) {
                    homes.put(entry.getKey(), new HerdHome(
                            new Position(wanted.x(), ground.get().y(), wanted.z()), home.roamer(), home.nextMove()));
                    break;
                }
            }
        }
    }

    /** Members that would feel crowded in a herd of {@code size}. */
    static List<HerdAnimal> crowdedMembers(List<HerdAnimal> members, int size, HerdPlanSettings s) {
        List<HerdAnimal> crowded = new ArrayList<>();
        for (HerdAnimal m : members) {
            if (size > s.crowdFactor() * m.preference().value()) {
                crowded.add(m);
            }
        }
        return crowded;
    }

    private static boolean canMerge(
            List<HerdAnimal> a, Position anchorA, List<HerdAnimal> b, Position anchorB, HerdPlanSettings s) {
        boolean lonely = wantsMore(a) || wantsMore(b);
        double radius = lonely ? s.lonelyMergeRadius() : s.mergeRadius();
        if (anchorA.distanceTo(anchorB) > radius || !compatible(lead(a), lead(b), s)) {
            return false;
        }
        List<HerdAnimal> combined = new ArrayList<>(a);
        combined.addAll(b);
        return crowdedMembers(combined, combined.size(), s).isEmpty();
    }

    /** The best allowed herd with room for {@code animal} within {@code radius}. */
    private static Optional<HerdId> bestHerdFor(
            HerdAnimal animal,
            Map<HerdId, List<HerdAnimal>> herds,
            Map<HerdId, HerdHome> homes,
            double radius,
            Predicate<HerdId> allowed,
            HerdPlanSettings s) {
        Optional<HerdId> best = Optional.empty();
        double bestScore = 0.0;
        for (Map.Entry<HerdId, List<HerdAnimal>> herd : herds.entrySet()) {
            List<HerdAnimal> members = herd.getValue();
            if (!allowed.test(herd.getKey()) || members.isEmpty()) {
                continue;
            }
            double distance = animal.position().distanceTo(anchorOf(homes, herd.getKey()));
            if (distance > radius || !compatible(animal.profile(), lead(members), s)) {
                continue;
            }
            List<HerdAnimal> after = new ArrayList<>(members);
            after.add(animal);
            if (!crowdedMembers(after, after.size(), s).isEmpty()) {
                continue;
            }
            double similarity = 1.0 - Math.abs(animal.preference().value() - averageWish(members)) / HerdPreference.MAX;
            double score = animal.profile().affinityTo(lead(members).species()).value() * similarity / (1.0 + distance);
            if (score > bestScore) {
                bestScore = score;
                best = Optional.of(herd.getKey());
            }
        }
        return best;
    }

    private static boolean compatible(SpeciesProfile a, SpeciesProfile b, HerdPlanSettings s) {
        return a.affinityTo(b.species()).value() >= s.minAffinity() && b.affinityTo(a.species()).value() >= s.minAffinity();
    }

    /** The species most members belong to (ties: the first seen). */
    static SpeciesProfile lead(List<HerdAnimal> members) {
        Map<SpeciesId, Integer> counts = new LinkedHashMap<>();
        Map<SpeciesId, SpeciesProfile> profiles = new HashMap<>();
        for (HerdAnimal m : members) {
            counts.merge(m.species(), 1, Integer::sum);
            profiles.putIfAbsent(m.species(), m.profile());
        }
        SpeciesId top = members.getFirst().species();
        int topCount = 0;
        for (Map.Entry<SpeciesId, Integer> e : counts.entrySet()) {
            if (e.getValue() > topCount) {
                top = e.getKey();
                topCount = e.getValue();
            }
        }
        return profiles.getOrDefault(top, members.getFirst().profile());
    }

    static double averageWish(List<HerdAnimal> members) {
        double sum = 0;
        for (HerdAnimal m : members) {
            sum += m.preference().value();
        }
        return sum / members.size();
    }

    private static boolean wantsMore(List<HerdAnimal> members) {
        return members.size() < averageWish(members) - 0.5;
    }

    private static Position centroid(List<HerdAnimal> members) {
        List<Position> points = new ArrayList<>();
        for (HerdAnimal m : members) {
            points.add(m.position());
        }
        return Position.centroid(points);
    }

    private static List<HerdAnimal> members(Map<HerdId, List<HerdAnimal>> herds, HerdId id) {
        return Optional.ofNullable(herds.get(id))
                .orElseThrow(() -> new IllegalStateException("herd " + id + " is not in this plan"));
    }

    private static Position anchorOf(Map<HerdId, HerdHome> homes, HerdId id) {
        return Optional.ofNullable(homes.get(id))
                .orElseThrow(() -> new IllegalStateException("herd " + id + " has no home"))
                .anchor();
    }

    private static HerdHome newHome(HerdId id, Position near, long now, Randomness random, Terrain terrain, HerdPlanSettings s) {
        boolean roamer = id.isRoamer(s.roamChance());
        Position anchor = findPasture(near, 6.0, random, terrain).orElse(near);
        return new HerdHome(anchor, roamer, now + interval(roamer, random, s));
    }

    private static HerdHome maintain(HerdHome home, long now, Randomness random, Terrain terrain, HerdPlanSettings s) {
        if (now >= home.nextMove()) {
            double distance = home.roamer() ? s.roamDistance() : s.driftDistance();
            for (int attempt = 0; attempt < PASTURE_ATTEMPTS; attempt++) {
                Optional<Position> next = terrain.pasture(awayPoint(home.anchor(), distance, random));
                if (next.isPresent()) {
                    return new HerdHome(next.get(), home.roamer(), now + interval(home.roamer(), random, s));
                }
            }
            return new HerdHome(home.anchor(), home.roamer(), now + interval(home.roamer(), random, s));
        }
        if (terrain.pasture(home.anchor()).isEmpty()) {
            return findPasture(home.anchor(), 8.0, random, terrain)
                    .map(better -> new HerdHome(better, home.roamer(), home.nextMove()))
                    .orElse(home);
        }
        return home;
    }

    /** {@code near} itself if it's good pasture, else a few random spots around it. */
    private static Optional<Position> findPasture(Position near, double radius, Randomness random, Terrain terrain) {
        Optional<Position> here = terrain.pasture(near);
        if (here.isPresent()) {
            return here;
        }
        for (int attempt = 0; attempt < PASTURE_ATTEMPTS; attempt++) {
            Optional<Position> spot = terrain.pasture(awayPoint(near, radius * (0.5 + random.nextDouble() / 2), random));
            if (spot.isPresent()) {
                return spot;
            }
        }
        return Optional.empty();
    }

    private static Position awayPoint(Position from, double distance, Randomness random) {
        double angle = random.nextDouble() * 2 * Math.PI;
        return new Position(from.x() + Math.cos(angle) * distance, from.y(), from.z() + Math.sin(angle) * distance);
    }

    private static long interval(boolean roamer, Randomness random, HerdPlanSettings s) {
        long base = roamer ? s.roamInterval() : s.driftInterval();
        return base + (long) (random.nextDouble() * base);
    }
}

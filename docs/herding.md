# Herding (System 1)

Implements the social preference and herding part of the design doc
("Flourishing Nature: Mob Social & Wellbeing Design").

## What it does

- Every cow, mooshroom, sheep, pig, chicken, horse and donkey gets a **herd preference** (how
  big a herd it wants, counting itself) the first time it enters a world. It's saved on the
  animal as the `flourishing_nature:herd_preference` data attachment. Existing animals in old
  worlds get one when their chunk loads.
- Babies inherit: their roll is centred on the average of their parents' preferences.
- Every 2 seconds (staggered per animal) an animal scans 16 blocks around it. Neighbors within
  8 blocks count toward its herd, weighted by species affinity and by how similar their
  preferences are. Then:
  - too few mates → walk to the best group in sight (`SeekHerdGoal`, priority 4);
  - more than 1.5x its preference → drift away from the crowd;
  - otherwise → light flocking keeps 2 to 4 blocks to the nearest mate (`HerdCohesionGoal`,
    priority 5, sharing a slot with vanilla strolling).
- Panic, breeding, temptation and following parents (priorities 1 to 4) always win. Babies,
  leashed animals and ridden animals don't herd.
- Goats are not included yet: they use vanilla's Brain AI, not goals.

## Where the code is

| Part | Location |
|---|---|
| Rules (pure Java, unit tested) | `core/.../core/herd/` |
| Species numbers | `core/.../core/herd/BuiltInProfiles.java` (datapack JSON later) |
| Attachment, goals, events | `neoforge/.../neoforge/herd/` |
| Minecraft null boundary | `neoforge/.../neoforge/boundary/EntityFacts.java` |
| In-game tests | `neoforge/.../neoforge/gametest/HerdGameTests.java` |

## Seeing it in game

Run `/flourishing herd_debug` (operator only) to toggle name tags like
`[herd] wants 5 · has 3.2 · seeking`. Animals a player has named are left alone.

## Testing

- `./gradlew :core:test` for the rules.
- `./gradlew :neoforge:runGameTestServer` runs the in-game tests in a walled 28x28 grass pen
  (`neoforge/src/main/resources/data/flourishing_nature/structure/herd_pen.nbt`). With the
  herding goals removed, `scattered_cows_gather` fails (cows stay about 23 blocks apart), so the
  test really measures herding.

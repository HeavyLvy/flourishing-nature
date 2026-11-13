# Herding (System 1)

Implements the social preference and herding part of the design doc
("Flourishing Nature: Mob Social & Wellbeing Design").

## What it does

- Every cow, mooshroom, sheep, pig, chicken, horse, donkey and wild wolf gets a **herd
  preference** (how big a herd it wants, counting itself) the first time it enters a world.
  The roll is sharply peaked: 80% get exactly the species average, 10% one more or one less,
  3% are true loners or party animals, and the rest land elsewhere in the typical range. It's saved on the
  animal as the `flourishing_nature:herd_preference` data attachment. Existing animals in old
  worlds get one when their chunk loads.
- Babies inherit: their roll is centred on the rounded average of their parents' preferences.
- **Wolf packs**: wolves want packs of about 4 (3 to 6). Their pack goals sit below hunting
  (priorities 7 and 8), so a wolf still chases prey, and they never herd-panic. Tamed wolves
  and other pets don't herd at all.
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

## Panic and predators

- **Herd panic.** When a herd animal takes damage that makes vanilla animals panic (the
  `minecraft:panic_causes` damage tag: attacks, arrows, explosions, fire...), every friendly
  herd mate within 10 blocks gets a 5 second alarm (`HerdPanicGoal`, priority 1). Each one runs
  8 to 12 blocks away from the attacker with a random swerve of up to 60 degrees, so the herd
  scatters instead of moving as one block. Herding and cohesion pause during the alarm, then
  the normal seek goal regroups them, possibly in a new spot or as two groups. Unrelated
  species (a pig next to cows) stay calm.
- **Prey flee predators.** Vanilla wolves hunt sheep, rabbits and foxes; foxes hunt chickens and
  rabbits; ocelots hunt chickens; stray cats hunt rabbits. Vanilla already makes rabbits flee
  wolves and foxes flee wolves and polar bears. This mod fills the gaps (`Predators`):
  sheep flee wild wolves, chickens flee foxes and ocelots, rabbits flee foxes and stray cats,
  starting at 10 blocks. Tamed wolves and cats are ignored. An animal that starts fleeing also
  alarms its herd.
- **Fair hunts.** Prey fleeing a hunter run at 90% of the hunter's chase speed (`ChaseSpeed`),
  so the hunter slowly gains and can catch them. Wolves chase at 0.30 blocks/tick, foxes at
  0.36, ocelots and cats at 0.30. Without an animal hunter (a player, an explosion), herd
  panic runs at full speed.

## Where the code is

| Part | Location |
|---|---|
| Rules (pure Java, unit tested) | `core/.../core/herd/` |
| Species numbers | `core/.../core/herd/BuiltInProfiles.java` (datapack JSON later) |
| Attachment, goals, events | `neoforge/.../neoforge/herd/` |
| Panic and predators | `core/.../herd/HerdAlarm.java`, `FleeRoute.java`; `neoforge/.../herd/HerdPanic*.java`, `Predators.java` |
| Minecraft null boundary | `neoforge/.../neoforge/boundary/` |
| In-game tests | `neoforge/.../neoforge/gametest/HerdGameTests.java` |

## Seeing it in game

Run `/flourishing herd_debug` (operator only) to toggle name tags like
`[herd] wants 5 · has 3.2 · seeking`. Animals a player has named are left alone.

## Testing

- `./gradlew :core:test` for the rules.
- `./gradlew :neoforge:runGameTestServer` runs the in-game tests in a walled 28x28 grass pen
  built by `TestPens` (the `herd_pen.nbt` structure only sets the size, because the framework
  placed our structure's blocks as air). With the
  herding goals removed, `scattered_cows_gather` fails (cows stay about 23 blocks apart), and
  with panic and predator goals removed, `herd_panics_together`, `sheep_flee_wolf` and
  `chicken_flees_fox` fail, so the tests really measure the features.

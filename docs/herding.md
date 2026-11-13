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
- Panic, breeding, temptation and following parents (priorities 1 to 4) always win. Babies,
  leashed animals and ridden animals don't herd.
- Goats are not included yet: they use vanilla's Brain AI, not goals.

## Herds are real groups

Herds used to be emergent (each animal steering alone), which caused a loop: a crowded animal
left, got lonely, and walked straight back. Now a herd is a thing with an id and a home, like a
household. The household decides who moves in, neighbours combine, and members who want out
leave together.

- Each herd animal carries a `flourishing_nature:herd` attachment with its herd's id. Every 2
  seconds a per-level `HerdManager` runs `HerdPlanner` (pure Java, in `core`) over every
  loaded herd animal:
  1. **Lost:** an animal more than 40 blocks from its herd's home leaves it.
  2. **Merge:** two herds whose homes are within 10 blocks (20 if either is well under the size
     it wants) merge if their species get along and nobody in the combined herd would feel
     crowded.
  3. **Crowding:** a member feels crowded when the herd is bigger than 1.5 times its own wish.
     Two or more crowded members split off together and set up a home about 14 blocks away.
     A single crowded member moves to a roomier compatible herd within 24 blocks if there is
     one, and otherwise puts up with it. Nobody storms off alone.
  4. **Join or found:** an animal without a herd joins the best herd within 16 blocks that has
     room for it (no one, including itself, would feel crowded), scored by affinity, similar
     wishes and distance. Otherwise it founds its own herd. Loners stay alone.
  5. **Homes:** a home is always dry, solid ground under open sky (`McTerrain`), so herds don't
     camp in water or caves. About 35% of herds **roam**: every 1 to 2 minutes they move their
     home 18 blocks. Settled herds shuffle it 4 blocks every 2 to 4 minutes.
- `FollowHerdGoal` (priority 4) walks a member back when it strays outside its herd's grazing
  circle (3 + 1.2 × √size blocks around the home), to a dry open spot inside it, a bit faster
  when far behind. `HerdCohesionGoal` (priority 5) keeps 2 to 4 blocks between herd mates and
  never steps into water or caves.
- Babies are born into a parent's herd. Homes are not saved: after a restart each herd's home
  is rebuilt from where its members stand.

## Panic and predators

- **Herd panic.** When a herd animal takes damage that makes vanilla animals panic (the
  `minecraft:panic_causes` damage tag: attacks, arrows, explosions, fire...), every friendly
  herd mate within 10 blocks gets a 5 second alarm (`HerdPanicGoal`, priority 1). Each one runs
  8 to 12 blocks away from the attacker with a random swerve of up to 60 degrees, so the herd
  scatters instead of moving as one block. Herding and cohesion pause during the alarm, then
  members walk back to their herd's home. Unrelated species (a pig next to cows) stay calm.
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
| Rules (pure Java, unit tested) | `core/.../core/herd/`, herd decisions in `HerdPlanner.java` |
| Herd manager, follow goal | `neoforge/.../herd/HerdManager.java`, `FollowHerdGoal.java` |
| Species numbers | `core/.../core/herd/BuiltInProfiles.java` (datapack JSON later) |
| Attachment, goals, events | `neoforge/.../neoforge/herd/` |
| Panic and predators | `core/.../herd/HerdAlarm.java`, `FleeRoute.java`; `neoforge/.../herd/HerdPanic*.java`, `Predators.java` |
| Minecraft null boundary | `neoforge/.../neoforge/boundary/` |
| In-game tests | `neoforge/.../neoforge/gametest/HerdGameTests.java` |

## Seeing it in game

Run `/flourishing herd_debug` (operator only) to toggle name tags like
`[herd] #3FA2 5/5 roaming · wellbeing 72 content`: herd id, herd size / this animal's wish,
whether the herd roams, and wellbeing. Animals a player has named are left alone.

## Testing

- `./gradlew :core:test` for the rules.
- `./gradlew :neoforge:runGameTestServer` runs the in-game tests in a walled 28x28 grass pen
  built by `TestPens` (the `herd_pen.nbt` structure only sets the size, because the framework
  placed our structure's blocks as air). With the
  herding goals removed, `scattered_cows_gather` fails (cows stay about 23 blocks apart), and
  with panic and predator goals removed, `herd_panics_together`, `sheep_flee_wolf` and
  `chicken_flees_fox` fail, so the tests really measure the features.

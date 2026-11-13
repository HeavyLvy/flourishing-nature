# Entity AI and pathfinding

## Two AI systems in vanilla [check source]

Analogy: **Goals** are a to-do list where the highest-priority runnable item wins. **Brain** is a mind with memories, senses and a daily routine, picking behaviours based on what it remembers.

| | Goals (`GoalSelector`) | Brain (`Brain<E>`) |
|---|---|---|
| Used by | Cow, Sheep, Pig, Chicken, Horse, Wolf, Cat, Llama, most older mobs | Villager, Piglin, Goat, Axolotl, Frog, Camel, Sniffer, Armadillo, Allay, Warden |
| Extending a vanilla mob | Easy: add goals from an event | Hard: brain is built in the mob's `*Ai` class; changing it usually needs a mixin |
| Data | Fields on the goal / the mob | `MemoryModuleType`s filled by `Sensor`s |

For the herding and wellbeing systems, start with **goals** on goal-based animals. Brain-based animals (goats, camels, armadillos) need a separate plan; note that in the design.

## Writing a goal [check source]

```java
public final class StayNearHerdGoal extends Goal {
    private final PathfinderMob mob;
    private final double speed;

    public StayNearHerdGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override public boolean canUse() { /* cheap check; find herd centre, too far? */ return false; }
    @Override public boolean canContinueToUse() { return !mob.getNavigation().isDone(); }
    @Override public void start() { /* mob.getNavigation().moveTo(x, y, z, speed); */ }
    @Override public void stop() { mob.getNavigation().stop(); }
    @Override public void tick() { }
    @Override public boolean requiresUpdateEveryTick() { return false; }
}
```

- `Goal.Flag`: `MOVE`, `LOOK`, `JUMP`, `TARGET`. Two running goals can't share a flag; the higher-priority (lower number) one wins.
- `canUse` runs often. Keep it cheap: throttle with a tick counter (`reducedTickDelay(...)` / `adjustedTickDelay(...)` helpers exist on `Goal`) and avoid scanning entities every tick.
- Entity scans: `level.getEntitiesOfClass(Cow.class, mob.getBoundingBox().inflate(r), predicate)` returns a `List`, never null.

## Adding goals to vanilla animals

```java
@SubscribeEvent
static void onJoin(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide()) return;
    if (event.getEntity() instanceof Cow cow) {
        cow.goalSelector.addGoal(5, new StayNearHerdGoal(cow, 1.0));
    }
}
```

[check source] `Mob#goalSelector` / `targetSelector` are `protected` in vanilla; NeoForge's access transformer has historically made them public. If they are not accessible in 26.3, add an access transformer entry (`src/main/resources/META-INF/accesstransformer.cfg`, declared in `neoforge.mods.toml`). `EntityJoinLevelEvent` fires on every load, so goals are not saved and must be re-added each time, which is what we want.

Read vanilla priorities first (`rg -n "registerGoals" -A 20 .mc-sources/net/minecraft/world/entity/animal/cow/Cow.java`) so new goals slot in sensibly: panic is usually 1, breed 2, tempt 3, follow parent 4, stroll 6, look 7–8.

## Vanilla goals worth studying for herding

| Goal | Why |
|---|---|
| `FollowFlockLeaderGoal` (fish schools) | Closest vanilla thing to herding: leader election, followers, school size |
| `LlamaFollowCaravanGoal` | Chain-following another mob |
| `FollowParentGoal` | Babies following adults: simple distance-based follow |
| `PanicGoal` | Flee when hurt; a herd-wide panic can extend it |
| `AvoidEntityGoal` | Keep distance from predators |
| `TemptGoal` | Follow a player holding food |
| `BreedGoal` | Find a mate in love mode, spawn the child |
| `EatBlockGoal` (sheep) | Eating grass, a natural hook for "grazing" wellbeing |

## Pathfinding [check source]

- `mob.getNavigation()` returns `PathNavigation` (`GroundPathNavigation` for land animals).
- `moveTo(double x, double y, double z, double speed)` / `moveTo(Entity, double speed)` return `boolean`; `createPath(...)` can return **null** (no path), so wrap it (see `null-safety.md`).
- `isDone()`, `stop()`, `getPath()` (nullable).
- `mob.setPathfindingMalus(PathType.WATER, -1.0F)` changes what an animal avoids.
- Pathfinding is costly. Group herd movement around one leader path and have followers use short follow paths, rather than every animal pathing to a far target.

## Attributes

`Attributes.MOVEMENT_SPEED`, `MAX_HEALTH`, `FOLLOW_RANGE`, `TEMPT_RANGE` [check source], etc. Change vanilla mobs' attributes with `EntityAttributeModificationEvent` (mod bus). For temporary effects (a happy animal moving faster) use `AttributeInstance#addOrUpdateTransientModifier(new AttributeModifier(Identifier, amount, Operation))`.

# No null: how to get there in a NeoForge mod

The rule: **our code never produces, stores, passes or returns `null`.** Minecraft and NeoForge still do, so null is allowed to exist only at the border, where we convert it immediately.

Analogy: Minecraft's API is a river with sticks in it (nulls). We build a filter at every intake pipe (the boundary wrappers), so the water in our own pipes is always clean, and a compiler check (NullAway) is the inspector that fails the build if anyone bypasses the filter.

## 1. Mark our packages non-null by default (JSpecify)

Add a `package-info.java` to **every** package under our root:

```java
@NullMarked
package io.github.heavylvy.flourishingnature.core.herd;

import org.jspecify.annotations.NullMarked;
```

Inside `@NullMarked` code, every type is non-null unless written `@Nullable`. NullAway is configured with `OnlyNullMarked`, so a package **without** `package-info.java` is silently unchecked: always add one.

Where `@Nullable` may appear:
- `core`: never. The `:core:checkNoNull` task fails the build on the word `null` or `@Nullable`.
- `neoforge`: only in `io.github.heavylvy.flourishingnature.neoforge.boundary`, and there only on parameters received from vanilla overrides.

[verified 2026-11-13] Minecraft 26.3 itself uses **JSpecify** annotations (about 2,200 files in `.mc-sources/` import `org.jspecify`; none use `javax.annotation.Nullable`). NullAway reads them, so vanilla's own `@Nullable` returns are visible to the checker and calling one without handling it fails the build.

## 2. Enforcement (already set up in the root `build.gradle`)

Applied to every module:

- Error Prone + NullAway, with `NullAway` as an error and options `OnlyNullMarked`, `JSpecifyMode`, `CheckOptionalEmptiness`, `CheckContracts`.
- `-Xlint:all,-processing,-serial -Werror`. Drop extra categories if they're too noisy against Minecraft classes, never `-Werror` itself.
- Versions are pinned in `gradle.properties` (`errorprone_version`, `nullaway_version`, `jspecify_version`). They support JDK 25; the `net.ltgt.errorprone` plugin adds the `--add-exports` compiler flags.

Verified on 2026-11-13: a `return null;` in `neoforge` fails with `[NullAway] returning @Nullable expression from method with @NonNull return type`, and `o == null` in `core` fails `checkNoNull`.

## 3. Boundary wrappers

Put every call that can return null behind a small static helper returning `Optional`:

```java
@NullMarked
public final class Levels {
    private Levels() {}

    public static Optional<Entity> entity(ServerLevel level, UUID id) {
        return Optional.ofNullable(level.getEntity(id));
    }

    public static Optional<Path> pathTo(PathNavigation nav, BlockPos target, int accuracy) {
        return Optional.ofNullable(nav.createPath(target, accuracy));
    }
}
```

Known nullable spots in vanilla to wrap [check source]: `ServerLevel#getEntity(UUID)`, `PathNavigation#createPath/getPath`, `Mob#getTarget`, `LivingEntity#getLastHurtByMob`, `Animal#getLoveCause`, `AgeableMob#getBreedOffspring`, `EntityType#create`, `MinecraftServer#getLevel`, `Level#getBlockEntity`, `LootContext#getOptionalParameter`, `SpawnGroupData` params, `Entity#getVehicle`, `getControllingPassenger`. Since vanilla is JSpecify-annotated, NullAway will point out any you miss.

When overriding a vanilla method whose parameter is nullable (e.g. `finalizeSpawn(..., @Nullable SpawnGroupData)`), take it as `@Nullable` and convert on the first line: `Optional<SpawnGroupData> group = Optional.ofNullable(data);`.

NeoForge APIs often already use `Optional` (`getExistingData`, `ValueInput#read`), so prefer those.

## 4. Everything else in "super strict typing"

- `record`s for all value data; `final` classes by default; `sealed` interfaces for closed sets (e.g. `sealed interface HerdRole permits Leader, Follower, Stray`) and exhaustive `switch` with pattern matching, no `default` branch, so the compiler catches a new case.
- No raw types, no `Object` parameters, no unchecked casts. Wildcard generics where vanilla demands them (`EntityType<?>`) but convert to concrete types at the boundary.
- Tiny domain types instead of bare primitives where units matter: `record Ticks(int value)`, `record WellbeingScore(float value)` with range checks in the compact constructor.
- Empty collections, never null collections: `List.of()`, `Map.of()`. Vanilla `ObjectArrayList` from loot is non-null.
- No `Optional` fields or parameters; `Optional` is for return values. Fields that may be absent become a sealed type or a sentinel record (`HerdId.NONE`).
- `Objects.requireNonNull` is not a substitute; it is a runtime crash, not a type guarantee. Use it only inside boundary wrappers where vanilla documents non-null but isn't annotated.

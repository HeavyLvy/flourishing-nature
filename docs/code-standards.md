# Code standards

## Null safety (enforced by the build)

- Every package has a `package-info.java` with `@NullMarked`. NullAway then rejects any
  expression that might be null wherever a non-null value is expected. A package without it
  is not checked, so always add it.
- Represent absence with `Optional<T>`. Never return, pass, or store null.
- `Optional.get()` without a prior check fails the build (`CheckOptionalEmptiness`). Prefer
  `map`, `orElse`, `ifPresent`, or pattern matching.
- `core` may not contain `null` or `@Nullable` at all; `./gradlew :core:checkNoNull`
  enforces this.
- In `neoforge`, Minecraft methods that can return null (e.g. `getTarget()`,
  `level.getBlockEntity(pos)`) are wrapped once, in a boundary helper:

```java
static Optional<LivingEntity> target(Mob mob) {
    return Optional.ofNullable(mob.getTarget());
}
```

## Types

- A value with meaning gets a record with validation:

```java
public record HerdSize(int value) {
    public HerdSize {
        if (value < 1) throw new IllegalArgumentException("herd size must be >= 1: " + value);
    }
}
```

- Prefer `sealed` interfaces + records over flag fields and enums-with-data.
- Use exhaustive `switch` over sealed types and enums, no `default` branch, so a new case
  fails compilation until it is handled.
- Classes are `final` unless designed for extension. Fields are `private final` where possible.
- No `var` where the type is not obvious from the right-hand side.

## General

- Java only. No Kotlin.
- No warnings: `-Xlint:all -Werror` and Error Prone are on. Suppressions need a comment.
- Tests for every `core` rule (`core/src/test`). In-game behavior gets a gametest.
- Comments explain why, not what.

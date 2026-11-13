# Minecraft's source code

## The legal line

Minecraft's code is Mojang's copyright. Unobfuscated does not mean open source. So this repo **never commits** vanilla source files or large copied chunks of them. Instead, every developer (and Claude Code) generates the source locally from their own game download, which is exactly what the NeoForge toolchain already does. Short snippets quoted in a discussion or a code comment for reference are fine; whole classes are not.

Analogy: a library lets you read every book, but you can't photocopy the shelf and hand it out.

## How the source gets onto your machine [verified]

Since 26.1, Mojang ships the game unobfuscated. When you run the first Gradle sync or build, ModDevGradle downloads Minecraft, applies NeoForge's patches, and decompiles it into a **sources jar** (`neoforge/build/moddev/artifacts/minecraft-patched-<neo_version>-sources.jar`). The first run took about 4 minutes on this machine; the docs say up to an hour on slow machines.

IntelliJ and Eclipse show that jar under External Libraries. VS Code's Java extension can also open library sources when you "Go to Definition", but browsing the whole tree is clumsy, and Claude Code can't grep inside a jar. So we unpack it.

## Unpack it for Claude Code

Run from the repo root after the first successful `./gradlew build`:

```bash
./scripts/extract-mc-sources.sh
```

It finds the patched Minecraft sources jar ModDevGradle produced, and unzips it into `.mc-sources/` (gitignored). Then Claude Code, `rg`, and VS Code search all work on real `.java` files:

```bash
rg -n "class TemptGoal" .mc-sources/
rg -n "extends Animal" .mc-sources/net/minecraft/world/entity/animal/ -l
```

Re-run the script after changing `neo_version` in `gradle.properties`.

## Package map for this mod [check source]

All under `net/minecraft/` in `.mc-sources/`. Mojang moved many animals into per-species subpackages in 1.21.11 (e.g. `animal/cow/Cow`), so search by class name when a path is off.

| Area | Where to look | Key classes |
|---|---|---|
| Entity basics | `world/entity/` | `Entity`, `LivingEntity`, `Mob`, `PathfinderMob`, `AgeableMob`, `EntityType`, `MobCategory` |
| Animals | `world/entity/animal/**` | `Animal`, `cow/Cow`, `cow/MushroomCow`, `sheep/Sheep`, `pig/Pig`, `chicken/Chicken`, `goat/Goat`, `horse/AbstractHorse`, `wolf/Wolf`, `TamableAnimal` |
| Goals (classic AI) | `world/entity/ai/goal/` | `Goal`, `GoalSelector`, `WrappedGoal`, `BreedGoal`, `TemptGoal`, `FollowParentGoal`, `PanicGoal`, `AvoidEntityGoal`, `WaterAvoidingRandomStrollGoal`, `LookAtPlayerGoal`, `RandomLookAroundGoal`, `EatBlockGoal`, `FollowFlockLeaderGoal`, `LlamaFollowCaravanGoal` |
| Target goals | `world/entity/ai/goal/target/` | `TargetGoal`, `HurtByTargetGoal`, `NearestAttackableTargetGoal` |
| Brain AI | `world/entity/ai/` (`Brain`), `ai/behavior/`, `ai/memory/`, `ai/sensing/`, `schedule/` | `Brain`, `Behavior`, `MemoryModuleType`, `SensorType`, `Activity`; animal brains: `goat/GoatAi`, `axolotl/AxolotlAi`, `camel/CamelAi`, `armadillo/ArmadilloAi`, `sniffer/SnifferAi`, `frog/FrogAi` |
| Pathfinding | `world/entity/ai/navigation/`, `world/level/pathfinder/` | `PathNavigation`, `GroundPathNavigation`, `Path`, `PathFinder`, `WalkNodeEvaluator`, `PathType` |
| Movement & look | `world/entity/ai/control/` | `MoveControl`, `LookControl`, `JumpControl` |
| Attributes | `world/entity/ai/attributes/` | `Attributes`, `AttributeSupplier`, `AttributeInstance`, `AttributeModifier` |
| Natural spawning | `world/level/NaturalSpawner`, `world/entity/SpawnPlacements`, `world/level/biome/MobSpawnSettings` | `NaturalSpawner`, `SpawnPlacements`, `SpawnPlacementTypes`, `MobSpawnSettings` |
| Biomes & climate | `world/level/biome/` | `Biome`, `Biome.ClimateSettings`, `Biomes`, `BiomeTags` (in `tags/`) |
| Weather & time | `server/level/ServerLevel`, `world/level/Level`, `world/attribute/` | `Level#isRaining`, `isThundering`, `isRainingAt`, `EnvironmentAttributes`, timelines |
| Loot | `world/level/storage/loot/`, `data/loot/` | `LootTable`, `LootContext`, `LootContextParams`, `BuiltInLootTables`, `EntityLootSubProvider` |
| Saving | `world/level/storage/` | `ValueInput`, `ValueOutput`, `SavedData` |
| Codecs & networking | `com/mojang/serialization` (DFU lib), `network/codec/` | `Codec`, `MapCodec`, `RecordCodecBuilder`, `StreamCodec`, `ByteBufCodecs` |
| IDs | `resources/Identifier` | `Identifier` (was `ResourceLocation` before 1.21.11) |
| Registries | `core/registries/` | `BuiltInRegistries`, `Registries` |

[verified] The extracted jar contains only `net/minecraft` (with NeoForge's patches applied), not NeoForge's own classes. NeoForge's API (events, attachments, biome modifiers, loot modifiers) is in a separate jar: `find ~/.gradle/caches -name "neoforge-<neo_version>-sources.jar"`, or use your IDE's Go to Definition.

## Tips for reading vanilla code

- To copy a vanilla behaviour, find the mob that already does it and read its `registerGoals()` or its `*Ai` brain class. Cows panicking and following wheat is all in `Cow#registerGoals`.
- Find every caller of something with `rg -n "methodName\(" .mc-sources/`.
- Some inlined constants (primitives, strings) are still not named, per the 26.1 notes. If you see a magic number, it was probably a constant in Mojang's code.

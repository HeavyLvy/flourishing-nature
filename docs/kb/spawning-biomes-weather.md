# Spawning, biomes and weather

## How natural spawning works [verified, NeoForge docs]

Analogy: each mob category has a population budget per area. Every tick for friendly categories (every 400 ticks for monsters), the game checks the budget around players, picks a weighted entry from the biome's spawn list, and tries up to three spots.

- `MobCategory#isFriendly()`: friendly categories (everything except monsters) try every tick.
- `MobCategory#isPersistent()`: animals also spawn during chunk generation. That's why animals mostly appear in herds when land is first generated and rarely afterwards. Important for the herding design: herds are mostly "born" with the chunk.
- `MobCategory#getMaxInstancesPerChunk()`: the cap (`CREATURE` is 10).
- A player must be within 128 blocks.

## Changing which biomes spawn what: biome modifiers [verified]

JSON at `data/flourishing_nature/neoforge/biome_modifier/<name>.json`:

```json
{
  "type": "neoforge:add_spawns",
  "biomes": "#minecraft:is_forest",
  "spawners": [
    { "type": "minecraft:cow", "weight": 8, "minCount": 4, "maxCount": 8 }
  ]
}
```

Other types: `neoforge:remove_spawns` (`biomes`, `entity_types`), `neoforge:add_spawn_costs` / `remove_spawn_costs`, `add_features`, `remove_features`, `add_carvers`, `remove_carvers`, `neoforge:none` (lets a pack disable a modifier). Datagen: `DatapackBuiltinEntriesProvider` with `NeoForgeRegistries.Keys.BIOME_MODIFIERS`. A datapack file at the same path overrides ours, which is good for configurability.

## Spawn rules for a type [verified]

`RegisterSpawnPlacementsEvent` (mod bus): `register(entityType, SpawnPlacementType, Heightmap.Types, predicate, Operation.AND|OR|REPLACE)`. Use `AND` to add an extra condition to a vanilla animal without replacing vanilla's. `SpawnPlacementTypes`: `ON_GROUND`, `IN_WATER`, `IN_LAVA`, `NO_RESTRICTIONS`.

## Spawn events [check source]

- `FinalizeSpawnEvent`: the place to initialise a freshly spawned animal (wellbeing defaults, assign to the herd of nearby same-species animals). `getSpawnType()` tells natural vs. breeding vs. spawn egg vs. chunk generation.
- `MobSpawnEvent.PositionCheck` / `SpawnPlacementCheck`: allow or deny a spawn attempt.
- Spawn group data: vanilla passes a `SpawnGroupData` (nullable) through `finalizeSpawn` so every member of one spawn pack can share state. That's a natural way to give a group spawn one herd ID.

## Biomes [check source]

- `level.getBiome(pos)` returns `Holder<Biome>`. Compare with `holder.is(Biomes.PLAINS)` or tags `holder.is(BiomeTags.IS_FOREST)`.
- Prefer **tags** over specific biome keys so modded biomes work. Define our own tags (e.g. `flourishing_nature:good_grazing`) in `data/flourishing_nature/tags/worldgen/biome/`.
- `Biome#getBaseTemperature()`, `climateSettings`, `hasPrecipitation()`, and `Biome#getPrecipitationAt(pos, seaLevel)` [check source] give temperature/rain/snow.

## Weather and time [check source]

- `level.isRaining()`, `level.isThundering()`, `level.isRainingAt(pos)` (also checks biome and sky exposure), `level.canSeeSky(pos)`.
- Since 1.21.11, many day/night and sky values are **environment attributes** driven by **timelines** (`minecraft:day`, `minecraft:moon`, `minecraft:villager_schedule`, `minecraft:early_game`); timelines replaced brain `Schedule`s. In 26.1 `EnvironmentAttributeSystem` uses a `ClockManager`. If the mod needs "is it night" or "is there shelter", look for `EnvironmentAttributes` in `.mc-sources/net/minecraft/world/attribute/` before reading raw day time.
- Weather is server-authoritative; compute wellbeing effects on the server only.

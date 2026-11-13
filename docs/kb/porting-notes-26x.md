# What changed since 1.21.x (why old tutorials break)

Most NeoForge tutorials online target 1.21.1. Analogy: they're maps of the same city drawn a few years ago. The streets are mostly there, but many were renamed.

## Versioning [verified]
- Minecraft dropped `1.x` numbering: after `1.21.11` came `26.1`, `26.2`, `26.3` (year.drop).
- NeoForge versions mirror it: `26.3.0.102` = MC 26.3, hotfix 0, Neo build 102. NeoForm is `<mc>-<build>`, e.g. `26.1-1`.
- Java 25, Gradle 9.1+.

## Names [verified]
- No obfuscation since 26.1; Mojang names everywhere, parameter names included. Parchment optional.
- `ResourceLocation` → `Identifier` (1.21.11). `FriendlyByteBuf#readResourceLocation` → `readIdentifier`, etc.
- Animals moved into per-species packages in 1.21.11: `animal.cow.Cow`, `animal.fox.Fox`, `animal.rabbit.Rabbit`, `animal.sheep` (models), variants like `CowVariant` moved alongside.
- `EntityType.Builder#build` takes a `ResourceKey` (since 1.21.2).

## Data and items [verified, 26.1]
- `ItemStackTemplate` (immutable item+count+components) is how data files describe stacks; creating `ItemStack`s needs loaded registries. `ItemInstance` is the shared interface. `FluidStackTemplate` likewise.
- `ChunkPos`: `new ChunkPos(blockPos)` → `ChunkPos.containing(blockPos)`; `asLong` → `pack`; `new ChunkPos(long)` → `unpack`.
- Loot: registries take `MapCodec` directly, `*Type` wrappers removed, `getType` → `codec`, `CODEC` fields → `MAP_CODEC`. `LootContextParams.TOOL` is an `ItemInstance` key.
- Saving: `ValueInput` / `ValueOutput` instead of raw `CompoundTag` in entity save methods (1.21.6+).

## World [verified]
- Environment attributes + timelines (`minecraft:day`, `minecraft:moon`, `minecraft:villager_schedule`, `minecraft:early_game`) drive sky, moon, and spawn-chance values; timelines replaced brain `Schedule`s (1.21.11). 26.1 moved the time source to `ClockManager`.

## 26.3 specifically [verified, partial]
- Loot overhaul: `ContextAwarePredicate` removed in favour of `Holder<LootItemCondition>`; new loot data types (`SlotSource`, `ContextFloatProvider`, `ContextIntProvider`); `LootPoolSingletonContainer` split; `LootTableSubProvider` uses a factory/context pattern with `run` replacing `generate`.
- Recipes and advancements are reloadable datapack registries.
- Client: big rendering rewrite (SDL instead of GLFW, "Renderpearl", order-independent transparency). Only matters if we add custom renderers.
- The 26.3 primer is very long; we read only its first half on 2026-11-13. Read `docs.neoforged.net/primer/docs/26.3/` sections on entities before implementing anything there.

## How to port a snippet from an old tutorial
1. Paste it, compile, see what doesn't resolve.
2. `rg -n "SimilarName" .mc-sources/` to find the new name.
3. Check the primers for 1.21.2 → 26.3 for the rename.

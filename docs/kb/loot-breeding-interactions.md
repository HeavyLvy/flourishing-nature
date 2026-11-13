# Drops, milking, shearing and breeding

## Changing drops: three tools

Analogy: the loot table is the recipe, a global loot modifier is a chef's tweak after cooking, and `LivingDropsEvent` is grabbing the plate on its way out.

1. **Loot tables** (`data/minecraft/loot_table/entities/cow.json`): overriding replaces vanilla and clashes with other mods. Avoid for vanilla animals.
2. **Global loot modifiers** [verified]: run after a loot table rolls, stack with other mods, data-driven. Best default.
   ```java
   public final class WellbeingDropsModifier extends LootModifier {
       public static final MapCodec<WellbeingDropsModifier> CODEC = RecordCodecBuilder.mapCodec(
           inst -> codecStart(inst).apply(inst, WellbeingDropsModifier::new));
       public WellbeingDropsModifier(LootItemCondition[] conditions) { super(conditions); }
       @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
           return loot; // read ctx's THIS_ENTITY param, scale counts by wellbeing
       }
       @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
   }
   ```
   Register the codec with `DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID)`. JSON lives at `data/<ns>/loot_modifiers/<path>.json`. [verified 26.3] `LootModifierManager` loads every file in that folder like a registry; there is no `global_loot_modifiers.json` list any more (a file by that name would be parsed as a modifier). The constructor takes `Optional<Holder<LootItemCondition>> condition, int priority`. The loot table id is `ctx.getQueriedLootTableId()`; `LootContext#getOptional(key)` is nullable. Limit to one table with the `neoforge:loot_table_id` condition. [check source] the exact constructor parameter type for conditions in 26.3; the 26.3 primer reorganised loot conditions toward `Holder<LootItemCondition>`.
3. **`LivingDropsEvent`**: edit the final `ItemEntity` list in code. Fine for quick logic; not datapack-tunable.

Loot context: `LootContextParams.THIS_ENTITY`, `ATTACKING_ENTITY`, `DAMAGE_SOURCE`. In 26.1 `LootContextParams.TOOL` became an `ItemInstance` key. `ctx.getOptionalParameter(...)` returns nullable [check source]; wrap it.

## Milking, shearing, feeding [check source]

- Vanilla handles these in `Mob#mobInteract(Player, InteractionHand)` overrides: milking in the cow class (`cow/AbstractCow` or `cow/Cow` in recent versions), shearing via the `Shearable` interface on `Sheep`, `MushroomCow`, `SnowGolem`, `Bogged`. NeoForge has historically had `IShearable` for modded shears; check whether it still exists in 26.3.
- To gate or change milking by wellbeing: listen to `PlayerInteractEvent.EntityInteract`, check `event.getTarget() instanceof AbstractCow` and the held item `is(Items.BUCKET)`, then `event.setCanceled(true)` and `event.setCancellationResult(InteractionResult.…)` to replace vanilla behaviour. Run only on the server side and return a consistent result on the client to avoid arm-swing desync.
- Food: `Animal#isFood(ItemStack)` decides tempt/breed food, usually via an item tag such as `ItemTags.COW_FOOD`. Adding items to that tag (datapack) is the cleanest way to extend diets.

## Breeding [check source]

- `Animal#setInLove(Player)`, `isInLove()`, `canMate(Animal)`, `getBreedOffspring(ServerLevel, AgeableMob)` (nullable return), `spawnChildFromBreeding(ServerLevel, Animal)`.
- `BabyEntitySpawnEvent` fires before the baby is added: cancel to block breeding (say, an unhappy animal), or `setChild(...)` to replace it. `getParentA()`, `getParentB()`, `getCausedByPlayer()`.
- Baby ageing: `AgeableMob#getAge()` (negative = baby), `setAge`, `ageUp`.
- A new baby joining its parents' herd: copy the parent's herd UUID attachment in `BabyEntitySpawnEvent` or `FinalizeSpawnEvent` with spawn type `BREEDING`.

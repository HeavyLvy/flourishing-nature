# Herbivores (living world, section 1)

Animals live off real plants: they walk to grass, ferns, flowers, crops and dropped food and eat
them, which fills their fullness (`FullnessApi.eat`). Full, happy herds breed on their own;
hungry herds move toward food; herds keep away from where they were attacked.

## Who eats what

| Species | Eats (favourites in bold) | Crops grass blocks to dirt |
|---|---|---|
| Cow, mooshroom | **tall grass**, **ferns**, short grass | only when very hungry (fullness < 0.25) |
| Sheep | **short grass**, tall grass, flowers (never the last of a kind within 8 blocks) | yes, like vanilla, more often when not full |
| Horse, donkey | **tall grass**, short grass, ferns | no |
| Chicken | **seeds on the ground** (`#minecraft:chicken_food`), short grass | no |
| Pig | **carrots, potatoes, beetroot on the ground**, ripe carrot, potato and beetroot crops | no |
| Rabbit | **ripe root crops**, ripe wheat, flowers | no |

- A favourite is worth walking twice as far for: cost = (distance + 1) / appetite, cheapest wins.
- One plant or item is `PLANT_MEAL` (0.08 fullness); tall grass is 1.5x that; a grass block
  cropped to dirt is `GRASS_BLOCK_MEAL` (0.06). Grazers empty in about two days, so a cow needs
  roughly one plant every three minutes.
- Which blocks and items are which food comes from tags, so datapacks can add modded plants:
  `#flourishing_nature:forage/{short_grass,tall_grass,fern,flower,root_crop,grain_crop}` (blocks)
  and `#flourishing_nature:forage/{seed_item,root_item}` (items). Crops count only when ripe,
  tall plants only by their lower half.
- The plant is removed (`destroyBlock`, no drops); one item is taken from a dropped stack.

## The grazing goal

`GrazeGoal` (priority 4, rabbits 5) runs only while fullness is under 0.9, searches at most every
5 seconds (plus jitter) within 8 blocks, walks to the target and eats it after a short chew.
After a meal it looks again after a second, so a hungry animal grazes several plants in a row.

Sheep lose vanilla's `EatBlockGoal`; `CropGrassGoal` (priority 5) does the same thing (crop the
grass under its feet, head-down animation) and also feeds fullness. Cows get it too, but it only
fires when they are very hungry.

### Grazing filters (extension point)

`GrazingFilters.register(GrazingFilter)` adds a rule `(level, animal, target) -> allowed`. The goal
tries candidates cheapest first and takes the first one every filter allows. Built in:

- **Base protection:** plants at block light 8 or more are never eaten (food on the ground is),
  and mob griefing off stops all plant eating.
- **Grazing circle:** a herd member only eats inside its herd's grazing circle (+1 block), so the
  follow goal doesn't drag it back mid-walk.
- **Sheep gardener's brake:** a sheep never eats the last flower of its kind within 8 blocks.

## Wellbeing food need

Food = half fullness (full from 0.7 up) + half edible plants within 8 blocks (16 is a full
score), or a recent hand-fed meal if that's higher. Species with no plant diet (wolves) are
scored on fullness alone. This replaces the old pasture-block count (the `#pasture` tag is gone).

## Self-breeding

Every 10 seconds per animal: an adult herd animal that can fall in love, is FULL and content or
thriving (`ProductionRules.canSelfBreed`), hasn't self-bred in 3 in-game days, and is not past
the soft population cap, looks for a herd mate of its species within 8 blocks that passes the
same check. Both fall in love (vanilla's breeding goal does the rest) and start the cooldown
(`flourishing_nature:last_self_bred` attachment).

**Soft population cap:** if 12 or more of the species (config, per-species overrides: chickens
16, pigs 8) are within 16 blocks, nobody there self-breeds. Players can still breed by hand.

Test hook: `SelfBreedingCheck.checkNow(animal)` returns the verdict
(`READY`, `NOT_READY_TO_LOVE`, `NOT_WELL_ENOUGH`, `COOLING_DOWN`, `CROWDED`).

## Herd homes: food and danger

`HerdManager.addHomeAdjuster` lets features nudge a herd's home after each planning pass
(before shelter expeditions). `ForagingHomes` does two things:

- **Danger memory:** when a herd animal is hurt by another entity, it and its herd mates within
  16 blocks remember the spot (`flourishing_nature:danger_memory`, up to 4 spots, an attack near
  a remembered spot refreshes it). The herd keeps its home at least 16 blocks away for one day;
  the area then shrinks to nothing over the next day. A home inside an area moves to the nearest
  usable spot 2 blocks past its edge. Hungry herds also never move toward a remembered spot.
- **Hungry herds roam:** every 20 seconds per herd, if the members' average fullness is under 0.9
  and 2 or fewer edible plants are left within 8 blocks of home, the herd samples 16 patches
  (7x7, at 12 and 20 blocks around home, tall grass counting double) and moves its home to the
  richest safe one. Test hook: `ForagingHomes.checkFoodSoon(level, herd)`.

## Config

`config/flourishing_nature-herbivores.toml` (synced server config; NeoForge 26.3 calls this type
`SYNCED`): `grazing.enabled`, `grazing.grazeBelow` (0.9), `grazing.veryHungryBelow` (0.25),
`grazing.searchRadius` (8), `grazing.searchInterval` (100), `selfBreeding.enabled`,
`selfBreeding.cooldownTicks` (72000), `selfBreeding.capRadius` (16), `selfBreeding.defaultCap`
(12), `selfBreeding.speciesCaps` (`minecraft:chicken=16`, `minecraft:pig=8`), `homes.followFood`,
`homes.scarceAt` (2), `danger.enabled`, `danger.radius` (16), `danger.holdTicks` (24000),
`danger.fadeTicks` (24000).

## Code

| Part | Location |
|---|---|
| Rules (pure Java, unit tested) | `core/.../core/herbivore/` (`Diets`, `Grazing`, `SelfBreeding`, `PopulationCaps`, `DangerMemory`, `HomeChoice`) |
| Goals, filters, events, config | `neoforge/.../neoforge/herbivore/` |
| Food tags | `neoforge/src/main/resources/data/flourishing_nature/tags/{block,item}/forage/` |
| In-game tests | `neoforge/.../gametest/HerbivoreGameTests.java` |

## Tests

`HerbivoreGameTests` (12): cow prefers tall grass, sheep spare the last flower, pig eats a dropped
carrot and ripe (not unripe) potatoes, chicken pecks seeds, rabbit nibbles flowers, food need
reads fullness and plants, full happy cows breed on their own (a calf is born), fed-but-not-full
and crowded cows don't, a hungry herd moves home to tall grass, a herd leaves where it was
attacked. Control runs: with the grazing goal disabled the 6 eating tests fail; with the flower
filter, appetite preference, love start, population cap, food-seeking homes, danger memory and the
plant part of the food need disabled, the 7 matching tests fail.

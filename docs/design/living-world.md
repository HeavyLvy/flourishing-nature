# Living world: build spec

Source: the design doc "Flourishing Nature: Mob Social & Wellbeing Design", as of 2026-11-13.
**Decided** items come from Cayden. **Recommended** items are still open: build them as stated
and keep the value in config so it can change. All numbers are starting guesses.

Core rule (Decided): **no per-chunk bookkeeping.** Everything runs on real blocks and real
animals. Hidden state lives only on individual animals (or as visible block states).

Base protection (Recommended): no trampling, bare-ground conversion or sapling succession on
blocks with block light 8 or more. Only loaded areas change (Recommended): use random ticks.

## Shared foundation (already built)

- `core/.../fullness`: `Fullness`, `HungerBand` (FULL ≥ 0.7, FED ≥ 0.4, HUNGRY ≥ 0.15,
  STARVING below), `FullnessMeter` (drains lazily from a timestamp), `FullnessRates` (grazers
  empty in ~2 days, a kill keeps wolves fed ~2 days, meal sizes).
- `core/.../production/ProductionRules`: product rate, drop roll bonus, minimum drops, breeding
  and self-breeding gates, max wool, from mood × hunger band.
- `neoforge/.../fullness/FullnessApi`: `fullness`, `band`, `eat`, `set`. Hand-feeding adds
  `HAND_FED`. Every feature reads and writes fullness only through this.

## 1. The living food web

1. **Plants grow** on random ticks, with light: grass blocks sprout short grass, short grass
   becomes tall grass, flowers spread to nearby grass. Rain gives an extra growth chance.
2. **Animals eat real plants.** Cows walk to short or tall grass and eat it (plant removed).
   Sheep do the same and also crop grass blocks to dirt (vanilla). Chickens peck seeds from
   broken grass. Pigs eat carrots, potatoes and beetroot they find. Rabbits nibble flowers and
   crops.
3. **Fullness** drains over time and fills when eating.
4. **Self-breeding:** an adult with high fullness and good wellbeing, next to a herd mate in the
   same state, enters love mode on its own, then a cooldown of a few in-game days.
5. **Overpopulation:** too many grazers strip the plants, fullness drops, breeding stops, and the
   herd splits or roams to find food.
6. **Predators follow the food:** wolves have fullness. A hungry pack drifts toward prey within
   about 48 blocks and hunts weak or young animals first. A kill keeps the pack full about 2
   days; the meat is eaten, not dropped.
7. **Pack dynamics:** a full pack can self-breed. A pack several days without a kill splits up
   and wanders off.
8. **Recovery:** fewer grazers, plants regrow, the loop repeats.

| What happens | Blocks | Visible effect |
|---|---|---|
| Heavy grazing | Grass block to dirt (sheep, and cows when very hungry) | Bare patches |
| Trampling | Each step on dirt: tiny chance to coarse dirt; very busy routes to dirt path | Trails |
| Grass reclaiming | Grass spreads onto dirt; tall grass next to coarse dirt loosens it back to dirt | Trails heal from the edges |
| Moss creeping | Humid biomes: moss spreads onto adjacent cobblestone, stone, gravel; moss on gravel slowly becomes dirt | Rubble greens over |
| Wallowing | Pigs on dirt next to water turn it to mud | Muddy banks |
| Seed carrying | Foxes carrying sweet berries, and chickens, can drop them; a dropped berry on grass may plant a bush | Berry bushes along routes |
| Succession | Ungrazed tall grass can sprout the biome's tree sapling at the biome's tree growth rate | Clearings fill in; plains almost never |

Ties: wellbeing food need reads real fullness plus edible plants within 8 blocks. When plants
around a home spot are gone, the herd picks a new home near tall grass. Herds sheltering under
trees trample the ground there. Gallops trample their route. Herds remember where they were
attacked and avoid that spot for 1 to 2 days. Safety net: a soft cap per species per small area.

## 2. Hunger without death (Decided)

| Fullness | Breeding | Products | Drops |
|---|---|---|---|
| Full | Can self-breed | Normal, scaled by wellbeing | Normal, scaled by wellbeing |
| Hungry | No | Half rate | -1 roll |
| Starving | No | None: no milk, no eggs; shearing gives at most 1 wool | Minimum: 1 meat, nothing else |

Hand-feeding a starving animal fixes it in a few bites. Wellbeing effects from the design doc:
milk charges (max 2; refill 12,000 ticks at 80+, 24,000 at 50 to 79, 72,000 at 25 to 49, none
below 25), drops (+1 roll at 80+, vanilla at 50 to 79, -1 roll at 25 to 49, 1 meat at 0 to 24,
via a global loot modifier), breeding (refuses love mode below 25; above 80, breeding cooldown
25% shorter and babies grow 25% faster).

## 3. Tree growth by biome (Decided: plains 10x slower than forests)

Datapack JSON so modded biomes can set their own. Tall grass and flowers regrow normally
everywhere.

| Biome | Tree growth | Ungrazed land to young woodland (days) |
|---|---|---|
| Forest, birch forest, taiga, dark forest, jungle | 1.0 | about 20 |
| Meadow, savanna, windswept hills | 0.3 | about 65 |
| Plains, sunflower plains, snowy plains | 0.1 | about 200 |
| Desert, badlands, beach, mushroom fields | 0 | never |

## 4. Chicken nests (Decided)

Chickens no longer drop eggs on the ground.

1. **Claim first:** a chicken with no nest claims the nearest empty nest within about 16 blocks.
2. **Gathering:** otherwise it gathers material within about 12 blocks, carried in its beak (like
   a fox holding an item), from dropped items and from blocks.
3. **Building:** at about 8 points it places a nest block on solid ground near its herd's home,
   preferring cover.
4. **Laying:** the hen walks to her nest and lays into it. Up to 4 eggs; players collect by
   using the nest.
5. **Breeding** lays a fertile egg (looks slightly different) instead of spawning a chick.
6. **Hatching:** each fertile egg's incubation counter advances only while a hen sits on the
   nest, pauses (never resets) when she leaves, and hatches at `incubation_ticks` (config,
   default 12000). A fertile egg taken by a player is an ordinary egg item.
7. **Ownership:** one nest per chicken; if the owner dies, the nest is empty and claimable.
8. **Crafting:** 5 sticks + 2 wheat or tall grass. Crafted nests never decay; wild nests with no
   hen for a few days fall apart into a few sticks.

| Material | Points | From items | From world blocks |
|---|---|---|---|
| Stick | 1 | yes | no |
| Leaf litter | 2 | yes | picks up leaf litter blocks |
| Short grass | 1 | yes | pulls short grass |
| Tall grass | 2 | yes | pulls tall grass |
| Wheat (optional) | 2 | yes | no |

## 5. A job for each farm animal

- **Cows, fertilizers:** prefer tall grass and ferns and trample ferns and tall grass they walk
  through. Resting at home, a fed, happy cow sometimes leaves a **cow pat** (thin block, `age`
  state, dries over ~3 days, bone-meals grass around it as it ages, then crumbles). Cows avoid
  eating within 2 blocks of a fresh pat. Chickens peck at pats as food. Brake: crammed or
  starving cows make none. Reward: a dried pat breaks into **dried dung**: bone meal on grass and
  crops, and furnace fuel. (Recommended: yes to the new block.)
- **Sheep, gardeners:** an unsheared sheep walking through flowers or tall grass can catch one
  seed in its wool (one held-item slot, shown as a tiny flower in the fleece; Recommended: one
  at a time). Where it lies down or grazes, the seed drops and plants on a free grass block.
  Sheep prefer short grass. Sheared sheep carry no seeds. Brake: never eats the last flower of a
  kind within 8 blocks; bare dirt takes no seeds.
- **Pigs, diggers:** in forests and on grass under trees, root grass or podzol into rooted dirt,
  sometimes turning up a carrot, potato, beetroot or mushroom; under oak or dark oak can plant a
  sapling. Near water or after rain, dig a **wallow** (one-block mud dip that fills with water in
  rain; frogs can spawn there). Rooted dirt regrows grass with more flowers, ferns, mushrooms.
  Brake: per-pig cooldown of several days per spot; too many pigs leave slow-healing coarse dirt.
- **Horses, well-diggers:** in dry, warm biomes (Recommended: only there), with no water nearby,
  a herd stops at low ground and paws a 1-block pit; with water underground within a few blocks
  it becomes a water hole. Water holes count as watering spots for home choice; grass and a
  sapling can sprout beside them (oasis). Brake: dig only with truly no water nearby; holes dry
  back to dirt after a long dry spell.

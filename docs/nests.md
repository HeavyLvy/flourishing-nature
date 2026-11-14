# Chicken nests

Spec: [design/living-world.md](design/living-world.md), section 4. Chickens no longer drop eggs on
the ground. Each hen has one nest; she lays into it, broods fertile eggs in it, and players
collect eggs from it.

## The nest block

`flourishing_nature:nest`, a low ring of straw (3 px tall). Block states:

| State | Meaning |
|---|---|
| `eggs` 0 to 4 | How many eggs it holds (each shows as a small egg) |
| `fertile` | At least one egg is fertile; those eggs are drawn with a speckled, warmer texture |
| `wild` | Built by a hen. Wild nests decay when abandoned and break into 3 sticks; crafted (`wild=false`) nests never decay and drop themselves |

The block entity (`NestBlockEntity`) stores the eggs in order (plain, or fertile with its own
warmth counter), the owner hen's UUID or none, and the tick a hen was last seen there. The
crafted-vs-wild flag lives in the `wild` block state rather than the block entity, so it is
visible and the loot table can read it.

- **Collecting:** using the nest with any hand takes the newest egg. A fertile egg comes out as an
  ordinary `minecraft:egg`. Breaking a nest drops its eggs as ordinary eggs.
- **Crafting:** shapeless, 5 sticks + 2 of `#flourishing_nature:nest_binding` (wheat, tall grass).
- **Decay:** a wild nest with no hen sitting on it and no living, loaded owner for
  `wild_decay_ticks` (default 72000, 3 in-game days) falls apart into 3 sticks (plus its eggs).

## What a hen does

All three jobs are goals at priority 3 (panic, breeding and the vanilla tempt goal still win);
bookkeeping runs once a second in `HenNests.update`.

1. **Claim:** a hen without a nest claims the nearest unowned nest within `claim_radius` (16).
   Loaded nests list themselves in a per-level, unsaved `NestIndex` as they tick, so no block
   scanning is needed. One nest per hen.
2. **Gather** (`GatherNestGoal`): otherwise she looks within `gather_radius` (12) for nest
   material, dropped items first, then growing plants, and carries each piece in her beak
   (her main-hand slot, drop chance 0):

   | Material | Points | Item tag | Block tag (pulled up) |
   |---|---|---|---|
   | Stick | 1 | `nest_material/small` | |
   | Short grass | 1 | `nest_material/small` | `nest_material/small` |
   | Tall grass | 2 | `nest_material/large` | `nest_material/large` |
   | Leaf litter | 2 | `nest_material/large` | `nest_material/large` |
   | Wheat | 2 | `nest_material/large` | |

3. **Build:** at `build_points` (8) she picks a spot within 5 blocks of her herd's home
   (`HerdManager.homeOf`; her own position if she has no herd yet): solid ground, room above, dry,
   no other nest within 2 blocks. Covered spots (no sky) win, then the one closest to home.
4. **Lay** (`LayInNestGoal`): her egg clock (`LayProgress`) gains
   `ProductionRules.productRate(mood, hungerBand)` per tick, so a content, fed hen lays every
   `lay_interval_ticks` (9000, vanilla's average), a thriving one twice as often, a hungry one half,
   and a starving or miserable one never. The clock never banks more than one egg. When an egg is
   due she walks onto her nest and lays it; a full nest (4 eggs) waits for a player.
5. **Breeding** (`BabyEntitySpawnEvent`, lowest priority, after production's own check): no chick
   spawns. The parent with a nest (or the first one) carries a fertile egg and lays it at her next
   visit. Love mode and cooldown reset as usual. If production cancels the birth (a parent can't
   breed), no egg either.
6. **Brood** (`BroodGoal`): a free hen whose nest holds fertile eggs starts a sitting bout with
   chance `brood_share` (0.75) per check (every 100 ticks), sits for `brood_bout_ticks` (1200),
   then takes a 600-tick break to eat.
7. **Hatch:** every second, if any adult chicken stands in the nest block, every fertile egg gains
   20 ticks of warmth. Warmth pauses when no hen sits and never resets. At `incubation_ticks`
   (12000) the egg hatches into a chick in the nest, joining the sitter's herd.
8. **Ownership:** when the owner dies the nest becomes unowned and claimable.

Vanilla laying is suppressed by holding `Chicken.eggTime` (public) at 6000 every tick, so the
vanilla `chicken_lay` gift loot never rolls; the vanilla loot table is left untouched.

Base protection: hens never pull plants or place a nest where block light is 8 or more, or where
mob griefing is off (`NestWorld.mayEdit`).

## Beak rendering (client)

`nest/client/BeakItemLayer` is a render layer on the chicken renderer, modelled on the fox's held
item layer: a render-state modifier copies the chicken's main-hand item into the render state, and
the layer draws it at the beak, following the head. It compiles against the 26.3 client but has
not been looked at in a running client (agents may not launch one); offsets may need a tweak.

## Config

`serverconfig/flourishing_nature-nests.toml` (synced): `claim_radius`, `gather_radius`,
`build_points`, `incubation_ticks`, `wild_decay_ticks`, `lay_interval_ticks`, `brood_share`,
`brood_bout_ticks`. Defaults in `core/.../nest/NestSettings.DEFAULT`.

## Test hooks

`HenNests.assign(hen, nest)`, `HenNests.makeEggDue(hen)`, `NestBlockEntity.addEgg(egg)`,
`NestBlockEntity.setLastHenSeen(tick)`, `NestBlockEntity.check(level, elapsed)`.

## Tests

Unit (`core/.../nest/NestTest`): clutch size, taking eggs, warmth adding up across bouts, plain
eggs never hatching, laying rates from ProductionRules, no egg banking, build points, decay,
ownership, brooding chance, site choice, settings validation.

Gametests (`NestGameTests`): `hen_claims_empty_nest`, `hen_builds_nest_from_dropped_material`,
`hen_builds_nest_from_pulled_grass`, `hen_lays_in_her_nest`, `chickens_drop_no_eggs_on_the_ground`,
`breeding_lays_a_fertile_egg`, `fertile_egg_hatches_only_while_sat_on`, `hen_sits_on_fertile_eggs`,
`player_collects_eggs`, `dead_hens_nest_is_free`, `abandoned_wild_nest_falls_apart`. A control
run with each behaviour disabled failed all 11.

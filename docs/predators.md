# Predators: wild wolf packs

Spec: [design/living-world.md](design/living-world.md), section 1 items 6 and 7 ("Predators
follow the food", "Pack dynamics"). Only **wild (untamed)** wolves are affected; tamed wolves
behave exactly as in vanilla.

A pack is a herd of wild wolves (see [herding.md](herding.md): packs of about 4).

## Behaviour

| Hunger (pack average) | What the pack does |
|---|---|
| Full (≥ 0.7) | No hunting. Two ready adults may have a pup. |
| Fed (≥ 0.4) | No hunting. |
| Hungry (≥ 0.15) | Hunts usual prey: tag `flourishing_nature:wolf_prey` (sheep, rabbits, foxes) and baby turtles on land. The pack's home drifts toward prey it can smell. |
| Starving (< 0.15) | Also hunts `flourishing_nature:wolf_prey_when_starving` (chickens, pigs, cows, goats). After days without a kill the pack breaks up. |

- **Hunger.** Wolves use the shared fullness meter (`FullnessApi`); `FullnessRates.PREDATOR_DRAIN`
  takes a wolf from full to hungry in about 2 days. A newly seen wolf starts at 0.6 (fed).
- **Hunting.** Vanilla's always-on prey target goals (`NonTameRandomTargetGoal` for animals and
  baby turtles) are removed from every wolf and replaced by one target goal at the same priority (5)
  and pace (about every 10 ticks) that only fires for a hungry wild adult. Skeleton hunting and
  retaliation are untouched. Range is the wolf's follow range (16 blocks).
- **Weak and young first** (`core/predator/PreyChoice`): effective distance = distance / (1 + 2 ×
  weakness), weakness = (baby ? 1 : 0) + (1 − health share). A lamb counts as a third as far as a
  healthy sheep; a healthy sheep right next to the wolf still beats a far lamb.
- **Never prey:** pets, tamed horses, leashed animals and name-tagged animals (they are the player's).
- **Drift.** Each herd-planner pass (every 2 s), a hungry pack's home moves 4 blocks toward the
  weakest prey within 48 blocks of it, stopping 6 blocks short. Done through
  `HerdManager.addHomeAdjuster`, so the existing follow goal walks the pack there; once prey is in
  follow range, the hunt goal takes over.
- **Kills.** When a wild wolf kills an animal, every wild pack mate within 24 blocks of the kill (and
  the killer) is set to full and remembers the meal; their remaining chases are called off. The
  carcass is eaten: `LivingDropsEvent` is cancelled (lowest priority), so no meat, wool, hide or held
  item drops, whatever the wellbeing loot modifier produced. XP is unchanged: vanilla only gives XP
  for player kills, so a wolf kill gives none.
- **Pups.** Each pack pass (every 40 ticks, offset 20 from the herd planner) a pack whose average
  is full, with fewer than 6 members, has a pup from two adults that are each grown (`age == 0`),
  pass `ProductionRules.canSelfBreed` (full, and thriving or content; a wolf never scored yet counts
  as content, like production does) and are past a 3-day cooldown (per-wolf memory attachment).
  Vanilla only lets *tame* wolves fall in love, so the pup is spawned directly with
  `Animal.spawnChildFromBreeding`. That still fires `BabyEntitySpawnEvent`, so the pup joins the
  pack, inherits a herd preference, and production's breeding gate still applies. It also gives the
  vanilla breeding hearts and XP orb, same as any other self-breeding.
- **Break-up.** A pack in which every adult is starving and nobody has eaten for 4 days breaks up:
  every member leaves the herd and travels alone 64 to 112 blocks away, each in its own direction
  (evenly spread, with a random turn). Travelling wolves are kept out of herd planning until they
  arrive (within 6 blocks) or a day passes, then settle and may found or join a new pack. Their meal
  clock restarts, so they get a full spell in the new territory before splitting again.
- **No death by hunger.** Nothing ever damages a hungry wolf.

## Per-wolf data

`flourishing_nature:pack_memory` (last meal tick, last pup tick; saved) and
`flourishing_nature:pack_dispersal` (target and give-up tick; saved, only while travelling).

## Config

`flourishing_nature-predators.toml` (NeoForge 26.3 calls the server-side type `SYNCED`):
`hunger_gates_hunting`, `kills_feed_pack`, `pack_breeding`, `pack_splitting` (all true),
`scent_radius` 48, `drift_step` 4, `share_radius` 24, `breed_cooldown_days` 3,
`max_breeding_pack_size` 6, `split_after_days` 4, `dispersal_min` 64, `dispersal_max` 112.
Prey lists are the two entity type tags above (datapack overridable).

## Test hooks

`WolfPacks.runPass(level)` runs a pack pass now; `WolfPacks.setLastMeal(wolf, ticksAgo)` and
`setLastBred(wolf, ticksAgo)` fake days passing; `FullnessApi.set` sets hunger.

## Tests

Unit: `core/src/test/.../predator/PackRulesTest` (diet bands, weak-first choice, drift, pack hunger,
breeding gate and cooldown, split timing, dispersal spread and ending).

Gametests (`PredatorGameTests`): `fed_wolf_ignores_sheep`, `hungry_wolf_takes_the_lamb`,
`kill_feeds_the_pack` (whole pack full, no items on the ground), `hungry_pack_drifts_to_prey`,
`full_pack_has_a_pup` (one pup in the pack, none within the cooldown, another after it),
`starving_pack_breaks_up` (left the herd, travelling, spread apart, unhurt).

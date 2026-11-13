# Production: hunger without death

What an animal gives (milk, drops, wool, babies) depends on its **mood** (wellbeing) and its
**hunger band** (fullness). All numbers come from `core/.../production/ProductionRules`; this
feature applies them in game. Spec: [design/living-world.md](design/living-world.md) section 2.

Only species with a herd profile are affected (cow, mooshroom, sheep, pig, chicken, horse,
donkey, goat, wolf). An animal that has not had a wellbeing check yet counts as content.

## Product rate

`productRate = mood × hunger`: thriving 2, content 1, stressed 1/3, miserable 0; full and fed 1,
hungry 0.5, starving 0. Chickens' egg laying is left to the nests feature, which reads the same rate.

## Milk charges

- Cows, mooshrooms and goats hold up to **2 charges**. A bucket (or a bowl on a mooshroom, for stew
  or suspicious stew) uses one.
- One charge refills in **24,000 ticks / productRate**: 12,000 thriving, 24,000 content, 72,000
  stressed, 48,000 for a hungry content animal, never when miserable or starving.
- With no charge ready the animal refuses: it calls out, a dispenser "click" plays, a puff of smoke
  and a one-second head shake. The bucket stays empty.
- Saved on the animal as `flourishing_nature:milk` (charges, refill progress, tick). Refill is
  worked out lazily from game time at the rate the animal has *when you next milk it*, so nothing
  ticks.
- The client never predicts milking (it can't know the charges): it only swings the arm and the
  server's inventory change follows.

## Drops (global loot modifier)

`flourishing_nature:wellbeing_drops` (`data/flourishing_nature/loot_modifiers/wellbeing_drops.json`;
NeoForge 26.3 loads every file in that folder, there is no `global_loot_modifiers.json` list any more).

| Mood / hunger | Death drops |
|---|---|
| Starving, or miserable | Minimum: 1 meat (`#flourishing_nature:minimum_drop`, raw and cooked meat), nothing else |
| Roll bonus > 0 (thriving and full/fed) | The animal's loot table is rolled once more per point |
| Roll bonus < 0 (stressed, hungry) | Each stack loses one item per point, never below 1 meat |

Death loot is told apart from other entity loot by its damage source parameter.

## Wool

- Shearing loot (same modifier): wool capped at `ProductionRules.maxWool`, so a starving sheep gives
  at most 1.
- Regrowth: vanilla regrows wool every time a sheared sheep eats. Now each regrowth only sticks
  with chance `woolRegrowChance = min(1, productRate)`: always for content or thriving and fed,
  half the meals when hungry, a third when stressed, never when starving or miserable. Detected as
  the sheared flag flipping between ticks, so it works with any eating goal.

## Breeding

- **No love mode** when `!ProductionRules.canBreed` (hungry, starving or miserable). Hand-feeding
  still feeds: the decision uses the hunger *before* the bite, the food is eaten and fills the
  meter, but no hearts. Once fed it accepts love on the next bite.
- Births are cancelled if either parent cannot breed when the baby would spawn (covers horses,
  which breed through their own feeding code, and love mode started before the animal got hungry).
- **Thriving** (wellbeing 80+): the parent's breeding cooldown is 25% shorter (4,500 instead of
  6,000 ticks), and a baby of parents averaging 80+ grows up 25% faster (19,200 instead of 24,000
  ticks). Applied one tick after the birth, since vanilla sets ages after the spawn event.
- Starving animals never die: nothing in the mod damages a hungry animal.

## Config

`flourishing_nature-production.toml` (synced server config):

| Key | Default | What |
|---|---|---|
| `milk.enabled` | true | Milk charges on/off |
| `milk.maxCharges` | 2 | Charges an udder holds |
| `milk.ticksPerCharge` | 24000 | Refill time at product rate 1 |
| `drops.enabled` | true | Wellbeing drops and wool cap |
| `wool.enabled` | true | Slower wool regrowth |
| `breeding.enabled` | true | Love refusal, birth gate, thriving bonuses |

## Code

| Part | Location |
|---|---|
| Rules (unit tested) | `core/.../production/` (`ProductionRules`, `MilkCharges`, `MilkSettings`, `DropRules`, `DropStack`) |
| Milking, refusal shake | `neoforge/.../production/Milking.java` |
| Loot modifier | `neoforge/.../production/WellbeingDropsModifier.java` |
| Breeding | `neoforge/.../production/BreedingEffects.java` |
| Wool regrowth | `neoforge/.../production/WoolGrowth.java` |
| Registration | `neoforge/.../production/ProductionEvents.java` |

## Tests

`neoforge/.../gametest/ProductionGameTests.java`: two buckets then a refusal; refill by mood and
hunger after a day; mooshroom stew uses charges; starving cows drop exactly 1 beef; thriving cows
drop an extra roll; starving sheep give 1 wool; starving sheep don't regrow wool (fed ones do);
a hungry cow eats wheat but doesn't fall in love (a fed one does); thriving parents rest less and
their calf grows faster; a starving cow never dies. Milk refill test hooks: `Milking.setCharges`,
`Milking.charges`.

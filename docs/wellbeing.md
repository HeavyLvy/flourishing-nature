# Wellbeing (System 2)

Each herd animal has a **wellbeing** score from 0 to 100, built from six needs scored 0 to 1
and weighted (design doc cow weights, used for every species for now):

| Need | Weight | How it is checked |
|---|---|---|
| Biome | 15 | 1.0 in a preferred biome, 0.5 tolerated, 0.1 elsewhere. Biome tags per species: `#flourishing_nature:wellbeing/preferred/minecraft/<species>` and `.../tolerated/...` |
| Sky | 15 | Rolling memory of seeing the sky at its head or within 4 blocks |
| Shelter | 10 | Only matters while rain or snow falls in its biome: 1.0 with a roof within 8 blocks, 0.2 exposed (rolling memory) |
| Food | 25 | Half fullness (full from 0.7), half edible plants of its diet within 8 blocks (16 is a full score); a hand-fed meal can lift it, fading over a day. See [herbivores.md](herbivores.md) |
| Space | 20 | Mobs within 3 blocks: full up to 4, 0 at 8, and 0 at vanilla's cramming limit |
| Social | 15 | Full when the herd it feels it has is within one of its preference, falling off either side |

- Checked every 5 seconds per animal, staggered by entity id.
- The score drifts toward its target by 1 point per 30 seconds, so a better pen pays off over
  minutes and one bad night doesn't ruin it. A newly checked animal starts at its target.
- Moods: 80+ thriving, 50 to 79 content, 25 to 49 stressed, under 25 miserable.
- Saved on the animal as the `flourishing_nature:wellbeing` attachment.

Effects on milk, drops, wool and breeding: see [production.md](production.md).

## Seeing it

- `/flourishing inspect` prints the nearest animal's wellbeing, mood and every need score.
- `/flourishing herd_debug` name tags now include wellbeing and mood.

## Code

| Part | Location |
|---|---|
| Rules (pure Java, unit tested) | `core/.../core/wellbeing/` |
| World sensing | `neoforge/.../wellbeing/WorldSenses.java` |
| Checks, feeding, inspect command | `neoforge/.../wellbeing/WellbeingEvents.java` |
| Tags | `neoforge/src/main/resources/data/flourishing_nature/tags/` |
| In-game tests | `neoforge/.../gametest/WellbeingGameTests.java` (open pasture is content; a dark, crammed barn is miserable) |

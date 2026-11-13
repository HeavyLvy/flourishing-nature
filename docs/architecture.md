# Architecture

Think of the mod as a translator and an author. `core` is the author: it writes the rules of
herding and wellbeing in plain Java and knows nothing about Minecraft. `neoforge` is the
translator: it reads the game world, turns it into `core`'s vocabulary, and applies
`core`'s decisions back to the game.

```
Minecraft / NeoForge  ──►  neoforge (boundary)  ──►  core (pure logic)
       nulls, raw ints        Optional, records         records only
```

## Modules

### `core`
- Plain Java 25. Depends only on JSpecify.
- Holds the domain: social preference, herd membership, wellbeing factors and the score,
  and how the score scales drops/milking/breeding.
- Inputs are snapshots (records) such as "this mob's nearby herd mates" or "this position's
  biome category, sky access, shelter". Outputs are decisions (records).
- Fully unit tested. No game launch needed to test a rule.

### `neoforge`
- `@Mod` entrypoint, registries, event listeners, entity data attachments, goals/AI.
- **Boundary layer**: the only code allowed to touch values that Minecraft may return as
  null. It converts them to `Optional` or a record before anything else sees them.
- Bundles `core`'s classes into its jar, so players install a single file.

## Adding a loader later

A Fabric port adds a `fabric/` module that depends on `core` and reimplements the boundary.
Nothing in `core` changes. That is the test for whether code belongs in `core`: would a
Fabric module need it unchanged?

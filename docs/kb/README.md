# Flourishing Nature knowledge base

Reference notes for building the mod on **Minecraft 26.3 + NeoForge**, written for both humans and Claude Code.
Think of this folder as the field guide, and Minecraft's own decompiled source (see `minecraft-source.md`) as the actual forest: the guide tells you where to look, the source is the ground truth.

## Verified toolchain (checked 2026-11-13)

| Thing | Value | Source |
|---|---|---|
| Minecraft | `26.3` (release) | MDK `gradle.properties` |
| NeoForge | `26.3.0.103` is the latest non-beta build on Maven and what this repo pins; the official MDK pins `26.3.0.102` | maven.neoforged.net metadata, MDK |
| NeoForge version scheme | `<mc major>.<mc minor>.<mc hotfix>.<neo build>`, e.g. `26.3.0.102` | NeoForge 26.1 release post |
| Java | **25** (`JavaLanguageVersion.of(25)`) | MDK `build.gradle`, NeoForge docs |
| Gradle | `9.2.1` (wrapper in MDK; 9.1.0+ required) | MDK wrapper |
| ModDevGradle plugin | `net.neoforged.moddev` `2.0.148` (this repo and the MDK) | MDK `build.gradle` |
| Obfuscation | **None.** Mojang ships unobfuscated jars since 26.1, so code uses Mojang's real names, including parameter names. Parchment is optional (only adds javadoc). | NeoForge 26.1 release post |
| Official MDK | `github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle` | GitHub |
| NeoForge docs | `docs.neoforged.net` (labelled 26.1, still the current docs line) | docs site |
| Porting primers | `docs.neoforged.net/primer/docs/26.1/`, `/26.2/`, `/26.3/` | docs site |

Re-check these before bumping versions. `minecraft_version_range=[26.3]` means the mod only loads on exactly 26.3.

## Files

| File | What it covers |
|---|---|
| [minecraft-source.md](minecraft-source.md) | Getting the full vanilla source on disk, the legal line, and a map of the packages that matter |
| [neoforge-essentials.md](neoforge-essentials.md) | Mod entrypoint, the two event buses, registration, config, datagen, run tasks |
| [entity-ai-and-pathfinding.md](entity-ai-and-pathfinding.md) | Goals vs. Brain, writing goals, attaching goals to vanilla animals, navigation, herding references |
| [entity-data-and-persistence.md](entity-data-and-persistence.md) | Data attachments for per-animal state, saving, syncing to client |
| [spawning-biomes-weather.md](spawning-biomes-weather.md) | Natural spawning, biome modifiers, spawn events, weather and environment attributes |
| [loot-breeding-interactions.md](loot-breeding-interactions.md) | Drops (loot tables, global loot modifiers, events), milking/shearing, breeding |
| [null-safety.md](null-safety.md) | How to have "no null" in our code while Minecraft's API still uses null |
| [porting-notes-26x.md](porting-notes-26x.md) | Renames and API shifts from 1.21.x to 26.3 that trip up old tutorials |

## Confidence labels

Each note marks claims as **[verified]** (checked against current docs, MDK or Maven on the date above) or **[check source]** (from knowledge of recent versions; confirm in `.mc-sources/` before relying on it). Mojang renames things every release, so when a note and the source disagree, the source wins and the note should be fixed.

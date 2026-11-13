# AGENTS.md

Rules for any AI agent (Claude Code, Codex, etc.) or human working in this repo.
Read this first, then the docs it links to.

## What this is

Flourishing Nature is a NeoForge mod for Minecraft **26.3** (Java 25). Animals form herds
based on a per-mob social preference, and a wellbeing score (biome, sky, shelter, food,
space, social) scales drops, milking and breeding.

## Layout

| Path | What lives there |
|---|---|
| `core/` | Loader-independent logic. **Never** imports `net.minecraft` or `net.neoforged`. Unit tested with JUnit. |
| `neoforge/` | NeoForge entrypoint, registration, events, and the null boundary that adapts Minecraft to `core`. |
| `docs/` | Project docs and the Minecraft/NeoForge knowledge base. Start at `docs/README.md`. |

Details: [docs/architecture.md](docs/architecture.md).

## Commands

```sh
./gradlew build                       # compile + null checks + tests, all modules
./gradlew :core:test                  # fast unit tests for core logic
./gradlew :neoforge:runClient         # dev client
./gradlew :neoforge:runGameTestServer # headless in-game tests, exits when done
```

Gradle downloads Java 25 itself. Do not install JDKs system-wide. On Arch, ask before any `pacman` install.

## Hard rules

1. **Java only.** No Kotlin, no Groovy in `src/`.
2. **No null.** Every package has `@NullMarked` in `package-info.java`. Missing values are
   `Optional<T>`. `core` may not contain the word `null` at all (`checkNoNull` enforces it).
   Only the boundary classes in `neoforge` may compare Minecraft results to `null`, and they
   convert to `Optional` immediately. See [docs/code-standards.md](docs/code-standards.md).
3. **Strict types.** Domain values get their own `record` (e.g. `HerdSize`, `Wellbeing`) with
   validation in the compact constructor. No raw `int`/`double` passed around for meaning.
4. **Warnings are errors.** `-Xlint:all -Werror` plus Error Prone. Fix the cause; do not
   suppress without a comment explaining why.
5. **Never commit Mojang's code.** Decompiled sources are generated locally by Gradle under
   `build/` and `~/.gradle`. Docs may quote short snippets and give class/method names only.
6. **Build must pass before every commit.** Run `./gradlew build`.

## Git workflow (summary)

Full rules: [docs/git-workflow.md](docs/git-workflow.md).

- `main` always builds. Never commit directly to `main`.
- One branch per change: `feat/…`, `fix/…`, `refactor/…`, `docs/…`, `test/…`, `chore/…`.
- Conventional Commits: `feat(herding): add social preference roll`.
- Small commits that each build. Open a PR to merge into `main`; squash merge.
- Agents commit on their branch as they work, but push and open PRs only when the owner asks
  or the task says so.

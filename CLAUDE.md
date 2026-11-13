# CLAUDE.md

@AGENTS.md

Claude Code specifics:

- The knowledge base for Minecraft 26.3 and NeoForge lives under `docs/`. Check it before
  guessing at an API.
- To verify in-game behavior, prefer `./gradlew :neoforge:runGameTestServer` (headless,
  scriptable) over launching the client.

## Minecraft and NeoForge knowledge

- Read docs/kb/README.md before working on gameplay code; it indexes notes per system.
- Ground truth for vanilla behaviour is the decompiled source in `.mc-sources/` (run `./scripts/extract-mc-sources.sh` if it is missing or `.mc-sources/.neo_version` differs from `neo_version` in gradle.properties). Search it with `rg` before guessing an API name; most online tutorials target 1.21.1 and use old names.
- Never commit, paste whole classes from, or copy files out of `.mc-sources/`. It is Mojang's copyrighted code.
- Claims marked [check source] in docs/kb are unverified for 26.3. Confirm them in `.mc-sources/`, and update the note (switch to [verified]) when you do.
- Null policy and strict typing rules: docs/kb/null-safety.md.

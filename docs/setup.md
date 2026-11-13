# Setup

## Requirements

- Any JDK 17+ to run Gradle (Arch: `jdk21-openjdk` works). Gradle's toolchain support
  downloads **Java 25** for compiling and running the game via the foojay resolver.
- Git, and optionally `gh` for PRs.

## First build

```sh
./gradlew build
```

The first run takes a few minutes: it downloads Minecraft 26.3, NeoForge, and decompiles
and recompiles the game (cached in `~/.gradle/caches/neoformruntime`).

## Running

| Task | Purpose |
|---|---|
| `./gradlew :neoforge:runClient` | Dev client. Game files go in `neoforge/run/`. |
| `./gradlew :neoforge:runServer` | Dedicated server, no GUI. |
| `./gradlew :neoforge:runGameTestServer` | Runs gametests headlessly, exits with a result code. |
| `./gradlew :neoforge:runData` | Data generators, output to `neoforge/src/generated/resources/`. |

## Browsing Minecraft source

Minecraft's decompiled, NeoForge-patched sources are generated locally. Run
`./gradlew :neoforge:neoForgeIdeSync` (or import in VS Code / IntelliJ) and the sources jar
is attached to the `minecraft` dependency. They are never committed.

## Versions

All versions are pinned in `gradle.properties`. Find new NeoForge builds at
https://projects.neoforged.net/neoforged/neoforge.

## Known harmless log noise

- `Error while loading the narrator ... libflite`: the text-to-speech library isn't
  installed. Ignore it, or install `flite` if you want the narrator.

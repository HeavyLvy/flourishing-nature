# NeoForge essentials (26.3)

Analogy: Minecraft is a theatre, NeoForge is the stage manager. You don't rewrite the play; you hand the stage manager cues ("when an animal spawns, do this") and props (registered entities, items, data).

## Project files in this repo [verified]

- `gradle.properties`: `minecraft_version`, `neo_version`, `mod_id`, `mod_name`, `mod_version`, `mod_group_id`, plus checker versions.
- `build.gradle` (root): shared Java 25 toolchain and strictness settings for all modules.
- `neoforge/build.gradle`: plugin `net.neoforged.moddev` 2.0.148, run configs `client`, `server`, `gameTestServer`, `data`.
- `neoforge/src/main/templates/META-INF/neoforge.mods.toml`: mod metadata template; Gradle's `generateModMetadata` fills in the properties.
- `neoforge/src/generated/resources/`: datagen output, included in the jar.

Tasks: `./gradlew build` (jar in `neoforge/build/libs`), `./gradlew :neoforge:runClient`, `:neoforge:runServer`, `:neoforge:runData`, `:neoforge:runGameTestServer`.
Run folder is `neoforge/run/`. Server first run: accept `neoforge/run/eula.txt`, set `online-mode=false` in `server.properties`. The docs recommend testing on a dedicated server, since server/client separation bugs only show up there.

## Entry point [verified]

```java
@Mod(ModIdentity.MOD_ID)
public final class FlourishingNatureNeoForge {
    public FlourishingNatureNeoForge(IEventBus modBus, ModContainer container) {
        ModAttachments.REGISTER.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, FnConfig.SPEC);
    }
}
```

`ModIdentity.MOD_ID` (`"flourishing_nature"`) lives in `core`.

## Two event buses [verified]

| Bus | What fires on it | How to listen |
|---|---|---|
| Mod bus (`IEventBus` passed to constructor) | Startup/lifecycle: registration, `EntityAttributeCreationEvent`, `EntityAttributeModificationEvent`, `RegisterSpawnPlacementsEvent`, `GatherDataEvent`, `ModConfigEvent.*`, `FMLCommonSetupEvent`. Events implement `IModBusEvent`; many run in parallel, so use `event.enqueueWork(...)` for main-thread work. | `modBus.addListener(MyClass::onX)` |
| Game bus (`NeoForge.EVENT_BUS`) | Gameplay: entity joins level, ticks, interactions, drops, deaths, spawns. | `NeoForge.EVENT_BUS.addListener(...)` or `@SubscribeEvent` |

`@EventBusSubscriber(modid = ModIdentity.MOD_ID)` on a class auto-registers its `static @SubscribeEvent` methods on the right bus. Client-only: `@EventBusSubscriber(value = Dist.CLIENT, modid = ...)`.
Priorities: `EventPriority.HIGHEST … LOWEST`. Cancellable events implement `ICancellableEvent` (`setCanceled`, `isCanceled`).

## Gameplay events this mod will use [check source for exact names]

All under `net.neoforged.neoforge.event`.

| Need | Event |
|---|---|
| Add goals / initial data to vanilla animals when loaded or spawned | `EntityJoinLevelEvent` (fires on load too, not just first spawn; check `event.loadedFromDisk()`) |
| Act on first natural spawn (set initial wellbeing, herd id) | `FinalizeSpawnEvent` |
| Allow/deny a spawn at a position | `MobSpawnEvent.PositionCheck`, `MobSpawnEvent.SpawnPlacementCheck` |
| Per-tick logic | `EntityTickEvent.Pre` / `.Post`, `LevelTickEvent.Post`, `ServerTickEvent.Post` |
| Player right-clicks an animal (milking, feeding, petting) | `PlayerInteractEvent.EntityInteract` (and `EntityInteractSpecific`) |
| Breeding outcome | `BabyEntitySpawnEvent` (cancellable; can change the child) |
| Taming | `AnimalTameEvent` |
| Damage and death | `LivingIncomingDamageEvent`, `LivingDamageEvent.Pre/Post`, `LivingDeathEvent` |
| Drops | `LivingDropsEvent`, `LivingExperienceDropEvent` |
| Attribute changes on vanilla mobs | `EntityAttributeModificationEvent` (mod bus) |

Rule of thumb: do gameplay work only on the logical server (`!level.isClientSide()`), and send results to the client via attachment sync or a payload.

## Registration [verified]

```java
public static final DeferredRegister<AttachmentType<?>> REGISTER =
    DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModIdentity.MOD_ID);

public static final DeferredRegister.Entities ENTITY_TYPES =
    DeferredRegister.createEntities(ModIdentity.MOD_ID);
```

`EntityType.Builder#build` takes a `ResourceKey<EntityType<?>>`; `ENTITY_TYPES.registerEntityType("name", Ctor::new, MobCategory.CREATURE)` builds it for you. IDs are `Identifier.fromNamespaceAndPath(modId, path)`.

## Config (`ModConfigSpec`) [verified]

```java
public final class FnConfig {
    public static final FnConfig INSTANCE;
    public static final ModConfigSpec SPEC;
    static {
        Pair<FnConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(FnConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    public final ModConfigSpec.IntValue herdRadius;

    private FnConfig(ModConfigSpec.Builder b) {
        b.push("herding");
        herdRadius = b.comment("Blocks an animal looks for herd mates").defineInRange("herd_radius", 16, 4, 64);
        b.pop();
    }
}
```

Types: `STARTUP` (read at registration, not synced), `CLIENT`, `COMMON` (not synced), `SERVER` (per-world override in `serverconfig/`, synced to clients). Gameplay tuning for this mod belongs in `SERVER`. Listen for `ModConfigEvent.Loading` / `.Reloading` on the mod bus.

Config is for numbers players tweak. Anything shaped like content (which animals herd, what they eat, wellbeing modifiers per biome) should be **datapack JSON** loaded through a datapack registry or a reload listener, so packs can override it.

## Datagen

Run `./gradlew :neoforge:runData`. Providers register in a `GatherDataEvent` listener (mod bus). Use it for loot tables, biome modifiers, tags, lang, and global loot modifiers instead of hand-writing JSON.

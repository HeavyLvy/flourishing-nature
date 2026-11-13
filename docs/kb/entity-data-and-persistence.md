# Per-animal data and persistence

Analogy: a data attachment is a backpack NeoForge straps onto any entity, chunk or level. Vanilla doesn't know it's there, but it gets saved and loaded with the entity.

Use attachments for wellbeing stats, herd membership, memories of the player, etc. on **vanilla** animals. Don't subclass vanilla animals for this.

## Registering [verified]

```java
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModIdentity.MOD_ID);

    public static final Supplier<AttachmentType<Wellbeing>> WELLBEING = REGISTER.register(
        "wellbeing",
        () -> AttachmentType.builder(() -> Wellbeing.DEFAULT)
            .serialize(Wellbeing.CODEC.fieldOf("wellbeing"))
            .sync(Wellbeing.STREAM_CODEC)
            .build());
}
```

- No `serialize(...)` → not saved (good for caches).
- `AttachmentType.serializable(...)` for types that save via `ValueInput`/`ValueOutput` themselves.
- `copyOnDeath` exists on the builder for player data (only with a serializer). Irrelevant for animals.
- `.sync(StreamCodec)` sends the whole value to tracking clients on change; `.sync(predicate, codec)` limits who gets it; a custom `AttachmentSyncHandler` can send deltas. Call `holder.syncData(TYPE)` to resend manually.

## Reading and writing [verified]

```java
Wellbeing w = cow.getData(ModAttachments.WELLBEING);   // creates the default if absent, never null
cow.setData(ModAttachments.WELLBEING, w.withHunger(0.5f));
boolean has = cow.hasData(ModAttachments.WELLBEING);
```

[check source] `getExistingData(TYPE)` returns `Optional<T>` when you don't want to create the default.

## Make the value an immutable record

```java
public record Wellbeing(float hunger, float social, float comfort) {
    public static final Wellbeing DEFAULT = new Wellbeing(1f, 1f, 1f);
    public static final Codec<Wellbeing> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("hunger").forGetter(Wellbeing::hunger),
        Codec.FLOAT.fieldOf("social").forGetter(Wellbeing::social),
        Codec.FLOAT.fieldOf("comfort").forGetter(Wellbeing::comfort)
    ).apply(i, Wellbeing::new));
    public static final StreamCodec<ByteBuf, Wellbeing> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, Wellbeing::hunger,
        ByteBufCodecs.FLOAT, Wellbeing::social,
        ByteBufCodecs.FLOAT, Wellbeing::comfort,
        Wellbeing::new);
    public Wellbeing withHunger(float v) { return new Wellbeing(v, social, comfort); }
}
```

Immutable records plus `setData` make every change explicit, which also makes sync reliable (mutating in place won't trigger a sync). Use `Codec.optionalFieldOf(..., default)` for fields added later so old saves still load.

In this repo the domain record itself lives in `core` (no Minecraft imports), so its `Codec`/`StreamCodec` go in a `neoforge` class (e.g. `WellbeingCodecs`) rather than on the record.

## Herd-level data

A herd spans many entities, so store it in level-wide `SavedData` (`ServerLevel#getDataStorage()`, `SavedDataType` with a codec) or a level attachment, keyed by a herd `UUID`; each animal's attachment stores only its herd `UUID`. Never store `Entity` references in saved data; store UUIDs and look them up (`ServerLevel#getEntity(UUID)` returns nullable, wrap it).

## Custom entities

If you ever add your own mob class, save extra fields in `addAdditionalSaveData(ValueOutput)` / `readAdditionalSaveData(ValueInput)` [check source]. Since 1.21.6 these replaced the raw `CompoundTag` versions. `ValueInput` has `read(key, codec)` returning `Optional<T>` and `getIntOr(key, default)`-style getters, which suit the no-null rule.

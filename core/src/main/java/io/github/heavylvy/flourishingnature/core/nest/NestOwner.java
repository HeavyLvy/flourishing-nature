package io.github.heavylvy.flourishingnature.core.nest;

import java.util.UUID;

/** Who a nest belongs to. One nest per hen; a nest with no living owner can be claimed. */
public sealed interface NestOwner {
    /** Free to claim. */
    record Unowned() implements NestOwner {}

    /** Claimed by the hen with this entity id. */
    record Hen(UUID id) implements NestOwner {}

    default boolean isOwnedBy(UUID hen) {
        return this instanceof Hen owner && owner.id().equals(hen);
    }
}

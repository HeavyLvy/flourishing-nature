package io.github.heavylvy.flourishingnature.core.production;

/**
 * One stack of an animal's death drops, as the drop rules see it.
 *
 * @param meat whether it counts as the guaranteed minimum drop (meat)
 * @param count items in the stack, 0 or more
 */
public record DropStack(boolean meat, int count) {
    public DropStack {
        if (count < 0) {
            throw new IllegalArgumentException("count must be 0 or more: " + count);
        }
    }

    public DropStack withCount(int newCount) {
        return new DropStack(meat, newCount);
    }
}

package io.github.heavylvy.flourishingnature.core.production;

/**
 * How milk charges work: an udder holds up to {@code maxCharges}, and one charge refills in
 * {@code ticksPerCharge} at product rate 1 (rate 2 halves it, rate 0 never refills).
 *
 * @param maxCharges 1 or more
 * @param ticksPerCharge 1 or more game ticks
 */
public record MilkSettings(int maxCharges, long ticksPerCharge) {
    /** Design doc: 2 charges, one per in-game day for a content, fed animal. */
    public static final MilkSettings DEFAULT = new MilkSettings(2, 24_000L);

    public MilkSettings {
        if (maxCharges < 1) {
            throw new IllegalArgumentException("max charges must be 1 or more: " + maxCharges);
        }
        if (ticksPerCharge < 1) {
            throw new IllegalArgumentException("ticks per charge must be 1 or more: " + ticksPerCharge);
        }
    }
}

package io.github.heavylvy.flourishingnature.core.herbivore;

import java.util.List;

/**
 * When a herd moves its home for food.
 *
 * @param scarceAt a home with this much food or less around it has run out
 * @param foodCheckInterval ticks between food checks per herd
 * @param patchRadii distances from home at which the herd looks for a new patch
 * @param patchHalfWidth each looked-at patch is a square this many blocks either side of its centre
 */
public record HomeSettings(int scarceAt, int foodCheckInterval, List<Double> patchRadii, int patchHalfWidth) {

    public static final HomeSettings DEFAULT = new HomeSettings(2, 400, List.of(12.0, 20.0), 3);

    public HomeSettings {
        if (scarceAt < 0 || foodCheckInterval < 1 || patchRadii.isEmpty() || patchHalfWidth < 0
                || patchRadii.stream().anyMatch(r -> !(r > 0.0))) {
            throw new IllegalArgumentException("bad home settings");
        }
        patchRadii = List.copyOf(patchRadii);
    }
}

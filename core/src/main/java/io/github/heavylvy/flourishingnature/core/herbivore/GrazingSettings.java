package io.github.heavylvy.flourishingnature.core.herbivore;

/**
 * Grazing tuning. Distances in blocks, times in ticks.
 *
 * @param grazeBelow an animal looks for food while its fullness is under this
 * @param veryHungryBelow cows crop grass blocks to dirt under this fullness
 * @param searchRadius how far an animal looks for food
 * @param searchInterval ticks between food searches
 * @param ediblePlantsReach wellbeing counts edible plants within this radius
 */
public record GrazingSettings(
        double grazeBelow, double veryHungryBelow, int searchRadius, int searchInterval, int ediblePlantsReach) {

    public static final GrazingSettings DEFAULT = new GrazingSettings(0.9, 0.25, 8, 100, 8);

    public GrazingSettings {
        if (!(grazeBelow > 0.0 && grazeBelow <= 1.0) || !(veryHungryBelow >= 0.0 && veryHungryBelow <= grazeBelow)) {
            throw new IllegalArgumentException("graze thresholds must be in (0, 1] and ordered");
        }
        if (searchRadius < 1 || searchInterval < 1 || ediblePlantsReach < 1) {
            throw new IllegalArgumentException("grazing radii and intervals must be positive");
        }
    }
}

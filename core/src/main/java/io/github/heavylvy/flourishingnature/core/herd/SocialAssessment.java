package io.github.heavylvy.flourishingnature.core.herd;

/**
 * The result of one social scan.
 *
 * @param socialCount sum of social weights of friendly neighbors within the herd radius, i.e. how
 *     many herd mates the mob feels it has
 * @param decision what to do about it
 */
public record SocialAssessment(double socialCount, HerdDecision decision) {
    public SocialAssessment {
        if (!(socialCount >= 0.0)) {
            throw new IllegalArgumentException("social count must be 0 or more: " + socialCount);
        }
    }
}

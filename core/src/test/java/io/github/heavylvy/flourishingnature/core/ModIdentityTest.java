package io.github.heavylvy.flourishingnature.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ModIdentityTest {
    @Test
    void modIdIsValidForNeoForge() {
        assertTrue(ModIdentity.MOD_ID.matches("[a-z][a-z0-9_]{1,63}"));
    }
}

/**
 * The airlock between Minecraft and our code. Minecraft hands out nulls and raw types; everything
 * here converts them to {@code Optional} and core records before anything else sees them. This is
 * the only package allowed to deal with null.
 */
@NullMarked
package io.github.heavylvy.flourishingnature.neoforge.boundary;

import org.jspecify.annotations.NullMarked;

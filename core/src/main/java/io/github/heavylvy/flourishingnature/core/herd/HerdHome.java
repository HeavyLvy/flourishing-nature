package io.github.heavylvy.flourishingnature.core.herd;

/**
 * Where a herd hangs out. Members graze around the anchor; roaming herds move it every minute or
 * two, settled herds only shuffle it a few blocks now and then.
 *
 * @param anchor the herd's home spot, always dry ground under open sky when the world allows
 * @param roamer whether this herd travels
 * @param nextMove game tick when the anchor moves next
 */
public record HerdHome(Position anchor, boolean roamer, long nextMove) {}

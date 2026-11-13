package io.github.heavylvy.flourishingnature.neoforge.wellbeing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.heavylvy.flourishingnature.core.wellbeing.Need;
import io.github.heavylvy.flourishingnature.core.wellbeing.NeedScore;
import io.github.heavylvy.flourishingnature.core.wellbeing.NeedScores;
import io.github.heavylvy.flourishingnature.core.wellbeing.RollingMemory;
import io.github.heavylvy.flourishingnature.core.wellbeing.Wellbeing;
import io.github.heavylvy.flourishingnature.core.wellbeing.WellbeingState;
import java.util.EnumMap;
import java.util.Map;

/** Save formats for core's wellbeing records, which can't depend on Mojang's codec library. */
final class WellbeingCodecs {
    private WellbeingCodecs() {}

    private static final Codec<Double> UNIT = Codec.doubleRange(0.0, 1.0);

    private static final Codec<NeedScores> NEEDS = RecordCodecBuilder.create(i -> i.group(
                    UNIT.fieldOf("biome").forGetter(n -> n.of(Need.BIOME).value()),
                    UNIT.fieldOf("sky").forGetter(n -> n.of(Need.SKY).value()),
                    UNIT.fieldOf("shelter").forGetter(n -> n.of(Need.SHELTER).value()),
                    UNIT.fieldOf("food").forGetter(n -> n.of(Need.FOOD).value()),
                    UNIT.fieldOf("space").forGetter(n -> n.of(Need.SPACE).value()),
                    UNIT.fieldOf("social").forGetter(n -> n.of(Need.SOCIAL).value()))
            .apply(i, WellbeingCodecs::needs));

    static final MapCodec<WellbeingState> STATE = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.doubleRange(0.0, Wellbeing.MAX).fieldOf("wellbeing").forGetter(s -> s.wellbeing().value()),
                    NEEDS.fieldOf("needs").forGetter(WellbeingState::needs),
                    UNIT.fieldOf("sky_memory").forGetter(s -> s.sky().value()),
                    UNIT.fieldOf("shelter_memory").forGetter(s -> s.shelter().value()))
            .apply(i, (w, needs, sky, shelter) ->
                    new WellbeingState(new Wellbeing(w), needs, new RollingMemory(sky), new RollingMemory(shelter))));

    static final MapCodec<Long> GAME_TICK = Codec.LONG.fieldOf("tick");

    private static NeedScores needs(double biome, double sky, double shelter, double food, double space, double social) {
        Map<Need, NeedScore> scores = new EnumMap<>(Need.class);
        scores.put(Need.BIOME, new NeedScore(biome));
        scores.put(Need.SKY, new NeedScore(sky));
        scores.put(Need.SHELTER, new NeedScore(shelter));
        scores.put(Need.FOOD, new NeedScore(food));
        scores.put(Need.SPACE, new NeedScore(space));
        scores.put(Need.SOCIAL, new NeedScore(social));
        return new NeedScores(scores);
    }
}

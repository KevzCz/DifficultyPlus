package net.pixeldreamstudios.difficultyplus.item.bias;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public record BiasDefinition(ResourceLocation id,
                             String name,
                             int colour,
                             int weight,
                             String rarity,
                             int minBias,
                             int maxBias,
                             boolean pierceFloor,
                             boolean pierceCeiling,
                             int minZoneLevel) {

    public static final BiasDefinition PLAIN = new BiasDefinition(
            ResourceLocation.fromNamespaceAndPath("difficultyplus", "plain"),
            "item.difficultyplus.bias.plain",
            0xFFC43D3D,
            1,
            "common",
            0,
            0,
            false,
            false,
            0);

    public int roll(RandomSource random) {
        if (maxBias <= minBias) {
            return minBias;
        }
        return minBias + random.nextInt(maxBias - minBias + 1);
    }
}

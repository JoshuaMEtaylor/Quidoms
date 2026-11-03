package com.quidoms.build;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Clean-room schematic format (JSON under data/&lt;ns&gt;/quidoms/schematics).
 * Not Structurize - local palette + relative positions only.
 */
public record Schematic(
        ResourceLocation id,
        String displayName,
        int sizeX,
        int sizeY,
        int sizeZ,
        List<BlockEntry> blocks
) {
    public record BlockEntry(int x, int y, int z, ResourceLocation block) {
        public static final Codec<BlockEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(BlockEntry::x),
                Codec.INT.fieldOf("y").forGetter(BlockEntry::y),
                Codec.INT.fieldOf("z").forGetter(BlockEntry::z),
                ResourceLocation.CODEC.fieldOf("block").forGetter(BlockEntry::block)
        ).apply(instance, BlockEntry::new));
    }

    public static final Codec<Schematic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(Schematic::id),
            Codec.STRING.fieldOf("displayName").forGetter(Schematic::displayName),
            Codec.INT.fieldOf("sizeX").forGetter(Schematic::sizeX),
            Codec.INT.fieldOf("sizeY").forGetter(Schematic::sizeY),
            Codec.INT.fieldOf("sizeZ").forGetter(Schematic::sizeZ),
            BlockEntry.CODEC.listOf().fieldOf("blocks").forGetter(Schematic::blocks)
    ).apply(instance, Schematic::new));
}

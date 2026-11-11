package com.ldtteam.domumornamentum.client.model;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * NeoForge 1.21.10 port of Domum's materially_textured geometry loader.
 * Resolves to the declared parent ({@code *_spec}) model; material remapping
 * is applied later via {@link MateriallyTexturedBlockStateModel}.
 */
@OnlyIn(Dist.CLIENT)
public class MateriallyTexturedUnbakedModel implements UnbakedModel
{
    private final ResourceLocation parent;

    public MateriallyTexturedUnbakedModel(final ResourceLocation parent)
    {
        this.parent = parent;
    }

    @Override
    public ResourceLocation parent()
    {
        return parent;
    }
}

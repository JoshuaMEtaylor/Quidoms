package com.minecolonies.core.client.render;

import com.minecolonies.core.entity.other.NewBobberEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderFishHook extends QuidomsNoopEntityRenderer<NewBobberEntity>
{
    public RenderFishHook(final EntityRendererProvider.Context context)
    {
        super(context);
    }
}

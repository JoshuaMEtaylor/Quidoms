package com.minecolonies.core.client.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Invisible/no-op entity renderer for NeoForge 1.21.10 while custom renderers are ported.
 */
@OnlyIn(Dist.CLIENT)
public class QuidomsNoopEntityRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState>
{
    public QuidomsNoopEntityRenderer(final EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState()
    {
        return new EntityRenderState();
    }
}

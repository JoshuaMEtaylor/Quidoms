package com.minecolonies.core.client.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * Empty renderer for entities which should not actually be rendered.
 */
@OnlyIn(Dist.CLIENT)
public class RenderSitting<T extends Entity> extends EntityRenderer<T, EntityRenderState>
{
    public RenderSitting(final EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull final T entity, @NotNull final Frustum clippingHelper, final double x, final double y, final double z)
    {
        return false;
    }

    @Override
    public EntityRenderState createRenderState()
    {
        return new EntityRenderState();
    }
}

package com.minecolonies.core.client.render;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Temporary 1.21.10 stub: old MultiBufferSource BE rendering removed.
 * Visuals to be restored on the SubmitNodeCollector / BlockEntityRenderState pipeline.
 */
@OnlyIn(Dist.CLIENT)
public class TileEntityDecoControllerRenderer extends QuidomsNoopBlockEntityRenderer<BlockEntity>
{
    public TileEntityDecoControllerRenderer(final BlockEntityRendererProvider.Context context)
    {
        super(context);
    }
}

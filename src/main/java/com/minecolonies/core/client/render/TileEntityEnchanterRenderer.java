package com.minecolonies.core.client.render;

import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Temporary 1.21.10 stub: old MultiBufferSource BE rendering removed.
 * Visuals to be restored on the SubmitNodeCollector / BlockEntityRenderState pipeline.
 */
@OnlyIn(Dist.CLIENT)
public class TileEntityEnchanterRenderer extends QuidomsNoopBlockEntityRenderer<TileEntityColonyBuilding>
{
    public TileEntityEnchanterRenderer(final BlockEntityRendererProvider.Context context)
    {
        super(context);
    }
}

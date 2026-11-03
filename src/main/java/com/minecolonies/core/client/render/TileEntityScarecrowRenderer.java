package com.minecolonies.core.client.render;

import com.minecolonies.api.tileentities.AbstractTileEntityScarecrow;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Temporary 1.21.10 stub: old MultiBufferSource BE rendering removed.
 * Visuals to be restored on the SubmitNodeCollector / BlockEntityRenderState pipeline.
 */
@OnlyIn(Dist.CLIENT)
public class TileEntityScarecrowRenderer extends QuidomsNoopBlockEntityRenderer<AbstractTileEntityScarecrow>
{
    public TileEntityScarecrowRenderer(final BlockEntityRendererProvider.Context context)
    {
        super(context);
    }
}

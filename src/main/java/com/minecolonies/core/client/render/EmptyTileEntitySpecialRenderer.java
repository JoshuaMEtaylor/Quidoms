package com.minecolonies.core.client.render;

import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EmptyTileEntitySpecialRenderer extends QuidomsNoopBlockEntityRenderer<AbstractTileEntityColonyBuilding>
{
    public EmptyTileEntitySpecialRenderer(final BlockEntityRendererProvider.Context context)
    {
        super(context);
    }
}

package com.minecolonies.core.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Temporary no-op cavalry overlay until HorseModel/EquineRenderState port is finished.
 */
@OnlyIn(Dist.CLIENT)
public class CavalryOverlayLayer extends RenderLayer<EquineRenderState, HorseModel>
{
    public CavalryOverlayLayer(final RenderLayerParent<EquineRenderState, HorseModel> parent)
    {
        super(parent);
    }

    @Override
    public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector collector,
      final int light,
      final EquineRenderState state,
      final float limbSwing,
      final float limbSwingAmount)
    {
        // no-op stub
    }
}

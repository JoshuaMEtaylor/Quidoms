package com.minecolonies.core.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal BlockEntityRenderer for NeoForge 1.21.10 render-state pipeline.
 * Used while individual BE renderers are ported off the old MultiBufferSource API.
 */
@OnlyIn(Dist.CLIENT)
public class QuidomsNoopBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, BlockEntityRenderState>
{
    public QuidomsNoopBlockEntityRenderer(final BlockEntityRendererProvider.Context context)
    {
    }

    @Override
    public BlockEntityRenderState createRenderState()
    {
        return new BlockEntityRenderState();
    }

    @Override
    public void extractRenderState(
      final T blockEntity,
      final BlockEntityRenderState state,
      final float partialTick,
      final Vec3 cameraPos,
      final @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress)
    {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
    }

    @Override
    public void submit(
      final BlockEntityRenderState state,
      final PoseStack poseStack,
      final SubmitNodeCollector collector,
      final CameraRenderState camera)
    {
        // no-op stub
    }
}

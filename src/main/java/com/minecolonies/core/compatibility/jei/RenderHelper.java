package com.minecolonies.core.compatibility.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;

/**
 * JEI GUI rendering utility helpers.
 * <p>1.21.10: GuiGraphics uses {@code Matrix3x2fStack} and the old PoseStack/RenderSystem
 * block-preview path is gone — stubbed until a RenderPipelines-based preview is ported.
 */
public class RenderHelper
{
    /**
     * Render a block model on a GUI (currently a no-op on NeoForge 1.21.10).
     */
    public static void renderBlock(final GuiGraphics ctx, final BlockState block, final float x, final float y, final float z, final float pitch, final float yaw, final float scale)
    {
        // TODO(1.21.10): restore 3D block preview with current GuiGraphics / pipeline APIs.
    }
}

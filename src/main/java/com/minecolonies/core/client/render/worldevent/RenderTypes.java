package com.minecolonies.core.client.render.worldevent;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class RenderTypes
{
    /**
     * Usable for rendering simple flat textures
     *
     * @param  resLoc texture location
     * @return        render type
     */
    public static RenderType worldEntityIcon(final ResourceLocation resLoc)
    {
        return InnerRenderTypes.WORLD_ENTITY_ICON.apply(resLoc);
    }

    public static final class InnerRenderTypes
    {
        private InnerRenderTypes()
        {
        }

        private static final Function<ResourceLocation, RenderType> WORLD_ENTITY_ICON = Util.memoize(texture -> {
            final RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(ResourceLocation.fromNamespaceAndPath("minecolonies", "pipeline/world_entity_icon"))
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withCull(false)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .build();
            return RenderType.create(
                "minecolonies_entity_icon",
                1024,
                false,
                false,
                pipeline,
                RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false))
                    .createCompositeState(false));
        });
    }
}

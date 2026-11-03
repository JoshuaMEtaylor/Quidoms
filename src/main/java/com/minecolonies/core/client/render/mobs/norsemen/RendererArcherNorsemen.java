package com.minecolonies.core.client.render.mobs.norsemen;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.client.model.raiders.ModelArcherNorsemen;
import com.minecolonies.core.event.ClientRegistryHandler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RendererArcherNorsemen extends AbstractRendererNorsemen<AbstractEntityMinecoloniesMonster, ModelArcherNorsemen>
{
    private static final ResourceLocation TEXTURE1 = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/norsemen_archer1.png");
    private static final ResourceLocation TEXTURE2 = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/norsemen_archer2.png");

    public RendererArcherNorsemen(final EntityRendererProvider.Context context)
    {
        super(context, new ModelArcherNorsemen(context.bakeLayer(ClientRegistryHandler.NORSEMEN_ARCHER)), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(final net.minecraft.client.renderer.entity.state.HumanoidRenderState state)
    {
        return TEXTURE1;
    }
}

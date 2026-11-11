package com.minecolonies.core.client.render.mobs.norsemen;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.client.model.raiders.ModelChiefNorsemen;
import com.minecolonies.core.event.ClientRegistryHandler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RendererChiefNorsemen extends AbstractRendererNorsemen<AbstractEntityMinecoloniesMonster, ModelChiefNorsemen>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/norsemen_chief.png");

    public RendererChiefNorsemen(final EntityRendererProvider.Context context)
    {
        super(context, new ModelChiefNorsemen(context.bakeLayer(ClientRegistryHandler.NORSEMEN_CHIEF)), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(final net.minecraft.client.renderer.entity.state.HumanoidRenderState state)
    {
        return TEXTURE;
    }
}

package com.minecolonies.core.client.render.mobs.norsemen;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.client.model.raiders.ModelShieldmaiden;
import com.minecolonies.core.event.ClientRegistryHandler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RendererShieldmaidenNorsemen extends AbstractRendererNorsemen<AbstractEntityMinecoloniesMonster, ModelShieldmaiden>
{
    private static final ResourceLocation TEXTURE1 = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/norsemen_shieldmaiden1.png");
    private static final ResourceLocation TEXTURE2 = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/norsemen_shieldmaiden2.png");

    public RendererShieldmaidenNorsemen(final EntityRendererProvider.Context context)
    {
        super(context, new ModelShieldmaiden(context.bakeLayer(ClientRegistryHandler.SHIELD_MAIDEN)), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(final net.minecraft.client.renderer.entity.state.HumanoidRenderState state)
    {
        return TEXTURE1;
    }
}

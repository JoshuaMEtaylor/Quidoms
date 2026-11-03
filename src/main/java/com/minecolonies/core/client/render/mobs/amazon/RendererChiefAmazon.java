package com.minecolonies.core.client.render.mobs.amazon;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.client.model.raiders.ModelAmazonChief;
import com.minecolonies.core.event.ClientRegistryHandler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RendererChiefAmazon extends AbstractRendererAmazon<AbstractEntityMinecoloniesMonster, ModelAmazonChief>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/amazon_chief.png");

    public RendererChiefAmazon(final EntityRendererProvider.Context context)
    {
        super(context, new ModelAmazonChief(context.bakeLayer(ClientRegistryHandler.AMAZON_CHIEF)), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(final net.minecraft.client.renderer.entity.state.HumanoidRenderState state)
    {
        return TEXTURE;
    }
}

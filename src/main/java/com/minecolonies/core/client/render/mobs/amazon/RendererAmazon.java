package com.minecolonies.core.client.render.mobs.amazon;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.client.model.raiders.ModelAmazon;
import com.minecolonies.core.event.ClientRegistryHandler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RendererAmazon extends AbstractRendererAmazon<AbstractEntityMinecoloniesMonster, ModelAmazon>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/amazon.png");

    public RendererAmazon(final EntityRendererProvider.Context context)
    {
        super(context, new ModelAmazon(context.bakeLayer(ClientRegistryHandler.AMAZON)), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(final net.minecraft.client.renderer.entity.state.HumanoidRenderState state)
    {
        return TEXTURE;
    }
}

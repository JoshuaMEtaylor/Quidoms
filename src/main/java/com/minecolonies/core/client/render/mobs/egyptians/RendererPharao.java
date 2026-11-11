package com.minecolonies.core.client.render.mobs.egyptians;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.client.model.raiders.ModelPharaoh;
import com.minecolonies.core.event.ClientRegistryHandler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RendererPharao extends AbstractRendererEgyptian<AbstractEntityMinecoloniesMonster, ModelPharaoh>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/pharao.png");

    public RendererPharao(final EntityRendererProvider.Context context)
    {
        super(context, new ModelPharaoh(context.bakeLayer(ClientRegistryHandler.PHARAO)), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(final net.minecraft.client.renderer.entity.state.HumanoidRenderState state)
    {
        return TEXTURE;
    }
}

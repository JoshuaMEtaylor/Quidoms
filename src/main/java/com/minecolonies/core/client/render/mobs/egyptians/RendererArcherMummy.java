package com.minecolonies.core.client.render.mobs.egyptians;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.client.model.raiders.ModelArcherMummy;
import com.minecolonies.core.event.ClientRegistryHandler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RendererArcherMummy extends AbstractRendererEgyptian<AbstractEntityMinecoloniesMonster, ModelArcherMummy>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/archer_mummy.png");

    public RendererArcherMummy(final EntityRendererProvider.Context context)
    {
        super(context, new ModelArcherMummy(context.bakeLayer(ClientRegistryHandler.ARCHER_MUMMY)), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(final net.minecraft.client.renderer.entity.state.HumanoidRenderState state)
    {
        return TEXTURE;
    }
}

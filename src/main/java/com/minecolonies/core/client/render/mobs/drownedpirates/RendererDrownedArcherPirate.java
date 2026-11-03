package com.minecolonies.core.client.render.mobs.drownedpirates;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RendererDrownedArcherPirate extends AbstractRendererDrownedPirate<AbstractEntityMinecoloniesMonster, HumanoidModel<net.minecraft.client.renderer.entity.state.HumanoidRenderState>>
{
    private static final ResourceLocation TEXTURE1 = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/drowned_pirate5.png");
    private static final ResourceLocation TEXTURE2 = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/drowned_pirate6.png");
    private static final ResourceLocation TEXTURE3 = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/drowned_pirate7.png");
    private static final ResourceLocation TEXTURE4 = ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/raiders/drowned_pirate8.png");

    public RendererDrownedArcherPirate(final EntityRendererProvider.Context context)
    {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(final net.minecraft.client.renderer.entity.state.HumanoidRenderState state)
    {
        return TEXTURE1;
    }
}

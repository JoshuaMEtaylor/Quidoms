package com.minecolonies.core.client.render.mobs.drownedpirates;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer used for Chief Barbarians.
 */
public class RendererDrownedChiefPirate extends AbstractRendererDrownedPirate<AbstractEntityMinecoloniesMonster, HumanoidModel<net.minecraft.client.renderer.entity.state.HumanoidRenderState>>
{
    /**
     * Texture of the entity.
     */
    private static final ResourceLocation TEXTURE1 = new ResourceLocation("minecolonies", "textures/entity/raiders/drowned_pirate_nude.png");


    /**
     * Constructor method for renderer
     *
     * @param context the renderManager
     */
    public RendererDrownedChiefPirate(final EntityRendererProvider.Context context)
    {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_ARMOR.head())), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(final net.minecraft.client.renderer.entity.state.HumanoidRenderState entity)
    {
        return TEXTURE1;
    }
}

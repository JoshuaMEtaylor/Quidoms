package com.minecolonies.core.client.render.mobs.barbarians;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract raider renderer for MC 1.21.10 EntityRenderState pipeline.
 */
public abstract class AbstractRendererBarbarian<T extends AbstractEntityMinecoloniesMonster, M extends HumanoidModel<HumanoidRenderState>>
    extends HumanoidMobRenderer<T, HumanoidRenderState, M>
{
    public AbstractRendererBarbarian(final EntityRendererProvider.Context context, final M modelBipedIn, final float shadowSize)
    {
        super(context, modelBipedIn, shadowSize);
        final ArmorModelSet<HumanoidModel<HumanoidRenderState>> armor = ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new);
        this.addLayer(new HumanoidArmorLayer<>(this, armor, context.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public HumanoidRenderState createRenderState()
    {
        return new HumanoidRenderState();
    }

    @Override
    public abstract @NotNull ResourceLocation getTextureLocation(@NotNull final HumanoidRenderState state);
}

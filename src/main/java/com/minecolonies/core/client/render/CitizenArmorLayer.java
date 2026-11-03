package com.minecolonies.core.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.ModelManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Placeholder for the legacy citizen armor layer. Vanilla {@link HumanoidArmorLayer}
 * is used from {@link RenderBipedCitizen} until custom skull/texture armor is re-ported.
 */
@OnlyIn(Dist.CLIENT)
public class CitizenArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>>
    extends HumanoidArmorLayer<S, M, A>
{
    public CitizenArmorLayer(
      final RenderLayerParent<S, M> parentLayer,
      final ArmorModelSet<A> armorModels,
      final EquipmentLayerRenderer equipmentRenderer)
    {
        super(parentLayer, armorModels, equipmentRenderer);
    }

    /** Compatibility constructor kept for any lingering call sites. */
    public CitizenArmorLayer(
      final RenderLayerParent<S, M> parentLayer,
      final A innerModel,
      final A outerModel,
      final ModelManager modelManager,
      final EntityModelSet modelSet)
    {
        super(parentLayer, new ArmorModelSet<>(innerModel, innerModel, outerModel, outerModel), null);
    }
}

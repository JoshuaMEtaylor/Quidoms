package com.quidoms.client;

import com.quidoms.entity.CitizenEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Uses the humanoid player mesh + default wide Steve skin (exists in 1.21.10 client jar).
 * Avoids PlayerModel-only parts so renderer registration cannot blow up on missing cape/ears.
 */
public class CitizenRenderer extends HumanoidMobRenderer<CitizenEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public CitizenRenderer(EntityRendererProvider.Context context) {
        // PLAYER layer includes humanoid roots HumanoidModel expects.
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}

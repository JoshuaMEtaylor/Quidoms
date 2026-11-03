package com.minecolonies.core.client.render;

import com.minecolonies.api.client.render.modeltype.CitizenModel;
import com.minecolonies.api.client.render.modeltype.CitizenRenderState;
import com.minecolonies.api.client.render.modeltype.IModelType;
import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.client.render.modeltype.registry.IModelTypeRegistry;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.apiimp.initializer.ModModelTypeInitializer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for the citizens (MC 1.21.10 EntityRenderState pipeline).
 */
public class RenderBipedCitizen extends HumanoidMobRenderer<AbstractEntityCitizen, CitizenRenderState, CitizenModel<CitizenRenderState>>
{
    private static final double SHADOW_SIZE = 0.5F;
    public static boolean isItGhostTime = false;

    public RenderBipedCitizen(final EntityRendererProvider.Context context)
    {
        super(context, new CitizenModel<>(context.bakeLayer(ModelLayers.PLAYER)), (float) SHADOW_SIZE);
        final ArmorModelSet<HumanoidModel<CitizenRenderState>> armor = ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new);
        this.addLayer(new HumanoidArmorLayer<>(this, armorModels, context.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer<>(this));
        ModModelTypeInitializer.init(context);
    }

    @Override
    public CitizenRenderState createRenderState()
    {
        return new CitizenRenderState();
    }

    @Override
    public void extractRenderState(@NotNull final AbstractEntityCitizen citizen, @NotNull final CitizenRenderState state, final float partialTicks)
    {
        super.extractRenderState(citizen, state, partialTicks);
        state.renderMetadata = citizen.getRenderMetadata() == null ? "" : citizen.getRenderMetadata();
        state.pose = citizen.getPose();
        state.mainHandEmpty = citizen.getMainHandItem().isEmpty();
        state.headSlotEmpty = citizen.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        state.civilianId = citizen.getCivilianID();
        state.female = citizen.isFemale();
        state.modelTypeId = citizen.getModelType();
        state.isGhost = isItGhostTime;
        state.bodyRotationOverride = 0.0F;

        if (citizen.getCitizenDataView() != null)
        {
            state.customTextureUUID = citizen.getCitizenDataView().getCustomTextureUUID();
            state.customTexture = citizen.getCitizenDataView().getCustomTexture();
            final ItemStack displayHead = citizen.getCitizenDataView().getDisplayArmor(EquipmentSlot.HEAD);
            state.displayHeadArmorEmpty = displayHead == null || displayHead.isEmpty();
        }
        else
        {
            state.customTextureUUID = null;
            state.customTexture = null;
            state.displayHeadArmorEmpty = true;
        }

        final IModelType modelType = IModelTypeRegistry.getInstance().getModelType(citizen.getModelType());
        state.texture = state.customTexture != null ? state.customTexture : modelType.getTexture(citizen);

        setupMainModelFrom(citizen, state);
    }

    private void setupMainModelFrom(@NotNull final AbstractEntityCitizen citizen, @NotNull final CitizenRenderState state)
    {
        final IModelType modelType = IModelTypeRegistry.getInstance().getModelType(citizen.getModelType());
        CitizenModel<CitizenRenderState> next = citizen.isFemale() ? modelType.getFemaleModel() : modelType.getMaleModel();
        if (next == null)
        {
            next = citizen.isFemale() ? modelType.getFemaleModel() : modelType.getMaleModel();
        }
        if (citizen.getCitizenDataView() != null && citizen.getCitizenDataView().getCustomTexture() != null)
        {
            next = IModelTypeRegistry.getInstance().getModelType(ModModelTypes.CUSTOM_ID).getMaleModel();
        }
        if (next != null)
        {
            this.model = next;
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull final CitizenRenderState state)
    {
        return state.texture != null ? state.texture : ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/entity/citizen/default/settlermale1.png");
    }
}

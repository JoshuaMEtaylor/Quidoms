package com.minecolonies.core.client.render;

import com.minecolonies.api.client.render.modeltype.CitizenModel;
import com.minecolonies.api.client.render.modeltype.CitizenRenderState;
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
 * Renderer for citizens (MC 1.21.10 EntityRenderState pipeline).
 * <p>
 * Quidoms: force vanilla Steve/Alex {@link ModelLayers#PLAYER} / {@link ModelLayers#PLAYER_SLIM}
 * geometry and default player skins. Job-specific CitizenModel meshes and pack textures are
 * bypassed client-side only — entity spawn, jobs, and AI are unchanged.
 */
public class RenderBipedCitizen extends HumanoidMobRenderer<AbstractEntityCitizen, CitizenRenderState, CitizenModel<CitizenRenderState>>
{
    private static final double SHADOW_SIZE = 0.5F;

    /** Vanilla default wide (Steve-arm) skins. */
    private static final String[] WIDE_SKINS = {
      "steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"
    };

    /** Vanilla default slim (Alex-arm) skins. */
    private static final String[] SLIM_SKINS = {
      "alex", "steve", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"
    };

    public static boolean isItGhostTime = false;

    private final CitizenModel<CitizenRenderState> wideModel;
    private final CitizenModel<CitizenRenderState> slimModel;

    public RenderBipedCitizen(final EntityRendererProvider.Context context)
    {
        super(context, new CitizenModel<>(context.bakeLayer(ModelLayers.PLAYER)), (float) SHADOW_SIZE);
        this.wideModel = this.model;
        this.slimModel = new CitizenModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM));
        final ArmorModelSet<HumanoidModel<CitizenRenderState>> armor =
          ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new);
        this.addLayer(new HumanoidArmorLayer<>(this, armor, context.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer<>(this));
        // Keep job model registry init for other systems / GUIs that may query model types.
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

        // Prefer downloaded custom player skin when present; otherwise vanilla default skins.
        // Full-body player skins draw the head from the same texture — clear UUID so CitizenModel
        // does not hide head/hat (legacy path assumed a separate head overlay layer).
        if (state.customTexture != null)
        {
            state.texture = state.customTexture;
            state.customTextureUUID = null;
        }
        else
        {
            state.texture = vanillaPlayerSkin(citizen);
            state.customTextureUUID = null;
        }

        setupMainModelFrom(citizen, state);
    }

    private void setupMainModelFrom(@NotNull final AbstractEntityCitizen citizen, @NotNull final CitizenRenderState state)
    {
        if (citizen.getCitizenDataView() != null && citizen.getCitizenDataView().getCustomTexture() != null)
        {
            // Custom skins are standard player-format; wide PLAYER mesh is the safe default.
            final CitizenModel<CitizenRenderState> custom =
              IModelTypeRegistry.getInstance().getModelType(ModModelTypes.CUSTOM_ID).getMaleModel();
            this.model = custom != null ? custom : wideModel;
            return;
        }

        // Female → slim (Alex arms); male → wide (Steve arms).
        this.model = citizen.isFemale() ? slimModel : wideModel;
    }

    /**
     * Pick a vanilla default player skin from textureId / gender.
     */
    private static ResourceLocation vanillaPlayerSkin(@NotNull final AbstractEntityCitizen citizen)
    {
        final boolean slim = citizen.isFemale();
        final String[] pack = slim ? SLIM_SKINS : WIDE_SKINS;
        final int idx = Math.floorMod(citizen.getTextureId(), pack.length);
        final String folder = slim ? "slim" : "wide";
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/" + folder + "/" + pack[idx] + ".png");
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull final CitizenRenderState state)
    {
        return state.texture != null
                 ? state.texture
                 : ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    }
}

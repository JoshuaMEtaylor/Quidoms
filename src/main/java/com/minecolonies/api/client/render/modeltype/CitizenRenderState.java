package com.minecolonies.api.client.render.modeltype;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Render-state carrier for Quidoms citizens under MC 1.21.10's EntityRenderState pipeline.
 */
public class CitizenRenderState extends HumanoidRenderState
{
    public String renderMetadata = "";
    public Pose pose = Pose.STANDING;
    public boolean mainHandEmpty = true;
    public boolean headSlotEmpty = true;
    public boolean displayHeadArmorEmpty = true;
    @Nullable
    public UUID customTextureUUID = null;
    @Nullable
    public ResourceLocation customTexture = null;
    public int civilianId = 0;
    public boolean female = false;
    public ResourceLocation modelTypeId = ModModelTypes.SETTLER_ID;
    public float bodyRotationOverride = 0.0F;
    public boolean isGhost = false;
    @Nullable
    public ResourceLocation texture = null;

    public boolean isWorking()
    {
        return renderMetadata != null && renderMetadata.contains("working");
    }

    public boolean displayHat()
    {
        if (pose == Pose.SLEEPING || !headSlotEmpty)
        {
            return false;
        }
        return displayHeadArmorEmpty && customTextureUUID == null;
    }

    public boolean metaContains(final String key)
    {
        return renderMetadata != null && renderMetadata.contains(key);
    }
}

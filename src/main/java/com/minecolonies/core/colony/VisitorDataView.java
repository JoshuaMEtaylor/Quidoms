package com.minecolonies.core.colony;

import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.IVisitorViewData;
import com.minecolonies.api.util.Utils;
import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * View data for visitors
 */
public class VisitorDataView extends CitizenDataView implements IVisitorViewData
{
    /**
     * The recruitment costs
     */
    private ItemStack recruitmentCosts;

    /**
     * Cached player info for custom texture.
     */
    private volatile ResourceLocation cachedTexture;

    /**
     * Create a CitizenData given an ID. Used as a super-constructor or during loading.
     *
     * @param id     ID of the Citizen.
     * @param colony Colony the Citizen belongs to.
     */
    public VisitorDataView(final int id, final IColonyView colony)
    {
        super(id, colony);
    }

    @Override
    public void deserialize(@NotNull final RegistryFriendlyByteBuf buf)
    {
        super.deserialize(buf);
        recruitmentCosts = Utils.deserializeCodecMess(buf);
        recruitmentCosts.setCount(buf.readInt());
    }

    @Override
    public ItemStack getRecruitCost()
    {
        return recruitmentCosts;
    }

    @Override
    public ResourceLocation getCustomTexture()
    {
        if (textureUUID == null)
        {
            return null;
        }

        final ResourceLocation defaultTexture = DefaultPlayerSkin.get(textureUUID).body().texturePath();

        if (cachedTexture == null)
        {
            // Async resolve via SkinManager.get(GameProfile); fall back to default until ready.
            final GameProfile profile = new GameProfile(textureUUID, "visitor");
            Minecraft.getInstance().getSkinManager().get(profile).thenAcceptAsync(optionalSkin ->
            {
                optionalSkin.map(PlayerSkin::body).map(t -> t.texturePath()).ifPresent(path ->
                {
                    if (!path.equals(defaultTexture))
                    {
                        cachedTexture = path;
                    }
                });
            }, Util.backgroundExecutor());
        }

        return cachedTexture == null ? defaultTexture : cachedTexture;
    }
}

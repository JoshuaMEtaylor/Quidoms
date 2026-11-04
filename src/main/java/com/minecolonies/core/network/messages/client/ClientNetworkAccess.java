package com.minecolonies.core.network.messages.client;

import com.ldtteam.structurize.storage.StructurePacks;
import com.ldtteam.structurize.storage.rendering.RenderingCache;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.util.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Locale;

import static com.ldtteam.structurize.api.constants.Constants.BLUEPRINT_FOLDER;
import static com.ldtteam.structurize.api.constants.Constants.SCANS_FOLDER;

/**
 * Client-only helpers for network payload handlers. Kept out of message class constant pools
 * so dedicated-server classloading of {@code PlayMessageType.forClient} payloads stays safe.
 */
public final class ClientNetworkAccess
{
    private ClientNetworkAccess() {}

    public static ResourceKey<Level> clientDimension()
    {
        return Minecraft.getInstance().level.dimension();
    }

    public static void updateCompatibility(final RegistryFriendlyByteBuf buffer)
    {
        final Level world = Minecraft.getInstance().level;
        try
        {
            IMinecoloniesAPI.getInstance().getColonyManager().getCompatibilityManager().deserialize(buffer, world);
        }
        catch (Exception e)
        {
            Log.getLogger().error("Failed to load compatibility manager", e);
        }
    }

    public static void playAudio(final ResourceLocation soundEvent, final SoundSource category, final Player player)
    {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
          soundEvent, category,
          1.0F, 1.0F, RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, player.getX(), player.getY(), player.getZ(), true));
    }

    public static void stopMusic()
    {
        Minecraft.getInstance().getSoundManager().stop();
        Minecraft.getInstance().getMusicManager().stopPlaying();
    }

    public static void saveStructureNbt(final CompoundTag compoundNBT, final String fileName, final Player player)
    {
        final String packName = Minecraft.getInstance().getUser().getName().toLowerCase(Locale.US);
        RenderingCache.getOrCreateBlueprintPreviewData("blueprint").setBlueprintFuture(
          StructurePacks.storeBlueprint(packName, compoundNBT, Minecraft.getInstance().gameDirectory.toPath()
            .resolve(BLUEPRINT_FOLDER)
            .resolve(Minecraft.getInstance().getUser().getName().toLowerCase(Locale.US))
            .resolve(SCANS_FOLDER).resolve(fileName), player.registryAccess()));
    }

    /** Reflective bridge used by server-loadable message classes. */
    public static Object invoke(final String method, final Class<?>[] types, final Object... args) throws ReflectiveOperationException
    {
        return ClientNetworkAccess.class.getMethod(method, types).invoke(null, args);
    }
}

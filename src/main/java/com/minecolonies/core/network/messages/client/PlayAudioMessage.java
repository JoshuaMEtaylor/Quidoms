package com.minecolonies.core.network.messages.client;

import com.ldtteam.common.network.AbstractClientPlayMessage;
import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Asks the client to play a specific music
 */
public class PlayAudioMessage extends AbstractClientPlayMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forClient(Constants.MOD_ID, "play_audio", PlayAudioMessage::new);

    private final ResourceLocation soundEvent;
    private final SoundSource      category;

    public PlayAudioMessage(final SoundEvent event)
    {
        this(event, SoundSource.MUSIC);
    }

    public PlayAudioMessage(final SoundEvent event, final SoundSource category)
    {
        super(TYPE);
        this.soundEvent = event.location();
        this.category = category;
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        buf.writeVarInt(category.ordinal());
        buf.writeResourceLocation(soundEvent);
    }

    protected PlayAudioMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        this.category = SoundSource.values()[buf.readVarInt()];
        this.soundEvent = buf.readResourceLocation();
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final Player player)
    {
        try
        {
            Class.forName("com.minecolonies.core.network.messages.client.ClientNetworkAccess")
              .getMethod("playAudio", ResourceLocation.class, SoundSource.class, Player.class)
              .invoke(null, soundEvent, category, player);
        }
        catch (ReflectiveOperationException ignored)
        {
        }
    }

    public static void sendToAll(final IColony col, final boolean important, final boolean stop, final PlayAudioMessage... messages)
    {
        final List<Player> players = important
          ? col.getImportantMessageEntityPlayers()
          : col.getMessagePlayerEntities();

        for (final Player player : players)
        {
            if (stop)
            {
                new StopMusicMessage().sendToPlayer((ServerPlayer) player);
            }

            for (final PlayAudioMessage pam : messages)
            {
                pam.sendToPlayer((ServerPlayer) player);
            }
        }
    }
}

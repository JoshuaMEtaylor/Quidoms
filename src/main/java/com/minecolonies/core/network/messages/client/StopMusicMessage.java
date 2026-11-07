package com.minecolonies.core.network.messages.client;

import com.ldtteam.common.network.AbstractClientPlayMessage;
import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Asks the client to stop its music
 */
public class StopMusicMessage extends AbstractClientPlayMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forClient(Constants.MOD_ID, "stop_music", StopMusicMessage::new);

    public StopMusicMessage()
    {
        super(TYPE);
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {

    }

    protected StopMusicMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final Player player)
    {
        try
        {
            Class.forName("com.minecolonies.core.network.messages.client.ClientNetworkAccess")
              .getMethod("stopMusic")
              .invoke(null);
        }
        catch (ReflectiveOperationException ignored)
        {
        }
    }
}

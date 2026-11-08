package com.minecolonies.core.colony;

import com.minecolonies.core.client.gui.WindowReactivateBuilding;
import net.minecraft.core.BlockPos;

/**
 * Client-only UI helpers. Kept out of {@link ColonyManager}'s constant pool so dedicated
 * server classloading does not pull in client GUI classes (NeoForge no longer strips {@code @OnlyIn}).
 */
public final class ClientColonyUi
{
    private ClientColonyUi() {}

    public static void openReactivationWindow(final BlockPos pos)
    {
        new WindowReactivateBuilding(pos).open();
    }
}

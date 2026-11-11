package com.minecolonies.api.blocks.interfaces;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Marker for hut / building blocks that historically opened a style browser on air right-click.
 * Quidoms disables that parchment search UI (missing texture); air RMB does nothing.
 */
public interface IBuildingBrowsableBlock
{
    /**
     * Whether air right-click opens {@code WindowBuildingBrowser}. Quidoms: always false.
     * Client-side only.
     */
    default boolean shouldBrowseBuildings(@NotNull final PlayerInteractEvent.RightClickItem event)
    {
        return false;
    }
}

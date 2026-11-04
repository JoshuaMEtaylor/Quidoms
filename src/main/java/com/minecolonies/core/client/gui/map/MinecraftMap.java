package com.minecolonies.core.client.gui.map;

import com.ldtteam.blockui.BOGuiGraphics;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneParams;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Simple minecraft map element.
 * <p>1.21.10: MapRenderer / GuiGraphics pipeline changed — draw is temporarily a no-op.
 */
public class MinecraftMap extends Pane
{
    public static final int MAP_SIZE = 128;
    public static final int MAP_CENTER = 64;

    private MapItemSavedData mapData;
    private MapId mapId;

    public MinecraftMap()
    {
        super();
    }

    public MinecraftMap(final PaneParams params)
    {
        super(params);
    }

    public void setMapData(final MapId mapId, final MapItemSavedData mapData)
    {
        this.mapId = mapId;
        this.mapData = mapData;
    }

    @Override
    public void drawSelf(final BOGuiGraphics ms, final double mx, final double my)
    {
        // TODO(1.21.10): restore map blit once MapRenderer is wired to BOGuiGraphics.
        if (mapData == null || mapId == null)
        {
            return;
        }
    }
}

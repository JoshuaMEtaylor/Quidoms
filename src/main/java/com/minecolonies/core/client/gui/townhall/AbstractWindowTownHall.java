package com.minecolonies.core.client.gui.townhall;

import com.ldtteam.blockui.Color;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.Image;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHallView;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.AbstractBuildingMainWindow;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.resources.ResourceLocation;

import static com.minecolonies.api.util.constant.WindowConstants.*;

/**
 * BOWindow for the town hall.
 * Quidoms: keep sidebar nav button visible (full chip hitbox + always-on label).
 */
public abstract class AbstractWindowTownHall extends AbstractBuildingMainWindow<ITownHallView>
{
    public static final int RED       = Color.getByName("red", 0);
    public static final int DARKGREEN = Color.getByName("darkgreen", 0);
    public static final int ORANGE    = Color.getByName("orange", 0);
    public static final int YELLOW = Color.getByName("yellow", 0);

    public AbstractWindowTownHall(final BuildingTownHall.View townHall, final String page)
    {
        super(townHall, new ResourceLocation(Constants.MOD_ID, "gui/townhall/").withSuffix(page));

        registerButton(BUTTON_ACTIONS, () -> new WindowMainPage(townHall).open());
        registerButton(BUTTON_INFOPAGE, () -> new WindowInfoPage(townHall).open());
        registerButton(BUTTON_PERMISSIONS, () -> new WindowPermissionsPage(townHall).open());
        registerButton(BUTTON_CITIZENS, () -> new WindowCitizenPage(townHall).open());
        registerButton(BUTTON_STATS, () -> new WindowStatsPage(townHall).open());
        registerButton(BUTTON_SETTINGS, () -> new WindowSettings(townHall).open());
        registerButton(BUTTON_ALLIANCE, () -> new WindowAlliancePage(townHall).open());

        final String id = getWindowId();
        final Image marker = findPaneOfTypeByID(id + "0", Image.class);
        if (marker != null)
        {
            marker.hide();
        }

        final ButtonImage nav = findPaneOfTypeByID(id, ButtonImage.class);
        if (nav != null)
        {
            nav.setImage(new ResourceLocation(Constants.MOD_ID, "textures/gui/quidcraft/nav_" + id + "_sel.png"));
            nav.show();
        }

        final ButtonImage title = findPaneOfTypeByID(id + "1", ButtonImage.class);
        if (title != null)
        {
            title.show();
        }
    }

    protected abstract String getWindowId();

    @Override
    protected boolean shouldRenderDefaultSidebar()
    {
        return false;
    }
}

package com.minecolonies.core.client.gui.citizen;

import com.minecolonies.api.colony.ICitizenDataView;

/**
 * Quidoms QD-109: happiness window removed. Opening this redirects to the main citizen page.
 */
public class HappinessWindowCitizen extends AbstractWindowCitizen
{
    public HappinessWindowCitizen(final ICitizenDataView citizen)
    {
        super(citizen, new net.minecraft.resources.ResourceLocation(
          com.minecolonies.api.util.constant.Constants.MOD_ID, "gui/citizen/main.xml"));
    }

    @Override
    public void onOpened()
    {
        // Redirect away from the deleted happiness UI.
        new MainWindowCitizen(citizen).open();
    }
}

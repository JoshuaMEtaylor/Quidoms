package com.minecolonies.core.client.gui.huts;

import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.AbstractWindowWorkerModuleBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * BOWindow for the builder hut.
 * Quidoms: the first-open {@code WindowHutGuide} overlay is disabled — building UIs open directly.
 */
public class WindowHutBuilderModule extends AbstractWindowWorkerModuleBuilding<BuildingBuilder.View>
{
    /**
     * Constructor for window builder hut.
     *
     * @param building {@link BuildingBuilder.View}.
     */
    public WindowHutBuilderModule(final BuildingBuilder.View building)
    {
        this(building, false);
    }

    /**
     * Constructor for window builder hut.
     *
     * @param needGuide unused (guide overlay removed); kept for call-site compatibility.
     * @param building  {@link BuildingBuilder.View}.
     */
    public WindowHutBuilderModule(final BuildingBuilder.View building, final boolean needGuide)
    {
        super(building, new ResourceLocation(Constants.MOD_ID, "gui/windowhutworkerplaceholder.xml"));
    }
}

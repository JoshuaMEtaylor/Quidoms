package com.minecolonies.core.colony.buildings.moduleviews;

import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.minecolonies.api.util.Utils;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.modules.building.WindowBuilderResModule;
import com.minecolonies.core.colony.buildings.utils.BuildingBuilderResource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BuildingResourcesModuleView extends AbstractBuildingModuleView
{
    /**
     * The resources they have to keep.
     */
    private final HashMap<String, BuildingBuilderResource> resources = new HashMap<>();

    /**
     * The building they are working on.
     */
    private int workOrderId;

    /**
     * Building progress.
     */
    private double progress;

    /**
     * Information on the phases.
     */
    private int finishedStages = 0;
    private int totalStages    = 1;

    /** Quidoms: schematic shortage counters from server. */
    private int schematicBlocksTotal = 0;
    private int schematicBlocksRemaining = 0;

    @Override
    public void deserialize(@NotNull final RegistryFriendlyByteBuf buf)
    {
        final int size = buf.readInt();
        resources.clear();

        for (int i = 0; i < size; i++)
        {
            final ItemStack itemStack = Utils.deserializeCodecMess(buf);;
            final int amountAvailable = buf.readInt();
            final int amountNeeded = buf.readInt();
            final BuildingBuilderResource resource = new BuildingBuilderResource(itemStack, amountNeeded, amountAvailable);
            final int hashCode = itemStack.getComponentsPatch().hashCode();
            final String key = itemStack.getDescriptionId() + "-" + hashCode;
            resources.put(key, resource);
        }

        workOrderId = buf.readInt();
        progress = buf.readDouble();
        totalStages = buf.readInt();
        finishedStages = buf.readInt();
        schematicBlocksTotal = buf.readInt();
        schematicBlocksRemaining = buf.readInt();
    }

    /**
     * Get the work order id.
     *
     * @return a string describing it.
     */
    public int getWorkOrderId()
    {
        return workOrderId;
    }

    /**
     * Getter for the needed resources.
     *
     * @return a copy of the HashMap(String, Object).
     */

    public Map<String, BuildingBuilderResource> getResources()
    {
        return Collections.unmodifiableMap(resources);
    }

    /**
     * Build progress as placed/total required shortage for the current work.
     *
     * @return 0–100 percent complete.
     */
    public int getProgress()
    {
        if (schematicBlocksTotal > 0)
        {
            final int placed = Math.max(0, schematicBlocksTotal - schematicBlocksRemaining);
            return Math.min(100, (int) Math.round(100.0 * placed / (double) schematicBlocksTotal));
        }
        // Quidoms: unknown/empty shortage must not read as 100% (legacy amountOfResources==0 bug).
        if (workOrderId > 0)
        {
            return 0;
        }
        return 0;
    }

    public int getSchematicBlocksTotal()
    {
        return schematicBlocksTotal;
    }

    public int getSchematicBlocksRemaining()
    {
        return schematicBlocksRemaining;
    }

    public int getSchematicBlocksPlaced()
    {
        return Math.max(0, schematicBlocksTotal - schematicBlocksRemaining);
    }

    /**
     * Get the current stage status.
     * @return the stage.
     */
    public int getCurrentStage()
    {
        return finishedStages;
    }

    /**
     * Get the total number of stages,
     * @return all stages.
     */
    public int getTotalStages()
    {
        return totalStages;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public BOWindow getWindow()
    {
        return new WindowBuilderResModule(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return new ResourceLocation(Constants.MOD_ID, "textures/gui/modules/inventory.png");
    }

    @Override
    public Component getDesc()
    {
        return Component.translatableEscape("com.minecolonies.coremod.gui.workerhuts.resourcelist");
    }
}

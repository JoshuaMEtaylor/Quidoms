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
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Quidoms: build % = placed/total schematic shortage. Never show 100% when total is unknown/0.
 */
public class BuildingResourcesModuleView extends AbstractBuildingModuleView
{
    private final HashMap<String, BuildingBuilderResource> resources = new HashMap<>();
    private int workOrderId;
    private double progress;
    private int finishedStages = 0;
    private int totalStages = 1;
    private int schematicBlocksTotal = 0;
    private int schematicBlocksRemaining = 0;

    @Override
    public void deserialize(@NotNull final RegistryFriendlyByteBuf buf)
    {
        final int size = buf.readInt();
        resources.clear();
        for (int i = 0; i < size; i++)
        {
            final ItemStack itemStack = Utils.deserializeCodecMess(buf);
            final int amountAvailable = buf.readInt();
            final int amountNeeded = buf.readInt();
            final BuildingBuilderResource resource = new BuildingBuilderResource(itemStack, amountNeeded, amountAvailable);
            final int hashCode = itemStack.getComponentsPatch().hashCode();
            final String key = itemStack.getItem().getDescriptionId() + "-" + hashCode;
            resources.put(key, resource);
        }
        workOrderId = buf.readInt();
        progress = buf.readDouble();
        totalStages = buf.readInt();
        finishedStages = buf.readInt();
        schematicBlocksTotal = buf.readInt();
        schematicBlocksRemaining = buf.readInt();
    }

    public int getWorkOrderId()
    {
        return workOrderId;
    }

    public Map<String, BuildingBuilderResource> getResources()
    {
        return Collections.unmodifiableMap(resources);
    }

    /**
     * @return 0–100 build percent from placed/total shortage (not resource-inventory fill).
     */
    public int getProgress()
    {
        if (schematicBlocksTotal > 0)
        {
            final int placed = Math.max(0, schematicBlocksTotal - schematicBlocksRemaining);
            return Math.min(100, (int) Math.round(100.0 * placed / (double) schematicBlocksTotal));
        }
        // Active WO with unknown/empty shortage → 0%, never the legacy 100% fallback.
        return 0;
    }

    public int getCurrentStage()
    {
        return finishedStages;
    }

    public int getTotalStages()
    {
        return totalStages;
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

    @Override
    public BOWindow getWindow()
    {
        return new WindowBuilderResModule(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/modules/inventory.png");
    }

    @Override
    public Component getDesc()
    {
        return Component.translatableEscape("com.minecolonies.coremod.gui.workerhuts.resourcelist");
    }
}

package com.minecolonies.core.util;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.buildings.modules.BuildingResourcesModule;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Quidoms QD-116/118: after person+hut hold the raw BOM (or finished already present),
 * convert/consume raws and grant finished placeables — no runtime crafting.
 */
public final class BuilderFinishedGrant
{
    private BuilderFinishedGrant()
    {
    }

    /**
     * Ensure the builder's person inventory holds enough of the finished {@code need}.
     * Counts finished already in person/hut as satisfied; converts dual-inv raws when complete.
     */
    public static boolean ensureFinishedInPerson(
      @NotNull final AbstractBuildingStructureBuilder building,
      @Nullable final ICitizenData worker,
      @NotNull final ItemStack need)
    {
        if (ItemStackUtils.isEmpty(need) || worker == null)
        {
            return ItemStackUtils.isEmpty(need);
        }

        final BuildingResourcesModule module = building.getFirstModuleOccurance(BuildingResourcesModule.class);
        if (module == null)
        {
            return false;
        }

        final InventoryCitizen person = worker.getInventory();
        final Predicate<ItemStack> match =
          candidate -> ItemStackUtils.compareItemStacksIgnoreStackSize(need, candidate, true, false);
        final int want = Math.max(1, need.getCount());

        if (InventoryUtils.getItemCountInItemHandler(person, match) >= want)
        {
            return true;
        }

        // Pull finished already sitting in the hut.
        final IItemHandler hut = building.getItemHandlerCap();
        if (hut != null)
        {
            final int have = InventoryUtils.getItemCountInItemHandler(person, match);
            final int missing = Math.max(0, want - have);
            if (missing > 0)
            {
                InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandler(
                  hut, match, missing, person);
            }
        }
        if (InventoryUtils.getItemCountInItemHandler(person, match) >= want)
        {
            return true;
        }

        // QD-118: person + hut raws together; skip raw consume for finished already present.
        module.tryConvertRawsToFinishedCredit(worker);

        if (!module.isFinishedConverted())
        {
            return false;
        }

        final int stillNeed = want - InventoryUtils.getItemCountInItemHandler(person, match);
        if (stillNeed <= 0)
        {
            return true;
        }

        final int taken = module.takeFinishedCredit(need, stillNeed);
        if (taken <= 0)
        {
            // Converted but this finished line was already satisfied elsewhere — treat as ok if present.
            return InventoryUtils.getItemCountInItemHandler(person, match) >= want;
        }

        final ItemStack grant = need.copy();
        grant.setCount(taken);
        if (!InventoryUtils.addItemStackToItemHandler(person, grant))
        {
            if (hut != null)
            {
                InventoryUtils.addItemStackToItemHandler(hut, grant);
                InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandler(
                  hut, match, want, person);
            }
            else
            {
                module.returnFinishedCredit(grant);
                Log.getLogger().info("[QuidomsBuilder] grant blocked — no space for {}", need.getItem());
            }
        }
        else
        {
            Log.getLogger().info("[QuidomsBuilder] granted finished {} x{} from raw credit", need.getItem(), taken);
        }

        return InventoryUtils.getItemCountInItemHandler(person, match) >= want;
    }
}

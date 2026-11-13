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
 * Quidoms QD-116: after the hut holds the full raw BOM, consume those raws and grant
 * finished placeables from a credit pool into the builder — no runtime crafting.
 */
public final class BuilderFinishedGrant
{
    private BuilderFinishedGrant()
    {
    }

    /**
     * If raws are fully deposited and not yet converted, consume hut raws and open the finished credit.
     * Then inject any still-needed finished stacks for {@code need} into the builder's person inventory.
     *
     * @return true when the person inventory holds enough of {@code need} afterwards.
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

        // Pull finished already sitting in the hut (post-grant overflow / prior inject).
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

        module.tryConvertRawsToFinishedCredit();

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
            return InventoryUtils.getItemCountInItemHandler(person, match) >= want;
        }

        final ItemStack grant = need.copy();
        grant.setCount(taken);
        if (!InventoryUtils.addItemStackToItemHandler(person, grant))
        {
            // Person full — park finished in hut and pull what fits next tick.
            if (hut != null)
            {
                InventoryUtils.addItemStackToItemHandler(hut, grant);
                InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandler(
                  hut, match, want, person);
            }
            else
            {
                // Rollback credit if we cannot store the grant anywhere.
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

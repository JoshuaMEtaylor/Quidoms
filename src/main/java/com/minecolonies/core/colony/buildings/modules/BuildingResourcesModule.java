package com.minecolonies.core.colony.buildings.modules;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.colony.buildings.modules.IPersistentModule;
import com.minecolonies.api.colony.jobs.IJobWithExternalWorkStations;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.workorders.IBuilderWorkOrder;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.Utils;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.buildings.utils.BuilderBucket;
import com.minecolonies.core.colony.buildings.utils.BuildingBuilderResource;
import com.minecolonies.core.colony.jobs.AbstractJobStructure;
import com.minecolonies.core.entity.ai.workers.util.BuildingProgressStage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_CURR_STAGE;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_TOTAL_STAGES;

/**
 * The structureBuilder building.
 */
public class BuildingResourcesModule extends AbstractBuildingModule implements IPersistentModule
{
    private static final String TAG_FINISHED_CONVERTED = "quidFinishedConverted";
    private static final String TAG_FINISHED_PLACEABLES = "quidFinishedPlaceables";
    private static final String TAG_FINISHED_CREDIT = "quidFinishedCredit";
    private static final String TAG_STACK = "stack";
    private static final String TAG_AMOUNT = "amount";

    /**
     * Contains all resources needed for a certain build (player-facing raw BOM).
     */
    private Map<String, BuildingBuilderResource> neededResources = new LinkedHashMap<>();

    /**
     * Exact finished placeables scanned from the schematic (QD-116).
     */
    private Map<String, BuildingBuilderResource> finishedPlaceables = new LinkedHashMap<>();

    /**
     * Remaining finished stacks to inject after raws were consumed (QD-116).
     */
    private Map<String, BuildingBuilderResource> finishedCredit = new LinkedHashMap<>();

    /**
     * True after hut raws were consumed and finished credit opened.
     */
    private boolean finishedConverted = false;

    /**
     * The different possible buckets.
     */
    private Deque<BuilderBucket> buckets = new ArrayDeque<>();

    /**
     * Total amount of stages.
     */
    private int totalStages  = 0;
    private int currentStage = 0;

    @Override
    public void deserializeNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag compound)
    {
        currentStage = compound.getIntOr(TAG_CURR_STAGE, 0);
        totalStages = compound.getIntOr(TAG_TOTAL_STAGES, 0);
        finishedConverted = compound.getBooleanOr(TAG_FINISHED_CONVERTED, false);
        finishedPlaceables = readResourceMap(provider, compound, TAG_FINISHED_PLACEABLES);
        finishedCredit = readResourceMap(provider, compound, TAG_FINISHED_CREDIT);
    }

    @Override
    public void serializeNBT(@NotNull final HolderLookup.Provider provider, CompoundTag compound)
    {
        compound.putInt(TAG_TOTAL_STAGES, totalStages);
        compound.putInt(TAG_CURR_STAGE, currentStage);
        compound.putBoolean(TAG_FINISHED_CONVERTED, finishedConverted);
        writeResourceMap(provider, compound, TAG_FINISHED_PLACEABLES, finishedPlaceables);
        writeResourceMap(provider, compound, TAG_FINISHED_CREDIT, finishedCredit);
    }

    @NotNull
    private static Map<String, BuildingBuilderResource> readResourceMap(
      @NotNull final HolderLookup.Provider provider,
      @NotNull final CompoundTag compound,
      @NotNull final String key)
    {
        final Map<String, BuildingBuilderResource> map = new LinkedHashMap<>();
        final ListTag list = compound.getListOrEmpty(key);
        for (int i = 0; i < list.size(); i++)
        {
            final CompoundTag entry = list.getCompoundOrEmpty(i);
            final ItemStack stack = ItemStackUtils.parseOptional(provider, entry.getCompoundOrEmpty(TAG_STACK));
            final int amount = entry.getIntOr(TAG_AMOUNT, 0);
            if (ItemStackUtils.isEmpty(stack) || amount <= 0)
            {
                continue;
            }
            final ItemStack keyStack = stack.copy();
            keyStack.setCount(1);
            final String mapKey = keyStack.getItem().getDescriptionId() + "-" + keyStack.getComponentsPatch().hashCode();
            map.put(mapKey, new BuildingBuilderResource(keyStack, amount));
        }
        return map;
    }

    private static void writeResourceMap(
      @NotNull final HolderLookup.Provider provider,
      @NotNull final CompoundTag compound,
      @NotNull final String key,
      @NotNull final Map<String, BuildingBuilderResource> map)
    {
        final ListTag list = new ListTag();
        for (final BuildingBuilderResource resource : map.values())
        {
            final CompoundTag entry = new CompoundTag();
            entry.put(TAG_STACK, ItemStackUtils.saveOptional(resource.getItemStack().copyWithCount(1), provider));
            entry.putInt(TAG_AMOUNT, resource.getAmount());
            list.add(entry);
        }
        compound.put(key, list);
    }

    /**
     * Method to serialize data to send it to the view.
     *
     * @param buf the used ByteBuffer.
     */
    @Override
    public void serializeToView(@NotNull final RegistryFriendlyByteBuf buf)
    {
        updateAvailableResources();
        buf.writeInt(neededResources.size());
        double qty = 0;
        for (@NotNull final BuildingBuilderResource resource : neededResources.values())
        {
            Utils.serializeCodecMess(buf, resource.getItemStack());
            buf.writeInt(resource.getAvailable());
            buf.writeInt(resource.getAmount());
            qty += resource.getAmount();
        }

        final Set<ICitizenData> set = building.getAllAssignedCitizen();
        final ICitizenData data = set.isEmpty() ? null : set.iterator().next();
        if (data != null && data.getJob() instanceof AbstractJobStructure)
        {
            final IBuilderWorkOrder workOrder = ((AbstractBuildingStructureBuilder) building).getWorkOrder();
            if (workOrder != null)
            {
                buf.writeInt(workOrder.getID());
                buf.writeDouble(workOrder.getAmountOfResources() == 0 ? 0 : qty / workOrder.getAmountOfResources());
                buf.writeInt(totalStages);
                buf.writeInt(currentStage);
                return;
            }
        }

        buf.writeInt(-1);
        buf.writeDouble(0.0);
        buf.writeInt(0);
        buf.writeInt(0);
    }

    /**
     * Update the available resources.
     * <p>
     * which are needed for the build and in the structureBuilder's chest or inventory
     */
    private void updateAvailableResources()
    {
        final Set<ICitizenData> set = building.getAllAssignedCitizen();
        final ICitizenData data = set.isEmpty() ? null : set.iterator().next();
        if (data == null)
        {
            return;
        }
        data.getEntity().ifPresent(structureBuilder -> {
            final InventoryCitizen structureBuilderInventory = data.getInventory();
            if (structureBuilderInventory == null)
            {
                return;
            }

            for (@NotNull final Map.Entry<String, BuildingBuilderResource> entry : neededResources.entrySet())
            {
                final BuildingBuilderResource resource = entry.getValue();

                resource.setAvailable(0);

                // Dual inventory: on-person + hut TE/racks. Ignore NBT so manual deposits count as supplied.
                final Predicate<ItemStack> match =
                  stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, resource.getItemStack(), true, false);

                if (structureBuilderInventory != null)
                {
                    resource.addAvailable(InventoryUtils.getItemCountInItemHandler(structureBuilderInventory, match));
                }

                if (building.getTileEntity() != null && building.getItemHandlerCap() != null)
                {
                    resource.addAvailable(InventoryUtils.getItemCountInItemHandler(building.getItemHandlerCap(), match));
                }

                if (data.getJob() instanceof final IJobWithExternalWorkStations jobExternalStations)
                {
                    for (final IBuilding station : jobExternalStations.getWorkStations())
                    {
                        if (station.getItemHandlerCap() != null)
                        {
                            resource.addAvailable(InventoryUtils.getItemCountInItemHandler(station.getItemHandlerCap(), match));
                        }
                    }
                }
            }
        });
    }

    /**
     * Get the needed resources for the current build.
     *
     * @return a new Hashmap.
     */
    public Map<String, BuildingBuilderResource> getNeededResources()
    {
        return Collections.unmodifiableMap(neededResources);
    }

    /**
     * Get the needed resources for the current build.
     *
     * @return the bucket.
     */
    @Nullable
    public BuilderBucket getRequiredResources()
    {
        return (buckets.isEmpty()
            || ((AbstractBuildingStructureBuilder) building).getProgress() == null
            || ((AbstractBuildingStructureBuilder) building).getProgress().getB() == BuildingProgressStage.CLEAR) ? null : buckets.getFirst();
    }

    /**
     * Get the resource from the identifier.
     *
     * @param res the resource to get.
     * @return the resource.
     */
    public BuildingBuilderResource getResourceFromIdentifier(final String res)
    {
        return neededResources.get(res);
    }

    /**
     * Add a new resource to the needed list.
     *
     * @param res    the resource.
     * @param amount the amount.
     */
    public void addNeededResource(@Nullable final ItemStack res, final int amount)
    {
        if (ItemStackUtils.isEmpty(res) || amount == 0)
        {
            return;
        }

        // QD-116: keep exact finished placeables separately from the player-facing raw BOM.
        addFinishedPlaceable(res, amount);

        // Quidoms QD-103: Domum framed → texture components, then crafting BOM (planks→logs).
        // Required list / QuidCraft / courier see raw only — never framed products.
        final java.util.List<ItemStack> expanded = com.minecolonies.core.util.BuilderInventoryCraft.expandToRaw(
            building.getColony().getWorld(), res, amount);
        for (final ItemStack part : expanded)
        {
            addNeededResourceRaw(part, part.getCount());
        }
    }

    private void addFinishedPlaceable(@NotNull final ItemStack res, final int amount)
    {
        final ItemStack keyStack = res.copy();
        keyStack.setCount(1);
        final int hashCode = keyStack.getComponentsPatch().hashCode();
        final String key = keyStack.getItem().getDescriptionId() + "-" + hashCode;
        BuildingBuilderResource resource = this.finishedPlaceables.get(key);
        if (resource == null)
        {
            resource = new BuildingBuilderResource(keyStack, amount);
        }
        else
        {
            resource.setAmount(resource.getAmount() + amount);
        }
        this.finishedPlaceables.put(key, resource);
    }

    public boolean isFinishedConverted()
    {
        return finishedConverted;
    }

    @NotNull
    public Map<String, BuildingBuilderResource> getFinishedPlaceables()
    {
        return Collections.unmodifiableMap(finishedPlaceables);
    }

    /**
     * Combined person + hut TE+racks count for a predicate (QD-118).
     */
    private int countDualInv(@Nullable final ICitizenData worker, @NotNull final Predicate<ItemStack> match)
    {
        int count = 0;
        final IItemHandler hut = building.getItemHandlerCap();
        if (hut != null)
        {
            count += InventoryUtils.getItemCountInItemHandler(hut, match);
        }
        if (worker != null && worker.getInventory() != null)
        {
            count += InventoryUtils.getItemCountInItemHandler(worker.getInventory(), match);
        }
        return count;
    }

    /**
     * Person + hut TE+racks together hold enough of every raw BOM entry (QD-118).
     */
    public boolean areRawsFullyDeposited()
    {
        return areRawsFullyDeposited(null);
    }

    public boolean areRawsFullyDeposited(@Nullable final ICitizenData worker)
    {
        if (neededResources.isEmpty())
        {
            return finishedConverted;
        }
        for (final BuildingBuilderResource resource : neededResources.values())
        {
            final Predicate<ItemStack> match =
              stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, resource.getItemStack(), true, false);
            if (countDualInv(worker, match) < resource.getAmount())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Names of raw BOM entries still short in person+hut (for wait chat).
     */
    @NotNull
    public String describeMissingRaws(@Nullable final ICitizenData worker)
    {
        if (neededResources.isEmpty())
        {
            return finishedConverted ? "" : "raw materials";
        }
        final StringBuilder sb = new StringBuilder();
        for (final BuildingBuilderResource resource : neededResources.values())
        {
            final Predicate<ItemStack> match =
              stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, resource.getItemStack(), true, false);
            final int have = countDualInv(worker, match);
            final int missing = resource.getAmount() - have;
            if (missing <= 0)
            {
                continue;
            }
            if (sb.length() > 0)
            {
                sb.append(", ");
            }
            sb.append(resource.getItemStack().getHoverName().getString()).append(" x").append(missing);
        }
        return sb.length() == 0 ? "" : sb.toString();
    }

    /**
     * Consume {@code amount} matching stacks from hut first, then person (QD-118).
     */
    private boolean consumeDualInv(
      @Nullable final ICitizenData worker,
      @NotNull final ItemStack template,
      final int amount)
    {
        if (amount <= 0)
        {
            return true;
        }
        int left = amount;
        final IItemHandler hut = building.getItemHandlerCap();
        if (hut != null)
        {
            final int inHut = InventoryUtils.getItemCountInItemHandler(hut,
              s -> ItemStackUtils.compareItemStacksIgnoreStackSize(s, template, true, false));
            final int fromHut = Math.min(left, inHut);
            if (fromHut > 0)
            {
                if (!InventoryUtils.attemptReduceStackInItemHandler(hut, template.copy(), fromHut, true, true))
                {
                    return false;
                }
                left -= fromHut;
            }
        }
        if (left <= 0)
        {
            return true;
        }
        if (worker == null || worker.getInventory() == null)
        {
            return false;
        }
        return InventoryUtils.attemptReduceStackInItemHandler(worker.getInventory(), template.copy(), left, true, true);
    }

    /**
     * QD-116/118: when person+hut raws cover the BOM, consume (hut first) and open finished credit.
     * Finished stacks already present are not re-granted / do not consume raws for that line.
     *
     * @return true if already converted or conversion succeeded this call.
     */
    public boolean tryConvertRawsToFinishedCredit()
    {
        return tryConvertRawsToFinishedCredit(null);
    }

    public boolean tryConvertRawsToFinishedCredit(@Nullable final ICitizenData worker)
    {
        if (finishedConverted)
        {
            return true;
        }
        if (finishedPlaceables.isEmpty())
        {
            return false;
        }
        if (!areRawsFullyDeposited(worker))
        {
            return false;
        }

        // Credit only what is not already present as finished in person+hut.
        finishedCredit = new LinkedHashMap<>();
        for (final Map.Entry<String, BuildingBuilderResource> entry : finishedPlaceables.entrySet())
        {
            final BuildingBuilderResource placeable = entry.getValue();
            final Predicate<ItemStack> match =
              stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, placeable.getItemStack(), true, false);
            final int already = countDualInv(worker, match);
            final int stillNeed = Math.max(0, placeable.getAmount() - already);
            if (stillNeed > 0)
            {
                finishedCredit.put(entry.getKey(),
                  new BuildingBuilderResource(placeable.getItemStack().copyWithCount(1), stillNeed));
            }
        }

        // Consume raws only when credit still needs finished injections.
        // If every finished line is already present, skip raw consume.
        if (!finishedCredit.isEmpty())
        {
            for (final BuildingBuilderResource resource : neededResources.values())
            {
                if (!consumeDualInv(worker, resource.getItemStack(), resource.getAmount()))
                {
                    Log.getLogger().warn("[QuidomsBuilder] raw consume failed for {} x{} — abort convert",
                      resource.getItemStack().getItem(), resource.getAmount());
                    finishedCredit = new LinkedHashMap<>();
                    return false;
                }
            }
        }

        finishedConverted = true;
        neededResources = new LinkedHashMap<>();
        buckets.clear();
        this.markDirty();
        Log.getLogger().info("[QuidomsBuilder] converted dual-inv raws → finished credit ({} kinds still to grant)",
          finishedCredit.size());
        return true;
    }

    /**
     * Take up to {@code amount} matching finished stacks from the credit pool.
     *
     * @return count actually taken.
     */
    public int takeFinishedCredit(@NotNull final ItemStack need, final int amount)
    {
        if (!finishedConverted || amount <= 0 || ItemStackUtils.isEmpty(need))
        {
            return 0;
        }
        final ItemStack keyStack = need.copyWithCount(1);
        final String key = keyStack.getItem().getDescriptionId() + "-" + keyStack.getComponentsPatch().hashCode();
        BuildingBuilderResource resource = finishedCredit.get(key);
        if (resource == null)
        {
            // Ignore NBT mismatches for vanilla stairs/planks — match by item only.
            for (final Map.Entry<String, BuildingBuilderResource> entry : finishedCredit.entrySet())
            {
                if (ItemStackUtils.compareItemStacksIgnoreStackSize(entry.getValue().getItemStack(), need, true, false))
                {
                    resource = entry.getValue();
                    break;
                }
            }
        }
        if (resource == null || resource.getAmount() <= 0)
        {
            return 0;
        }
        final int take = Math.min(amount, resource.getAmount());
        resource.setAmount(resource.getAmount() - take);
        finishedCredit.entrySet().removeIf(e -> e.getValue().getAmount() <= 0);
        this.markDirty();
        return take;
    }

    public void returnFinishedCredit(@NotNull final ItemStack stack)
    {
        if (ItemStackUtils.isEmpty(stack) || !finishedConverted)
        {
            return;
        }
        addToMap(finishedCredit, stack, stack.getCount());
        this.markDirty();
    }

    private static void addToMap(
      @NotNull final Map<String, BuildingBuilderResource> map,
      @NotNull final ItemStack res,
      final int amount)
    {
        final ItemStack keyStack = res.copyWithCount(1);
        final String key = keyStack.getItem().getDescriptionId() + "-" + keyStack.getComponentsPatch().hashCode();
        BuildingBuilderResource resource = map.get(key);
        if (resource == null)
        {
            map.put(key, new BuildingBuilderResource(keyStack, amount));
        }
        else
        {
            resource.setAmount(resource.getAmount() + amount);
        }
    }

    private void addNeededResourceRaw(@Nullable final ItemStack res, final int amount)
    {
        if (ItemStackUtils.isEmpty(res) || amount == 0)
        {
            return;
        }
        final int hashCode = res.getComponentsPatch().hashCode();
        final String key = res.getItem().getDescriptionId() + "-" + hashCode;
        BuildingBuilderResource resource = this.neededResources.get(key);
        if (resource == null)
        {
            resource = new BuildingBuilderResource(res, amount);
        }
        else
        {
            resource.setAmount(resource.getAmount() + amount);
        }
        this.neededResources.put(key, resource);

        BuilderBucket last = buckets.isEmpty() ? null : buckets.removeLast();

        final int stacks = (int) Math.ceil((double) amount / res.getMaxStackSize());
        final int max = building.getAllAssignedCitizen().iterator().next().getInventory().getSlots() - 9;

        if (last == null || last.getTotalStacks() >= max || last.getTotalStacks() + stacks >= max)
        {
            if (last != null)
            {
                buckets.add(last);
            }
            last = new BuilderBucket();

            last.setTotalStacks(stacks);
            last.addOrAdjustResource(key, amount);
            buckets.add(last);
        }
        else
        {
            int currentQty = last.getResourceMap().getOrDefault(key, 0);
            final int currentStacks = (int) Math.ceil((double) currentQty / res.getMaxStackSize());
            final int newStacks = (int) Math.ceil((double) (currentQty + amount) / res.getMaxStackSize());
            final Map<String, Integer> map = last.getResourceMap();
            last.setTotalStacks(last.getTotalStacks() + newStacks - currentStacks);
            last.addOrAdjustResource(key, currentQty + amount);
            buckets.add(last);
        }

        this.markDirty();
    }

    /**
     * Reduce a resource of the needed list.
     *
     * @param res    the resource.
     * @param amount the amount.
     */
    public void reduceNeededResource(final ItemStack res, final int amount)
    {
        if (ItemStackUtils.isEmpty(res) || amount <= 0)
        {
            return;
        }

        // QD-103: Domum framed places 1:1 with texture components on the needed map — reduce those.
        // Vanilla convertibles (planks→logs) keep ceil-BOM on add; per-place log reduce would
        // over-decrement (1 plank ≠ 1 log), so leave crafting BOM accounting as before.
        if (com.minecolonies.core.util.DomumOrnamentumUtils.getBlock(res) != null
              && !com.ldtteam.domumornamentum.client.model.data.MaterialTextureData.readFromItemStack(res).isEmpty())
        {
            final java.util.List<ItemStack> parts = com.minecolonies.core.util.BuilderInventoryCraft.expandToRaw(
                building.getColony().getWorld(), res, amount);
            for (final ItemStack part : parts)
            {
                reduceNeededResourceRaw(part, part.getCount());
            }
            return;
        }

        reduceNeededResourceRaw(res, amount);
    }

    private void reduceNeededResourceRaw(final ItemStack res, final int amount)
    {
        final int hashCode = res.getComponentsPatch().hashCode();
        final String name = res.getItem().getDescriptionId() + "-" + hashCode;

        final BuilderBucket last = buckets.isEmpty() ? null : getRequiredResources();

        if (last != null)
        {
            final Map<String, Integer> map = last.getResourceMap();
            if (map.containsKey(name))
            {
                int qty = map.get(name) - amount;
                if (qty > 0)
                {
                    last.addOrAdjustResource(name, map.get(name) - amount);
                }
                else
                {
                    last.removeResources(name);
                }
            }

            if (map.isEmpty())
            {
                buckets.remove();
            }
        }

        int preAmount = 0;
        if (this.neededResources.containsKey(name))
        {
            preAmount = this.neededResources.get(name).getAmount();
        }

        if (preAmount - amount <= 0)
        {
            this.neededResources.remove(name);
        }
        else
        {
            this.neededResources.get(name).setAmount(preAmount - amount);
        }
        this.markDirty();
    }

    /**
     * Resets the needed resources completely.
     */
    public void resetNeededResources()
    {
        neededResources = new HashMap<>();
        buckets.clear();
        finishedPlaceables = new LinkedHashMap<>();
        finishedCredit = new LinkedHashMap<>();
        finishedConverted = false;
        this.markDirty();
    }

    /**
     * Check if the structureBuilder requires a certain ItemStack for the current construction.
     *
     * @param stack the stack to test.
     * @return true if so.
     */
    public boolean requiresResourceForBuilding(final ItemStack stack)
    {
        final int hashCode = stack.getComponentsPatch().hashCode();
        return neededResources.containsKey(stack.getItem().getDescriptionId() + "-" + hashCode);
    }

    /**
     * Check or request if the contents of a specific batch are in the inventory of the building. This ignores the worker inventory (that is remaining stuff from previous rounds,
     * or already belongs to another bucket)
     *
     * @param requiredResources the bucket to check and request.
     * @param worker            the worker.
     */
    public void checkOrRequestBucket(@Nullable final BuilderBucket requiredResources, final ICitizenData worker)
    {
        if (requiredResources == null)
        {
            return;
        }

        final ImmutableList<IRequest<? extends Stack>> list = building.getOpenRequestsOfType(-1, TypeToken.of(Stack.class));
        final Set<ItemStorage> requestedItems = new HashSet<>();
        for (final IRequest<? extends Stack> request : list)
        {
            requestedItems.add(new ItemStorage(request.getRequest().getStack()));
        }

        for (final Map.Entry<String, Integer> entry : requiredResources.getResourceMap().entrySet())
        {
            final ItemStorage itemStack = neededResources.get(entry.getKey());
            if (itemStack == null)
            {
                continue;
            }

            int count = InventoryUtils.hasBuildingEnoughElseCount(building,
              stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, itemStack.getItemStack()), entry.getValue());

            if (count >= entry.getValue())
            {
                continue;
            }

            count += InventoryUtils.getItemCountInItemHandler(worker.getInventory(), stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, itemStack.getItemStack()));
            if (count >= entry.getValue())
            {
                continue;
            }

            int requestCount = entry.getValue() - count;
            // QD-76: structure builders provision exact shortage from QuidCraft before colony requests.
            // (They do not go through AbstractEntityAIBasic.checkIfRequestForItemExistOrCreate.)
            final String citizenName = worker == null ? "" : worker.getName();
            if (com.minecolonies.core.compatibility.quidcraft.QuidCraftMarketBridge.tryBuyForBuilding(
              building, itemStack.getItemStack(), requestCount, true, citizenName, worker))
            {
                count = InventoryUtils.hasBuildingEnoughElseCount(building,
                  stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, itemStack.getItemStack()), entry.getValue());
                count += InventoryUtils.getItemCountInItemHandler(worker.getInventory(),
                  stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, itemStack.getItemStack()));
                if (count >= entry.getValue())
                {
                    continue;
                }
                requestCount = entry.getValue() - count;
            }

            if (requestCount <= 0 || requestedItems.contains(new ItemStorage(itemStack.getItemStack())))
            {
                continue;
            }

            building.createRequest(new Stack(itemStack.getItemStack(), requestCount * ((AbstractBuildingStructureBuilder) building).getResourceBatchMultiplier(), 1), true);
        }
    }

    /**
     * Go to the next stage.
     */
    public void nextStage()
    {
        if (this.currentStage + 1 > totalStages)
        {
            totalStages++;
        }
        this.currentStage++;
    }

    /**
     * Set the total number of stages.
     *
     * @param total the total.
     */
    public void setTotalStages(final int total)
    {
        this.totalStages = total;
        this.currentStage = 0;
    }

    /**
     * Return the next bucket to work on.
     *
     * @return the next bucket or a tuple with null inside if non available.
     */
    @Nullable
    public BuilderBucket getNextBucket()
    {
        final Iterator<BuilderBucket> iterator = buckets.iterator();
        if (iterator.hasNext())
        {
            iterator.next();
        }

        if (iterator.hasNext())
        {
            return iterator.next();
        }
        return null;
    }
}

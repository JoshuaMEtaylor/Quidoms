package com.minecolonies.core.compatibility.quidcraft;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.buildings.utils.BuildingBuilderResource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soft QuidCraft marketplace bridge. Uses reflection so Quidoms compiles and runs without QuidCraft.
 * <p>
 * QD-73/76: builders provision exact shortage qty via {@code QuidAccess.provisionMarketBuy} —
 * payment is treated as completed (no Quid debit), but CommodityMarket fill + price walk still apply.
 * Structure builders hit this from {@code BuildingResourcesModule.checkOrRequestBucket} (not AIBasic).
 * Non-catalogue items are skipped so the normal request flow can run.
 */
public final class QuidCraftMarketBridge
{
    public static final String MOD_ID = "quidcraft";

    /** Per colony+item cooldown after an attempt (ms). */
    private static final long COOLDOWN_MS = 3_000L;

    private static final Map<String, Long> COOLDOWNS = new ConcurrentHashMap<>();

    private static Boolean presentCached;
    private static boolean reflectFailed;
    private static Boolean provisionApiCached;

    private QuidCraftMarketBridge()
    {
    }

    public static boolean isPresent()
    {
        if (presentCached == null)
        {
            presentCached = ModList.get().isLoaded(MOD_ID);
        }
        return presentCached && !reflectFailed;
    }

    public static boolean tryBuyForBuilding(@NotNull final IBuilding building, @NotNull final ItemStack stack, final int count, final boolean matchNBT)
    {
        return tryBuyForBuilding(building, stack, count, matchNBT, "", null);
    }

    public static boolean tryBuyForBuilding(
      @NotNull final IBuilding building,
      @NotNull final ItemStack stack,
      final int count,
      final boolean matchNBT,
      @NotNull final String citizenName)
    {
        return tryBuyForBuilding(building, stack, count, matchNBT, citizenName, null);
    }

    /**
     * Attempt to provision exact {@code count} of {@code stack}'s item from QuidCraft into the
     * builder inventory and/or building. No Quid debit. Returns true if enough is held afterwards.
     */
    public static boolean tryBuyForBuilding(
      @NotNull final IBuilding building,
      @NotNull final ItemStack stack,
      final int count,
      final boolean matchNBT,
      @NotNull final String citizenName,
      @Nullable final ICitizenData worker)
    {
        if (!isPresent() || stack.isEmpty() || count <= 0 || building.getColony() == null || building.getColony().getWorld() == null)
        {
            return false;
        }
        if (building.getColony().getWorld().isClientSide())
        {
            return false;
        }

        final IColony colony = building.getColony();
        final String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        final String coolKey = colony.getID() + "|" + itemId;
        final long nowMs = System.currentTimeMillis();
        final Long until = COOLDOWNS.get(coolKey);
        if (until != null && until > nowMs)
        {
            return false;
        }

        final UUID orderId = UUID.randomUUID();
        final UUID ownerId = colony.getPermissions().getOwner();
        final String ownerName = colony.getPermissions().getOwnerName() == null ? "colony-owner" : colony.getPermissions().getOwnerName();
        final String colonyName = colony.getName() == null ? ("colony-" + colony.getID()) : colony.getName();
        final UUID safeOwner = ownerId == null ? new UUID(0L, 0L) : ownerId;

        try
        {
            if (!quidReady())
            {
                // Economy not open yet — brief cooldown then retry.
                COOLDOWNS.put(coolKey, nowMs + 1_000L);
                return false;
            }

            final Level world = colony.getWorld();
            final BlockPos pos = building.getPosition();
            final String dimension = world.dimension().location().toString();

            final QuidCraftProvisionOutcome provision = invokeProvision(itemId, count, dimension, pos.getX(), pos.getZ(), nowMs);
            if (!provision.onCatalogue())
            {
                // Not listed / no API — fall through; short cooldown avoids spam.
                COOLDOWNS.put(coolKey, nowMs + 2_000L);
                return false;
            }

            COOLDOWNS.put(coolKey, nowMs + COOLDOWN_MS);

            if (provision.filled() <= 0)
            {
                // Still record so Colonies tab shows the attempt (room empty, etc.).
                recordOrder(orderId, colony.getID(), colonyName, safeOwner, ownerName, citizenName, itemId, count, false, 0L,
                  provision.detail().isEmpty() ? "no-fill" : provision.detail(), nowMs);
                return false;
            }

            final int delivered = deposit(building, worker, stack, provision.filled(), matchNBT);
            final int haveBuilding = InventoryUtils.hasBuildingEnoughElseCount(building,
              new com.minecolonies.api.crafting.ItemStorage(stack, true, matchNBT), count);
            int haveWorker = 0;
            if (worker != null && worker.getInventory() != null)
            {
                haveWorker = InventoryUtils.getItemCountInItemHandler(worker.getInventory(),
                  s -> ItemStackUtils.compareItemStacksIgnoreStackSize(s, stack, true, matchNBT));
            }
            final boolean enough = haveBuilding + haveWorker >= count;
            final String detail = (provision.detail().isEmpty() ? "provision" : provision.detail())
                                   + ";delivered=" + delivered
                                   + (enough ? ";ok" : ";partial");
            recordOrder(orderId, colony.getID(), colonyName, safeOwner, ownerName, citizenName, itemId, count, enough || delivered > 0,
              provision.notionalCostMinor(), detail, nowMs);
            if (delivered > 0)
            {
                Log.getLogger().info("QuidCraft provisioned {} x{} for colony {} (delivered={}, enough={})",
                  itemId, provision.filled(), colonyName, delivered, enough);
            }
            return enough;
        }
        catch (final ClassNotFoundException | NoSuchMethodException e)
        {
            // QuidCraft API missing / outdated — disable until reload.
            reflectFailed = true;
            Log.getLogger().warn("QuidCraft present but provision API reflect failed; disabling bridge: " + e);
            return false;
        }
        catch (final NoClassDefFoundError e)
        {
            // Never permanently disable for our own missing nested/top-level types — brief cooldown and retry.
            final String msg = String.valueOf(e.getMessage());
            if (msg.contains("com.quidcraft.") || msg.contains("QuidApi") || msg.contains("QuidAccess"))
            {
                reflectFailed = true;
                Log.getLogger().warn("QuidCraft present but provision API class missing; disabling bridge: " + e);
            }
            else
            {
                COOLDOWNS.put(coolKey, nowMs + COOLDOWN_MS);
                Log.getLogger().warn("QuidCraft provision classload glitch (bridge stays enabled): {}", e.toString());
            }
            return false;
        }
        catch (final Throwable t)
        {
            COOLDOWNS.put(coolKey, nowMs + COOLDOWN_MS);
            recordOrder(orderId, colony.getID(), colonyName, safeOwner, ownerName, citizenName, itemId, count, false, 0L, t.toString(), nowMs);
            Log.getLogger().warn("QuidCraft provision buy failed for {}: {}", itemId, t.toString());
            return false;
        }
    }

    public static void tryProvisionNeededResources(@NotNull final IBuilding building, @NotNull final String citizenName)
    {
        tryProvisionNeededResources(building, citizenName, null);
    }

    /**
     * Sweep structure needed-resources and provision each shortage.
     */
    public static void tryProvisionNeededResources(
      @NotNull final IBuilding building,
      @NotNull final String citizenName,
      @Nullable final ICitizenData worker)
    {
        if (!isPresent() || !(building instanceof AbstractBuildingStructureBuilder structureBuilder))
        {
            return;
        }
        try
        {
            final Map<String, BuildingBuilderResource> needed = structureBuilder.getNeededResources();
            if (needed == null || needed.isEmpty())
            {
                return;
            }
            for (final BuildingBuilderResource entry : needed.values())
            {
                if (entry == null || entry.getItemStack() == null || entry.getItemStack().isEmpty() || entry.getAmount() <= 0)
                {
                    continue;
                }
                final ItemStack stack = entry.getItemStack();
                int have = InventoryUtils.hasBuildingEnoughElseCount(building,
                  new com.minecolonies.api.crafting.ItemStorage(stack, true, false), entry.getAmount());
                if (worker != null && worker.getInventory() != null)
                {
                    have += InventoryUtils.getItemCountInItemHandler(worker.getInventory(),
                      s -> ItemStackUtils.compareItemStacksIgnoreStackSize(s, stack, true, false));
                }
                final int shortage = entry.getAmount() - have;
                if (shortage > 0)
                {
                    tryBuyForBuilding(building, stack, shortage, false, citizenName, worker);
                }
            }
        }
        catch (final Throwable t)
        {
            Log.getLogger().warn("QuidCraft structure provision sweep failed: {}", t.toString());
        }
    }

    private static void recordOrder(
      final UUID orderId,
      final int colonyId,
      final String colonyName,
      final UUID ownerId,
      final String ownerName,
      final String citizenName,
      final String itemId,
      final int quantity,
      final boolean success,
      final long costMinor,
      final String detail,
      final long now)
    {
        try
        {
            final Class<?> api = Class.forName("com.quidcraft.api.QuidApi");
            if (!Boolean.TRUE.equals(api.getMethod("ready").invoke(null)))
            {
                return;
            }
            final Object access = api.getMethod("get").invoke(null);
            access.getClass().getMethod(
              "recordColonyMarketOrder",
              UUID.class, int.class, String.class, UUID.class, String.class, String.class, String.class,
              int.class, boolean.class, long.class, String.class, long.class
            ).invoke(access, orderId, colonyId, colonyName, ownerId, ownerName, citizenName == null ? "" : citizenName, itemId, quantity, success, costMinor, detail == null ? "" : detail, now);
        }
        catch (final NoSuchMethodException e)
        {
            // Older QuidCraft without Colonies tab API — ignore.
        }
        catch (final Throwable t)
        {
            Log.getLogger().debug("Could not record QuidCraft colony order: {}", t.toString());
        }
    }

    private static boolean quidReady() throws Exception
    {
        final Class<?> api = Class.forName("com.quidcraft.api.QuidApi");
        return Boolean.TRUE.equals(api.getMethod("ready").invoke(null));
    }

    private static QuidCraftProvisionOutcome invokeProvision(
      final String itemId,
      final int quantity,
      final String dimension,
      final int x,
      final int z,
      final long now) throws Exception
    {
        final Class<?> api = Class.forName("com.quidcraft.api.QuidApi");
        final Object access = api.getMethod("get").invoke(null);
        if (provisionApiCached == null)
        {
            try
            {
                access.getClass().getMethod(
                  "provisionMarketBuy",
                  UUID.class, String.class, int.class, String.class, int.class, int.class, long.class);
                provisionApiCached = true;
            }
            catch (final NoSuchMethodException e)
            {
                provisionApiCached = false;
            }
        }
        if (!Boolean.TRUE.equals(provisionApiCached))
        {
            Log.getLogger().warn("QuidCraft loaded but provisionMarketBuy is missing — need QuidCraft >= 1.0.29 for builder buys.");
            return new QuidCraftProvisionOutcome(false, 0, 0L, "no-provision-api");
        }
        final Object result = access.getClass().getMethod(
          "provisionMarketBuy",
          UUID.class, String.class, int.class, String.class, int.class, int.class, long.class
        ).invoke(access, UUID.randomUUID(), itemId, quantity, dimension, x, z, now);
        if (result == null)
        {
            return new QuidCraftProvisionOutcome(false, 0, 0L, "null-result");
        }
        final boolean onCatalogue = Boolean.TRUE.equals(result.getClass().getMethod("onCatalogue").invoke(result));
        final int filled = ((Number) result.getClass().getMethod("filled").invoke(result)).intValue();
        final long cost = ((Number) result.getClass().getMethod("notionalCostMinor").invoke(result)).longValue();
        final Object detail = result.getClass().getMethod("detail").invoke(result);
        return new QuidCraftProvisionOutcome(onCatalogue, filled, cost, detail == null ? "" : detail.toString());
    }

    private static int deposit(
      @NotNull final IBuilding building,
      @Nullable final ICitizenData worker,
      @NotNull final ItemStack prototype,
      final int amount,
      final boolean matchNBT)
    {
        if (amount <= 0)
        {
            return 0;
        }
        final Item item = prototype.getItem();
        ItemStack remaining = matchNBT ? prototype.copyWithCount(amount) : new ItemStack(item, amount);
        if (remaining.isEmpty())
        {
            remaining = new ItemStack(item, amount);
        }
        int placed = 0;

        // Prefer builder inventory — Required Resources "available" counts citizen + hut.
        if (worker != null && worker.getInventory() != null && !remaining.isEmpty())
        {
            final int before = remaining.getCount();
            remaining = InventoryUtils.addItemStackToItemHandlerWithResult(worker.getInventory(), remaining);
            if (remaining == null || remaining.isEmpty())
            {
                placed += before;
                remaining = ItemStack.EMPTY;
            }
            else
            {
                placed += Math.max(0, before - remaining.getCount());
            }
        }

        while (!remaining.isEmpty() && remaining.getCount() > 0)
        {
            final int batch = Math.min(remaining.getCount(), Math.max(1, remaining.getMaxStackSize()));
            ItemStack slice = remaining.copyWithCount(batch);
            ItemStack leftover = slice;
            if (building instanceof AbstractBuilding abstractBuilding)
            {
                leftover = abstractBuilding.forceTransferStack(slice, building.getColony().getWorld());
                if (leftover == null)
                {
                    leftover = ItemStack.EMPTY;
                }
            }
            else if (building.getTileEntity() != null)
            {
                leftover = InventoryUtils.addItemStackToProviderWithResult(building.getTileEntity(), slice);
                if (leftover == null)
                {
                    leftover = ItemStack.EMPTY;
                }
            }
            final int got = batch - (leftover.isEmpty() ? 0 : leftover.getCount());
            if (got <= 0)
            {
                break;
            }
            placed += got;
            remaining.shrink(got);
        }
        return placed;
    }
}

package com.minecolonies.core.compatibility.quidcraft;

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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soft QuidCraft marketplace bridge. Uses reflection so Quidoms compiles and runs without QuidCraft.
 * <p>
 * QD-73: builders/citizens provision exact shortage qty via {@code QuidAccess.provisionMarketBuy} —
 * payment is treated as completed (no Quid debit), but CommodityMarket fill + price walk still apply.
 * Non-catalogue items are skipped so the normal request flow can run.
 */
public final class QuidCraftMarketBridge
{
    public static final String MOD_ID = "quidcraft";

    /** Per colony+item cooldown after an attempt (ms). */
    private static final long COOLDOWN_MS = 5_000L;

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
        return tryBuyForBuilding(building, stack, count, matchNBT, "");
    }

    /**
     * Attempt to provision exact {@code count} of {@code stack}'s item from QuidCraft into the building.
     * No owner inventory / Quid balance required. Returns true if the building holds enough afterwards.
     */
    public static boolean tryBuyForBuilding(
      @NotNull final IBuilding building,
      @NotNull final ItemStack stack,
      final int count,
      final boolean matchNBT,
      @NotNull final String citizenName)
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
                return false;
            }

            final Level world = colony.getWorld();
            final BlockPos pos = building.getPosition();
            final String dimension = world.dimension().location().toString();

            final ProvisionResult provision = invokeProvision(itemId, count, dimension, pos.getX(), pos.getZ(), nowMs);
            if (!provision.onCatalogue())
            {
                // Not listed — fall through to normal requests; short cooldown avoids spam.
                COOLDOWNS.put(coolKey, nowMs + 2_000L);
                return false;
            }

            COOLDOWNS.put(coolKey, nowMs + COOLDOWN_MS);

            if (provision.filled() <= 0)
            {
                recordOrder(orderId, colony.getID(), colonyName, safeOwner, ownerName, citizenName, itemId, count, false, 0L,
                  provision.detail().isEmpty() ? "no-fill" : provision.detail(), nowMs);
                return false;
            }

            final int delivered = depositIntoBuilding(building, stack, provision.filled(), matchNBT);
            final boolean enough = InventoryUtils.hasBuildingEnoughElseCount(building,
              new com.minecolonies.api.crafting.ItemStorage(stack, true, matchNBT), count) >= count;
            final String detail = (provision.detail().isEmpty() ? "provision" : provision.detail())
                                   + ";delivered=" + delivered
                                   + (enough ? ";ok" : ";partial");
            // Payment always treated completed when catalogue fill ran; cost is notional only.
            recordOrder(orderId, colony.getID(), colonyName, safeOwner, ownerName, citizenName, itemId, count, enough || delivered > 0,
              provision.notionalCostMinor(), detail, nowMs);
            return enough;
        }
        catch (final ClassNotFoundException | NoSuchMethodException | NoClassDefFoundError e)
        {
            reflectFailed = true;
            Log.getLogger().warn("QuidCraft present but provision API reflect failed; disabling bridge: " + e);
            return false;
        }
        catch (final Throwable t)
        {
            COOLDOWNS.put(coolKey, nowMs + COOLDOWN_MS);
            recordOrder(orderId, colony.getID(), colonyName, safeOwner, ownerName, citizenName, itemId, count, false, 0L, t.toString(), nowMs);
            Log.getLogger().debug("QuidCraft provision buy failed for {}: {}", itemId, t.toString());
            return false;
        }
    }

    /**
     * After a structure lists needed resources, try to provision each shortage into the building.
     */
    public static void tryProvisionNeededResources(@NotNull final IBuilding building, @NotNull final String citizenName)
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
                final int have = InventoryUtils.hasBuildingEnoughElseCount(building,
                  new com.minecolonies.api.crafting.ItemStorage(stack, true, false), entry.getAmount());
                final int shortage = entry.getAmount() - have;
                if (shortage > 0)
                {
                    tryBuyForBuilding(building, stack, shortage, false, citizenName);
                }
            }
        }
        catch (final Throwable t)
        {
            Log.getLogger().debug("QuidCraft structure provision sweep failed: {}", t.toString());
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

    private static ProvisionResult invokeProvision(
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
            // Older QuidCraft without provision API — treat as absent catalogue for fallthrough.
            return new ProvisionResult(false, 0, 0L, "no-provision-api");
        }
        final Object result = access.getClass().getMethod(
          "provisionMarketBuy",
          UUID.class, String.class, int.class, String.class, int.class, int.class, long.class
        ).invoke(access, UUID.randomUUID(), itemId, quantity, dimension, x, z, now);
        if (result == null)
        {
            return new ProvisionResult(false, 0, 0L, "null-result");
        }
        final boolean onCatalogue = Boolean.TRUE.equals(result.getClass().getMethod("onCatalogue").invoke(result));
        final int filled = ((Number) result.getClass().getMethod("filled").invoke(result)).intValue();
        final long cost = ((Number) result.getClass().getMethod("notionalCostMinor").invoke(result)).longValue();
        final Object detail = result.getClass().getMethod("detail").invoke(result);
        return new ProvisionResult(onCatalogue, filled, cost, detail == null ? "" : detail.toString());
    }

    private static int depositIntoBuilding(
      @NotNull final IBuilding building,
      @NotNull final ItemStack prototype,
      final int amount,
      final boolean matchNBT)
    {
        if (amount <= 0)
        {
            return 0;
        }
        final Item item = prototype.getItem();
        // Prefer plain stacks unless NBT matching was requested.
        ItemStack toGive = matchNBT ? prototype.copyWithCount(amount) : new ItemStack(item, amount);
        if (toGive.isEmpty())
        {
            toGive = new ItemStack(item, amount);
        }
        int remaining = amount;
        while (remaining > 0 && !toGive.isEmpty())
        {
            final int batch = Math.min(remaining, Math.max(1, toGive.getMaxStackSize()));
            ItemStack slice = toGive.copyWithCount(batch);
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
            final int placed = batch - (leftover.isEmpty() ? 0 : leftover.getCount());
            if (placed <= 0)
            {
                break;
            }
            remaining -= placed;
            toGive.setCount(remaining);
        }
        return amount - remaining;
    }

    private record ProvisionResult(boolean onCatalogue, int filled, long notionalCostMinor, String detail)
    {
    }
}

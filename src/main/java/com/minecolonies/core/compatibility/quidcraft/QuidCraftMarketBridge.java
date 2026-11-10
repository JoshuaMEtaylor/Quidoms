package com.minecolonies.core.compatibility.quidcraft;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soft QuidCraft marketplace bridge. Uses reflection so Quidoms compiles and runs without QuidCraft.
 * When QuidCraft is loaded and ready, missing worker materials can be bought from the materials market
 * against the colony owner's Quid account before/alongside the normal request system.
 */
public final class QuidCraftMarketBridge
{
    public static final String MOD_ID = "quidcraft";

    /** Per colony+item cooldown after an attempt (ms). */
    private static final long COOLDOWN_MS = 15_000L;

    private static final Map<String, Long> COOLDOWNS = new ConcurrentHashMap<>();

    private static Boolean presentCached;
    private static boolean reflectFailed;

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

    /**
     * Attempt to buy {@code count} of {@code stack}'s item from QuidCraft into the colony owner's inventory,
     * then move matching stacks into the building. Returns true if the building now holds enough.
     */
    public static boolean tryBuyForBuilding(@NotNull final IBuilding building, @NotNull final ItemStack stack, final int count, final boolean matchNBT)
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
        COOLDOWNS.put(coolKey, nowMs + COOLDOWN_MS);

        try
        {
            if (!quidReady())
            {
                return false;
            }

            final UUID ownerId = colony.getPermissions().getOwner();
            if (ownerId == null)
            {
                return false;
            }

            final Level world = colony.getWorld();
            final ServerPlayer owner = world.getServer() == null ? null : world.getServer().getPlayerList().getPlayer(ownerId);
            final String ownerName = colony.getPermissions().getOwnerName() == null ? "colony-owner" : colony.getPermissions().getOwnerName();

            final BlockPos pos = building.getPosition();
            final String dimension = world.dimension().location().toString();
            final Object economy = economyHostGet();
            if (economy == null)
            {
                return false;
            }

            final Object actor = newActor(ownerId, ownerName, false, 0);
            final Object buySide = tradeSideBuy();
            final UUID requestId = UUID.randomUUID();

            final int before = owner == null ? 0 : countMatching(owner, stack, matchNBT);

            final Object outcome = invokeTrade(economy, actor, requestId, itemId, buySide, count, dimension, pos.getX(), pos.getZ(), System.currentTimeMillis());
            if (outcome == null || !outcomeOk(outcome))
            {
                return false;
            }

            // Pull Claims into inventory when the owner is online.
            if (owner != null)
            {
                tryClaimAll(economy, actor);
            }

            if (owner == null)
            {
                // Purchase may sit in Claims until the owner is online; request system still runs.
                Log.getLogger().info("QuidCraft bought {} x{} for colony {} (owner offline — Claims).", itemId, count, colony.getName());
                return false;
            }

            final int after = countMatching(owner, stack, matchNBT);
            final int gained = Math.max(0, after - before);
            if (gained <= 0)
            {
                // Bought into Claims or already reserved; leave request path active.
                return false;
            }

            final int toMove = Math.min(gained, count);
            moveFromPlayerToBuilding(owner, building, stack, toMove, matchNBT);

            // Same ItemStorage flag pattern as AbstractEntityAIBasic request checks.
            final int inBuilding = InventoryUtils.hasBuildingEnoughElseCount(building,
              new com.minecolonies.api.crafting.ItemStorage(stack, true, matchNBT), Math.min(toMove, count));
            return inBuilding >= Math.min(toMove, count);
        }
        catch (final ClassNotFoundException | NoSuchMethodException | NoClassDefFoundError e)
        {
            reflectFailed = true;
            Log.getLogger().warn("QuidCraft present but market API reflect failed; disabling bridge: " + e);
            return false;
        }
        catch (final Throwable t)
        {
            Log.getLogger().debug("QuidCraft market buy failed for {}: {}", BuiltInRegistries.ITEM.getKey(stack.getItem()), t.toString());
            return false;
        }
    }

    private static boolean quidReady() throws Exception
    {
        final Class<?> api = Class.forName("com.quidcraft.api.QuidApi");
        final Method ready = api.getMethod("ready");
        return Boolean.TRUE.equals(ready.invoke(null));
    }

    private static Object economyHostGet() throws Exception
    {
        final Class<?> host = Class.forName("com.quidcraft.server.EconomyHost");
        return host.getMethod("get").invoke(null);
    }

    private static Object newActor(final UUID id, final String name, final boolean spectator, final int perm) throws Exception
    {
        final Class<?> actorCl = Class.forName("com.quidcraft.economy.Actor");
        final Constructor<?> ctor = actorCl.getConstructor(UUID.class, String.class, boolean.class, int.class);
        return ctor.newInstance(id, name, spectator, perm);
    }

    private static Object tradeSideBuy() throws Exception
    {
        final Class<?> sideCl = Class.forName("com.quidcraft.market.TradeSide");
        return Enum.valueOf(sideCl.asSubclass(Enum.class), "BUY");
    }

    private static Object invokeTrade(
      final Object economy,
      final Object actor,
      final UUID requestId,
      final String itemId,
      final Object side,
      final int quantity,
      final String dimension,
      final int x,
      final int z,
      final long now) throws Exception
    {
        final Method trade = economy.getClass().getMethod(
          "trade",
          Class.forName("com.quidcraft.economy.Actor"),
          UUID.class,
          String.class,
          Class.forName("com.quidcraft.market.TradeSide"),
          int.class,
          String.class,
          int.class,
          int.class,
          long.class);
        return trade.invoke(economy, actor, requestId, itemId, side, quantity, dimension, x, z, now);
    }

    private static boolean outcomeOk(final Object outcome) throws Exception
    {
        final Method ok = outcome.getClass().getMethod("ok");
        return Boolean.TRUE.equals(ok.invoke(outcome));
    }

    private static void tryClaimAll(final Object economy, final Object actor)
    {
        try
        {
            final Method claimAll = economy.getClass().getMethod(
              "claimAll",
              Class.forName("com.quidcraft.economy.Actor"),
              UUID.class,
              long.class);
            claimAll.invoke(economy, actor, UUID.randomUUID(), System.currentTimeMillis());
        }
        catch (final Throwable ignored)
        {
            // Optional nicety; buy may already have delivered to inventory.
        }
    }

    private static int countMatching(@NotNull final ServerPlayer player, @NotNull final ItemStack stack, final boolean matchNBT)
    {
        final IItemHandler handler = new InvWrapper(player.getInventory());
        return InventoryUtils.getItemCountInItemHandler(handler,
          s -> ItemStackUtils.compareItemStacksIgnoreStackSize(s, stack, true, matchNBT));
    }

    private static void moveFromPlayerToBuilding(
      @NotNull final ServerPlayer player,
      @NotNull final IBuilding building,
      @NotNull final ItemStack stack,
      final int count,
      final boolean matchNBT)
    {
        final IItemHandler playerInv = new InvWrapper(player.getInventory());
        int remaining = count;
        for (int slot = 0; slot < playerInv.getSlots() && remaining > 0; slot++)
        {
            final ItemStack inSlot = playerInv.getStackInSlot(slot);
            if (!ItemStackUtils.compareItemStacksIgnoreStackSize(inSlot, stack, true, matchNBT))
            {
                continue;
            }
            final int take = Math.min(remaining, inSlot.getCount());
            final ItemStack extracted = playerInv.extractItem(slot, take, false);
            if (extracted.isEmpty())
            {
                continue;
            }
            ItemStack leftover = extracted;
            if (building instanceof AbstractBuilding abstractBuilding)
            {
                leftover = abstractBuilding.forceTransferStack(extracted, building.getColony().getWorld());
                if (leftover == null)
                {
                    leftover = ItemStack.EMPTY;
                }
            }
            else if (building.getTileEntity() != null)
            {
                leftover = InventoryUtils.addItemStackToProviderWithResult(building.getTileEntity(), extracted);
                if (leftover == null)
                {
                    leftover = ItemStack.EMPTY;
                }
            }
            if (!leftover.isEmpty())
            {
                // Return what did not fit.
                playerInv.insertItem(slot, leftover, false);
                remaining -= (take - leftover.getCount());
            }
            else
            {
                remaining -= take;
            }
        }
    }
}

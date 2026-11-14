package com.minecolonies.core.compatibility.quidcraft;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Soft QuidCraft bridge for kingdom founding cost and treasury account bootstrap.
 * Quidoms does not hard-depend on QuidCraft; when QuidCraft is absent founding is free.
 */
public final class QuidCraftKingdomBridge
{
    public static final String MOD_ID = "quidcraft";

    /** Minimum founding cost: 1000 Quid (pennies). */
    public static final long FOUNDING_COST_MINOR = 1000L * 100L;

    private static Boolean presentCached;
    private static boolean reflectFailed;

    private QuidCraftKingdomBridge()
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
     * Charge the founding cost from the player's personal Quid into a new kingdom treasury account.
     *
     * @return {@code true} when founding may continue (charged, or QuidCraft not ready)
     */
    public static boolean tryFundKingdomFounding(final ServerPlayer player, final String colonyName)
    {
        if (!isPresent())
        {
            return true;
        }
        try
        {
            if (!quidReady())
            {
                return true;
            }
            final Object access = quidAccess();
            if (access == null)
            {
                return true;
            }

            final String name = colonyName.trim();
            final UUID kingdomId = kingdomAccountId(name);
            final long now = System.currentTimeMillis();
            access.getClass()
              .getMethod("openAccount", UUID.class, String.class, long.class)
              .invoke(access, kingdomId, name, now);

            final UUID playerId = (UUID) player.getClass().getMethod("getUUID").invoke(player);
            final Object account = access.getClass().getMethod("account", UUID.class).invoke(access, playerId);
            final long available = accountAvailable(account);
            if (available < FOUNDING_COST_MINOR)
            {
                notifyNeedQuid(player, access);
                return false;
            }

            final UUID requestId = UUID.nameUUIDFromBytes(
              ("founding:" + playerId + ":" + name).getBytes(StandardCharsets.UTF_8));
            final Object prior = access.getClass().getMethod("find", UUID.class, UUID.class).invoke(access, playerId, requestId);
            if (prior != null && Boolean.TRUE.equals(prior.getClass().getMethod("ok").invoke(prior)))
            {
                return true;
            }

            final Object result = access.getClass()
              .getMethod("transfer", UUID.class, UUID.class, UUID.class, long.class, String.class, UUID.class, long.class)
              .invoke(access, requestId, playerId, kingdomId, FOUNDING_COST_MINOR, "Kingdom founding", playerId, now);
            if (result == null || !Boolean.TRUE.equals(result.getClass().getMethod("ok").invoke(result)))
            {
                notifyNeedQuid(player, access);
                return false;
            }
            return true;
        }
        catch (final Throwable ex)
        {
            reflectFailed = true;
            System.err.println("QuidCraft kingdom founding bridge failed: " + ex);
            return true;
        }
    }

    public static UUID kingdomAccountId(final String colonyName)
    {
        return UUID.nameUUIDFromBytes(("kingdom:" + colonyName.trim()).getBytes(StandardCharsets.UTF_8));
    }

    /** Pretty founding fee for UI / messages (defaults to £1000.00 when QuidCraft absent). */
    public static String formatFoundingCostPretty()
    {
        try
        {
            if (isPresent() && quidReady())
            {
                final Object access = quidAccess();
                if (access != null)
                {
                    return formatAmount(access, FOUNDING_COST_MINOR);
                }
            }
        }
        catch (final Throwable ignored)
        {
        }
        return formatAmount(null, FOUNDING_COST_MINOR);
    }

    private static void notifyNeedQuid(final ServerPlayer player, final Object access)
    {
        final String pretty = formatAmount(access, FOUNDING_COST_MINOR);
        try
        {
            final Class<?> component = Class.forName("net.minecraft.network.chat.Component");
            final Object msg = component.getMethod("translatable", String.class, Object[].class)
              .invoke(null, "com.minecolonies.core.founding.needquid", new Object[] {pretty});
            player.getClass().getMethod("sendSystemMessage", Class.forName("net.minecraft.network.chat.Component"))
              .invoke(player, msg);
            // Also surface as action-bar toast so closing the covenant is not silent.
            player.getClass().getMethod("displayClientMessage", Class.forName("net.minecraft.network.chat.Component"), boolean.class)
              .invoke(player, msg, true);
        }
        catch (final Throwable ignored)
        {
            try
            {
                final Object lit = Class.forName("net.minecraft.network.chat.Component").getMethod("literal", String.class)
                  .invoke(null, "Founding a kingdom costs " + pretty + ". Deposit more Quid, then try again.");
                player.getClass().getMethod("displayClientMessage", Class.forName("net.minecraft.network.chat.Component"), boolean.class)
                  .invoke(player, lit, false);
            }
            catch (final Throwable ignored2)
            {
            }
        }
    }

    private static boolean quidReady() throws Exception
    {
        final Class<?> api = Class.forName("com.quidcraft.api.QuidApi");
        return Boolean.TRUE.equals(api.getMethod("ready").invoke(null));
    }

    private static Object quidAccess() throws Exception
    {
        final Class<?> api = Class.forName("com.quidcraft.api.QuidApi");
        return api.getMethod("get").invoke(null);
    }

    private static long accountAvailable(final Object account) throws Exception
    {
        if (account == null)
        {
            return 0L;
        }
        try
        {
            return ((Number) account.getClass().getMethod("available").invoke(account)).longValue();
        }
        catch (final NoSuchMethodException ignored)
        {
            final long balance = ((Number) account.getClass().getMethod("balance").invoke(account)).longValue();
            long reserved = 0L;
            try
            {
                reserved = ((Number) account.getClass().getMethod("reserved").invoke(account)).longValue();
            }
            catch (final NoSuchMethodException ignored2)
            {
            }
            return balance - reserved;
        }
    }

    private static String formatAmount(final Object access, final long minor)
    {
        try
        {
            return String.valueOf(access.getClass().getMethod("format", long.class).invoke(access, minor));
        }
        catch (final Throwable ignored)
        {
            return "£" + (minor / 100L) + "." + String.format("%02d", minor % 100L);
        }
    }

    /** Remaining Infrastructure allotment for builder market buys. Soft-fail open if API missing. */
    public static long infraBudgetRemaining(final String kingdomName)
    {
        try
        {
            if (!isPresent() || !quidReady())
            {
                return Long.MAX_VALUE / 4L;
            }
            final Object access = quidAccess();
            final Object value = access.getClass().getMethod("infraBudgetRemaining", String.class).invoke(access, kingdomName);
            return value instanceof Number ? ((Number) value).longValue() : 0L;
        }
        catch (final Throwable ignored)
        {
            return Long.MAX_VALUE / 4L;
        }
    }

    /** Charge kingdom treasury against Infrastructure allotment after a successful provision. */
    public static boolean chargeInfra(final String kingdomName, final long amount, final UUID actor, final long now)
    {
        if (amount <= 0L)
        {
            return true;
        }
        try
        {
            if (!isPresent() || !quidReady())
            {
                return true;
            }
            final Object access = quidAccess();
            final Object result = access.getClass()
              .getMethod("chargeKingdomInfra", UUID.class, String.class, long.class, UUID.class, long.class)
              .invoke(access, UUID.randomUUID(), kingdomName, amount, actor, now);
            return result != null && Boolean.TRUE.equals(result.getClass().getMethod("ok").invoke(result));
        }
        catch (final NoSuchMethodException missing)
        {
            return true;
        }
        catch (final Throwable ex)
        {
            System.err.println("Infra charge failed: " + ex);
            return false;
        }
    }
}

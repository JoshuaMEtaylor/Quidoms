package com.minecolonies.core.compatibility.quidcraft;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.quidcraft.api.AccountRef;
import com.quidcraft.api.QuidAccess;
import com.quidcraft.api.QuidApi;
import com.quidcraft.api.QuidResult;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Tip bridge for QuidCraft kingdom founding.
 * All player feedback uses QuidCraft HUD toast only — never chat / action-bar.
 */
public final class QuidCraftKingdomBridge
{
    public static final String MOD_ID = "quidcraft";

    /** Minimum founding cost: 1000 Quid (pennies). */
    public static final long FOUNDING_COST_MINOR = 1000L * 100L;

    private static Boolean presentCached;

    private QuidCraftKingdomBridge()
    {
    }

    public static boolean isPresent()
    {
        if (presentCached == null)
        {
            presentCached = ModList.get().isLoaded(MOD_ID);
        }
        return presentCached;
    }

    /**
     * Charge founding cost from the player's personal Quid into a new kingdom treasury.
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
            if (!QuidApi.ready())
            {
                return true;
            }
            final QuidAccess access = QuidApi.get();
            if (access == null)
            {
                return true;
            }

            final String name = colonyName == null ? "" : colonyName.trim();
            if (name.isEmpty())
            {
                toastFunds(player);
                return false;
            }

            final UUID kingdomId = kingdomAccountId(name);
            final long now = System.currentTimeMillis();
            access.openAccount(kingdomId, name, now);

            final UUID playerId = player.getUUID();
            final AccountRef account = access.account(playerId);
            final long available = account == null ? 0L : account.available();
            if (available < FOUNDING_COST_MINOR)
            {
                toastFunds(player);
                return false;
            }

            final UUID requestId = UUID.nameUUIDFromBytes(
              ("founding:" + playerId + ":" + name).getBytes(StandardCharsets.UTF_8));
            final QuidResult prior = access.find(playerId, requestId);
            if (prior != null && prior.ok())
            {
                return true;
            }

            final QuidResult result = access.transfer(
              requestId, playerId, kingdomId, FOUNDING_COST_MINOR, "Kingdom founding", playerId, now);
            if (result == null || !result.ok())
            {
                toastFunds(player);
                return false;
            }
            return true;
        }
        catch (final NoClassDefFoundError | ExceptionInInitializerError missing)
        {
            System.err.println("QuidCraft founding API missing: " + missing);
            return true;
        }
        catch (final Throwable ex)
        {
            System.err.println("QuidCraft kingdom founding bridge failed: " + ex);
            ex.printStackTrace();
            toast(player, "quidcraft.toast.founding_failed");
            return false;
        }
    }

    public static UUID kingdomAccountId(final String colonyName)
    {
        final String name = colonyName == null ? "" : colonyName.trim();
        return UUID.nameUUIDFromBytes(("kingdom:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static String formatFoundingCostPretty()
    {
        try
        {
            if (isPresent() && QuidApi.ready())
            {
                final QuidAccess access = QuidApi.get();
                if (access != null)
                {
                    return access.format(FOUNDING_COST_MINOR);
                }
            }
        }
        catch (final Throwable ignored)
        {
        }
        return "£" + (FOUNDING_COST_MINOR / 100L) + "." + String.format("%02d", FOUNDING_COST_MINOR % 100L);
    }

    /** Afford fail: “Not enough quid”. No chat / action-bar. */
    public static void toastFunds(final ServerPlayer player)
    {
        toast(player, "quidcraft.toast.funds");
    }

    /**
     * Server → client QuidCraft HUD toast. Never chat, never action-bar.
     * Soft no-op when QuidCraft is absent.
     */
    public static void toast(final ServerPlayer player, final String messageKey)
    {
        if (player == null || messageKey == null || messageKey.isBlank())
        {
            return;
        }
        try
        {
            if (!isPresent() || !QuidApi.ready())
            {
                return;
            }
            final QuidAccess access = QuidApi.get();
            if (access == null)
            {
                return;
            }
            if ("quidcraft.toast.funds".equals(messageKey) || "quidcraft.fail.funds".equals(messageKey))
            {
                access.toastFunds(player.getUUID());
            }
            else
            {
                access.toast(player.getUUID(), messageKey);
            }
        }
        catch (final Throwable ex)
        {
            System.err.println("QuidCraft toast failed: " + ex);
        }
    }

    public static long infraBudgetRemaining(final String kingdomName)
    {
        try
        {
            if (!isPresent() || !QuidApi.ready())
            {
                return Long.MAX_VALUE / 4L;
            }
            return QuidApi.get().infraBudgetRemaining(kingdomName);
        }
        catch (final Throwable ignored)
        {
            return Long.MAX_VALUE / 4L;
        }
    }

    public static boolean chargeInfra(final String kingdomName, final long amount, final UUID actor, final long now)
    {
        if (amount <= 0L)
        {
            return true;
        }
        try
        {
            if (!isPresent() || !QuidApi.ready())
            {
                return true;
            }
            final QuidResult result = QuidApi.get().chargeKingdomInfra(UUID.randomUUID(), kingdomName, amount, actor, now);
            return result != null && result.ok();
        }
        catch (final NoSuchMethodError | NoClassDefFoundError missing)
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

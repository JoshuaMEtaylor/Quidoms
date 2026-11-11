package com.minecolonies.core.compatibility.quidcraft;

/**
 * Top-level outcome for QuidCraft provision attempts.
 * Kept out of {@link QuidCraftMarketBridge} so fat-jar / classloader paths cannot miss a nested
 * {@code QuidCraftMarketBridge$ProvisionResult} and permanently disable the bridge.
 */
public final class QuidCraftProvisionOutcome
{
    private final boolean onCatalogue;
    private final int filled;
    private final long notionalCostMinor;
    private final String detail;

    public QuidCraftProvisionOutcome(final boolean onCatalogue, final int filled, final long notionalCostMinor, final String detail)
    {
        this.onCatalogue = onCatalogue;
        this.filled = filled;
        this.notionalCostMinor = notionalCostMinor;
        this.detail = detail == null ? "" : detail;
    }

    public boolean onCatalogue()
    {
        return onCatalogue;
    }

    public int filled()
    {
        return filled;
    }

    public long notionalCostMinor()
    {
        return notionalCostMinor;
    }

    public String detail()
    {
        return detail;
    }
}

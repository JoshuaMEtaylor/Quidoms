/*
 * Decompiled with CFR 0.152.
 */
package com.minecolonies.core.compatibility.quidcraft;

public final class QuidCraftProvisionOutcome {
    private final boolean onCatalogue;
    private final int filled;
    private final long notionalCostMinor;
    private final String detail;

    public QuidCraftProvisionOutcome(boolean bl, int n, long l, String string) {
        this.onCatalogue = bl;
        this.filled = n;
        this.notionalCostMinor = l;
        this.detail = string == null ? "" : string;
    }

    public boolean onCatalogue() {
        return this.onCatalogue;
    }

    public int filled() {
        return this.filled;
    }

    public long notionalCostMinor() {
        return this.notionalCostMinor;
    }

    public String detail() {
        return this.detail;
    }
}

package com.minecolonies.api.colony.savedata;

/** Tracks re-entrant colony save-data loads so claim/reclaim cannot re-enter computeIfAbsent. */
public final class ColonySaveDataLoadGuard {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private ColonySaveDataLoadGuard() {}
    public static void enter() { DEPTH.set(DEPTH.get() + 1); }
    public static void exit() {
        int next = Math.max(0, DEPTH.get() - 1);
        if (next == 0) DEPTH.remove(); else DEPTH.set(next);
    }
    public static boolean isLoading() { return DEPTH.get() > 0; }
}

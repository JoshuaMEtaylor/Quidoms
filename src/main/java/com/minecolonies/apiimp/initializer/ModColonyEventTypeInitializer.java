package com.minecolonies.apiimp.initializer;

import com.minecolonies.api.colony.colonyEvents.registry.ColonyEventTypeRegistryEntry;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Initializer for colony event types, register new event types here.
 * Quidoms: raid colony event types are intentionally not registered.
 */
public final class ModColonyEventTypeInitializer
{
    public final static DeferredRegister<ColonyEventTypeRegistryEntry> DEFERRED_REGISTER = DeferredRegister.create(CommonMinecoloniesAPIImpl.COLONY_EVENT_TYPES, Constants.MOD_ID);

    private ModColonyEventTypeInitializer()
    {
        throw new IllegalStateException("Tried to initialize: ModColonyEventTypeInitializer but this is a Utility class.");
    }

    static
    {
        // Quidoms: colony raid event types are not registered.
    }
}

package com.quidoms.server;

import com.quidoms.entity.CitizenEntity;
import com.quidoms.registry.ModEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModBusEvents {
    private ModBusEvents() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModBusEvents::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CITIZEN.get(), CitizenEntity.createAttributes().build());
    }
}

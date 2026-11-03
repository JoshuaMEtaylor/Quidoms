package com.quidoms.server;

import com.quidoms.block.HouseBlock;
import com.quidoms.colony.CitizenData;
import com.quidoms.colony.Colony;
import com.quidoms.colony.ColonyManager;
import com.quidoms.entity.CitizenEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Periodic colony maintenance: respawn missing citizen entities, keep housing filled.
 * Intentionally has no raid / invasion hooks.
 */
public final class ColonyTicker {
    private ColonyTicker() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != ServerLevel.OVERWORLD) {
            return;
        }
        if (level.getServer().getTickCount() % 100 != 0) {
            return;
        }
        ColonyManager manager = ColonyManager.get(level);
        boolean dirty = false;
        for (Colony colony : manager.colonies().values()) {
            if (colony.citizens().size() < colony.housingCapacity()) {
                HouseBlock.fillHousing(level, colony, manager);
                dirty = true;
            }
            for (CitizenData data : colony.citizens().values()) {
                if (data.entityUuid().isPresent()) {
                    Entity existing = level.getEntity(data.entityUuid().get());
                    if (existing instanceof CitizenEntity) {
                        continue;
                    }
                }
                HouseBlock.spawnCitizen(level, colony, data);
                dirty = true;
            }
        }
        if (dirty) {
            manager.markChanged();
        }
    }
}

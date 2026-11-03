package com.quidoms.ai;

import java.util.Map;

import com.quidoms.build.BuilderWork;
import com.quidoms.colony.CitizenData;
import com.quidoms.colony.Colony;
import com.quidoms.colony.ColonyManager;
import com.quidoms.colony.JobType;
import com.quidoms.entity.CitizenEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class CitizenAi {
    private final CitizenEntity citizen;
    private int tickCounter;

    public CitizenAi(CitizenEntity citizen) {
        this.citizen = citizen;
    }

    public void tick(ServerLevel level) {
        tickCounter++;
        ColonyManager manager = ColonyManager.get(level);
        Colony colony = manager.byId(citizen.colonyId()).orElse(null);
        if (colony == null) {
            return;
        }
        CitizenData data = colony.citizens().get(citizen.citizenId());
        if (data == null) {
            return;
        }
        data.setLastPos(citizen.blockPosition());
        data.setEntityUuid(citizen.getUUID());

        // Sleep / eat loop (simple day-night saturation cycle).
        long dayTime = level.getDayTime() % 24000L;
        boolean night = dayTime >= 13000L && dayTime <= 23000L;
        if (night) {
            data.setSleeping(true);
            data.setSaturation(Math.min(20.0F, data.saturation() + 0.02F));
        } else {
            data.setSleeping(false);
            data.setSaturation(Math.max(0.0F, data.saturation() - 0.005F));
            if (data.saturation() < 4.0F) {
                // Seek home when hungry.
                data.homeBuildingId().map(id -> colony.buildings().get(id)).ifPresent(home -> {
                    moveToward(level, home.pos());
                });
            }
        }

        if (data.job() == JobType.BUILDER && !data.sleeping() && tickCounter % 10 == 0) {
            BuilderWork.tickBuilder(level, colony, data, citizen, manager);
        } else if (data.job() == JobType.UNEMPLOYED && !data.sleeping() && tickCounter % 40 == 0) {
            wanderNear(colony.center());
        }

        manager.markChanged();
    }

    private void moveToward(Level level, BlockPos target) {
        citizen.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0D);
    }

    private void wanderNear(BlockPos center) {
        if (citizen.getNavigation().isDone()) {
            double x = center.getX() + citizen.getRandom().nextInt(7) - 3;
            double z = center.getZ() + citizen.getRandom().nextInt(7) - 3;
            citizen.getNavigation().moveTo(x, center.getY(), z, 0.7D);
        }
    }
}

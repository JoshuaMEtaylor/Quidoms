package com.quidoms.block;

import com.quidoms.colony.BuildingData;
import com.quidoms.colony.BuildingType;
import com.quidoms.colony.CitizenData;
import com.quidoms.colony.Colony;
import com.quidoms.colony.ColonyManager;
import com.quidoms.entity.CitizenEntity;
import com.quidoms.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public class HouseBlock extends Block implements EntityBlock {
    private static final String[] NAMES = {
            "Avery", "Blair", "Casey", "Drew", "Eden", "Finley", "Gray", "Harper", "Indigo", "Jules"
    };

    public HouseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HutBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel) || placer == null) {
            return;
        }
        ColonyManager manager = ColonyManager.get(serverLevel);
        Colony colony = manager.byOwner(placer.getUUID()).or(() -> manager.byPos(pos)).orElse(null);
        if (colony == null) {
            if (placer instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("quidoms.message.need_colony"), true);
            }
            return;
        }
        BuildingData building = colony.addBuilding(BuildingType.HOUSE, pos);
        if (level.getBlockEntity(pos) instanceof HutBlockEntity hut) {
            hut.setColonyId(colony.id());
            hut.setBuildingId(building.id());
        }
        fillHousing(serverLevel, colony, manager);
        manager.markChanged();
    }

    public static void fillHousing(ServerLevel level, Colony colony, ColonyManager manager) {
        while (colony.citizens().size() < colony.housingCapacity()) {
            String name = NAMES[level.random.nextInt(NAMES.length)];
            CitizenData data = colony.addCitizen(name);
            BuildingData home = colony.buildings().values().stream()
                    .filter(b -> b.type() == BuildingType.HOUSE)
                    .filter(b -> colony.citizens().values().stream()
                            .filter(c -> c.homeBuildingId().orElse(-1).equals(b.id()))
                            .count() < b.type().bedSlots())
                    .findFirst()
                    .orElse(null);
            if (home != null) {
                data.setHomeBuildingId(home.id());
            }
            spawnCitizen(level, colony, data);
        }
        manager.markChanged();
    }

    public static void spawnCitizen(ServerLevel level, Colony colony, CitizenData data) {
        BlockPos spawn = colony.center().above();
        CitizenEntity entity = ModEntities.CITIZEN.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (entity == null) {
            return;
        }
        entity.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0.0F, 0.0F);
        entity.bind(colony.id(), data.id());
        entity.setCustomName(Component.literal(data.name()));
        entity.setCustomNameVisible(true);
        level.addFreshEntity(entity);
        data.setEntityUuid(entity.getUUID());
        data.setLastPos(spawn);
    }
}

package com.quidoms.block;

import com.quidoms.colony.BuildingData;
import com.quidoms.colony.BuildingType;
import com.quidoms.colony.Colony;
import com.quidoms.colony.ColonyManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public class WarehouseBlock extends Block implements EntityBlock {
    public WarehouseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarehouseBlockEntity(pos, state);
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
        BuildingData building = colony.addBuilding(BuildingType.WAREHOUSE, pos);
        if (level.getBlockEntity(pos) instanceof WarehouseBlockEntity warehouse) {
            warehouse.setColonyId(colony.id());
            warehouse.setBuildingId(building.id());
        }
        manager.markChanged();
        if (placer instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("quidoms.message.warehouse_ready"), false);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof WarehouseBlockEntity warehouse) {
            serverPlayer.openMenu(warehouse, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}

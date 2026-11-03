package com.quidoms.block;

import com.quidoms.colony.BuildingData;
import com.quidoms.colony.BuildingType;
import com.quidoms.colony.Colony;
import com.quidoms.colony.ColonyManager;
import com.quidoms.network.OpenColonyScreenS2C;
import com.quidoms.network.QuidomsNetwork;

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

public class TownHallBlock extends Block implements EntityBlock {
    public static final int INITIAL_CLAIM_RADIUS = 2;

    public TownHallBlock(Properties properties) {
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
        if (manager.byOwner(placer.getUUID()).isPresent()) {
            if (placer instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("quidoms.message.already_own_colony"), true);
            }
            return;
        }
        String name = placer.getName().getString() + "'s Colony";
        Colony colony = manager.createColony(serverLevel, placer.getUUID(), name, pos, INITIAL_CLAIM_RADIUS);
        BuildingData building = colony.addBuilding(BuildingType.TOWN_HALL, pos);
        manager.markChanged();
        if (level.getBlockEntity(pos) instanceof HutBlockEntity hut) {
            hut.setColonyId(colony.id());
            hut.setBuildingId(building.id());
        }
        if (placer instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("quidoms.message.colony_founded", name), false);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        ColonyManager manager = ColonyManager.get(serverLevel);
        Colony colony = null;
        if (level.getBlockEntity(pos) instanceof HutBlockEntity hut && hut.colonyId() > 0) {
            colony = manager.byId(hut.colonyId()).orElse(null);
        }
        if (colony == null) {
            colony = manager.byPos(pos).orElse(null);
        }
        if (colony == null) {
            serverPlayer.displayClientMessage(Component.translatable("quidoms.message.no_colony"), true);
            return InteractionResult.FAIL;
        }
        if (!colony.permissions().canAccess(serverPlayer.getUUID())
                && !colony.permissions().owner().equals(serverPlayer.getUUID())) {
            // Owner always has access; visitors with rank still allowed for overview in v1.
        }
        QuidomsNetwork.sendToPlayer(serverPlayer, OpenColonyScreenS2C.fromColony(colony));
        return InteractionResult.CONSUME;
    }
}

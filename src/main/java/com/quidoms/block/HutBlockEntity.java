package com.quidoms.block;

import com.quidoms.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class HutBlockEntity extends BlockEntity {
    private int colonyId;
    private int buildingId;

    public HutBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HUT.get(), pos, state);
    }

    public int colonyId() {
        return colonyId;
    }

    public void setColonyId(int colonyId) {
        this.colonyId = colonyId;
        setChanged();
    }

    public int buildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("ColonyId", colonyId);
        output.putInt("BuildingId", buildingId);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        colonyId = input.getIntOr("ColonyId", 0);
        buildingId = input.getIntOr("BuildingId", 0);
    }
}

package com.quidoms.registry;

import com.quidoms.QuidomsMod;
import com.quidoms.block.HutBlockEntity;
import com.quidoms.block.WarehouseBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuidomsMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HutBlockEntity>> HUT =
            BLOCK_ENTITIES.register("hut", () -> new BlockEntityType<>(
                    HutBlockEntity::new,
                    ModBlocks.TOWN_HALL.get(),
                    ModBlocks.HOUSE.get(),
                    ModBlocks.BUILDER_HUT.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarehouseBlockEntity>> WAREHOUSE =
            BLOCK_ENTITIES.register("warehouse", () -> new BlockEntityType<>(
                    WarehouseBlockEntity::new,
                    ModBlocks.WAREHOUSE.get()));

    private ModBlockEntities() {
    }
}

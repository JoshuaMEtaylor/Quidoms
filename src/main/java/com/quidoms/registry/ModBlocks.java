package com.quidoms.registry;

import com.quidoms.QuidomsMod;
import com.quidoms.block.BuilderHutBlock;
import com.quidoms.block.HouseBlock;
import com.quidoms.block.TownHallBlock;
import com.quidoms.block.WarehouseBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QuidomsMod.MOD_ID);

    private static BlockBehaviour.Properties hutProps(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(2.5F, 6.0F)
                .sound(SoundType.WOOD)
                .requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<TownHallBlock> TOWN_HALL = BLOCKS.register("town_hall",
            () -> new TownHallBlock(hutProps(MapColor.WOOD)));
    public static final DeferredBlock<HouseBlock> HOUSE = BLOCKS.register("house",
            () -> new HouseBlock(hutProps(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<BuilderHutBlock> BUILDER_HUT = BLOCKS.register("builder_hut",
            () -> new BuilderHutBlock(hutProps(MapColor.COLOR_BROWN)));
    public static final DeferredBlock<WarehouseBlock> WAREHOUSE = BLOCKS.register("warehouse",
            () -> new WarehouseBlock(hutProps(MapColor.COLOR_LIGHT_GRAY)));

    private ModBlocks() {
    }
}

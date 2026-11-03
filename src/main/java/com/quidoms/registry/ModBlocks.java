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

    // NeoForge 1.21.10 requires Properties.setId via registerBlock(... Supplier).
    public static final DeferredBlock<TownHallBlock> TOWN_HALL = BLOCKS.registerBlock(
            "town_hall", TownHallBlock::new, () -> hutProps(MapColor.WOOD));
    public static final DeferredBlock<HouseBlock> HOUSE = BLOCKS.registerBlock(
            "house", HouseBlock::new, () -> hutProps(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<BuilderHutBlock> BUILDER_HUT = BLOCKS.registerBlock(
            "builder_hut", BuilderHutBlock::new, () -> hutProps(MapColor.COLOR_BROWN));
    public static final DeferredBlock<WarehouseBlock> WAREHOUSE = BLOCKS.registerBlock(
            "warehouse", WarehouseBlock::new, () -> hutProps(MapColor.COLOR_LIGHT_GRAY));

    private ModBlocks() {
    }
}

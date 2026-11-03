package com.quidoms.registry;

import com.quidoms.QuidomsMod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuidomsMod.MOD_ID);

    public static final DeferredItem<BlockItem> TOWN_HALL = ITEMS.registerSimpleBlockItem("town_hall", ModBlocks.TOWN_HALL);
    public static final DeferredItem<BlockItem> HOUSE = ITEMS.registerSimpleBlockItem("house", ModBlocks.HOUSE);
    public static final DeferredItem<BlockItem> BUILDER_HUT = ITEMS.registerSimpleBlockItem("builder_hut", ModBlocks.BUILDER_HUT);
    public static final DeferredItem<BlockItem> WAREHOUSE = ITEMS.registerSimpleBlockItem("warehouse", ModBlocks.WAREHOUSE);

    public static final DeferredItem<Item> COLONY_BANNER = ITEMS.registerSimpleItem("colony_banner");

    private ModItems() {
    }
}

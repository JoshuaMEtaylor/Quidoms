package com.quidoms.registry;

import com.quidoms.QuidomsMod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuidomsMod.MOD_ID);

    // Prefer Holder overloads so Item.Properties get setId() on 1.21.10.
    public static final DeferredItem<BlockItem> TOWN_HALL = ITEMS.registerSimpleBlockItem(ModBlocks.TOWN_HALL);
    public static final DeferredItem<BlockItem> HOUSE = ITEMS.registerSimpleBlockItem(ModBlocks.HOUSE);
    public static final DeferredItem<BlockItem> BUILDER_HUT = ITEMS.registerSimpleBlockItem(ModBlocks.BUILDER_HUT);
    public static final DeferredItem<BlockItem> WAREHOUSE = ITEMS.registerSimpleBlockItem(ModBlocks.WAREHOUSE);

    public static final DeferredItem<Item> COLONY_BANNER = ITEMS.registerSimpleItem("colony_banner");

    private ModItems() {
    }
}

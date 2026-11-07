package com.minecolonies.api.blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * NeoForge 1.21.10 requires Properties.setId before Block/Item construction.
 * Quidoms still constructs blocks in {@code ModBlocksInitializer} before Registry.register;
 * stash the pending id here.
 */
public final class MinecoloniesBlockIds
{
    private static final ThreadLocal<ResourceLocation> PENDING_BLOCK = new ThreadLocal<>();
    private static final ThreadLocal<ResourceLocation> PENDING_ITEM = new ThreadLocal<>();

    private MinecoloniesBlockIds() {}

    public static void pushBlock(final ResourceLocation id) { PENDING_BLOCK.set(id); }
    public static void popBlock() { PENDING_BLOCK.remove(); }
    public static void pushItem(final ResourceLocation id) { PENDING_ITEM.set(id); }
    public static void popItem() { PENDING_ITEM.remove(); }

    public static BlockBehaviour.Properties withPendingBlockId(final BlockBehaviour.Properties properties)
    {
        final ResourceLocation id = PENDING_BLOCK.get();
        return id != null ? properties.setId(ResourceKey.create(Registries.BLOCK, id)) : properties;
    }

    public static Item.Properties withPendingItemId(final Item.Properties properties)
    {
        final ResourceLocation id = PENDING_ITEM.get();
        return id != null ? properties.setId(ResourceKey.create(Registries.ITEM, id)) : properties;
    }

    public static Item.Properties itemProps(final String path)
    {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(com.minecolonies.api.util.constant.Constants.MOD_ID, path)));
    }

    public static Item.Properties itemProps(final String path, final Item.Properties base)
    {
        return base.setId(ResourceKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(com.minecolonies.api.util.constant.Constants.MOD_ID, path)));
    }
}


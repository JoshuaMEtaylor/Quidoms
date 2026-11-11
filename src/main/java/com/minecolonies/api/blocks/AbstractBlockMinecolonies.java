package com.minecolonies.api.blocks;

import com.minecolonies.api.blocks.interfaces.IBlockMinecolonies;
import net.minecraft.core.Registry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public abstract class AbstractBlockMinecolonies<B extends AbstractBlockMinecolonies<B>> extends Block implements IBlockMinecolonies<B>
{
    public AbstractBlockMinecolonies(final Properties properties)
    {
        super(MinecoloniesBlockIds.withPendingBlockId(properties));
    }

    @Override
    public void registerBlockItem(final Registry<Item> registry, final Item.Properties properties)
    {
        MinecoloniesBlockIds.pushItem(getRegistryName());
        try
        {
            Registry.register(registry, getRegistryName(), new BlockItem(this, MinecoloniesBlockIds.withPendingItemId(properties)));
        }
        finally
        {
            MinecoloniesBlockIds.popItem();
        }
    }

    @Override
    public B registerBlock(final Registry<Block> registry)
    {
        Registry.register(registry, getRegistryName(), this);
        return (B) this;
    }
}

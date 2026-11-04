package com.minecolonies.api.items;

import com.minecolonies.api.blocks.AbstractBlockHut;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A custom item class for hut blocks.
 */
public class ItemBlockHut extends BlockItem
{
    /**
     * This items block.
     */
    private AbstractBlockHut<?> block;

    /**
     * Creates a new ItemBlockHut representing the item form of the given {@link AbstractBlockHut}.
     * 
     * @param block   the {@link AbstractBlockHut} this item represents.
     * @param builder the item properties to use.
     */
    public ItemBlockHut(AbstractBlockHut<?> block, Properties builder)
    {
        super(block, builder);
        this.block = block;
    }

    /**
     * Prefer building translation keys (e.g. "Town Hall") over raw registry/description ids.
     */
    @Override
    public @NotNull Component getName(@NotNull final ItemStack stack)
    {
        try
        {
            if (block.getBuildingEntry() != null)
            {
                return Component.translatable(block.getBuildingEntry().getTranslationKey());
            }
        }
        catch (final Exception ignored)
        {
            // Fall through to block/item description id.
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull final ItemStack stack, @NotNull final TooltipContext context, @NotNull final TooltipDisplay display, @NotNull final Consumer<Component> tooltip, @NotNull final TooltipFlag flags)
    {
        super.appendHoverText(stack, context, display, tooltip, flags);
        block.appendHoverText(stack, context, display, tooltip, flags);
    }
}

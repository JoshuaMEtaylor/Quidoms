package com.minecolonies.core.items;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import static com.minecolonies.api.util.constant.Constants.STACKSIZE;

/**
 * Class describing the Ancient Tome item.
 */
public class ItemAncientTome extends AbstractItemMinecolonies
{
    /**
     * Sets the name, creative tab, and registers the Ancient Tome item.
     *
     * @param properties the properties.
     */
    public ItemAncientTome(final Properties properties)
    {
        super("ancienttome", properties.stacksTo(STACKSIZE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false));
    }

    @Override
    public void inventoryTick(final ItemStack stack, final ServerLevel worldIn, final Entity entityIn, final EquipmentSlot slot)
    {
        super.inventoryTick(stack, worldIn, entityIn, slot);
        // Quidoms: raids disabled; keep glint off.
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        final IColony colony = IColonyManager.getInstance().getClosestColony(worldIn, entityIn.blockPosition());
        if (colony != null)
        {
            // retained colony lookup for future non-raid use; glint stays false under Quidoms cuts
        }
    }
}

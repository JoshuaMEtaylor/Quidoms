package com.minecolonies.core.items;

import com.minecolonies.api.blocks.MinecoloniesBlockIds;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.NotNull;

/**
 * Class handling the Plate Armor.
 */
public class ItemPlateArmor extends Item
{
    /**
     * Constructor method for the Plate Armor
     *
     * @param name            the name.
     * @param materialIn      the material of the armour.
     * @param equipmentSlotIn the equipment slot of it.
     * @param durability      custom max damage (overrides material formula).
     */
    public ItemPlateArmor(
      @NotNull final String name,
      @NotNull final ArmorMaterial materialIn,
      @NotNull final ArmorType equipmentSlotIn,
      final int durability)
    {
        super(MinecoloniesBlockIds.itemProps(name, new Item.Properties().humanoidArmor(materialIn, equipmentSlotIn).durability(durability)));
    }
}

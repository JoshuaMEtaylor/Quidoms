package com.minecolonies.core.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.NotNull;

/**
 * Class handling the Pirate Gear.
 */
public class ItemPirateGear extends Item
{
    /**
     * Constructor method for the Pirate Gear
     *
     * @param name            the name.
     * @param materialIn      the material of the armour.
     * @param equipmentSlotIn the equipment slot of it.
     * @param durability      custom max damage (overrides material formula).
     */
    public ItemPirateGear(
      @NotNull final String name,
      @NotNull final ArmorMaterial materialIn,
      @NotNull final ArmorType equipmentSlotIn,
      final int durability)
    {
        super(new Item.Properties().humanoidArmor(materialIn, equipmentSlotIn).durability(durability));
    }
}

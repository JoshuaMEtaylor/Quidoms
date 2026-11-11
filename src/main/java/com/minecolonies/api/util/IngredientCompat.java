package com.minecolonies.api.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Arrays;

/**
 * Helpers for Ingredient / SizedIngredient API changes in NeoForge 1.21.10
 * (getItems() removed; use {@link Ingredient#items()} holders).
 */
public final class IngredientCompat
{
    private IngredientCompat() {}

    public static ItemStack[] getItems(final Ingredient ingredient)
    {
        return ingredient.items().map(ItemStack::new).toArray(ItemStack[]::new);
    }

    public static ItemStack[] getItems(final SizedIngredient sized)
    {
        return sized.ingredient().items()
                 .map(h -> new ItemStack(h, sized.count()))
                 .toArray(ItemStack[]::new);
    }

    public static boolean isEmpty(final SizedIngredient sized)
    {
        return sized.ingredient().isEmpty() || sized.count() <= 0 || getItems(sized).length == 0;
    }
}

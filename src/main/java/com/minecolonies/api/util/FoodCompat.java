package com.minecolonies.api.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Compatibility helpers for NeoForge 1.21.10 food/consumable APIs.
 */
public final class FoodCompat
{
    private FoodCompat() {}

    @Nullable
    public static FoodProperties getFoodProperties(final ItemStack stack)
    {
        return stack.get(DataComponents.FOOD);
    }

    @Nullable
    public static FoodProperties getFoodProperties(final ItemStack stack, @Nullable final LivingEntity eater)
    {
        return getFoodProperties(stack);
    }

    public static boolean isEdible(final ItemStack stack)
    {
        return getFoodProperties(stack) != null || stack.has(DataComponents.CONSUMABLE);
    }

    /**
     * Legacy {@code FoodProperties#usingConvertsTo()} was removed; remainder items are no longer
     * encoded on FoodProperties in 1.21.10.
     */
    public static Optional<ItemStack> usingConvertsTo(final ItemStack stack)
    {
        return Optional.empty();
    }
}

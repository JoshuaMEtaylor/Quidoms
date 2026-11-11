package com.minecolonies.core.util;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.compatibility.IFurnaceRecipes;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.crafting.RecipeStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class FurnaceRecipes implements IFurnaceRecipes
{
    /**
     * Furnace recipes.
     */
    private Map<ItemStorage, RecipeStorage> recipes = new HashMap<>();
    private Map<ItemStorage, RecipeStorage> reverseRecipes = new HashMap<>();

    /**
     * Load all the recipes in the recipe storage.
     *
     * @param recipeManager  The recipe manager to parse.
     */
    public void loadRecipes(final RecipeManager recipeManager, final Level level)
    {
        recipes.clear();
        reverseRecipes.clear();
        com.minecolonies.api.util.RecipeCompat.getAllRecipesFor(recipeManager, RecipeType.SMELTING).forEach(holder -> {
            final SmeltingRecipe recipe = holder.value();
            final java.util.List<Ingredient> list = com.minecolonies.api.util.RecipeCompat.getIngredients(recipe);
            if (list.size() == 1)
            {
                for(final ItemStack smeltable: com.minecolonies.api.util.IngredientCompat.getItems(list.get(0)))
                {
                    if (!smeltable.isEmpty())
                    {
                        final ItemStack result = com.minecolonies.api.util.RecipeCompat.getResultItem(recipe, level);
                        final RecipeStorage storage = RecipeStorage.builder()
                                .withInputs(ImmutableList.of(new ItemStorage(smeltable)))
                                .withPrimaryOutput(result)
                                .withGridSize(1)
                                .withIntermediate(Blocks.FURNACE)
                                .withRecipeId(com.minecolonies.api.util.RecipeCompat.idLocation(holder))
                                .build();

                        recipes.put(storage.getCleanedInput().get(0), storage);

                        final ItemStack output = result.copy();
                        output.setCount(1);
                        reverseRecipes.put(new ItemStorage(output), storage);
                    }
                }
            }
        });
    }

    @Override
    public ItemStack getSmeltingResult(final ItemStack itemStack)
    {
        final RecipeStorage storage = recipes.getOrDefault(new ItemStorage(itemStack), null);
        if (storage != null)
        {
            return storage.getPrimaryOutput();
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public RecipeStorage getFirstSmeltingRecipeByResult(final ItemStorage storage)
    {
        return reverseRecipes.get(storage);
    }
}

package com.minecolonies.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Quidoms change: expand schematic block items into recursive crafting ingredients
 * so builders request base materials instead of exotic one-off crafted blocks.
 */
public final class IngredientBomExpand
{
    private IngredientBomExpand()
    {
    }

    /**
     * Expand a stack into ingredient stacks when a server level is available.
     */
    @NotNull
    public static List<ItemStack> expand(@Nullable final Level level, @NotNull final ItemStack stack, final int amount)
    {
        final List<ItemStack> out = new ArrayList<>();
        if (ItemStackUtils.isEmpty(stack) || amount <= 0)
        {
            return out;
        }
        if (!(level instanceof ServerLevel serverLevel))
        {
            final ItemStack copy = stack.copy();
            copy.setCount(amount);
            out.add(copy);
            return out;
        }

        final RecipeManager recipes = serverLevel.getServer().getRecipeManager();
        final Map<String, RecipeInputs> index = indexCrafting(recipes, serverLevel);
        final Map<String, Integer> raw = new LinkedHashMap<>();
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null)
        {
            final ItemStack copy = stack.copy();
            copy.setCount(amount);
            out.add(copy);
            return out;
        }
        raw.put(id.toString(), amount);
        final Map<String, Integer> expanded = expandMap(raw, index);
        for (final Map.Entry<String, Integer> e : expanded.entrySet())
        {
            final Item item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(e.getKey()));
            if (item == null || item == Items.AIR)
            {
                continue;
            }
            out.add(new ItemStack(item, e.getValue()));
        }
        if (out.isEmpty())
        {
            final ItemStack copy = stack.copy();
            copy.setCount(amount);
            out.add(copy);
        }
        return out;
    }

    private record RecipeInputs(int resultCount, List<Map.Entry<String, Integer>> inputs)
    {
    }

    private static Map<String, RecipeInputs> indexCrafting(final RecipeManager recipes, final ServerLevel level)
    {
        final Map<String, RecipeInputs> index = new HashMap<>();
        for (final RecipeHolder<CraftingRecipe> holder : recipes.getAllRecipesFor(RecipeType.CRAFTING))
        {
            final CraftingRecipe crafting = holder.value();
            final ItemStack result = crafting.getResultItem(level.registryAccess());
            if (result.isEmpty())
            {
                continue;
            }
            final ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (resultId == null || index.containsKey(resultId.toString()))
            {
                continue;
            }
            final List<Map.Entry<String, Integer>> inputs = new ArrayList<>();
            for (final Ingredient ingredient : crafting.getIngredients())
            {
                final ItemStack[] stacks = ingredient.getItems();
                if (stacks.length == 0 || stacks[0].isEmpty() || stacks[0].getItem() == Items.AIR)
                {
                    continue;
                }
                final ResourceLocation inId = BuiltInRegistries.ITEM.getKey(stacks[0].getItem());
                if (inId != null)
                {
                    inputs.add(Map.entry(inId.toString(), 1));
                }
            }
            if (!inputs.isEmpty())
            {
                index.put(resultId.toString(), new RecipeInputs(Math.max(1, result.getCount()), inputs));
            }
        }
        return index;
    }

    private static Map<String, Integer> expandMap(final Map<String, Integer> counts, final Map<String, RecipeInputs> recipes)
    {
        final Map<String, Integer> out = new LinkedHashMap<>();
        final Set<String> stack = new HashSet<>();
        for (final Map.Entry<String, Integer> entry : counts.entrySet())
        {
            expandOne(entry.getKey(), entry.getValue(), recipes, out, stack);
        }
        return out;
    }

    private static void expandOne(
        final String itemId,
        final int amount,
        final Map<String, RecipeInputs> recipes,
        final Map<String, Integer> out,
        final Set<String> stack)
    {
        if (amount <= 0 || itemId == null || itemId.isEmpty())
        {
            return;
        }
        if (!stack.add(itemId))
        {
            out.merge(itemId, amount, Integer::sum);
            return;
        }
        final RecipeInputs recipe = recipes.get(itemId);
        if (recipe == null || recipe.inputs().isEmpty() || recipe.resultCount() <= 0)
        {
            out.merge(itemId, amount, Integer::sum);
            stack.remove(itemId);
            return;
        }
        final int batches = (amount + recipe.resultCount() - 1) / recipe.resultCount();
        for (final Map.Entry<String, Integer> input : recipe.inputs())
        {
            expandOne(input.getKey(), input.getValue() * batches, recipes, out, stack);
        }
        stack.remove(itemId);
    }
}

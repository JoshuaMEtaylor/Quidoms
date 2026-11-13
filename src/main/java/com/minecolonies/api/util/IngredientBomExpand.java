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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * Quidoms: expand schematic finished items into recursive crafting ingredients (raw BOM).
 *
 * <p><b>Deterministic recipe policy (QD-119):</b>
 * <ol>
 *   <li>Seed known vanilla wood recipes (log→4 planks, 6 planks→4 stairs, 3 planks→6 slabs) so
 *       yields never collapse to 1:1 from a bad RecipeCompat first-hit.</li>
 *   <li>For other results, index crafting recipes and keep the best candidate:
 *       fewest total input units, then highest {@code resultCount}, then first registry order.</li>
 *   <li>Expansion uses {@code batches = ceil(needed / resultCount)}; each input contributes
 *       {@code inputCount * batches}, then recurses. Cycles stop at the cyclic item.</li>
 * </ol>
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
        seedVanillaWoodRecipes(index);

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
        int inputUnits()
        {
            int sum = 0;
            for (final Map.Entry<String, Integer> e : inputs)
            {
                sum += Math.max(1, e.getValue());
            }
            return sum;
        }
    }

    /**
     * Prefer fewest inputs, then highest yield (log→4 planks beats multi-ingredient crafts).
     */
    private static boolean isBetter(@NotNull final RecipeInputs candidate, @Nullable final RecipeInputs current)
    {
        if (current == null)
        {
            return true;
        }
        final int candScore = candidate.inputUnits() * 1000 - candidate.resultCount();
        final int curScore = current.inputUnits() * 1000 - current.resultCount();
        return candScore < curScore;
    }

    private static Map<String, RecipeInputs> indexCrafting(final RecipeManager recipes, final ServerLevel level)
    {
        final Map<String, RecipeInputs> index = new HashMap<>();
        for (final RecipeHolder<CraftingRecipe> holder : RecipeCompat.getAllRecipesFor(recipes, RecipeType.CRAFTING))
        {
            final CraftingRecipe crafting = holder.value();
            final ItemStack result = RecipeCompat.getResultItem(crafting, level);
            if (result.isEmpty())
            {
                continue;
            }
            final ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (resultId == null)
            {
                continue;
            }
            final List<Map.Entry<String, Integer>> inputs = new ArrayList<>();
            for (final Ingredient ingredient : RecipeCompat.getIngredients(crafting))
            {
                if (ingredient.isEmpty())
                {
                    continue;
                }
                final ItemStack[] stacks = IngredientCompat.getItems(ingredient);
                if (stacks.length == 0 || stacks[0].isEmpty() || stacks[0].getItem() == Items.AIR)
                {
                    continue;
                }
                // Deterministic representative: first item in ingredient (tag order).
                final ResourceLocation inId = BuiltInRegistries.ITEM.getKey(stacks[0].getItem());
                if (inId != null)
                {
                    inputs.add(Map.entry(inId.toString(), 1));
                }
            }
            if (inputs.isEmpty())
            {
                continue;
            }
            final RecipeInputs candidate = new RecipeInputs(Math.max(1, result.getCount()), inputs);
            final String key = resultId.toString();
            if (isBetter(candidate, index.get(key)))
            {
                index.put(key, candidate);
            }
        }
        return index;
    }

    /**
     * Hardcoded vanilla wood yields so BOM never 1:1 log-per-plank from a bad first recipe hit.
     * Representative log item is the unstripped log for each wood type.
     */
    private static void seedVanillaWoodRecipes(@NotNull final Map<String, RecipeInputs> index)
    {
        seedWood(index, Items.OAK_PLANKS, Items.OAK_STAIRS, Items.OAK_SLAB, Items.OAK_LOG);
        seedWood(index, Items.SPRUCE_PLANKS, Items.SPRUCE_STAIRS, Items.SPRUCE_SLAB, Items.SPRUCE_LOG);
        seedWood(index, Items.BIRCH_PLANKS, Items.BIRCH_STAIRS, Items.BIRCH_SLAB, Items.BIRCH_LOG);
        seedWood(index, Items.JUNGLE_PLANKS, Items.JUNGLE_STAIRS, Items.JUNGLE_SLAB, Items.JUNGLE_LOG);
        seedWood(index, Items.ACACIA_PLANKS, Items.ACACIA_STAIRS, Items.ACACIA_SLAB, Items.ACACIA_LOG);
        seedWood(index, Items.DARK_OAK_PLANKS, Items.DARK_OAK_STAIRS, Items.DARK_OAK_SLAB, Items.DARK_OAK_LOG);
        seedWood(index, Items.MANGROVE_PLANKS, Items.MANGROVE_STAIRS, Items.MANGROVE_SLAB, Items.MANGROVE_LOG);
        seedWood(index, Items.CHERRY_PLANKS, Items.CHERRY_STAIRS, Items.CHERRY_SLAB, Items.CHERRY_LOG);
        seedWood(index, Items.BAMBOO_PLANKS, Items.BAMBOO_STAIRS, Items.BAMBOO_SLAB, Items.BAMBOO_BLOCK);
        seedWood(index, Items.CRIMSON_PLANKS, Items.CRIMSON_STAIRS, Items.CRIMSON_SLAB, Items.CRIMSON_STEM);
        seedWood(index, Items.WARPED_PLANKS, Items.WARPED_STAIRS, Items.WARPED_SLAB, Items.WARPED_STEM);
    }

    private static void seedWood(
      @NotNull final Map<String, RecipeInputs> index,
      @NotNull final Item planks,
      @NotNull final Item stairs,
      @NotNull final Item slab,
      @NotNull final Item log)
    {
        final String plankId = id(planks);
        final String logId = id(log);
        final String stairId = id(stairs);
        final String slabId = id(slab);
        if (plankId == null || logId == null)
        {
            return;
        }
        // 1 log → 4 planks
        index.put(plankId, new RecipeInputs(4, List.of(Map.entry(logId, 1))));
        if (stairId != null)
        {
            // 6 planks → 4 stairs
            index.put(stairId, new RecipeInputs(4, List.of(Map.entry(plankId, 6))));
        }
        if (slabId != null)
        {
            // 3 planks → 6 slabs
            index.put(slabId, new RecipeInputs(6, List.of(Map.entry(plankId, 3))));
        }
    }

    @Nullable
    private static String id(@NotNull final Item item)
    {
        final ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
        return loc == null ? null : loc.toString();
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
        // QD-120: dyes / bone meal / dyeing recipes are terminal — do not reverse-expand
        // white terracotta → white dye → bone meal phantoms.
        if (isTerminalBuildingRaw(itemId))
        {
            out.merge(itemId, amount, Integer::sum);
            stack.remove(itemId);
            return;
        }
        final RecipeInputs recipe = recipes.get(itemId);
        if (recipe == null || recipe.inputs().isEmpty() || recipe.resultCount() <= 0 || recipeHasDyeOrBoneMeal(recipe))
        {
            out.merge(itemId, amount, Integer::sum);
            stack.remove(itemId);
            return;
        }
        // ceil(needed / outputCount) batches
        final int batches = (amount + recipe.resultCount() - 1) / recipe.resultCount();
        for (final Map.Entry<String, Integer> input : recipe.inputs())
        {
            expandOne(input.getKey(), input.getValue() * batches, recipes, out, stack);
        }
        stack.remove(itemId);
    }

    private static boolean isTerminalBuildingRaw(@NotNull final String itemId)
    {
        final Item item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(itemId));
        if (item == null || item == Items.AIR)
        {
            return true;
        }
        if (item == Items.BONE_MEAL || item instanceof DyeItem)
        {
            return true;
        }
        final ItemStack stack = new ItemStack(item);
        return stack.is(Tags.Items.DYES) || stack.is(ItemTags.WOOL) || stack.is(ItemTags.TERRACOTTA);
    }

    private static boolean recipeHasDyeOrBoneMeal(@NotNull final RecipeInputs recipe)
    {
        for (final Map.Entry<String, Integer> input : recipe.inputs())
        {
            // Only treat dye/bone-meal inputs as blocking reverse-expand; wool/terracotta as
            // *inputs* of a recipe are fine to refuse expansion of the *result*.
            final Item item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(input.getKey()));
            if (item == null || item == Items.AIR)
            {
                continue;
            }
            if (item == Items.BONE_MEAL || item instanceof DyeItem || new ItemStack(item).is(Tags.Items.DYES))
            {
                return true;
            }
        }
        return false;
    }
}

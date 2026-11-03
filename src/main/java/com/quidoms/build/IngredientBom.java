package com.quidoms.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Pure recursive bill-of-materials expander.
 * Collapses crafted intermediates into base ingredients where a recipe lookup allows.
 * Safe to unit-test without NeoForge game bootstrap.
 */
public final class IngredientBom {
    public record RecipeInputs(int resultCount, List<Map.Entry<String, Integer>> inputs) {
    }

    private IngredientBom() {
    }

    /**
     * Expand {@code counts} keyed by item id string using {@code recipes}.
     * Recipe lookup returns inputs for crafting one batch of that item, or null if base.
     */
    public static Map<String, Integer> expand(
            Map<String, Integer> counts,
            Function<String, RecipeInputs> recipes) {
        Map<String, Integer> out = new LinkedHashMap<>();
        Set<String> stack = new HashSet<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            expandOne(entry.getKey(), entry.getValue(), recipes, out, stack);
        }
        return out;
    }

    private static void expandOne(
            String itemId,
            int amount,
            Function<String, RecipeInputs> recipes,
            Map<String, Integer> out,
            Set<String> stack) {
        if (amount <= 0 || itemId == null || itemId.isEmpty()) {
            return;
        }
        if (!stack.add(itemId)) {
            out.merge(itemId, amount, Integer::sum);
            return;
        }
        RecipeInputs recipe = recipes.apply(itemId);
        if (recipe == null || recipe.inputs().isEmpty() || recipe.resultCount() <= 0) {
            out.merge(itemId, amount, Integer::sum);
            stack.remove(itemId);
            return;
        }
        int batches = (amount + recipe.resultCount() - 1) / recipe.resultCount();
        for (Map.Entry<String, Integer> input : recipe.inputs()) {
            expandOne(input.getKey(), input.getValue() * batches, recipes, out, stack);
        }
        stack.remove(itemId);
    }

    public static Map<String, Integer> merge(Map<String, Integer> a, Map<String, Integer> b) {
        Map<String, Integer> out = new HashMap<>(a);
        b.forEach((k, v) -> out.merge(k, v, Integer::sum));
        return out;
    }

    public static List<Map.Entry<String, Integer>> inputsOf(String itemId, int count) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        list.add(Map.entry(itemId, count));
        return list;
    }
}

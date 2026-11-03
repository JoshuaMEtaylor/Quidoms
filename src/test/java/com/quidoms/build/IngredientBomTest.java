package com.quidoms.build;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class IngredientBomTest {
    @Test
    void expandsCraftedIntermediateIntoBase() {
        Map<String, Integer> raw = Map.of("minecraft:oak_planks", 4);
        Map<String, Integer> expanded = IngredientBom.expand(raw, id -> {
            if ("minecraft:oak_planks".equals(id)) {
                return new IngredientBom.RecipeInputs(4, List.of(Map.entry("minecraft:oak_log", 1)));
            }
            return null;
        });
        assertEquals(Map.of("minecraft:oak_log", 1), expanded);
    }

    @Test
    void leavesBaseMaterialsAlone() {
        Map<String, Integer> raw = Map.of("minecraft:cobblestone", 8);
        Map<String, Integer> expanded = IngredientBom.expand(raw, id -> null);
        assertEquals(Map.of("minecraft:cobblestone", 8), expanded);
    }
}

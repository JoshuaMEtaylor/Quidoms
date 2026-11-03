package com.quidoms.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.quidoms.block.WarehouseBlockEntity;
import com.quidoms.colony.BuildingData;
import com.quidoms.colony.BuildingType;
import com.quidoms.colony.CitizenData;
import com.quidoms.colony.Colony;
import com.quidoms.colony.ColonyManager;
import com.quidoms.entity.CitizenEntity;
import com.quidoms.tool.ColonistTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builder job: recursive ingredient BOM → pull from warehouse → craft intermediates
 * in the citizen inventory → place schematic target blocks.
 */
public final class BuilderWork {
    private BuilderWork() {
    }

    public static Map<ResourceLocation, Integer> computeBom(Level level, Schematic schematic) {
        Map<String, Integer> raw = new LinkedHashMap<>();
        for (Schematic.BlockEntry entry : schematic.blocks()) {
            Block block = BuiltInRegistries.BLOCK.getValue(entry.block());
            if (block == null || block == Blocks.AIR) {
                continue;
            }
            Item item = block.asItem();
            if (item == null || item == Items.AIR) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null) {
                raw.merge(itemId.toString(), 1, Integer::sum);
            }
        }

        Map<String, Integer> expanded = raw;
        if (level instanceof ServerLevel serverLevel) {
            Map<String, IngredientBom.RecipeInputs> index = indexCraftingRecipes(serverLevel.getServer().getRecipeManager());
            expanded = IngredientBom.expand(raw, index::get);
        }

        Map<ResourceLocation, Integer> out = new LinkedHashMap<>();
        expanded.forEach((id, count) -> out.put(ResourceLocation.parse(id), count));
        return out;
    }

    private static Map<String, IngredientBom.RecipeInputs> indexCraftingRecipes(RecipeManager recipes) {
        Map<String, IngredientBom.RecipeInputs> index = new HashMap<>();
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe crafting)) {
                continue;
            }
            ItemStack result = resultOf(crafting);
            if (result.isEmpty()) {
                continue;
            }
            ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (resultId == null || index.containsKey(resultId.toString())) {
                continue;
            }
            List<Map.Entry<String, Integer>> inputs = new ArrayList<>();
            for (Ingredient ingredient : crafting.placementInfo().ingredients()) {
                Optional<Item> first = ingredient.items().map(h -> h.value()).findFirst();
                if (first.isEmpty() || first.get() == Items.AIR) {
                    continue;
                }
                ResourceLocation inId = BuiltInRegistries.ITEM.getKey(first.get());
                if (inId != null) {
                    inputs.add(Map.entry(inId.toString(), 1));
                }
            }
            if (!inputs.isEmpty()) {
                index.put(resultId.toString(), new IngredientBom.RecipeInputs(Math.max(1, result.getCount()), inputs));
            }
        }
        return index;
    }

    private static ItemStack resultOf(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.assemble(net.minecraft.world.item.crafting.CraftingInput.of(0, 0, List.of()), null);
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            return shapeless.assemble(net.minecraft.world.item.crafting.CraftingInput.of(0, 0, List.of()), null);
        }
        return ItemStack.EMPTY;
    }

    public static void tickBuilder(
            ServerLevel level,
            Colony colony,
            CitizenData data,
            CitizenEntity citizen,
            ColonyManager manager) {
        ColonistTools.enforceInventory(citizen);
        ensureBuilderTool(citizen);

        WorkOrder order = findActiveOrder(colony);
        if (order == null) {
            return;
        }
        Schematic schematic = SchematicManager.INSTANCE.get(order.schematicId()).orElse(null);
        if (schematic == null) {
            order.setStatus(WorkOrder.Status.COMPLETE);
            manager.markChanged();
            return;
        }
        if (order.status() == WorkOrder.Status.PENDING) {
            order.setStatus(WorkOrder.Status.IN_PROGRESS);
        }
        if (order.nextIndex() >= schematic.blocks().size()) {
            order.setStatus(WorkOrder.Status.COMPLETE);
            manager.markChanged();
            return;
        }

        // Restock from warehouse when carrying little.
        if (citizen.getMainHandItem().getCount() < 8) {
            pullFromWarehouse(level, colony, citizen, order);
        }

        Schematic.BlockEntry entry = schematic.blocks().get(order.nextIndex());
        BlockPos placePos = order.origin().offset(entry.x(), entry.y(), entry.z());
        Block wanted = BuiltInRegistries.BLOCK.getValue(entry.block());
        if (wanted == null) {
            wanted = Blocks.AIR;
        }

        citizen.getNavigation().moveTo(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5, 1.05D);
        if (citizen.blockPosition().distSqr(placePos) > 12.0D) {
            return;
        }

        BlockState current = level.getBlockState(placePos);
        if (current.is(wanted)) {
            order.setNextIndex(order.nextIndex() + 1);
            manager.markChanged();
            return;
        }

        Item needItem = wanted.asItem();
        if (needItem != null && needItem != Items.AIR) {
            if (!ensurePlaceItem(level, colony, citizen, order, needItem)) {
                // Seek warehouse for missing materials.
                colony.findBuilding(BuildingType.WAREHOUSE).ifPresent(wh ->
                        citizen.getNavigation().moveTo(wh.pos().getX() + 0.5, wh.pos().getY(), wh.pos().getZ() + 0.5, 1.0D));
                return;
            }
            // Consume one placeable item from hand / offhand.
            if (!consumeHeld(citizen, needItem)) {
                return;
            }
            decrementBom(order, needItem);
        }

        level.setBlock(placePos, wanted.defaultBlockState(), Block.UPDATE_ALL);
        order.setNextIndex(order.nextIndex() + 1);
        if (order.nextIndex() >= schematic.blocks().size()) {
            order.setStatus(WorkOrder.Status.COMPLETE);
        }
        manager.markChanged();
    }

    private static void ensureBuilderTool(CitizenEntity citizen) {
        ItemStack main = citizen.getMainHandItem();
        if (main.isEmpty()) {
            // Any-tier tool, unbreakable, player-equivalent attributes — no citizen level gate.
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            ColonistTools.makeUnbreakable(pick);
            citizen.setItemSlot(EquipmentSlot.MAINHAND, pick);
        } else {
            ColonistTools.makeUnbreakable(main);
        }
        ColonistTools.makeUnbreakable(citizen.getOffhandItem());
    }

    private static WorkOrder findActiveOrder(Colony colony) {
        WorkOrder best = null;
        for (WorkOrder order : colony.workOrders().values()) {
            if (order.status() == WorkOrder.Status.COMPLETE) {
                continue;
            }
            if (order.status() == WorkOrder.Status.IN_PROGRESS) {
                return order;
            }
            if (best == null) {
                best = order;
            }
        }
        return best;
    }

    private static void pullFromWarehouse(ServerLevel level, Colony colony, CitizenEntity citizen, WorkOrder order) {
        BuildingData warehouseBuilding = colony.findBuilding(BuildingType.WAREHOUSE).orElse(null);
        if (warehouseBuilding == null) {
            return;
        }
        if (!(level.getBlockEntity(warehouseBuilding.pos()) instanceof WarehouseBlockEntity warehouse)) {
            return;
        }
        // Prefer BOM ingredients still remaining.
        for (Map.Entry<ResourceLocation, Integer> entry : List.copyOf(order.remainingBom().entrySet())) {
            Item item = BuiltInRegistries.ITEM.getValue(entry.getKey());
            if (item == null || item == Items.AIR) {
                continue;
            }
            ItemStack taken = warehouse.takeOne(item);
            if (!taken.isEmpty()) {
                putMaterials(citizen, taken);
                return;
            }
        }
    }

    /**
     * Ensure citizen holds the target block item. If only ingredients are available,
     * craft/convert via warehouse + recipes (e.g. logs → planks).
     */
    private static boolean ensurePlaceItem(
            ServerLevel level,
            Colony colony,
            CitizenEntity citizen,
            WorkOrder order,
            Item needItem) {
        if (hasItem(citizen, needItem)) {
            return true;
        }
        // Try craft one batch from warehouse ingredients using a matching recipe.
        Map<String, IngredientBom.RecipeInputs> index = indexCraftingRecipes(level.getServer().getRecipeManager());
        ResourceLocation needId = BuiltInRegistries.ITEM.getKey(needItem);
        if (needId == null) {
            return false;
        }
        IngredientBom.RecipeInputs recipe = index.get(needId.toString());
        BuildingData warehouseBuilding = colony.findBuilding(BuildingType.WAREHOUSE).orElse(null);
        WarehouseBlockEntity warehouse = warehouseBuilding != null
                && level.getBlockEntity(warehouseBuilding.pos()) instanceof WarehouseBlockEntity wh ? wh : null;

        if (recipe != null && warehouse != null) {
            // Check warehouse has each input.
            boolean ok = true;
            Map<Item, Integer> needCounts = new HashMap<>();
            for (Map.Entry<String, Integer> in : recipe.inputs()) {
                Item item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(in.getKey()));
                if (item == null || item == Items.AIR) {
                    ok = false;
                    break;
                }
                needCounts.merge(item, in.getValue(), Integer::sum);
            }
            if (ok) {
                for (Map.Entry<Item, Integer> e : needCounts.entrySet()) {
                    if (warehouse.count(e.getKey()) < e.getValue()) {
                        ok = false;
                        break;
                    }
                }
            }
            if (ok) {
                for (Map.Entry<Item, Integer> e : needCounts.entrySet()) {
                    warehouse.tryTake(e.getKey(), e.getValue());
                }
                ItemStack crafted = new ItemStack(needItem, recipe.resultCount());
                putMaterials(citizen, crafted);
                return true;
            }
        }

        // Direct take of the placeable item from warehouse.
        if (warehouse != null) {
            ItemStack taken = warehouse.takeOne(needItem);
            if (!taken.isEmpty()) {
                putMaterials(citizen, taken);
                return true;
            }
        }
        return hasItem(citizen, needItem);
    }

    private static void putMaterials(CitizenEntity citizen, ItemStack stack) {
        ColonistTools.makeUnbreakable(stack);
        ItemStack off = citizen.getOffhandItem();
        if (off.isEmpty()) {
            citizen.setItemSlot(EquipmentSlot.OFFHAND, stack);
        } else if (ItemStack.isSameItemSameComponents(off, stack)) {
            off.grow(stack.getCount());
        } else {
            citizen.setItemSlot(EquipmentSlot.OFFHAND, stack);
        }
    }

    private static boolean hasItem(CitizenEntity citizen, Item item) {
        return !citizen.getOffhandItem().isEmpty() && citizen.getOffhandItem().is(item);
    }

    private static boolean consumeHeld(CitizenEntity citizen, Item item) {
        // Materials live in offhand; mainhand keeps the unbreakable tool.
        ItemStack off = citizen.getOffhandItem();
        if (!off.isEmpty() && off.is(item)) {
            off.shrink(1);
            if (off.isEmpty()) {
                citizen.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    private static void decrementBom(WorkOrder order, Item placedOrIngredient) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(placedOrIngredient);
        if (id != null && order.remainingBom().containsKey(id)) {
            int left = order.remainingBom().get(id) - 1;
            if (left <= 0) {
                order.remainingBom().remove(id);
            } else {
                order.remainingBom().put(id, left);
            }
            return;
        }
        if (!order.remainingBom().isEmpty()) {
            ResourceLocation first = order.remainingBom().keySet().iterator().next();
            int left = order.remainingBom().get(first) - 1;
            if (left <= 0) {
                order.remainingBom().remove(first);
            } else {
                order.remainingBom().put(first, left);
            }
        }
    }
}

package com.quidoms.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.quidoms.colony.BuildingType;
import com.quidoms.colony.CitizenData;
import com.quidoms.colony.Colony;
import com.quidoms.colony.ColonyManager;
import com.quidoms.entity.CitizenEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Builder job: ingredient BOM → consume nearby player supplies → place schematic blocks.
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
            RecipeManager recipes = serverLevel.getServer().getRecipeManager();
            Map<String, IngredientBom.RecipeInputs> index = indexCraftingRecipes(recipes);
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
                // Prefer first recipe; skip duplicates / cycles later via IngredientBom stack.
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
            ResourceLocation needId = BuiltInRegistries.ITEM.getKey(needItem);
            // Prefer consuming collapsed BOM entries; fall back to the direct block item.
            if (!consumeForBlock(level, colony, order, needId, placePos)) {
                return;
            }
        }

        level.setBlock(placePos, wanted.defaultBlockState(), Block.UPDATE_ALL);
        order.setNextIndex(order.nextIndex() + 1);
        if (order.nextIndex() >= schematic.blocks().size()) {
            order.setStatus(WorkOrder.Status.COMPLETE);
        }
        manager.markChanged();
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

    /**
     * Consume one unit of materials for placing {@code needId}. Tries remaining BOM keys first
     * (ingredient expansion), then nearby player inventories around warehouse/origin.
     */
    private static boolean consumeForBlock(
            ServerLevel level,
            Colony colony,
            WorkOrder order,
            ResourceLocation needId,
            BlockPos placePos) {
        // If BOM still lists this item (or any entry), require a matching inventory take.
        Item needItem = BuiltInRegistries.ITEM.getValue(needId);
        if (needItem == null || needItem == Items.AIR) {
            return true;
        }

        BlockPos gatherCenter = colony.findBuilding(BuildingType.WAREHOUSE)
                .map(b -> b.pos())
                .orElse(placePos);

        if (!takeOneFromNearbyPlayers(level, gatherCenter, needItem) && !takeOneFromNearbyPlayers(level, placePos, needItem)) {
            // Creative owners may place without inventory; still decrement BOM when present.
            boolean creativeOwnerNearby = level.getEntitiesOfClass(
                    ServerPlayer.class,
                    new AABB(gatherCenter).inflate(16.0D),
                    p -> colony.permissions().canManage(p.getUUID()) && p.getAbilities().instabuild
            ).stream().findAny().isPresent();
            if (!creativeOwnerNearby) {
                return false;
            }
        }

        // Decrement BOM: prefer exact item, else any positive entry (collapsed ingredients already tracked).
        if (order.remainingBom().containsKey(needId)) {
            int left = order.remainingBom().get(needId) - 1;
            if (left <= 0) {
                order.remainingBom().remove(needId);
            } else {
                order.remainingBom().put(needId, left);
            }
        } else if (!order.remainingBom().isEmpty()) {
            // Block was expanded into base ingredients — leave BOM until those are consumed via matching takes.
            // When placing oak_planks that expanded to oak_logs, consumeOne already took planks OR we took from creative.
            // Best-effort: decrement first BOM entry by 1 so UI progresses.
            ResourceLocation first = order.remainingBom().keySet().iterator().next();
            int left = order.remainingBom().get(first) - 1;
            if (left <= 0) {
                order.remainingBom().remove(first);
            } else {
                order.remainingBom().put(first, left);
            }
        }
        return true;
    }

    private static boolean takeOneFromNearbyPlayers(ServerLevel level, BlockPos center, Item item) {
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, new AABB(center).inflate(12.0D));
        for (ServerPlayer player : players) {
            if (player.getAbilities().instabuild) {
                return true;
            }
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && stack.is(item)) {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        inv.setItem(i, ItemStack.EMPTY);
                    }
                    player.containerMenu.broadcastChanges();
                    return true;
                }
            }
        }
        return false;
    }
}

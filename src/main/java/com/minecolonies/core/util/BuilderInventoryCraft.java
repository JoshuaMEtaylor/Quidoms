package com.minecolonies.core.util;

import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlockComponent;
import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.minecolonies.api.util.IngredientBomExpand;
import com.minecolonies.api.util.IngredientCompat;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.RecipeCompat;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Quidoms QD-103: builders request raw materials only and craft/convert finished
 * products (vanilla crafting + Domum framed) inside their own inventory.
 */
public final class BuilderInventoryCraft
{
    private BuilderInventoryCraft()
    {
    }

    /**
     * Expand a finished schematic stack into raw requestable stacks (Domum components
     * then recursive crafting BOM). Identity when nothing expands.
     */
    @NotNull
    public static List<ItemStack> expandToRaw(@Nullable final Level level, @NotNull final ItemStack stack, final int amount)
    {
        final List<ItemStack> out = new ArrayList<>();
        if (ItemStackUtils.isEmpty(stack) || amount <= 0)
        {
            return out;
        }

        final List<ItemStack> seeds = expandDomumComponents(stack, amount);
        if (seeds.isEmpty())
        {
            seeds.add(copyWithCount(stack, amount));
        }

        for (final ItemStack seed : seeds)
        {
            final List<ItemStack> bom = IngredientBomExpand.expand(level, seed, seed.getCount());
            if (bom.isEmpty())
            {
                out.add(seed);
            }
            else
            {
                out.addAll(bom);
            }
        }
        return mergeStacks(out);
    }

    /**
     * True when expandToRaw yields something other than the finished product itself
     * (planks→logs, Domum framed→components, etc.).
     */
    public static boolean isConvertible(@Nullable final Level level, @NotNull final ItemStack stack)
    {
        if (ItemStackUtils.isEmpty(stack))
        {
            return false;
        }
        // Domum framed / textured blocks are always converted from components in-inv (QD-103/112).
        if (DomumOrnamentumUtils.getBlock(stack) != null)
        {
            return true;
        }
        final List<ItemStack> raw = expandToRaw(level, stack, Math.max(1, stack.getCount()));
        if (raw.isEmpty())
        {
            return false;
        }
        if (raw.size() != 1)
        {
            return true;
        }
        final ItemStack only = raw.get(0);
        return only.getItem() != stack.getItem()
                 || only.getCount() != Math.max(1, stack.getCount())
                 || !ItemStack.isSameItemSameComponents(only, copyWithCount(stack, only.getCount()));
    }

    /**
     * True when this finished stack must never be courier/QuidCraft-requested — only its raw BOM.
     */
    public static boolean mustNotRequestFinished(@Nullable final Level level, @NotNull final ItemStack stack)
    {
        return isConvertible(level, stack);
    }

    /**
     * Craft/assemble {@code need} into {@code inv} from raw ingredients already present.
     * Returns true when the inventory holds at least {@code need.getCount()} matching stacks after.
     */
    public static boolean tryCraftInInventory(
      @Nullable final Level level,
      @NotNull final IItemHandler inv,
      @NotNull final ItemStack need)
    {
        if (ItemStackUtils.isEmpty(need))
        {
            return true;
        }
        final int want = Math.max(1, need.getCount());
        final java.util.function.Predicate<ItemStack> match =
          candidate -> ItemStackUtils.compareItemStacksIgnoreStackSize(need, candidate, true, true);

        if (InventoryUtils.getItemCountInItemHandler(inv, match) >= want)
        {
            return true;
        }

        if (tryAssembleDomum(inv, need, want))
        {
            return InventoryUtils.getItemCountInItemHandler(inv, match) >= want;
        }

        if (level instanceof ServerLevel serverLevel)
        {
            tryCraftVanilla(serverLevel, inv, need, want);
        }

        return InventoryUtils.getItemCountInItemHandler(inv, match) >= want;
    }

    /**
     * Domum framed → one of each textured component × amount.
     */
    @NotNull
    private static List<ItemStack> expandDomumComponents(@NotNull final ItemStack stack, final int amount)
    {
        final List<ItemStack> out = new ArrayList<>();
        final IMateriallyTexturedBlock doBlock = DomumOrnamentumUtils.getBlock(stack);
        if (doBlock == null)
        {
            return out;
        }
        final MaterialTextureData data = MaterialTextureData.readFromItemStack(stack);

        // Prefer texture data; fall back to component defaults so Framed White never stays as a request id (QD-112).
        boolean any = false;
        for (final IMateriallyTexturedBlockComponent component : doBlock.getComponents())
        {
            Block block = data.isEmpty() ? null : data.getTexturedComponents().get(component.getId());
            if (block == null || block == Blocks.AIR)
            {
                block = component.getDefault();
            }
            if (block == null || block == Blocks.AIR)
            {
                continue;
            }
            if (component.isOptional() && (data.isEmpty() || !data.getTexturedComponents().containsKey(component.getId())))
            {
                continue;
            }
            out.add(new ItemStack(block, amount));
            any = true;
        }
        if (!any && !data.isEmpty())
        {
            for (final Block block : data.getTexturedComponents().values())
            {
                if (block != null && block != Blocks.AIR)
                {
                    out.add(new ItemStack(block, amount));
                }
            }
        }
        return out;
    }

    private static boolean tryAssembleDomum(@NotNull final IItemHandler inv, @NotNull final ItemStack need, final int want)
    {
        final IMateriallyTexturedBlock doBlock = DomumOrnamentumUtils.getBlock(need);
        if (doBlock == null)
        {
            return false;
        }
        final MaterialTextureData data = MaterialTextureData.readFromItemStack(need);
        if (data.isEmpty())
        {
            return false;
        }

        final List<ItemStack> components = expandDomumComponents(need, 1);
        if (components.isEmpty())
        {
            return false;
        }

        int made = 0;
        while (made < want)
        {
            // Verify all components present for one assemble.
            for (final ItemStack comp : components)
            {
                final int have = InventoryUtils.getItemCountInItemHandler(inv,
                  s -> ItemStackUtils.compareItemStacksIgnoreStackSize(comp, s, true, false));
                if (have < 1)
                {
                    return made > 0;
                }
            }
            // Consume one of each, insert one framed product.
            for (final ItemStack comp : components)
            {
                if (!InventoryUtils.attemptReduceStackInItemHandler(inv, comp, 1, true, true))
                {
                    Log.getLogger().warn("[QuidomsBuilder] Domum assemble consume failed for {}", comp.getItem());
                    return made > 0;
                }
            }
            final ItemStack product = need.copy();
            product.setCount(1);
            if (!InventoryUtils.addItemStackToItemHandler(inv, product))
            {
                // Roll back components if we cannot insert the product.
                for (final ItemStack comp : components)
                {
                    InventoryUtils.addItemStackToItemHandler(inv, comp.copy());
                }
                return made > 0;
            }
            made++;
        }
        if (made > 0)
        {
            Log.getLogger().info("[QuidomsBuilder] crafted Domum {} x{} in builder inv", need.getItem(), made);
        }
        return made > 0;
    }

    private static void tryCraftVanilla(
      @NotNull final ServerLevel level,
      @NotNull final IItemHandler inv,
      @NotNull final ItemStack need,
      final int want)
    {
        final CraftRecipe recipe = findCraftingRecipe(level, need.getItem());
        if (recipe == null)
        {
            return;
        }

        int have = InventoryUtils.getItemCountInItemHandler(inv,
          s -> ItemStackUtils.compareItemStacksIgnoreStackSize(need, s, true, false));
        int made = 0;
        while (have < want)
        {
            // Enough of each input?
            for (final Map.Entry<Item, Integer> in : recipe.inputs.entrySet())
            {
                final int needIn = in.getValue();
                final int haveIn = InventoryUtils.getItemCountInItemHandler(inv, s -> s.getItem() == in.getKey());
                if (haveIn < needIn)
                {
                    if (made > 0)
                    {
                        Log.getLogger().info("[QuidomsBuilder] crafted {} x{} in builder inv", need.getItem(), made);
                    }
                    return;
                }
            }
            for (final Map.Entry<Item, Integer> in : recipe.inputs.entrySet())
            {
                final ItemStack consume = new ItemStack(in.getKey(), in.getValue());
                if (!InventoryUtils.attemptReduceStackInItemHandler(inv, consume, in.getValue(), true, true))
                {
                    return;
                }
            }
            final ItemStack product = new ItemStack(need.getItem(), recipe.resultCount);
            // Preserve need components only when identical item (vanilla planks: empty components).
            if (!InventoryUtils.addItemStackToItemHandler(inv, product))
            {
                // Rollback inputs
                for (final Map.Entry<Item, Integer> in : recipe.inputs.entrySet())
                {
                    InventoryUtils.addItemStackToItemHandler(inv, new ItemStack(in.getKey(), in.getValue()));
                }
                return;
            }
            made += recipe.resultCount;
            have += recipe.resultCount;
        }
        if (made > 0)
        {
            Log.getLogger().info("[QuidomsBuilder] crafted {} x{} in builder inv", need.getItem(), made);
        }
    }

    @Nullable
    private static CraftRecipe findCraftingRecipe(@NotNull final ServerLevel level, @NotNull final Item resultItem)
    {
        final RecipeManager recipes = level.getServer().getRecipeManager();
        CraftRecipe best = null;
        int bestScore = Integer.MAX_VALUE;
        for (final RecipeHolder<CraftingRecipe> holder : RecipeCompat.getAllRecipesFor(recipes, RecipeType.CRAFTING))
        {
            final CraftingRecipe crafting = holder.value();
            final ItemStack result = RecipeCompat.getResultItem(crafting, level);
            if (result.isEmpty() || result.getItem() != resultItem)
            {
                continue;
            }
            final Map<Item, Integer> inputs = new LinkedHashMap<>();
            for (final Ingredient ingredient : RecipeCompat.getIngredients(crafting))
            {
                final ItemStack[] stacks = IngredientCompat.getItems(ingredient);
                if (stacks.length == 0 || stacks[0].isEmpty() || stacks[0].getItem() == Items.AIR)
                {
                    continue;
                }
                inputs.merge(stacks[0].getItem(), 1, Integer::sum);
            }
            if (inputs.isEmpty())
            {
                continue;
            }
            // Prefer fewest inputs then highest output (log→4 planks beats multi-ingredient crafts).
            final int score = inputs.values().stream().mapToInt(Integer::intValue).sum() * 100 - Math.max(1, result.getCount());
            if (score < bestScore)
            {
                bestScore = score;
                best = new CraftRecipe(Math.max(1, result.getCount()), inputs);
            }
        }
        return best;
    }

    @NotNull
    private static List<ItemStack> mergeStacks(@NotNull final List<ItemStack> stacks)
    {
        final Map<String, ItemStack> merged = new LinkedHashMap<>();
        for (final ItemStack stack : stacks)
        {
            if (ItemStackUtils.isEmpty(stack))
            {
                continue;
            }
            final String key = BuiltInRegistries.ITEM.getKey(stack.getItem()) + "#" + stack.getComponentsPatch().hashCode();
            final ItemStack existing = merged.get(key);
            if (existing == null)
            {
                merged.put(key, stack.copy());
            }
            else
            {
                existing.grow(stack.getCount());
            }
        }
        return new ArrayList<>(merged.values());
    }

    @NotNull
    private static ItemStack copyWithCount(@NotNull final ItemStack stack, final int count)
    {
        final ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private record CraftRecipe(int resultCount, Map<Item, Integer> inputs)
    {
    }
}

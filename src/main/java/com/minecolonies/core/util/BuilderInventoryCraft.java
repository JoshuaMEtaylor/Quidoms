package com.minecolonies.core.util;

import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlockComponent;
import com.ldtteam.domumornamentum.block.decorative.ExtraBlock;
import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.recipe.ModRecipeTypes;
import com.ldtteam.domumornamentum.recipe.architectscutter.ArchitectsCutterRecipe;
import com.minecolonies.api.util.IngredientBomExpand;
import com.minecolonies.api.util.IngredientCompat;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.RecipeCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Quidoms QD-103/114: builders request raw materials only and craft/convert finished
 * products (vanilla crafting + Domum framed) inside their own inventory before place.
 */
public final class BuilderInventoryCraft
{
    private BuilderInventoryCraft()
    {
    }

    /**
     * Expand a finished schematic stack into raw requestable stacks (Domum architect-cutter
     * components / ExtraBlock materials, then recursive crafting BOM). Identity when nothing expands.
     */
    @NotNull
    public static List<ItemStack> expandToRaw(@Nullable final Level level, @NotNull final ItemStack stack, final int amount)
    {
        final List<ItemStack> out = new ArrayList<>();
        if (ItemStackUtils.isEmpty(stack) || amount <= 0)
        {
            return out;
        }

        final List<ItemStack> seeds = new ArrayList<>();
        if (DomumOrnamentumUtils.getBlock(stack) != null)
        {
            // QD-122: architect-cutter components (textures / safe defaults) — never keep finished Domum.
            seeds.addAll(expandDomumComponents(level, stack, amount));
        }
        if (seeds.isEmpty())
        {
            seeds.add(copyWithCount(stack, amount));
        }

        for (final ItemStack seed : seeds)
        {
            final List<ItemStack> bom = IngredientBomExpand.expand(level, seed, seed.getCount());
            final List<ItemStack> parts = bom.isEmpty() ? List.of(seed) : bom;
            for (final ItemStack part : parts)
            {
                if (isExtraBlock(part))
                {
                    // Crafting expand may have left ExtraBlock terminal — force material raws.
                    final List<ItemStack> materials = expandExtraBlockMaterials(part, part.getCount());
                    if (!materials.isEmpty())
                    {
                        for (final ItemStack mat : materials)
                        {
                            out.addAll(IngredientBomExpand.expand(level, mat, mat.getCount()));
                        }
                        continue;
                    }
                }
                if (!isDomumFinishedOrExtra(part))
                {
                    out.add(part);
                }
                else if (DomumOrnamentumUtils.getBlock(part) != null)
                {
                    // Nested Domum (shouldn't loop forever — components are usually vanilla/Extra).
                    final List<ItemStack> nested = expandDomumComponents(level, part, part.getCount());
                    for (final ItemStack n : nested)
                    {
                        if (isExtraBlock(n))
                        {
                            out.addAll(expandExtraBlockMaterials(n, n.getCount()));
                        }
                        else if (!isDomumFinishedOrExtra(n))
                        {
                            out.addAll(IngredientBomExpand.expand(level, n, n.getCount()));
                        }
                    }
                }
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
        if (DomumOrnamentumUtils.getBlock(stack) != null || isExtraBlock(stack))
        {
            return true;
        }
        if (plankFallback(stack.getItem()) != null)
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
     * Server-safe localized name for wait-chat (avoids Domum format-key leftovers).
     */
    @NotNull
    public static String localizedDepositName(@NotNull final ItemStack stack)
    {
        if (ItemStackUtils.isEmpty(stack))
        {
            return "?";
        }
        final Language lang = Language.getInstance();
        final String descId = stack.getItem().getDescriptionId();
        if (lang.has(descId))
        {
            return lang.getOrDefault(descId);
        }
        // Domum shingles etc.: build "%s Shingles" from registry id + cover material.
        final IMateriallyTexturedBlock doBlock = DomumOrnamentumUtils.getBlock(stack);
        if (doBlock != null)
        {
            final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(stack.getItem() instanceof net.minecraft.world.item.BlockItem bi
              ? bi.getBlock() : Blocks.AIR);
            if (blockId != null && blockId.getNamespace().equals("domum_ornamentum"))
            {
                final MaterialTextureData data = MaterialTextureData.readFromItemStack(stack);
                String material = "";
                final List<IMateriallyTexturedBlockComponent> comps = new ArrayList<>(doBlock.getComponents());
                if (!comps.isEmpty())
                {
                    final IMateriallyTexturedBlockComponent cover = comps.get(0);
                    Block mat = data.getTexturedComponents().get(cover.getId());
                    if (mat == null || mat == Blocks.AIR)
                    {
                        mat = cover.getDefault();
                    }
                    if (mat != null && mat != Blocks.AIR)
                    {
                        final String matId = mat.asItem().getDescriptionId();
                        material = lang.has(matId) ? lang.getOrDefault(matId) : mat.getName().getString();
                    }
                }
                // Domum keys: domum_ornamentum.shingle.name.format.block.domum_ornamentum.shingle
                final String doFormat = "domum_ornamentum.shingle.name.format.block." + blockId.getNamespace() + "." + blockId.getPath();
                if (lang.has(doFormat))
                {
                    return lang.getOrDefault(doFormat).replace("%s", material.isEmpty() ? "Wood" : material);
                }
                if (!material.isEmpty())
                {
                    return material + " " + (lang.has(descId) ? lang.getOrDefault(descId) : blockId.getPath().replace('_', ' '));
                }
            }
        }
        final String hover = stack.getHoverName().getString();
        // Reject unresolved translation keys leaking into chat.
        if (hover.contains("domum_ornamentum.") && hover.contains(".format."))
        {
            return blockPathFallback(stack);
        }
        return hover;
    }

    @NotNull
    private static String blockPathFallback(@NotNull final ItemStack stack)
    {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null)
        {
            return stack.getItem().toString();
        }
        final String[] parts = id.getPath().split("_");
        final StringBuilder sb = new StringBuilder();
        for (final String part : parts)
        {
            if (part.isEmpty())
            {
                continue;
            }
            if (sb.length() > 0)
            {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
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
        return tryCraftForBuilder(level, inv, null, need);
    }

    /**
     * QD-114: pull ingredients from hut (TE+racks) into the worker, craft finished product
     * into the worker inventory (where place/consume expects it).
     *
     * @return true when person inventory holds enough of {@code need} after.
     */
    public static boolean tryCraftForBuilder(
      @Nullable final Level level,
      @NotNull final IItemHandler personInv,
      @Nullable final IItemHandler hutInv,
      @NotNull final ItemStack need)
    {
        if (ItemStackUtils.isEmpty(need))
        {
            return true;
        }
        final int want = Math.max(1, need.getCount());
        final Predicate<ItemStack> match =
          candidate -> ItemStackUtils.compareItemStacksIgnoreStackSize(need, candidate, true, false);

        if (InventoryUtils.getItemCountInItemHandler(personInv, match) >= want)
        {
            return true;
        }

        // Prefer Domum assemble when the need is a textured Domum block.
        if (DomumOrnamentumUtils.getBlock(need) != null)
        {
            pullIngredientsForDomum(personInv, hutInv, need, want);
            if (tryAssembleDomum(personInv, need, want)
                  && InventoryUtils.getItemCountInItemHandler(personInv, match) >= want)
            {
                return true;
            }
        }

        final CraftRecipe recipe = resolveCraftRecipe(level, need.getItem());
        if (recipe != null)
        {
            pullIngredientsForRecipe(personInv, hutInv, recipe, want);
            tryCraftWithRecipe(personInv, need, want, recipe);
        }

        final boolean ok = InventoryUtils.getItemCountInItemHandler(personInv, match) >= want;
        if (!ok)
        {
            Log.getLogger().info(
              "[QuidomsBuilder] craft-before-place FAILED need={} want={} recipe={} personHas={} hutPresent={}",
              need.getItem(),
              want,
              recipe == null ? "none" : recipe.describe(),
              InventoryUtils.getItemCountInItemHandler(personInv, match),
              hutInv != null);
        }
        return ok;
    }

    /**
     * QD-122: Domum ExtraBlock (brick_extra / slate / thatch…) → type.getMaterial() raws.
     * Crafting recipe is typically 5 material → 4 extra (brick_extra.json).
     */
    @NotNull
    private static List<ItemStack> expandExtraBlockMaterials(@NotNull final ItemStack stack, final int amount)
    {
        final List<ItemStack> out = new ArrayList<>();
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)
              || !(blockItem.getBlock() instanceof ExtraBlock extra))
        {
            return out;
        }
        final Item material = extra.getType().getMaterial();
        if (material == null || material == Items.AIR)
        {
            return out;
        }
        final int batches = (amount + 3) / 4;
        out.add(new ItemStack(material, batches * 5));
        return out;
    }

    private static boolean isExtraBlock(@NotNull final ItemStack stack)
    {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem bi
                 && bi.getBlock() instanceof ExtraBlock;
    }

    private static boolean isDomumFinishedOrExtra(@NotNull final ItemStack stack)
    {
        return DomumOrnamentumUtils.getBlock(stack) != null || isExtraBlock(stack);
    }

    /**
     * Domum framed/shingle → architect-cutter component materials (schematic textures, else safe defaults).
     * QD-120: never invent terracotta/wool/dye phantoms when texture data omitted them.
     * QD-122: empty texture still expands via safe defaults (shingle cover/support) so finished Domum
     * never stays on the deposit list when a cutter path exists.
     */
    @NotNull
    private static List<ItemStack> expandDomumComponents(
      @Nullable final Level level,
      @NotNull final ItemStack stack,
      final int amount)
    {
        return expandDomumComponents(level, stack, amount, true);
    }

    @NotNull
    private static List<ItemStack> expandDomumComponents(@NotNull final ItemStack stack, final int amount)
    {
        return expandDomumComponents(null, stack, amount, true);
    }

    @NotNull
    private static List<ItemStack> expandDomumComponents(
      @Nullable final Level level,
      @NotNull final ItemStack stack,
      final int amount,
      final boolean applyCutterYield)
    {
        final List<ItemStack> out = new ArrayList<>();
        final IMateriallyTexturedBlock doBlock = DomumOrnamentumUtils.getBlock(stack);
        if (doBlock == null)
        {
            return out;
        }
        final MaterialTextureData data = MaterialTextureData.readFromItemStack(stack);
        final int perCraft = applyCutterYield ? Math.max(1, architectsCutterResultCount(level, stack)) : 1;
        final int batches = (amount + perCraft - 1) / perCraft;

        for (final IMateriallyTexturedBlockComponent component : doBlock.getComponents())
        {
            Block block = data.getTexturedComponents().get(component.getId());
            if (block == null || block == Blocks.AIR)
            {
                // Missing from schematic texture: use Domum default unless it is a known phantom fill
                // (TimberFrame center → WHITE_TERRACOTTA). Explicit texture always wins.
                if (component.isOptional() && !data.isEmpty())
                {
                    continue;
                }
                block = component.getDefault();
                if (block == null || block == Blocks.AIR || isPhantomDomumDefault(block))
                {
                    continue;
                }
            }
            out.add(new ItemStack(block, batches));
        }
        if (out.isEmpty() && !data.isEmpty())
        {
            for (final Block block : data.getTexturedComponents().values())
            {
                if (block != null && block != Blocks.AIR && !isPhantomDomumDefault(block))
                {
                    out.add(new ItemStack(block, batches));
                }
            }
        }
        return out;
    }

    /**
     * Architects-cutter craft yield for this Domum block (shingles usually 4). Default 1.
     */
    private static int architectsCutterResultCount(@Nullable final Level level, @NotNull final ItemStack stack)
    {
        if (!(level instanceof ServerLevel serverLevel))
        {
            return 1;
        }
        final Block block = stack.getItem() instanceof net.minecraft.world.item.BlockItem bi ? bi.getBlock() : null;
        if (block == null)
        {
            return 1;
        }
        final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (blockId == null)
        {
            return 1;
        }
        try
        {
            for (final RecipeHolder<ArchitectsCutterRecipe> holder :
              RecipeCompat.getAllRecipesFor(serverLevel.getServer().getRecipeManager(), ModRecipeTypes.ARCHITECTS_CUTTER.get()))
            {
                if (blockId.equals(holder.value().getBlockName()))
                {
                    return Math.max(1, holder.value().getCount());
                }
            }
        }
        catch (final Exception ex)
        {
            Log.getLogger().debug("[QuidomsBuilder] architects cutter lookup failed: {}", ex.toString());
        }
        return 1;
    }

    /**
     * Defaults that QD-120 must not invent when schematic texture omitted them.
     */
    private static boolean isPhantomDomumDefault(@NotNull final Block block)
    {
        final ItemStack stack = new ItemStack(block);
        return stack.is(ItemTags.TERRACOTTA) || stack.is(ItemTags.WOOL) || stack.is(Tags.Items.DYES)
                 || block.asItem() == Items.BONE_MEAL || block == Blocks.WHITE_TERRACOTTA;
    }

    private static void pullIngredientsForDomum(
      @NotNull final IItemHandler personInv,
      @Nullable final IItemHandler hutInv,
      @NotNull final ItemStack need,
      final int want)
    {
        if (hutInv == null)
        {
            return;
        }
        final List<ItemStack> components = expandDomumComponents(need, want);
        for (final ItemStack comp : components)
        {
            final Predicate<ItemStack> match =
              s -> ItemStackUtils.compareItemStacksIgnoreStackSize(comp, s, true, false);
            final int have = InventoryUtils.getItemCountInItemHandler(personInv, match);
            final int missing = Math.max(0, comp.getCount() - have);
            if (missing > 0)
            {
                InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandler(
                  hutInv, match, missing, personInv);
            }
        }
    }

    private static void pullIngredientsForRecipe(
      @NotNull final IItemHandler personInv,
      @Nullable final IItemHandler hutInv,
      @NotNull final CraftRecipe recipe,
      final int want)
    {
        if (hutInv == null)
        {
            return;
        }
        final int craftsNeeded = (want + recipe.resultCount - 1) / recipe.resultCount;
        for (final IngredientSlot slot : recipe.inputs)
        {
            final int needCount = slot.count * craftsNeeded;
            final int have = InventoryUtils.getItemCountInItemHandler(personInv, slot::matches);
            final int missing = Math.max(0, needCount - have);
            if (missing > 0)
            {
                InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandler(
                  hutInv, slot::matches, missing, personInv);
            }
        }
    }

    private static boolean tryAssembleDomum(@NotNull final IItemHandler inv, @NotNull final ItemStack need, final int want)
    {
        final IMateriallyTexturedBlock doBlock = DomumOrnamentumUtils.getBlock(need);
        if (doBlock == null)
        {
            return false;
        }

        final List<ItemStack> components = expandDomumComponents(null, need, 1, false);
        if (components.isEmpty())
        {
            return false;
        }

        int made = 0;
        while (made < want)
        {
            for (final ItemStack comp : components)
            {
                final int have = InventoryUtils.getItemCountInItemHandler(inv,
                  s -> ItemStackUtils.compareItemStacksIgnoreStackSize(comp, s, true, false));
                if (have < 1)
                {
                    if (made > 0)
                    {
                        Log.getLogger().info("[QuidomsBuilder] crafted Domum {} x{} in builder inv", need.getItem(), made);
                    }
                    return made > 0;
                }
            }
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
                for (final ItemStack comp : components)
                {
                    InventoryUtils.addItemStackToItemHandler(inv, comp.copy());
                }
                Log.getLogger().info("[QuidomsBuilder] Domum craft blocked — no free slot for {}", need.getItem());
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

    private static void tryCraftWithRecipe(
      @NotNull final IItemHandler inv,
      @NotNull final ItemStack need,
      final int want,
      @NotNull final CraftRecipe recipe)
    {
        int have = InventoryUtils.getItemCountInItemHandler(inv,
          s -> ItemStackUtils.compareItemStacksIgnoreStackSize(need, s, true, false));
        int made = 0;
        while (have < want)
        {
            for (final IngredientSlot slot : recipe.inputs)
            {
                if (InventoryUtils.getItemCountInItemHandler(inv, slot::matches) < slot.count)
                {
                    if (made > 0)
                    {
                        Log.getLogger().info("[QuidomsBuilder] crafted {} x{} in builder inv", need.getItem(), made);
                    }
                    return;
                }
            }

            final List<ItemStack> consumed = new ArrayList<>();
            boolean ok = true;
            for (final IngredientSlot slot : recipe.inputs)
            {
                final ItemStack taken = extractMatching(inv, slot, slot.count);
                if (taken.isEmpty())
                {
                    ok = false;
                    break;
                }
                consumed.add(taken);
            }
            if (!ok)
            {
                for (final ItemStack back : consumed)
                {
                    InventoryUtils.addItemStackToItemHandler(inv, back);
                }
                return;
            }

            final ItemStack product = new ItemStack(need.getItem(), recipe.resultCount);
            if (!InventoryUtils.addItemStackToItemHandler(inv, product))
            {
                for (final ItemStack back : consumed)
                {
                    InventoryUtils.addItemStackToItemHandler(inv, back);
                }
                Log.getLogger().info("[QuidomsBuilder] craft blocked — no free slot for {}", need.getItem());
                return;
            }
            made += recipe.resultCount;
            have += recipe.resultCount;
        }
        if (made > 0)
        {
            Log.getLogger().info("[QuidomsBuilder] crafted {} x{} in builder inv (before place)", need.getItem(), made);
        }
    }

    @NotNull
    private static ItemStack extractMatching(
      @NotNull final IItemHandler inv,
      @NotNull final IngredientSlot slot,
      final int quantity)
    {
        int left = quantity;
        ItemStack collected = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSlots() && left > 0; i++)
        {
            final ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty() || !slot.matches(stack))
            {
                continue;
            }
            final ItemStack extracted = inv.extractItem(i, left, false);
            if (extracted.isEmpty())
            {
                continue;
            }
            if (collected.isEmpty())
            {
                collected = extracted.copy();
            }
            else
            {
                collected.grow(extracted.getCount());
            }
            left -= extracted.getCount();
        }
        return left > 0 ? ItemStack.EMPTY : collected;
    }

    @Nullable
    private static CraftRecipe resolveCraftRecipe(@Nullable final Level level, @NotNull final Item resultItem)
    {
        CraftRecipe best = null;
        if (level instanceof ServerLevel serverLevel)
        {
            best = findCraftingRecipe(serverLevel, resultItem);
        }
        final CraftRecipe fallback = plankFallback(resultItem);
        if (fallback == null)
        {
            return best;
        }
        if (best == null)
        {
            return fallback;
        }
        // Prefer the fewer-input recipe (log→planks) over multi-ingredient datapack crafts.
        return fallback.inputUnits() <= best.inputUnits() ? fallback : best;
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
            final List<IngredientSlot> inputs = new ArrayList<>();
            for (final Ingredient ingredient : RecipeCompat.getIngredients(crafting))
            {
                if (ingredient.isEmpty())
                {
                    continue;
                }
                final ItemStack[] stacks = IngredientCompat.getItems(ingredient);
                if (stacks.length == 0)
                {
                    continue;
                }
                // QD-114: keep the full ingredient (tag-aware), not only stacks[0].
                inputs.add(IngredientSlot.ofIngredient(ingredient, 1));
            }
            if (inputs.isEmpty())
            {
                continue;
            }
            final int score = inputs.stream().mapToInt(s -> s.count).sum() * 100 - Math.max(1, result.getCount());
            if (score < bestScore)
            {
                bestScore = score;
                best = new CraftRecipe(Math.max(1, result.getCount()), inputs);
            }
        }
        return best;
    }

    /**
     * Hardcoded log→planks so craft works even when RecipeCompat/placementInfo misses vanilla recipes.
     */
    @Nullable
    private static CraftRecipe plankFallback(@NotNull final Item planks)
    {
        final TagKey<Item> logTag = plankLogTag(planks);
        if (logTag == null)
        {
            return null;
        }
        return new CraftRecipe(4, List.of(IngredientSlot.ofTag(logTag, 1)));
    }

    @Nullable
    private static TagKey<Item> plankLogTag(@NotNull final Item planks)
    {
        if (planks == Items.OAK_PLANKS)
        {
            return ItemTags.OAK_LOGS;
        }
        if (planks == Items.SPRUCE_PLANKS)
        {
            return ItemTags.SPRUCE_LOGS;
        }
        if (planks == Items.BIRCH_PLANKS)
        {
            return ItemTags.BIRCH_LOGS;
        }
        if (planks == Items.JUNGLE_PLANKS)
        {
            return ItemTags.JUNGLE_LOGS;
        }
        if (planks == Items.ACACIA_PLANKS)
        {
            return ItemTags.ACACIA_LOGS;
        }
        if (planks == Items.DARK_OAK_PLANKS)
        {
            return ItemTags.DARK_OAK_LOGS;
        }
        if (planks == Items.MANGROVE_PLANKS)
        {
            return ItemTags.MANGROVE_LOGS;
        }
        if (planks == Items.CHERRY_PLANKS)
        {
            return ItemTags.CHERRY_LOGS;
        }
        if (planks == Items.BAMBOO_PLANKS)
        {
            return ItemTags.BAMBOO_BLOCKS;
        }
        if (planks == Items.CRIMSON_PLANKS)
        {
            return ItemTags.CRIMSON_STEMS;
        }
        if (planks == Items.WARPED_PLANKS)
        {
            return ItemTags.WARPED_STEMS;
        }
        return null;
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

    private record IngredientSlot(int count, @Nullable Ingredient ingredient, @Nullable TagKey<Item> tag, @Nullable Item exact)
    {
        static IngredientSlot ofIngredient(@NotNull final Ingredient ingredient, final int count)
        {
            return new IngredientSlot(count, ingredient, null, null);
        }

        static IngredientSlot ofTag(@NotNull final TagKey<Item> tag, final int count)
        {
            return new IngredientSlot(count, null, tag, null);
        }

        boolean matches(@NotNull final ItemStack stack)
        {
            if (stack.isEmpty())
            {
                return false;
            }
            if (ingredient != null)
            {
                return ingredient.test(stack);
            }
            if (tag != null)
            {
                return stack.is(tag);
            }
            return exact != null && stack.getItem() == exact;
        }
    }

    private record CraftRecipe(int resultCount, List<IngredientSlot> inputs)
    {
        int inputUnits()
        {
            return inputs.stream().mapToInt(s -> s.count).sum();
        }

        String describe()
        {
            return "out=" + resultCount + " in=" + inputUnits();
        }
    }
}

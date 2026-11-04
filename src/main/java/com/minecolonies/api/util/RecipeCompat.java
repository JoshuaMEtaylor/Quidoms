package com.minecolonies.api.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Helpers bridging NeoForge 1.21.10 Recipe API removals
 * ({@code getIngredients}/{@code getResultItem}/{@code canCraftInDimensions}/{@code getAllRecipesFor}).
 */
public final class RecipeCompat
{
    private RecipeCompat() {}

    @NotNull
    public static List<Ingredient> getIngredients(@NotNull final Recipe<?> recipe)
    {
        try
        {
            return new ArrayList<>(recipe.placementInfo().ingredients());
        }
        catch (final Exception ex)
        {
            return Collections.emptyList();
        }
    }

    @NotNull
    public static ItemStack getResultItem(@NotNull final Recipe<?> recipe, @NotNull final Level level)
    {
        final List<RecipeDisplay> displays = recipe.display();
        if (!displays.isEmpty())
        {
            final ContextMap ctx = SlotDisplayContext.fromLevel(level);
            final ItemStack stack = displays.get(0).result().resolveForFirstStack(ctx);
            if (!stack.isEmpty())
            {
                return stack;
            }
        }
        return getResultItem(recipe, level.registryAccess());
    }

    @NotNull
    public static ItemStack getResultItem(@NotNull final Recipe<?> recipe, @NotNull final HolderLookup.Provider registries)
    {
        if (recipe instanceof final SingleItemRecipe single)
        {
            final ItemStack[] stacks = IngredientCompat.getItems(single.input());
            if (stacks.length > 0)
            {
                return recipe.assemble(new SingleRecipeInput(stacks[0]), registries);
            }
        }

        if (recipe instanceof final CraftingRecipe crafting)
        {
            final List<Ingredient> ingredients = getIngredients(recipe);
            final List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(Math.max(9, ingredients.size()), ItemStack.EMPTY));
            for (int i = 0; i < ingredients.size(); i++)
            {
                final ItemStack[] options = IngredientCompat.getItems(ingredients.get(i));
                if (options.length > 0)
                {
                    stacks.set(i, options[0].copy());
                }
            }
            final int width = recipe instanceof ShapedRecipe shaped ? Math.max(1, shaped.getWidth()) : 3;
            final int height = recipe instanceof ShapedRecipe shaped ? Math.max(1, shaped.getHeight()) : 3;
            final CraftingInput input = CraftingInput.of(width, height, stacks.subList(0, width * height));
            try
            {
                if (crafting.matches(input, null))
                {
                    return crafting.assemble(input, registries);
                }
            }
            catch (final Exception ignored)
            {
            }
            try
            {
                return crafting.assemble(input, registries);
            }
            catch (final Exception ignored)
            {
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean canCraftInDimensions(@NotNull final Recipe<?> recipe, final int width, final int height)
    {
        if (recipe instanceof final ShapedRecipe shaped)
        {
            return shaped.getWidth() <= width && shaped.getHeight() <= height;
        }
        return getIngredients(recipe).size() <= width * height;
    }

    @NotNull
    public static <T extends Recipe<?>> Collection<RecipeHolder<T>> getAllRecipesFor(
      @NotNull final RecipeManager manager,
      @NotNull final RecipeType<T> type)
    {
        return manager.recipeMap().byType(type);
    }

    @Nullable
    public static ResourceLocation idLocation(@NotNull final RecipeHolder<?> holder)
    {
        return holder.id().location();
    }

    /**
     * Full {@link RecipeManager} is server-side only on 1.21.10 (client has a limited {@link net.minecraft.world.item.crafting.RecipeAccess}).
     */
    @Nullable
    public static RecipeManager asRecipeManager(@Nullable final Level level)
    {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            return serverLevel.recipeAccess();
        }
        return null;
    }

    @NotNull
    public static Collection<RecipeHolder<?>> getRecipesOrEmpty(@Nullable final Level level)
    {
        final RecipeManager manager = asRecipeManager(level);
        return manager == null ? java.util.List.of() : manager.getRecipes();
    }

}

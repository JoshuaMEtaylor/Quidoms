package com.ldtteam.domumornamentum.block;

import com.ldtteam.domumornamentum.shingles.ShingleHeightType;
import com.ldtteam.domumornamentum.util.Constants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Domum creative tabs. Output wrapper catches per-block fill failures so one broken
 * materially-textured block cannot abort the whole tab (logged as DO populate errors).
 */
public final class ModCreativeTabs
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ModCreativeTabs.class);

    public static final DeferredRegister<CreativeModeTab> TAB_REG = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENERAL = TAB_REG.register("general", () -> CreativeModeTab.builder()
        .icon(() -> new ItemStack(ModBlocks.getInstance().getArchitectsCutter()))
        .title(Component.translatable("itemGroup." + Constants.MOD_ID + ".general"))
        .displayItems(new OutputAwareGenerator((config, output) -> {
            output.accept(ModBlocks.getInstance().getArchitectsCutter());
            ModBlocks.getInstance().getTimberFrames().forEach(output::accept);

            for (ShingleHeightType heightType : ShingleHeightType.values())
            {
                output.accept(ModBlocks.getInstance().getShingle(heightType));
            }

            output.accept(ModBlocks.getInstance().getShingleSlab());
            output.accept(ModBlocks.getInstance().getPaperWall());
            output.accept(ModBlocks.getInstance().getTiledPaperWall());

            ModBlocks.getInstance().getPillars().forEach(output::accept);
            ModBlocks.getInstance().getFramedLights().forEach(output::accept);
            ModBlocks.getInstance().getAllBrickBlocks().forEach(output::accept);

            output.accept(ModBlocks.getInstance().getFence());
            output.accept(ModBlocks.getInstance().getFenceGate());
            output.accept(ModBlocks.getInstance().getSlab());
            output.accept(ModBlocks.getInstance().getWall());
            output.accept(ModBlocks.getInstance().getStair());
            output.accept(ModBlocks.getInstance().getTrapdoor());
            output.accept(ModBlocks.getInstance().getDoor());
            output.accept(ModBlocks.getInstance().getPanel());
            output.accept(ModBlocks.getInstance().getPost());
            output.accept(ModBlocks.getInstance().getFancyDoor());
            output.accept(ModBlocks.getInstance().getFancyTrapdoor());
            output.accept(ModBlocks.getInstance().getDynamicTimberFrame());
        })).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXTRA_BLOCKS = TAB_REG.register("extra_blocks", () -> CreativeModeTab.builder()
        .withTabsBefore(GENERAL.getId())
        .icon(() -> {
            final var extras = ModBlocks.getInstance().getExtraTopBlocks();
            return extras.isEmpty() ? ItemStack.EMPTY : new ItemStack(extras.get(0));
        })
        .title(Component.translatable("itemGroup." + Constants.MOD_ID + ".extra-blocks"))
        .displayItems(new OutputAwareGenerator((config, output) -> {
            ModBlocks.getInstance().getExtraTopBlocks().forEach(output::accept);
            ModBlocks.getInstance().getBricks().forEach(output::accept);
            output.accept(ModBlocks.getInstance().getStandingBarrel());
            output.accept(ModBlocks.getInstance().getLayingBarrel());
        })).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FLOATING_CARPETS = TAB_REG.register("floating_carpets", () -> CreativeModeTab.builder()
        .withTabsBefore(EXTRA_BLOCKS.getId())
        .icon(() -> {
            final var carpets = ModBlocks.getInstance().getFloatingCarpets();
            return carpets.isEmpty() ? ItemStack.EMPTY : new ItemStack(carpets.get(0));
        })
        .title(Component.translatable("itemGroup." + Constants.MOD_ID + ".floating-carpets"))
        .displayItems(new OutputAwareGenerator((config, output) -> {
            ModBlocks.getInstance().getFloatingCarpets().forEach(output::accept);
        })).build());

    private ModCreativeTabs()
    {
    }

    private record OutputAwareGenerator(CreativeModeTab.DisplayItemsGenerator delegate) implements CreativeModeTab.DisplayItemsGenerator
    {
        @Override
        public void accept(@NotNull final CreativeModeTab.ItemDisplayParameters featureFlagSet, @NotNull final CreativeModeTab.Output output)
        {
            try
            {
                delegate.accept(featureFlagSet, new Output(output));
            }
            catch (final Throwable ex)
            {
                LOGGER.warn("Error populating Domum Ornamentum creative tab items", ex);
            }
        }

        private record Output(CreativeModeTab.Output delegate) implements CreativeModeTab.Output
        {
            @Override
            public void accept(@NotNull final ItemStack stack, final CreativeModeTab.@NotNull TabVisibility visibility)
            {
                delegate.accept(stack, visibility);
            }

            @Override
            public void accept(@NotNull final ItemLike itemLike)
            {
                try
                {
                    if (itemLike instanceof ICachedItemGroupBlock cachedItemGroupBlock)
                    {
                        final NonNullList<ItemStack> stacks = NonNullList.create();
                        cachedItemGroupBlock.fillItemCategory(stacks);
                        stacks.forEach(delegate::accept);
                    }
                    else
                    {
                        delegate.accept(itemLike);
                    }
                }
                catch (final Throwable ex)
                {
                    LOGGER.warn("Skipping Domum creative item {} due to fill error", itemLike, ex);
                }
            }
        }
    }
}

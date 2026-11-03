package com.quidoms.registry;

import com.quidoms.QuidomsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, QuidomsMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.quidoms"))
            .icon(() -> ModItems.TOWN_HALL.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.TOWN_HALL.get());
                output.accept(ModItems.HOUSE.get());
                output.accept(ModItems.BUILDER_HUT.get());
                output.accept(ModItems.WAREHOUSE.get());
                output.accept(ModItems.COLONY_BANNER.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}

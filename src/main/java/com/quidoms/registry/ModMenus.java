package com.quidoms.registry;

import com.quidoms.QuidomsMod;
import com.quidoms.client.ColonyMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, QuidomsMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ColonyMenu>> COLONY =
            MENUS.register("colony", () -> IMenuTypeExtension.create(ColonyMenu::fromNetwork));

    private ModMenus() {
    }
}

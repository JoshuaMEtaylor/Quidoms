package com.quidoms.client;

import com.quidoms.registry.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Lightweight menu token so NeoForge can open a screen without container slots. */
public class ColonyMenu extends AbstractContainerMenu {
    private final int colonyId;

    public ColonyMenu(int containerId, Inventory inventory, int colonyId) {
        super(ModMenus.COLONY.get(), containerId);
        this.colonyId = colonyId;
    }

    public static ColonyMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        return new ColonyMenu(containerId, inventory, buf.readVarInt());
    }

    public int colonyId() {
        return colonyId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

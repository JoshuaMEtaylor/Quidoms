package com.quidoms.inventory;

import com.quidoms.block.WarehouseBlockEntity;
import com.quidoms.registry.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WarehouseMenu extends AbstractContainerMenu {
    private final WarehouseBlockEntity warehouse;

    public WarehouseMenu(int containerId, Inventory playerInv, WarehouseBlockEntity warehouse) {
        super(ModMenus.WAREHOUSE.get(), containerId);
        this.warehouse = warehouse;
        checkContainerSize(warehouse, WarehouseBlockEntity.SIZE);
        warehouse.startOpen(playerInv.player);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(warehouse, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public static WarehouseMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        return new WarehouseMenu(containerId, inventory, (WarehouseBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < WarehouseBlockEntity.SIZE) {
                if (!this.moveItemStackTo(stack, WarehouseBlockEntity.SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, WarehouseBlockEntity.SIZE, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return warehouse.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        warehouse.stopOpen(player);
    }
}

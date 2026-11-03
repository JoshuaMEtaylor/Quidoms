package com.quidoms.block;

import com.quidoms.inventory.WarehouseMenu;
import com.quidoms.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class WarehouseBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int colonyId;
    private int buildingId;

    public WarehouseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAREHOUSE.get(), pos, state);
    }

    public int colonyId() {
        return colonyId;
    }

    public void setColonyId(int colonyId) {
        this.colonyId = colonyId;
        setChanged();
    }

    public int buildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("quidoms.container.warehouse");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new WarehouseMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    /** Pull up to {@code amount} of {@code item} into {@code into}; returns taken count. */
    public int extractItem(Item item, int amount, ItemStack into) {
        int taken = 0;
        for (int i = 0; i < SIZE && taken < amount; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            int move = Math.min(amount - taken, stack.getCount());
            if (into.isEmpty()) {
                into = stack.split(move);
                // caller must use returned stack; mutate via return
            } else if (ItemStack.isSameItemSameComponents(into, stack)) {
                stack.shrink(move);
                into.grow(move);
            } else {
                continue;
            }
            taken += move;
            if (stack.isEmpty()) {
                items.set(i, ItemStack.EMPTY);
            }
            setChanged();
        }
        return taken;
    }

    public int count(Item item) {
        int total = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public boolean tryTake(Item item, int amount) {
        if (count(item) < amount) {
            return false;
        }
        int left = amount;
        for (int i = 0; i < SIZE && left > 0; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            int move = Math.min(left, stack.getCount());
            stack.shrink(move);
            left -= move;
            if (stack.isEmpty()) {
                items.set(i, ItemStack.EMPTY);
            }
        }
        setChanged();
        return left == 0;
    }

    public ItemStack takeOne(Item item) {
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.is(item)) {
                ItemStack out = stack.split(1);
                if (stack.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }
                setChanged();
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean tryInsert(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        for (int i = 0; i < SIZE; i++) {
            ItemStack slot = items.get(i);
            if (slot.isEmpty()) {
                items.set(i, stack.copyAndClear());
                setChanged();
                return true;
            }
            if (ItemStack.isSameItemSameComponents(slot, stack)) {
                int space = Math.min(slot.getMaxStackSize() - slot.getCount(), stack.getCount());
                if (space > 0) {
                    slot.grow(space);
                    stack.shrink(space);
                    setChanged();
                    if (stack.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return stack.isEmpty();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("ColonyId", colonyId);
        output.putInt("BuildingId", buildingId);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        colonyId = input.getIntOr("ColonyId", 0);
        buildingId = input.getIntOr("BuildingId", 0);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }
}

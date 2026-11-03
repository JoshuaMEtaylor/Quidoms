package com.quidoms.tool;

import com.quidoms.entity.CitizenEntity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Colonists may use any tool tier with player-equivalent performance.
 * Any tool/weapon on a colonist is forced unbreakable — no citizen level gates.
 */
public final class ColonistTools {
    private ColonistTools() {
    }

    public static void makeUnbreakable(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.isDamageableItem() || isToolOrWeapon(stack)) {
            stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        }
    }

    public static boolean isToolOrWeapon(ItemStack stack) {
        return stack.isDamageableItem();
    }

    public static void enforceInventory(CitizenEntity citizen) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = citizen.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                makeUnbreakable(stack);
            }
        }
    }
}

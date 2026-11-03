package com.quidoms.tool;

import com.quidoms.entity.CitizenEntity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Colonists may use any tool tier with player-equivalent performance.
 * Any tool/weapon on a colonist is forced unbreakable — no citizen level gates.
 * Enchantments and item attribute modifiers (mining speed / attack damage) are left intact.
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
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.isDamageableItem()) {
            return true;
        }
        ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return !mods.modifiers().isEmpty();
    }

    public static void enforceInventory(CitizenEntity citizen) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = citizen.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                makeUnbreakable(stack);
            }
        }
    }

    /** Attack damage contribution from the held stack (player-equivalent item attributes). */
    public static double heldAttackDamage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double total = 0.0D;
        for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE)
                    && (entry.slot() == EquipmentSlotGroup.MAINHAND
                    || entry.slot() == EquipmentSlotGroup.ANY
                    || entry.slot() == EquipmentSlotGroup.HAND)) {
                AttributeModifier modifier = entry.modifier();
                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    total += modifier.amount();
                }
            }
        }
        return total;
    }
}

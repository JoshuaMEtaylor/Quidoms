package com.minecolonies.api.items;

import com.google.common.collect.Maps;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * Custom armor materials for Quidoms / MineColonies gear (1.21.10 component API).
 */
public final class ModArmorMaterials
{
    public static final ResourceKey<EquipmentAsset> SANTA_HAT_ASSET = createAsset("santa_hat");
    public static final ResourceKey<EquipmentAsset> PLATE_ARMOR_ASSET = createAsset("plate_armor");
    public static final ResourceKey<EquipmentAsset> BUILD_GOGGLES_ASSET = createAsset("build_goggles");
    public static final ResourceKey<EquipmentAsset> PIRATE_ARMOR_1_ASSET = createAsset("pirate");
    public static final ResourceKey<EquipmentAsset> PIRATE_ARMOR_2_ASSET = createAsset("pirate2");

    public static final ArmorMaterial SANTA_HAT = new ArmorMaterial(
      1,
      makeDefense(0, 0, 0, 0),
      500,
      SoundEvents.ARMOR_EQUIP_LEATHER,
      0.0F,
      0.0F,
      ModTags.repairs_cosmetic_armor,
      SANTA_HAT_ASSET
    );

    public static final ArmorMaterial PLATE_ARMOR = new ArmorMaterial(
      25,
      makeDefense(3, 6, 8, 3),
      37,
      SoundEvents.ARMOR_EQUIP_IRON,
      0.0F,
      0.0F,
      ItemTags.REPAIRS_IRON_ARMOR,
      PLATE_ARMOR_ASSET
    );

    public static final ArmorMaterial BUILD_GOGGLES = new ArmorMaterial(
      1,
      makeDefense(0, 0, 0, 0),
      20,
      SoundEvents.ARMOR_EQUIP_LEATHER,
      0.0F,
      0.0F,
      ModTags.repairs_cosmetic_armor,
      BUILD_GOGGLES_ASSET
    );

    public static final ArmorMaterial PIRATE_ARMOR_1 = new ArmorMaterial(
      20,
      makeDefense(2, 5, 6, 2),
      5,
      SoundEvents.ARMOR_EQUIP_LEATHER,
      0.0F,
      0.0F,
      ItemTags.REPAIRS_DIAMOND_ARMOR,
      PIRATE_ARMOR_1_ASSET
    );

    public static final ArmorMaterial PIRATE_ARMOR_2 = new ArmorMaterial(
      15,
      makeDefense(3, 6, 8, 3),
      5,
      SoundEvents.ARMOR_EQUIP_LEATHER,
      2.0F,
      0.0F,
      ItemTags.REPAIRS_DIAMOND_ARMOR,
      PIRATE_ARMOR_2_ASSET
    );

    private ModArmorMaterials()
    {
        throw new IllegalStateException("Tried to initialize: ModArmorMaterials but this is a Utility class.");
    }

    private static ResourceKey<EquipmentAsset> createAsset(final String name)
    {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name));
    }

    private static Map<ArmorType, Integer> makeDefense(final int boots, final int leggings, final int chestplate, final int helmet)
    {
        return Maps.newEnumMap(
          Map.of(
            ArmorType.BOOTS, boots,
            ArmorType.LEGGINGS, leggings,
            ArmorType.CHESTPLATE, chestplate,
            ArmorType.HELMET, helmet,
            ArmorType.BODY, chestplate
          )
        );
    }
}

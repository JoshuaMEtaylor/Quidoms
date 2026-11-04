package com.minecolonies.apiimp.initializer;

import com.minecolonies.api.blocks.MinecoloniesBlockIds;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.entity.ModEntities;
import com.minecolonies.api.items.ModArmorMaterials;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.items.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import static com.minecolonies.api.blocks.decorative.AbstractBlockGate.IRON_GATE;
import static com.minecolonies.api.blocks.decorative.AbstractBlockGate.WOODEN_GATE;

@EventBusSubscriber(modid = Constants.MOD_ID)
public final class ModItemsInitializer
{
    private ModItemsInitializer()
    {
        throw new IllegalStateException("Tried to initialize: ModItemsInitializer but this is a Utility class.");
    }

    private static Item spawnEgg(final EntityType<?> type)
    {
        return new SpawnEggItem(MinecoloniesBlockIds.itemProps(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + "_spawn_egg", new Item.Properties().spawnEgg(type)));
    }

    @SubscribeEvent
    public static void registerItems(RegisterEvent event)
    {
        if (event.getRegistryKey().equals(Registries.ITEM))
        {
            ModItemsInitializer.init(event.getRegistry(Registries.ITEM));
        }
    }

    /**
     * Initates all the blocks. At the correct time.
     *
     * @param registry the registry.
     */
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public static void init(final Registry<Item> registry)
    {
        ModItems.scepterLumberjack = new ItemScepterLumberjack(MinecoloniesBlockIds.itemProps("scepterlumberjack"));
        ModItems.supplyChest = new ItemSupplyChestDeployer(MinecoloniesBlockIds.itemProps("supplychestdeployer"));
        ModItems.permTool = new ItemScepterPermission(MinecoloniesBlockIds.itemProps("scepterpermission"));
        ModItems.scepterGuard = new ItemScepterGuard(MinecoloniesBlockIds.itemProps("scepterguard"));
        ModItems.assistantHammer_Gold = new ItemAssistantHammer("assistanthammer_gold", MinecoloniesBlockIds.itemProps("assistanthammer_gold", new Item.Properties().durability(200)), 1);
        ModItems.assistantHammer_Iron = new ItemAssistantHammer("assistanthammer_iron", MinecoloniesBlockIds.itemProps("assistanthammer_iron", new Item.Properties().durability(400)), 2);
        ModItems.assistantHammer_Diamond = new ItemAssistantHammer("assistanthammer_diamond", MinecoloniesBlockIds.itemProps("assistanthammer_diamond", new Item.Properties().durability(1000)), 3);
        ModItems.bannerRallyGuards = new ItemBannerRallyGuards(MinecoloniesBlockIds.itemProps("banner_rally_guards"));
        ModItems.supplyCamp = new ItemSupplyCampDeployer(MinecoloniesBlockIds.itemProps("supplycampdeployer"));
        ModItems.ancientTome = new ItemAncientTome(MinecoloniesBlockIds.itemProps("ancienttome"));
        ModItems.chiefSword = new ItemChiefSword(MinecoloniesBlockIds.itemProps("chiefsword", new Item.Properties().durability(1500)));
        ModItems.scimitar = new ItemIronScimitar(MinecoloniesBlockIds.itemProps("iron_scimitar", new Item.Properties().durability(250)));
        ModItems.clipboard = new ItemClipboard(MinecoloniesBlockIds.itemProps("clipboard"));
        ModItems.compost = new ItemCompost(MinecoloniesBlockIds.itemProps("compost"));
        ModItems.resourceScroll = new ItemResourceScroll(MinecoloniesBlockIds.itemProps("resourcescroll"));
        ModItems.pharaoscepter = new ItemPharaoScepter(MinecoloniesBlockIds.itemProps("pharaoscepter", new Item.Properties().durability(400)));
        ModItems.firearrow = new ItemFireArrow(MinecoloniesBlockIds.itemProps("firearrow"));
        ModItems.scepterBeekeeper = new ItemScepterBeekeeper(MinecoloniesBlockIds.itemProps("scepterbeekeeper"));
        ModItems.mistletoe = new ItemMistletoe(MinecoloniesBlockIds.itemProps("mistletoe"));
        ModItems.spear = new ItemSpear(MinecoloniesBlockIds.itemProps("spear"));
        ModItems.questLog = new ItemQuestLog(MinecoloniesBlockIds.itemProps("questlog"));

        ModItems.breadDough = new ItemBreadDough(MinecoloniesBlockIds.itemProps("bread_dough"));
        ModItems.cookieDough = new ItemCookieDough(MinecoloniesBlockIds.itemProps("cookie_dough"));
        ModItems.cakeBatter = new ItemCakeBatter(MinecoloniesBlockIds.itemProps("cake_batter"));
        ModItems.rawPumpkinPie = new ItemRawPumpkinPie(MinecoloniesBlockIds.itemProps("raw_pumpkin_pie"));

        ModItems.milkyBread = new ItemMilkyBread(MinecoloniesBlockIds.itemProps("milky_bread"));
        ModItems.sugaryBread = new ItemSugaryBread(MinecoloniesBlockIds.itemProps("sugary_bread"));
        ModItems.goldenBread = new ItemGoldenBread(MinecoloniesBlockIds.itemProps("golden_bread"));
        ModItems.chorusBread = new ItemChorusBread(MinecoloniesBlockIds.itemProps("chorus_bread"));

        ModItems.adventureToken = new ItemAdventureToken(MinecoloniesBlockIds.itemProps("adventure_token"));

        ModItems.scrollColonyTP = new ItemScrollColonyTP(MinecoloniesBlockIds.itemProps("scroll_tp", new Item.Properties().stacksTo(16)));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "scroll_tp"), ModItems.scrollColonyTP);

        ModItems.scrollColonyAreaTP = new ItemScrollColonyAreaTP(MinecoloniesBlockIds.itemProps("scroll_area_tp", new Item.Properties().stacksTo(16)));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "scroll_area_tp"), ModItems.scrollColonyAreaTP);

        ModItems.scrollBuff = new ItemScrollBuff(MinecoloniesBlockIds.itemProps("scroll_buff", new Item.Properties().stacksTo(16)));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "scroll_buff"), ModItems.scrollBuff);

        ModItems.scrollGuardHelp = new ItemScrollGuardHelp(MinecoloniesBlockIds.itemProps("scroll_guard_help", new Item.Properties().stacksTo(16)));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "scroll_guard_help"), ModItems.scrollGuardHelp);

        ModItems.scrollHighLight = new ItemScrollHighlight(MinecoloniesBlockIds.itemProps("scroll_highlight", new Item.Properties().stacksTo(16)));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "scroll_highlight"), ModItems.scrollHighLight);

        ModItems.santaHat = new ItemSantaHead("santa_hat", ModArmorMaterials.SANTA_HAT, ArmorType.HELMET, MinecoloniesBlockIds.itemProps("santa_hat"));
        ModItems.irongate = new ItemGate(IRON_GATE, ModBlocks.blockIronGate, MinecoloniesBlockIds.itemProps(IRON_GATE));
        ModItems.woodgate = new ItemGate(WOODEN_GATE, ModBlocks.blockWoodenGate, MinecoloniesBlockIds.itemProps(WOODEN_GATE));

        ModItems.flagBanner = new ItemColonyFlagBanner("colony_banner", MinecoloniesBlockIds.itemProps("colony_banner"));
        ModItems.pirateHelmet_1 = new ItemPirateGear("pirate_hat", ModArmorMaterials.PIRATE_ARMOR_1, ArmorType.HELMET, 350);
        ModItems.pirateChest_1 = new ItemPirateGear("pirate_top", ModArmorMaterials.PIRATE_ARMOR_1, ArmorType.CHESTPLATE, 550);
        ModItems.pirateLegs_1 = new ItemPirateGear("pirate_leggins", ModArmorMaterials.PIRATE_ARMOR_1, ArmorType.LEGGINGS, 500);
        ModItems.pirateBoots_1 = new ItemPirateGear("pirate_boots", ModArmorMaterials.PIRATE_ARMOR_1, ArmorType.BOOTS, 400);

        ModItems.pirateHelmet_2 = new ItemPirateGear("pirate_cap", ModArmorMaterials.PIRATE_ARMOR_2, ArmorType.HELMET, 200);
        ModItems.pirateChest_2 = new ItemPirateGear("pirate_chest", ModArmorMaterials.PIRATE_ARMOR_2, ArmorType.CHESTPLATE, 350);
        ModItems.pirateLegs_2 = new ItemPirateGear("pirate_legs", ModArmorMaterials.PIRATE_ARMOR_2, ArmorType.LEGGINGS, 300);
        ModItems.pirateBoots_2 = new ItemPirateGear("pirate_shoes", ModArmorMaterials.PIRATE_ARMOR_2, ArmorType.BOOTS, 250);

        ModItems.plateArmorHelmet = new ItemPlateArmor("plate_armor_helmet", ModArmorMaterials.PLATE_ARMOR, ArmorType.HELMET, 350);
        ModItems.plateArmorChest = new ItemPlateArmor("plate_armor_chest", ModArmorMaterials.PLATE_ARMOR, ArmorType.CHESTPLATE, 500);
        ModItems.plateArmorLegs = new ItemPlateArmor("plate_armor_legs", ModArmorMaterials.PLATE_ARMOR, ArmorType.LEGGINGS, 450);
        ModItems.plateArmorBoots = new ItemPlateArmor("plate_armor_boots", ModArmorMaterials.PLATE_ARMOR, ArmorType.BOOTS, 400);

        ModItems.sifterMeshString = new ItemSifterMesh("sifter_mesh_string", MinecoloniesBlockIds.itemProps("sifter_mesh_string", new Item.Properties().durability(500)));
        ModItems.sifterMeshFlint = new ItemSifterMesh("sifter_mesh_flint", MinecoloniesBlockIds.itemProps("sifter_mesh_flint", new Item.Properties().durability(1000)));
        ModItems.sifterMeshIron = new ItemSifterMesh("sifter_mesh_iron", MinecoloniesBlockIds.itemProps("sifter_mesh_iron", new Item.Properties().durability(1500)));
        ModItems.sifterMeshDiamond = new ItemSifterMesh("sifter_mesh_diamond", MinecoloniesBlockIds.itemProps("sifter_mesh_diamond", new Item.Properties().durability(2000)));

        ModItems.magicpotion = new ItemMagicPotion("magicpotion", MinecoloniesBlockIds.itemProps("magicpotion"));
        ModItems.buildGoggles = new ItemBuildGoggles("build_goggles", MinecoloniesBlockIds.itemProps("build_goggles"));
        ModItems.scanAnalyzer = new ItemScanAnalyzer("scan_analyzer", MinecoloniesBlockIds.itemProps("scan_analyzer"));
        ModItems.colonyMap = new ItemColonyMap(MinecoloniesBlockIds.itemProps("colonymap"));

        // All Biomes
        // Tier 1 Food Items
        ModItems.cheddar_cheese = new ItemFood((MinecoloniesBlockIds.itemProps("cheddar_cheese")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        ModItems.feta_cheese = new ItemFood((MinecoloniesBlockIds.itemProps("feta_cheese")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        ModItems.cooked_rice = new ItemFood((MinecoloniesBlockIds.itemProps("cooked_rice")).usingConvertsTo(Items.BOWL).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        ModItems.tofu = new ItemFood((MinecoloniesBlockIds.itemProps("tofu")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        ModItems.flatbread = new ItemFood((MinecoloniesBlockIds.itemProps("flatbread")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        ModItems.cheese_ravioli = new ItemFood((MinecoloniesBlockIds.itemProps("cheese_ravioli")).food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.1F).build()), 1);
        ModItems.chicken_broth = new ItemFood((MinecoloniesBlockIds.itemProps("chicken_broth")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        ModItems.meat_ravioli = new ItemFood((MinecoloniesBlockIds.itemProps("meat_ravioli")).food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.1F).build()), 1);
        ModItems.mint_jelly = new ItemFood((MinecoloniesBlockIds.itemProps("mint_jelly")).food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.1F).build()), 1);
        ModItems.mint_tea = new ItemFood((MinecoloniesBlockIds.itemProps("mint_tea")).food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.1F).build()), 1);
        ModItems.polenta = new ItemFood((MinecoloniesBlockIds.itemProps("polenta")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        ModItems.potato_soup = new ItemFood((MinecoloniesBlockIds.itemProps("potato_soup")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        ModItems.veggie_ravioli = new ItemFood((MinecoloniesBlockIds.itemProps("veggie_ravioli")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        ModItems.yogurt = new ItemFood((MinecoloniesBlockIds.itemProps("yogurt")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        ModItems.manchet_bread = new ItemFood((MinecoloniesBlockIds.itemProps("manchet_bread")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);

        // Tier 2 Food Items
        ModItems.lembas_scone = new ItemFood((MinecoloniesBlockIds.itemProps("lembas_scone")).food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.25F).build()), 2);
        ModItems.muffin = new ItemFood((MinecoloniesBlockIds.itemProps("muffin")).food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.25F).build()), 2);
        ModItems.pottage = new ItemFood((MinecoloniesBlockIds.itemProps("pottage")).usingConvertsTo(Items.BOWL).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.25F).build()), 2);
        ModItems.pasta_plain = new ItemFood((MinecoloniesBlockIds.itemProps("pasta_plain")).usingConvertsTo(Items.BOWL).food(new FoodProperties.Builder().nutrition(11).saturationModifier(0.25F).build()), 2);
        ModItems.apple_pie = new ItemFood((MinecoloniesBlockIds.itemProps("apple_pie")).food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.25F).build()), 2);
        ModItems.plain_cheesecake = new ItemFood((MinecoloniesBlockIds.itemProps("plain_cheesecake")).food(new FoodProperties.Builder().nutrition(11).saturationModifier(0.25F).build()), 2);
        ModItems.baked_salmon = new ItemFood((MinecoloniesBlockIds.itemProps("baked_salmon")).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.25F).build()), 2);
        ModItems.eggdrop_soup = new ItemFood((MinecoloniesBlockIds.itemProps("eggdrop_soup")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 2);
        ModItems.fish_n_chips = new ItemFood((MinecoloniesBlockIds.itemProps("fish_n_chips")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 2);
        ModItems.pierogi = new ItemFood((MinecoloniesBlockIds.itemProps("pierogi")).food(new FoodProperties.Builder().nutrition(11).saturationModifier(0.25F).build()), 2);
        ModItems.veggie_soup = new ItemFood((MinecoloniesBlockIds.itemProps("veggie_soup")).food(new FoodProperties.Builder().nutrition(11).saturationModifier(0.25F).build()), 2);
        ModItems.yogurt_with_berries = new ItemFood((MinecoloniesBlockIds.itemProps("yogurt_with_berries")).usingConvertsTo(Items.BOWL).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.25F).build()), 2);
        ModItems.borscht = new ItemFood((MinecoloniesBlockIds.itemProps("borscht")).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.25F).build()), 2);

        // Tier 3 Food items
        ModItems.hand_pie = new ItemFood((MinecoloniesBlockIds.itemProps("hand_pie")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);
        ModItems.mintchoco_cheesecake = new ItemFood((MinecoloniesBlockIds.itemProps("mintchoco_cheesecake")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);
        ModItems.schnitzel = new ItemFood((MinecoloniesBlockIds.itemProps("schnitzel")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);
        ModItems.steak_dinner = new ItemFood((MinecoloniesBlockIds.itemProps("steak_dinner")).food(new FoodProperties.Builder().nutrition(12).saturationModifier(0.25F).build()), 3);

        // Cold Biomes
        // Tier 1
        ModItems.squash_soup = new ItemFood((MinecoloniesBlockIds.itemProps("squash_soup")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        // Tier 2
        ModItems.cabochis = new ItemFood((MinecoloniesBlockIds.itemProps("cabochis")).usingConvertsTo(Items.BOWL).food(new FoodProperties.Builder().nutrition(11).saturationModifier(0.25F).build()), 2);
        ModItems.veggie_quiche = new ItemFood((MinecoloniesBlockIds.itemProps("veggie_quiche")).food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.25F).build()), 2);
        // Tier 3
        ModItems.lamb_stew = new ItemFood((MinecoloniesBlockIds.itemProps("lamb_stew")).usingConvertsTo(Items.BOWL).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);
        ModItems.fish_dinner = new ItemFood((MinecoloniesBlockIds.itemProps("fish_dinner")).food(new FoodProperties.Builder().nutrition(12).saturationModifier(0.25F).build()), 3);

        // Hot Humid Biomes
        // Tier 1
        ModItems.pea_soup = new ItemFood((MinecoloniesBlockIds.itemProps("pea_soup")).food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.1F).build()), 1);
        // Tier 2
        ModItems.rice_ball = new ItemFood((MinecoloniesBlockIds.itemProps("rice_ball")).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.25F).build()), 2);
        ModItems.mutton_dinner = new ItemFood((MinecoloniesBlockIds.itemProps("mutton_dinner")).food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.25F).build()), 2);
        // Tier 3
        ModItems.sushi_roll = new ItemFood((MinecoloniesBlockIds.itemProps("sushi_roll")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);
        ModItems.ramen = new ItemFood((MinecoloniesBlockIds.itemProps("ramen")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);
        ModItems.fried_rice = new ItemFood((MinecoloniesBlockIds.itemProps("fried_rice")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);

        // Temperate Biomes
        // Tier 1
        ModItems.corn_chowder = new ItemFood((MinecoloniesBlockIds.itemProps("corn_chowder")).food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.1F).build()), 1);
        ModItems.tortillas = new ItemFood((MinecoloniesBlockIds.itemProps("tortillas")).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.1F).build()), 1);
        // Tier 2
        ModItems.pasta_tomato = new ItemFood((MinecoloniesBlockIds.itemProps("pasta_tomato")).usingConvertsTo(Items.BOWL).food(new FoodProperties.Builder().nutrition(11).saturationModifier(0.25F).build()), 2);
        ModItems.cheese_pizza = new ItemFood((MinecoloniesBlockIds.itemProps("cheese_pizza")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 2);
        // Tier 3
        ModItems.eggplant_dolma = new ItemFood((MinecoloniesBlockIds.itemProps("eggplant_dolma")).food(new FoodProperties.Builder().nutrition(12).saturationModifier(0.25F).build()), 3);
        ModItems.stuffed_pita = new ItemFood((MinecoloniesBlockIds.itemProps("stuffed_pita")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);
        ModItems.mushroom_pizza = new ItemFood((MinecoloniesBlockIds.itemProps("mushroom_pizza")).food(new FoodProperties.Builder().nutrition(12).saturationModifier(0.25F).build()), 3);

        // Hot Dry Biomes
        // Tier 1
        ModItems.spicy_grilled_chicken = new ItemFood((MinecoloniesBlockIds.itemProps("spicy_grilled_chicken")).food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.1F).build()), 1);
        // Tier 2
        ModItems.pepper_hummus = new ItemFood((MinecoloniesBlockIds.itemProps("pepper_hummus")).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.25F).build()), 2);
        ModItems.kebab = new ItemFood((MinecoloniesBlockIds.itemProps("kebab")).food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.25F).build()), 2);
        // Tier 3
        ModItems.pita_hummus = new ItemFood((MinecoloniesBlockIds.itemProps("pita_hummus")).food(new FoodProperties.Builder().nutrition(12).saturationModifier(0.25F).build()), 3);
        ModItems.spicy_eggplant = new ItemFood((MinecoloniesBlockIds.itemProps("spicy_eggplant")).food(new FoodProperties.Builder().nutrition(12).saturationModifier(0.25F).build()), 3);

        // Require trading
        // Tier 2
        ModItems.congee = new ItemFood((MinecoloniesBlockIds.itemProps("congee")).usingConvertsTo(Items.BOWL).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.25F).build()), 2);
        ModItems.kimchi = new ItemFood((MinecoloniesBlockIds.itemProps("kimchi")).food(new FoodProperties.Builder().nutrition(11).saturationModifier(0.25F).build()), 2);
        // Tier 3
        ModItems.stew_trencher = new ItemFood((MinecoloniesBlockIds.itemProps("stew_trencher")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);
        ModItems.stuffed_pepper = new ItemFood((MinecoloniesBlockIds.itemProps("stuffed_pepper")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);
        ModItems.tacos = new ItemFood((MinecoloniesBlockIds.itemProps("tacos")).food(new FoodProperties.Builder().nutrition(13).saturationModifier(0.25F).build()), 3);

        // Just dough
        ModItems.muffin_dough = new Item((MinecoloniesBlockIds.itemProps("muffin_dough")));
        ModItems.manchet_dough = new Item((MinecoloniesBlockIds.itemProps("manchet_dough")));
        ModItems.raw_noodle = new Item((MinecoloniesBlockIds.itemProps("raw_noodle")));
        ModItems.butter = new Item((MinecoloniesBlockIds.itemProps("butter")));
        ModItems.cornmeal = new Item((MinecoloniesBlockIds.itemProps("cornmeal")));
        ModItems.creamcheese = new Item((MinecoloniesBlockIds.itemProps("creamcheese")));
        ModItems.soysauce = new Item((MinecoloniesBlockIds.itemProps("soysauce")));

        ModItems.large_empty_bottle = new ItemLargeBottle((MinecoloniesBlockIds.itemProps("large_empty_bottle")));
        ModItems.large_milk_bottle = new ItemLargeBottle((MinecoloniesBlockIds.itemProps("large_milk_bottle", new Item.Properties().craftRemainder(ModItems.large_empty_bottle))));
        ModItems.large_water_bottle = new ItemLargeBottle((MinecoloniesBlockIds.itemProps("large_water_bottle", new Item.Properties().craftRemainder(ModItems.large_empty_bottle))));
        ModItems.large_soy_milk_bottle = new ItemLargeBottle((MinecoloniesBlockIds.itemProps("large_soy_milk_bottle", new Item.Properties().craftRemainder(ModItems.large_empty_bottle))));

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "supplychestdeployer"), ModItems.supplyChest);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "scan_analyzer"), ModItems.scanAnalyzer);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "scepterpermission"), ModItems.permTool);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "scepterguard"), ModItems.scepterGuard);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "banner_rally_guards"), ModItems.bannerRallyGuards);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "supplycampdeployer"), ModItems.supplyCamp);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "ancienttome"), ModItems.ancientTome);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "chiefsword"), ModItems.chiefSword);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "clipboard"), ModItems.clipboard);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "compost"), ModItems.compost);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "resourcescroll"), ModItems.resourceScroll);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "iron_scimitar"), ModItems.scimitar);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "scepterlumberjack"), ModItems.scepterLumberjack);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pharaoscepter"), ModItems.pharaoscepter);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "firearrow"), ModItems.firearrow);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "scepterbeekeeper"), ModItems.scepterBeekeeper);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "mistletoe"), ModItems.mistletoe);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "spear"), ModItems.spear);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "questlog"), ModItems.questLog);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "colonymap"), ModItems.colonyMap);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "assistanthammer_gold"), ModItems.assistantHammer_Gold);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "assistanthammer_iron"), ModItems.assistantHammer_Iron);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "assistanthammer_diamond"), ModItems.assistantHammer_Diamond);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "bread_dough"), ModItems.breadDough);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "cookie_dough"), ModItems.cookieDough);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "cake_batter"), ModItems.cakeBatter);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "raw_pumpkin_pie"), ModItems.rawPumpkinPie);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "milky_bread"), ModItems.milkyBread);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "sugary_bread"), ModItems.sugaryBread);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "golden_bread"), ModItems.goldenBread);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "chorus_bread"), ModItems.chorusBread);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "adventure_token"), ModItems.adventureToken);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pirate_hat"), ModItems.pirateHelmet_1);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pirate_top"), ModItems.pirateChest_1);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pirate_leggins"), ModItems.pirateLegs_1);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pirate_boots"), ModItems.pirateBoots_1);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pirate_cap"), ModItems.pirateHelmet_2);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pirate_chest"), ModItems.pirateChest_2);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pirate_legs"), ModItems.pirateLegs_2);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pirate_shoes"), ModItems.pirateBoots_2);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "plate_armor_helmet"), ModItems.plateArmorHelmet);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "plate_armor_chest"), ModItems.plateArmorChest);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "plate_armor_legs"), ModItems.plateArmorLegs);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "plate_armor_boots"), ModItems.plateArmorBoots);


        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "santa_hat"), ModItems.santaHat);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, IRON_GATE), ModItems.irongate);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, WOODEN_GATE), ModItems.woodgate);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "colony_banner"), ModItems.flagBanner);


        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "sifter_mesh_string"), ModItems.sifterMeshString);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "sifter_mesh_flint"), ModItems.sifterMeshFlint);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "sifter_mesh_iron"), ModItems.sifterMeshIron);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "sifter_mesh_diamond"), ModItems.sifterMeshDiamond);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "magicpotion"), ModItems.magicpotion);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "build_goggles"), ModItems.buildGoggles);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "butter"), ModItems.butter);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "cabochis"), ModItems.cabochis);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "cheddar_cheese"), ModItems.cheddar_cheese);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "congee"), ModItems.congee);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "cooked_rice"), ModItems.cooked_rice);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "eggplant_dolma"), ModItems.eggplant_dolma);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "feta_cheese"), ModItems.feta_cheese);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "flatbread"), ModItems.flatbread);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "hand_pie"), ModItems.hand_pie);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "lamb_stew"), ModItems.lamb_stew);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "lembas_scone"), ModItems.lembas_scone);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "manchet_bread"), ModItems.manchet_bread);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "manchet_dough"), ModItems.manchet_dough);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "muffin"), ModItems.muffin);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "muffin_dough"), ModItems.muffin_dough);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pasta_plain"), ModItems.pasta_plain);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pasta_tomato"), ModItems.pasta_tomato);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pepper_hummus"), ModItems.pepper_hummus);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pita_hummus"), ModItems.pita_hummus);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pottage"), ModItems.pottage);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "raw_noodle"), ModItems.raw_noodle);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "rice_ball"), ModItems.rice_ball);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "stew_trencher"), ModItems.stew_trencher);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "stuffed_pepper"), ModItems.stuffed_pepper);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "stuffed_pita"), ModItems.stuffed_pita);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "sushi_roll"), ModItems.sushi_roll);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "tofu"), ModItems.tofu);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "cheese_ravioli"), ModItems.cheese_ravioli);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "chicken_broth"), ModItems.chicken_broth);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "corn_chowder"), ModItems.corn_chowder);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "spicy_grilled_chicken"), ModItems.spicy_grilled_chicken);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "kebab"), ModItems.kebab);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "meat_ravioli"), ModItems.meat_ravioli);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "mint_jelly"), ModItems.mint_jelly);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "mint_tea"), ModItems.mint_tea);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pea_soup"), ModItems.pea_soup);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "polenta"), ModItems.polenta);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "potato_soup"), ModItems.potato_soup);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "squash_soup"), ModItems.squash_soup);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "veggie_ravioli"), ModItems.veggie_ravioli);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "yogurt"), ModItems.yogurt);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "baked_salmon"), ModItems.baked_salmon);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "eggdrop_soup"), ModItems.eggdrop_soup);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "fish_n_chips"), ModItems.fish_n_chips);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "kimchi"), ModItems.kimchi);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pierogi"), ModItems.pierogi);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "veggie_quiche"), ModItems.veggie_quiche);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "veggie_soup"), ModItems.veggie_soup);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "yogurt_with_berries"), ModItems.yogurt_with_berries);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "borscht"), ModItems.borscht);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "fish_dinner"), ModItems.fish_dinner);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "mutton_dinner"), ModItems.mutton_dinner);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "ramen"), ModItems.ramen);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "fried_rice"), ModItems.fried_rice);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "schnitzel"), ModItems.schnitzel);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "steak_dinner"), ModItems.steak_dinner);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "tacos"), ModItems.tacos);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "cornmeal"), ModItems.cornmeal);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "creamcheese"), ModItems.creamcheese);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "soysauce"), ModItems.soysauce);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "tortillas"), ModItems.tortillas);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "apple_pie"), ModItems.apple_pie);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "cheese_pizza"), ModItems.cheese_pizza);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "mushroom_pizza"), ModItems.mushroom_pizza);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "plain_cheesecake"), ModItems.plain_cheesecake);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "mintchoco_cheesecake"), ModItems.mintchoco_cheesecake);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "spicy_eggplant"), ModItems.spicy_eggplant);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "large_empty_bottle"), ModItems.large_empty_bottle);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "large_water_bottle"), ModItems.large_water_bottle);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "large_milk_bottle"), ModItems.large_milk_bottle);
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "large_soy_milk_bottle"), ModItems.large_soy_milk_bottle);

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "barbarianegg"), spawnEgg(ModEntities.CAMP_BARBARIAN));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "barbarcheregg"), spawnEgg(ModEntities.CAMP_ARCHERBARBARIAN));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "barbchiefegg"), spawnEgg(ModEntities.CAMP_CHIEFBARBARIAN));

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pirateegg"), spawnEgg(ModEntities.CAMP_PIRATE));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "piratearcheregg"), spawnEgg(ModEntities.CAMP_ARCHERPIRATE));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "piratecaptainegg"), spawnEgg(ModEntities.CAMP_CHIEFPIRATE));

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "mummyegg"), spawnEgg(ModEntities.CAMP_MUMMY));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "mummyarcheregg"), spawnEgg(ModEntities.CAMP_ARCHERMUMMY));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "pharaoegg"), spawnEgg(ModEntities.CAMP_PHARAO));

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "shieldmaidenegg"), spawnEgg(ModEntities.CAMP_SHIELDMAIDEN));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "norsemenarcheregg"), spawnEgg(ModEntities.CAMP_NORSEMEN_ARCHER));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "norsemenchiefegg"), spawnEgg(ModEntities.CAMP_NORSEMEN_CHIEF));

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "amazonegg"), spawnEgg(ModEntities.CAMP_AMAZON));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "amazonspearmanegg"), spawnEgg(ModEntities.CAMP_AMAZONSPEARMAN));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "amazonchiefegg"), spawnEgg(ModEntities.CAMP_AMAZONCHIEF));

        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "drownedpirateegg"), spawnEgg(ModEntities.CAMP_DROWNED_PIRATE));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "drownedpiratearcheregg"), spawnEgg(ModEntities.CAMP_DROWNED_ARCHERPIRATE));
        Registry.register(registry, new ResourceLocation(Constants.MOD_ID, "drownedpiratecaptainegg"), spawnEgg(ModEntities.CAMP_DROWNED_CHIEFPIRATE));
    }
}

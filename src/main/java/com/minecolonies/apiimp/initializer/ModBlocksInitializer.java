package com.minecolonies.apiimp.initializer;

import com.minecolonies.api.blocks.MinecoloniesBlockIds;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.blocks.*;
import com.minecolonies.core.blocks.decorative.BlockColonyFlagBanner;
import com.minecolonies.core.blocks.decorative.BlockColonyFlagWallBanner;
import com.minecolonies.core.blocks.decorative.BlockConstructionTape;
import com.minecolonies.core.blocks.decorative.BlockGate;
import com.minecolonies.core.blocks.huts.*;
import com.minecolonies.core.blocks.schematic.BlockWaypoint;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;

import static com.minecolonies.api.blocks.decorative.AbstractBlockGate.IRON_GATE;
import static com.minecolonies.api.blocks.decorative.AbstractBlockGate.WOODEN_GATE;
import static com.minecolonies.core.blocks.MinecoloniesCropBlock.*;
import static com.minecolonies.core.blocks.MinecoloniesFarmland.FARMLAND;
import static com.minecolonies.core.blocks.MinecoloniesFarmland.FLOODED_FARMLAND;

/**
 * This class deals with the initialization of blocks and their items.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class ModBlocksInitializer
{

    private ModBlocksInitializer()
    {
        throw new IllegalStateException("Tried to initialize: ModBlockInitializer but this is a Utility class.");
    }


    @SuppressWarnings("unchecked")
    private static <B extends Block> B reg(
        final Registry<Block> registry, final String path, final java.util.function.Supplier<B> factory)
    {
        MinecoloniesBlockIds.pushBlock(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path));
        try
        {
            final B block = factory.get();
            if (block instanceof com.minecolonies.api.blocks.interfaces.IBlockMinecolonies<?> ibm)
            {
                return (B) ibm.registerBlock(registry);
            }
            Registry.register(registry, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path), block);
            return block;
        }
        finally
        {
            MinecoloniesBlockIds.popBlock();
        }
    }



    @SubscribeEvent
    public static void registerBlocks(RegisterEvent event)
    {
        if (event.getRegistryKey().equals(Registries.BLOCK))
        {
            ModBlocksInitializer.init(event.getRegistry(Registries.BLOCK));
        }
    }

    /**
     * Initializes {@link ModBlocks} with the block instances.
     *
     * @param registry The registry to register the new blocks.
     */
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public static void init(final Registry<Block> registry)
    {
        ModBlocks.blockHutBaker = reg(registry, "blockhutbaker", () -> new BlockHutBaker());
        ModBlocks.blockHutBlacksmith = reg(registry, "blockhutblacksmith", () -> new BlockHutBlacksmith());
        ModBlocks.blockHutBuilder = reg(registry, "blockhutbuilder", () -> new BlockHutBuilder());
        ModBlocks.blockHutHome = reg(registry, "blockhutcitizen", () -> new BlockHutCitizen());
        ModBlocks.blockHutDeliveryman = reg(registry, "blockhutdeliveryman", () -> new BlockHutDeliveryman());
        ModBlocks.blockHutFarmer = reg(registry, "blockhutfarmer", () -> new BlockHutFarmer());
        ModBlocks.blockScarecrow = reg(registry, "blockhutfield", () -> new BlockScarecrow());
        ModBlocks.blockHutFisherman = reg(registry, "blockhutfisherman", () -> new BlockHutFisherman());
        ModBlocks.blockHutGuardTower = reg(registry, "blockhutguardtower", () -> new BlockHutGuardTower());
        ModBlocks.blockHutLumberjack = reg(registry, "blockhutlumberjack", () -> new BlockHutLumberjack());
        ModBlocks.blockHutMiner = reg(registry, "blockhutminer", () -> new BlockHutMiner());
        ModBlocks.blockHutStonemason = reg(registry, "blockhutstonemason", () -> new BlockHutStonemason());
        ModBlocks.blockHutTownHall = reg(registry, "blockhuttownhall", () -> new BlockHutTownHall());
        ModBlocks.blockHutWareHouse = reg(registry, "blockhutwarehouse", () -> new BlockHutWareHouse());
        ModBlocks.blockHutShepherd = reg(registry, "blockhutshepherd", () -> new BlockHutShepherd());
        ModBlocks.blockHutCowboy = reg(registry, "blockhutcowboy", () -> new BlockHutCowboy());
        ModBlocks.blockHutSwineHerder = reg(registry, "blockhutswineherder", () -> new BlockHutSwineHerder());
        ModBlocks.blockHutChickenHerder = reg(registry, "blockhutchickenherder", () -> new BlockHutChickenHerder());
        ModBlocks.blockHutBarracks = reg(registry, "blockhutbarracks", () -> new BlockHutBarracks());
        ModBlocks.blockHutBarracksTower = reg(registry, "blockhutbarrackstower", () -> new BlockHutBarracksTower());
        ModBlocks.blockHutCook = reg(registry, "blockhutcook", () -> new BlockHutCook());
        ModBlocks.blockHutSmeltery = reg(registry, "blockhutsmeltery", () -> new BlockHutSmeltery());
        ModBlocks.blockHutComposter = reg(registry, "blockhutcomposter", () -> new BlockHutComposter());
        ModBlocks.blockHutLibrary = reg(registry, "blockhutlibrary", () -> new BlockHutLibrary());
        ModBlocks.blockHutArchery = reg(registry, "blockhutarchery", () -> new BlockHutArchery());
        ModBlocks.blockHutSawmill = reg(registry, "blockhutsawmill", () -> new BlockHutSawmill());
        ModBlocks.blockHutCombatAcademy = reg(registry, "blockhutcombatacademy", () -> new BlockHutCombatAcademy());
        ModBlocks.blockHutStoneSmeltery = reg(registry, "blockhutstonesmeltery", () -> new BlockHutStoneSmeltery());
        ModBlocks.blockHutCrusher = reg(registry, "blockhutcrusher", () -> new BlockHutCrusher());
        ModBlocks.blockHutSifter = reg(registry, "blockhutsifter", () -> new BlockHutSifter());
        ModBlocks.blockHutFlorist = reg(registry, "blockhutflorist", () -> new BlockHutFlorist());
        ModBlocks.blockHutEnchanter = reg(registry, "blockhutenchanter", () -> new BlockHutEnchanter());
        ModBlocks.blockHutUniversity = reg(registry, "blockhutuniversity", () -> new BlockHutUniversity());
        ModBlocks.blockHutHospital = reg(registry, "blockhuthospital", () -> new BlockHutHospital());
        ModBlocks.blockHutSchool = reg(registry, "blockhutschool", () -> new BlockHutSchool());
        ModBlocks.blockHutGlassblower = reg(registry, "blockhutglassblower", () -> new BlockHutGlassblower());
        ModBlocks.blockHutDyer = reg(registry, "blockhutdyer", () -> new BlockHutDyer());
        ModBlocks.blockHutFletcher = reg(registry, "blockhutfletcher", () -> new BlockHutFletcher());
        ModBlocks.blockHutMechanic = reg(registry, "blockhutmechanic", () -> new BlockHutMechanic());
        ModBlocks.blockHutTavern = reg(registry, "blockhuttavern", () -> new BlockHutTavern());
        ModBlocks.blockHutPlantation = reg(registry, "blockhutplantation", () -> new BlockHutPlantation());
        ModBlocks.blockPlantationField = reg(registry, "blockhutplantationfield", () -> new BlockPlantationField());
        ModBlocks.blockHutRabbitHutch = reg(registry, "blockhutrabbithutch", () -> new BlockHutRabbitHutch());
        ModBlocks.blockHutConcreteMixer = reg(registry, "blockhutconcretemixer", () -> new BlockHutConcreteMixer());
        ModBlocks.blockHutBeekeeper = reg(registry, "blockhutbeekeeper", () -> new BlockHutBeekeeper());
        ModBlocks.blockHutMysticalSite = reg(registry, "blockhutmysticalsite", () -> new BlockHutMysticalSite());
        ModBlocks.blockHutGraveyard = reg(registry, "blockhutgraveyard", () -> new BlockHutGraveyard());
        ModBlocks.blockHutNetherWorker = reg(registry, "blockhutnetherworker", () -> new BlockHutNetherWorker());
        ModBlocks.blockHutAlchemist = reg(registry, "blockhutalchemist", () -> new BlockHutAlchemist());
        ModBlocks.blockHutKitchen = reg(registry, "blockhutkitchen", () -> new BlockHutKitchen());
        ModBlocks.blockHutGateHouse = reg(registry, "blockhutgatehouse", () -> new BlockHutGateHouse());
        ModBlocks.blockHutStable = reg(registry, "blockhutstable", () -> new BlockHutStable());

        ModBlocks.blockConstructionTape = reg(registry, "blockconstructiontape", () -> new BlockConstructionTape());
        ModBlocks.blockRack = reg(registry, "blockminecoloniesrack", () -> new BlockMinecoloniesRack());
        ModBlocks.blockGrave = reg(registry, "blockminecoloniesgrave", () -> new BlockMinecoloniesGrave());
        ModBlocks.blockNamedGrave = reg(registry, "blockminecoloniesnamedgrave", () -> new BlockMinecoloniesNamedGrave());
        ModBlocks.blockWayPoint = reg(registry, "blockwaypoint", () -> new BlockWaypoint());
        ModBlocks.blockPostBox = reg(registry, "blockpostbox", () -> new BlockPostBox());
        ModBlocks.blockStash = reg(registry, "blockstash", () -> new BlockStash());
        ModBlocks.blockDecorationPlaceholder = reg(registry, "decorationcontroller", () -> new BlockDecorationController());
        ModBlocks.blockBarrel = reg(registry, "barrel_block", () -> new BlockBarrel());
        ModBlocks.blockCompostedDirt = reg(registry, "composted_dirt", () -> new BlockCompostedDirt());
        ModBlocks.blockColonyBanner = reg(registry, "colony_banner", () -> new BlockColonyFlagBanner());
        ModBlocks.blockColonyWallBanner = reg(registry, "colony_wall_banner", () -> new BlockColonyFlagWallBanner());
        ModBlocks.blockIronGate = reg(registry, IRON_GATE, () -> new BlockGate(IRON_GATE, 10f, 6, 8));
        ModBlocks.blockWoodenGate = reg(registry, WOODEN_GATE, () -> new BlockGate(WOODEN_GATE, 7f, 6, 5));
        ModBlocks.farmland = reg(registry, FARMLAND, () -> new MinecoloniesFarmland(FARMLAND, false, 15.0));
        ModBlocks.floodedFarmland = reg(registry, FLOODED_FARMLAND, () -> new MinecoloniesFarmland(FLOODED_FARMLAND, true, 13.0));
        ModBlocks.blockColonySign = reg(registry, "colonysign", () -> new BlockColonySign());

        // Could in the future add alternative versions of these crops that can be planted by the player and grow at a slower rate.
        ModBlocks.blockBellPepper = reg(registry, BELL_PEPPER, () -> new MinecoloniesCropBlock(BELL_PEPPER, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.temperateBiomes));
        ModBlocks.blockCabbage = reg(registry, CABBAGE, () -> new MinecoloniesCropBlock(CABBAGE, ModBlocks.farmland, List.of(Blocks.FERN), ModTags.coldBiomes));
        ModBlocks.blockChickpea = reg(registry, CHICKPEA, () -> new MinecoloniesCropBlock(CHICKPEA, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.DEAD_BUSH), ModTags.dryBiomes));
        ModBlocks.blockDurum = reg(registry, DURUM, () -> new MinecoloniesCropBlock(DURUM, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), null));
        ModBlocks.blockEggplant = reg(registry, EGGPLANT, () -> new MinecoloniesCropBlock(EGGPLANT, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), null));
        ModBlocks.blockGarlic = reg(registry, GARLIC, () -> new MinecoloniesCropBlock(GARLIC, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), null));
        ModBlocks.blockOnion = reg(registry, ONION, () -> new MinecoloniesCropBlock(ONION, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), null));
        ModBlocks.blockSoyBean = reg(registry, SOYBEAN, () -> new MinecoloniesCropBlock(SOYBEAN, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN), ModTags.humidBiomes));
        ModBlocks.blockTomato = reg(registry, TOMATO, () -> new MinecoloniesCropBlock(TOMATO, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.temperateBiomes));
        ModBlocks.blockRice = reg(registry, RICE, () -> new MinecoloniesCropBlock(RICE, ModBlocks.floodedFarmland, List.of(Blocks.SEAGRASS, Blocks.SMALL_DRIPLEAF), ModTags.humidBiomes));

        ModBlocks.blockButternutSquash = reg(registry, BUTTERNUT_SQUASH, () -> new MinecoloniesCropBlock(BUTTERNUT_SQUASH, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.coldBiomes));
        ModBlocks.blockCorn = reg(registry, CORN, () -> new MinecoloniesCropBlock(CORN, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.temperateBiomes));
        ModBlocks.blockMint = reg(registry, MINT, () -> new MinecoloniesCropBlock(MINT, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), null));
        ModBlocks.blockNetherPepper = reg(registry, NETHER_PEPPER, () -> new MinecoloniesCropBlock(NETHER_PEPPER, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.dryBiomes));
        ModBlocks.blockPeas = reg(registry, PEAS, () -> new MinecoloniesCropBlock(PEAS, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.humidBiomes));

        ModBlocks.blockSimpleQuarry = reg(registry, "simplequarry", () -> new SimpleQuarry());
        ModBlocks.blockMediumQuarry = reg(registry, "mediumquarry", () -> new MediumQuarry());
        //ModBlocks.blockLargeQuarry = new LargeQuarry().registerBlock(registry);
    }

    @SubscribeEvent
    public static void registerItems(RegisterEvent event)
    {
        if (event.getRegistryKey().equals(Registries.ITEM))
        {
            ModBlocksInitializer.registerBlockItem(event.getRegistry(Registries.ITEM));
        }
    }

    /**
     * Initializes the registry with the relevant item produced by the relevant blocks.
     *
     * @param registry The item registry to add the items too.
     */

    private static void regItem(final Registry<Item> registry, final com.minecolonies.api.blocks.interfaces.IBlockMinecolonies<?> block)
    {
        final net.minecraft.resources.ResourceLocation id = block.getRegistryName();
        MinecoloniesBlockIds.pushItem(id);
        try
        {
            block.registerBlockItem(registry, MinecoloniesBlockIds.withPendingItemId(new Item.Properties()));
        }
        finally
        {
            MinecoloniesBlockIds.popItem();
        }
    }

    public static void registerBlockItem(final Registry<Item> registry)
    {
        regItem(registry, ModBlocks.blockHutBaker);
        regItem(registry, ModBlocks.blockHutBlacksmith);
        regItem(registry, ModBlocks.blockHutBuilder);
        regItem(registry, ModBlocks.blockHutHome);
        regItem(registry, ModBlocks.blockHutDeliveryman);
        regItem(registry, ModBlocks.blockHutFarmer);
        regItem(registry, ModBlocks.blockScarecrow);
        regItem(registry, ModBlocks.blockHutFisherman);
        regItem(registry, ModBlocks.blockHutGuardTower);
        regItem(registry, ModBlocks.blockHutLumberjack);
        regItem(registry, ModBlocks.blockHutMiner);
        regItem(registry, ModBlocks.blockHutStonemason);
        regItem(registry, ModBlocks.blockHutTownHall);
        regItem(registry, ModBlocks.blockHutWareHouse);
        regItem(registry, ModBlocks.blockHutShepherd);
        regItem(registry, ModBlocks.blockHutCowboy);
        regItem(registry, ModBlocks.blockHutSwineHerder);
        regItem(registry, ModBlocks.blockHutChickenHerder);
        regItem(registry, ModBlocks.blockHutBarracksTower);
        regItem(registry, ModBlocks.blockHutBarracks);
        regItem(registry, ModBlocks.blockHutCook);
        regItem(registry, ModBlocks.blockHutSmeltery);
        regItem(registry, ModBlocks.blockHutComposter);
        regItem(registry, ModBlocks.blockHutLibrary);
        regItem(registry, ModBlocks.blockHutArchery);
        regItem(registry, ModBlocks.blockHutCombatAcademy);
        regItem(registry, ModBlocks.blockHutSawmill);
        regItem(registry, ModBlocks.blockHutStoneSmeltery);
        regItem(registry, ModBlocks.blockHutCrusher);
        regItem(registry, ModBlocks.blockHutSifter);
        regItem(registry, ModBlocks.blockHutFlorist);
        regItem(registry, ModBlocks.blockHutEnchanter);
        regItem(registry, ModBlocks.blockHutUniversity);
        regItem(registry, ModBlocks.blockHutHospital);
        regItem(registry, ModBlocks.blockHutSchool);
        regItem(registry, ModBlocks.blockHutGlassblower);
        regItem(registry, ModBlocks.blockHutDyer);
        regItem(registry, ModBlocks.blockHutFletcher);
        regItem(registry, ModBlocks.blockHutMechanic);
        regItem(registry, ModBlocks.blockHutTavern);
        regItem(registry, ModBlocks.blockHutPlantation);
        regItem(registry, ModBlocks.blockPlantationField);
        regItem(registry, ModBlocks.blockHutRabbitHutch);
        regItem(registry, ModBlocks.blockHutConcreteMixer);
        regItem(registry, ModBlocks.blockHutBeekeeper);
        regItem(registry, ModBlocks.blockHutMysticalSite);
        regItem(registry, ModBlocks.blockHutGraveyard);
        regItem(registry, ModBlocks.blockHutNetherWorker);
        regItem(registry, ModBlocks.blockHutAlchemist);
        regItem(registry, ModBlocks.blockHutKitchen);
        regItem(registry, ModBlocks.blockHutGateHouse);
        regItem(registry, ModBlocks.blockHutStable);

        regItem(registry, ModBlocks.blockConstructionTape);
        regItem(registry, ModBlocks.blockRack);
        regItem(registry, ModBlocks.blockGrave);
        regItem(registry, ModBlocks.blockNamedGrave);
        regItem(registry, ModBlocks.blockWayPoint);
        regItem(registry, ModBlocks.blockBarrel);
        regItem(registry, ModBlocks.blockPostBox);
        regItem(registry, ModBlocks.blockStash);
        regItem(registry, ModBlocks.blockDecorationPlaceholder);
        regItem(registry, ModBlocks.blockCompostedDirt);
        regItem(registry, ModBlocks.farmland);
        regItem(registry, ModBlocks.floodedFarmland);
        regItem(registry, ModBlocks.blockColonySign);

        regItem(registry, ModBlocks.blockBellPepper);
        regItem(registry, ModBlocks.blockCabbage);
        regItem(registry, ModBlocks.blockChickpea);
        regItem(registry, ModBlocks.blockDurum);
        regItem(registry, ModBlocks.blockEggplant);
        regItem(registry, ModBlocks.blockGarlic);
        regItem(registry, ModBlocks.blockOnion);
        regItem(registry, ModBlocks.blockSoyBean);
        regItem(registry, ModBlocks.blockTomato);
        regItem(registry, ModBlocks.blockRice);
        regItem(registry, ModBlocks.blockButternutSquash);
        regItem(registry, ModBlocks.blockCorn);
        regItem(registry, ModBlocks.blockMint);
        regItem(registry, ModBlocks.blockNetherPepper);
        regItem(registry, ModBlocks.blockPeas);

        regItem(registry, ModBlocks.blockSimpleQuarry);
        regItem(registry, ModBlocks.blockMediumQuarry);
        //regItem(registry, ModBlocks.blockLargeQuarry);
    }
}

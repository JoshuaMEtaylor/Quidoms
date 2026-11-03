package com.quidoms;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.quidoms.network.QuidomsNetwork;
import com.quidoms.registry.ModAttachments;
import com.quidoms.registry.ModBlockEntities;
import com.quidoms.registry.ModBlocks;
import com.quidoms.registry.ModCreativeTabs;
import com.quidoms.registry.ModEntities;
import com.quidoms.registry.ModItems;
import com.quidoms.registry.ModMenus;
import com.quidoms.server.ModBusEvents;
import com.quidoms.server.QuidomsEvents;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(QuidomsMod.MOD_ID)
public class QuidomsMod {
    public static final String MOD_ID = "quidoms";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QuidomsMod(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModAttachments.ATTACHMENT_TYPES.register(modBus);

        modBus.addListener(QuidomsNetwork::register);
        ModBusEvents.register(modBus);
        NeoForge.EVENT_BUS.register(QuidomsEvents.class);
        LOGGER.info("Quidoms loaded");
    }
}

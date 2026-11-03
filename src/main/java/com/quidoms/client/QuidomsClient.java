package com.quidoms.client;

import com.quidoms.QuidomsMod;
import com.quidoms.network.OpenColonyScreenS2C;
import com.quidoms.registry.ModEntities;
import com.quidoms.registry.ModMenus;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@Mod(value = QuidomsMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = QuidomsMod.MOD_ID, value = Dist.CLIENT)
public class QuidomsClient {
    public QuidomsClient() {
    }

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        QuidomsMod.LOGGER.info("Quidoms client ready");
    }

    @SubscribeEvent
    static void registerMenus(RegisterMenuScreensEvent event) {
        event.register(ModMenus.WAREHOUSE.get(), WarehouseScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CITIZEN.get(), CitizenRenderer::new);
    }

    @SubscribeEvent
    static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenColonyScreenS2C.TYPE, QuidomsClient::openColony);
    }

    private static void openColony(OpenColonyScreenS2C payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new ColonyScreen(payload)));
    }
}

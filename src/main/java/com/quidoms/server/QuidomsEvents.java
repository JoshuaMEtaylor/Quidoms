package com.quidoms.server;

import com.quidoms.QuidomsMod;
import com.quidoms.build.SchematicManager;
import com.quidoms.colony.ColonyManager;
import com.quidoms.registry.ModAttachments;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

public final class QuidomsEvents {
    private QuidomsEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        QuidomsMod.LOGGER.info("Quidoms server starting — colony manager ready (no raid systems)");
    }

    @SubscribeEvent
    public static void onReload(AddServerReloadListenersEvent event) {
        event.addListener(
                ResourceLocation.fromNamespaceAndPath(QuidomsMod.MOD_ID, "schematics"),
                SchematicManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        ColonyManager manager = ColonyManager.get(serverLevel);
        ChunkPos pos = chunk.getPos();
        manager.byChunk(pos).ifPresent(colony -> {
            Integer current = chunk.getData(ModAttachments.CHUNK_COLONY_ID.get());
            if (current == null || current != colony.id()) {
                chunk.setData(ModAttachments.CHUNK_COLONY_ID.get(), colony.id());
                chunk.markUnsaved();
            }
        });
    }
}

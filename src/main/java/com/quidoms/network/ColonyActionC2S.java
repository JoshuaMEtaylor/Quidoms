package com.quidoms.network;

import com.quidoms.QuidomsMod;
import com.quidoms.build.BuilderWork;
import com.quidoms.build.Schematic;
import com.quidoms.build.SchematicManager;
import com.quidoms.build.WorkOrder;
import com.quidoms.colony.Colony;
import com.quidoms.colony.ColonyManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ColonyActionC2S(int colonyId, String action, String schematicId, BlockPos origin)
        implements CustomPacketPayload {
    public static final Type<ColonyActionC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuidomsMod.MOD_ID, "colony_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ColonyActionC2S> CODEC =
            StreamCodec.of(ColonyActionC2S::write, ColonyActionC2S::read);

    private static void write(RegistryFriendlyByteBuf buf, ColonyActionC2S value) {
        buf.writeVarInt(value.colonyId);
        buf.writeUtf(value.action, 32);
        buf.writeUtf(value.schematicId, 128);
        buf.writeBlockPos(value.origin);
    }

    private static ColonyActionC2S read(RegistryFriendlyByteBuf buf) {
        return new ColonyActionC2S(buf.readVarInt(), buf.readUtf(32), buf.readUtf(128), buf.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ColonyActionC2S payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ColonyManager manager = ColonyManager.get(player.level());
            Colony colony = manager.byId(payload.colonyId).orElse(null);
            if (colony == null || !colony.permissions().canManage(player.getUUID())) {
                player.displayClientMessage(Component.translatable("quidoms.message.no_permission"), true);
                return;
            }
            if ("start_build".equals(payload.action)) {
                ResourceLocation id = ResourceLocation.parse(payload.schematicId);
                Schematic schematic = SchematicManager.INSTANCE.get(id).orElse(null);
                if (schematic == null) {
                    player.displayClientMessage(Component.translatable("quidoms.message.unknown_schematic"), true);
                    return;
                }
                WorkOrder order = new WorkOrder(0, id, payload.origin);
                order.setRemainingBom(BuilderWork.computeBom(player.level(), schematic));
                order.setStatus(WorkOrder.Status.PENDING);
                colony.addWorkOrder(order);
                manager.markChanged();
                player.displayClientMessage(Component.translatable("quidoms.message.build_started", schematic.displayName()), false);
                QuidomsNetwork.sendToPlayer(player, OpenColonyScreenS2C.fromColony(colony));
            } else if ("refresh".equals(payload.action)) {
                QuidomsNetwork.sendToPlayer(player, OpenColonyScreenS2C.fromColony(colony));
            }
        });
    }
}

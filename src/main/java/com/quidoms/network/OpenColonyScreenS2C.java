package com.quidoms.network;

import java.util.ArrayList;
import java.util.List;

import com.quidoms.QuidomsMod;
import com.quidoms.build.WorkOrder;
import com.quidoms.colony.BuildingData;
import com.quidoms.colony.CitizenData;
import com.quidoms.colony.Colony;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenColonyScreenS2C(
        int colonyId,
        String name,
        int citizenCount,
        int housingCap,
        List<String> citizenLines,
        List<String> buildingLines,
        List<String> workOrderLines,
        List<String> bomLines
) implements CustomPacketPayload {
    public static final Type<OpenColonyScreenS2C> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuidomsMod.MOD_ID, "open_colony"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenColonyScreenS2C> CODEC =
            StreamCodec.of(OpenColonyScreenS2C::write, OpenColonyScreenS2C::read);

    public static OpenColonyScreenS2C fromColony(Colony colony) {
        List<String> citizens = new ArrayList<>();
        for (CitizenData citizen : colony.citizens().values()) {
            citizens.add(citizen.name() + " · " + citizen.job().id() + " · sat " + String.format("%.0f", citizen.saturation()));
        }
        List<String> buildings = new ArrayList<>();
        for (BuildingData building : colony.buildings().values()) {
            buildings.add(building.type().id() + " @ " + building.pos().toShortString());
        }
        List<String> orders = new ArrayList<>();
        List<String> bom = new ArrayList<>();
        for (WorkOrder order : colony.workOrders().values()) {
            orders.add("#" + order.id() + " " + order.schematicId() + " [" + order.status() + "] " + order.nextIndex());
            if (order.status() != WorkOrder.Status.COMPLETE) {
                order.remainingBom().forEach((id, count) -> bom.add(count + " × " + id));
            }
        }
        return new OpenColonyScreenS2C(
                colony.id(),
                colony.name(),
                colony.citizens().size(),
                colony.housingCapacity(),
                citizens,
                buildings,
                orders,
                bom);
    }

    private static void write(RegistryFriendlyByteBuf buf, OpenColonyScreenS2C value) {
        buf.writeVarInt(value.colonyId);
        buf.writeUtf(value.name, 64);
        buf.writeVarInt(value.citizenCount);
        buf.writeVarInt(value.housingCap);
        writeLines(buf, value.citizenLines);
        writeLines(buf, value.buildingLines);
        writeLines(buf, value.workOrderLines);
        writeLines(buf, value.bomLines);
    }

    private static OpenColonyScreenS2C read(RegistryFriendlyByteBuf buf) {
        return new OpenColonyScreenS2C(
                buf.readVarInt(),
                buf.readUtf(64),
                buf.readVarInt(),
                buf.readVarInt(),
                readLines(buf),
                readLines(buf),
                readLines(buf),
                readLines(buf));
    }

    private static void writeLines(RegistryFriendlyByteBuf buf, List<String> lines) {
        buf.writeVarInt(lines.size());
        for (String line : lines) {
            buf.writeUtf(line, 200);
        }
    }

    private static List<String> readLines(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> lines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            lines.add(buf.readUtf(200));
        }
        return lines;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

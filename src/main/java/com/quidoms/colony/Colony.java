package com.quidoms.colony;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quidoms.build.WorkOrder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.ChunkPos;

public final class Colony {
    private final int id;
    private String name;
    private BlockPos center;
    private final ColonyPermissions permissions;
    private final Map<Integer, BuildingData> buildings = new HashMap<>();
    private final Map<UUID, CitizenData> citizens = new HashMap<>();
    private final Map<Integer, WorkOrder> workOrders = new HashMap<>();
    private final Set<Long> claimedChunks = new HashSet<>();
    private int nextBuildingId = 1;
    private int nextWorkOrderId = 1;

    public Colony(int id, String name, UUID owner, BlockPos center) {
        this.id = id;
        this.name = name;
        this.center = center.immutable();
        this.permissions = new ColonyPermissions(owner);
    }

    private Colony(
            int id,
            String name,
            BlockPos center,
            ColonyPermissions permissions,
            List<BuildingData> buildings,
            List<CitizenData> citizens,
            List<WorkOrder> workOrders,
            List<Long> claimedChunks,
            int nextBuildingId,
            int nextWorkOrderId) {
        this.id = id;
        this.name = name;
        this.center = center.immutable();
        this.permissions = permissions;
        for (BuildingData building : buildings) {
            this.buildings.put(building.id(), building);
        }
        for (CitizenData citizen : citizens) {
            this.citizens.put(citizen.id(), citizen);
        }
        for (WorkOrder order : workOrders) {
            this.workOrders.put(order.id(), order);
        }
        this.claimedChunks.addAll(claimedChunks);
        this.nextBuildingId = nextBuildingId;
        this.nextWorkOrderId = nextWorkOrderId;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BlockPos center() {
        return center;
    }

    public void setCenter(BlockPos center) {
        this.center = center.immutable();
    }

    public ColonyPermissions permissions() {
        return permissions;
    }

    public Map<Integer, BuildingData> buildings() {
        return buildings;
    }

    public Map<UUID, CitizenData> citizens() {
        return citizens;
    }

    public Map<Integer, WorkOrder> workOrders() {
        return workOrders;
    }

    public Set<Long> claimedChunks() {
        return claimedChunks;
    }

    public BuildingData addBuilding(BuildingType type, BlockPos pos) {
        BuildingData building = new BuildingData(nextBuildingId++, type, pos);
        buildings.put(building.id(), building);
        return building;
    }

    public Optional<BuildingData> buildingAt(BlockPos pos) {
        for (BuildingData building : buildings.values()) {
            if (building.pos().equals(pos)) {
                return Optional.of(building);
            }
        }
        return Optional.empty();
    }

    public Optional<BuildingData> findBuilding(BuildingType type) {
        for (BuildingData building : buildings.values()) {
            if (building.type() == type) {
                return Optional.of(building);
            }
        }
        return Optional.empty();
    }

    public int housingCapacity() {
        int beds = 0;
        for (BuildingData building : buildings.values()) {
            beds += building.type().bedSlots();
        }
        return Math.max(beds, buildings.containsKey(townHallIdOrZero()) || hasTownHall() ? 1 : 0);
    }

    private boolean hasTownHall() {
        return buildings.values().stream().anyMatch(b -> b.type() == BuildingType.TOWN_HALL);
    }

    private int townHallIdOrZero() {
        return findBuilding(BuildingType.TOWN_HALL).map(BuildingData::id).orElse(0);
    }

    public CitizenData addCitizen(String name) {
        CitizenData citizen = new CitizenData(UUID.randomUUID(), name);
        citizens.put(citizen.id(), citizen);
        return citizen;
    }

    public WorkOrder addWorkOrder(WorkOrder orderTemplate) {
        WorkOrder order = new WorkOrder(nextWorkOrderId++, orderTemplate.schematicId(), orderTemplate.origin());
        order.setRemainingBom(orderTemplate.remainingBom());
        order.setStatus(orderTemplate.status());
        workOrders.put(order.id(), order);
        return order;
    }

    public void claimChunk(ChunkPos chunk) {
        claimedChunks.add(chunk.toLong());
    }

    public boolean ownsChunk(ChunkPos chunk) {
        return claimedChunks.contains(chunk.toLong());
    }

    public static final Codec<Colony> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(Colony::id),
            Codec.STRING.fieldOf("name").forGetter(Colony::name),
            BlockPos.CODEC.fieldOf("center").forGetter(Colony::center),
            ColonyPermissions.CODEC.fieldOf("permissions").forGetter(Colony::permissions),
            BuildingData.CODEC.listOf().fieldOf("buildings").forGetter(c -> List.copyOf(c.buildings.values())),
            CitizenData.CODEC.listOf().fieldOf("citizens").forGetter(c -> List.copyOf(c.citizens.values())),
            WorkOrder.CODEC.listOf().fieldOf("workOrders").forGetter(c -> List.copyOf(c.workOrders.values())),
            Codec.LONG.listOf().fieldOf("claimedChunks").forGetter(c -> new ArrayList<>(c.claimedChunks)),
            Codec.INT.fieldOf("nextBuildingId").forGetter(c -> c.nextBuildingId),
            Codec.INT.fieldOf("nextWorkOrderId").forGetter(c -> c.nextWorkOrderId)
    ).apply(instance, Colony::new));
}

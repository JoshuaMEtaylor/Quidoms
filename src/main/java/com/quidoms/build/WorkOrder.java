package com.quidoms.build;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class WorkOrder {
    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETE;

        public static Status byId(String id) {
            try {
                return Status.valueOf(id.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return PENDING;
            }
        }

        public String id() {
            return name();
        }
    }

    private final int id;
    private final ResourceLocation schematicId;
    private final BlockPos origin;
    private Status status = Status.PENDING;
    private int nextIndex;
    private final Map<ResourceLocation, Integer> remainingBom = new HashMap<>();

    public WorkOrder(int id, ResourceLocation schematicId, BlockPos origin) {
        this.id = id;
        this.schematicId = schematicId;
        this.origin = origin.immutable();
    }

    private WorkOrder(
            int id,
            ResourceLocation schematicId,
            BlockPos origin,
            Status status,
            int nextIndex,
            Map<ResourceLocation, Integer> remainingBom) {
        this(id, schematicId, origin);
        this.status = status;
        this.nextIndex = nextIndex;
        this.remainingBom.putAll(remainingBom);
    }

    public int id() {
        return id;
    }

    public ResourceLocation schematicId() {
        return schematicId;
    }

    public BlockPos origin() {
        return origin;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status == null ? Status.PENDING : status;
    }

    public int nextIndex() {
        return nextIndex;
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex = Math.max(0, nextIndex);
    }

    public Map<ResourceLocation, Integer> remainingBom() {
        return remainingBom;
    }

    public void setRemainingBom(Map<ResourceLocation, Integer> bom) {
        remainingBom.clear();
        if (bom != null) {
            bom.forEach((id, count) -> {
                if (id != null && count != null && count > 0) {
                    remainingBom.put(id, count);
                }
            });
        }
    }

    public static final Codec<WorkOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(WorkOrder::id),
            ResourceLocation.CODEC.fieldOf("schematicId").forGetter(WorkOrder::schematicId),
            BlockPos.CODEC.fieldOf("origin").forGetter(WorkOrder::origin),
            Codec.STRING.xmap(Status::byId, Status::id).optionalFieldOf("status", Status.PENDING).forGetter(WorkOrder::status),
            Codec.INT.optionalFieldOf("nextIndex", 0).forGetter(WorkOrder::nextIndex),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                    .optionalFieldOf("remainingBom", Map.of())
                    .forGetter(o -> Map.copyOf(o.remainingBom))
    ).apply(instance, WorkOrder::new));
}

package com.quidoms.colony;

import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

public final class BuildingData {
    private final int id;
    private BuildingType type;
    private BlockPos pos;
    private Optional<UUID> assignedCitizen = Optional.empty();
    private int level = 1;

    public BuildingData(int id, BuildingType type, BlockPos pos) {
        this.id = id;
        this.type = type;
        this.pos = pos.immutable();
    }

    public int id() {
        return id;
    }

    public BuildingType type() {
        return type;
    }

    public BlockPos pos() {
        return pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos.immutable();
    }

    public Optional<UUID> assignedCitizen() {
        return assignedCitizen;
    }

    public void setAssignedCitizen(UUID citizenId) {
        this.assignedCitizen = Optional.ofNullable(citizenId);
    }

    public void clearAssignedCitizen() {
        this.assignedCitizen = Optional.empty();
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public static final Codec<BuildingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(BuildingData::id),
            Codec.STRING.xmap(BuildingType::byId, BuildingType::id).fieldOf("type").forGetter(BuildingData::type),
            BlockPos.CODEC.fieldOf("pos").forGetter(BuildingData::pos),
            UUIDUtil.CODEC.optionalFieldOf("assignedCitizen").forGetter(BuildingData::assignedCitizen),
            Codec.INT.optionalFieldOf("level", 1).forGetter(BuildingData::level)
    ).apply(instance, BuildingData::fromCodec));

    private static BuildingData fromCodec(int id, BuildingType type, BlockPos pos, Optional<UUID> assigned, int level) {
        BuildingData data = new BuildingData(id, type, pos);
        assigned.ifPresent(data::setAssignedCitizen);
        data.setLevel(level);
        return data;
    }
}

package com.quidoms.colony;

import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

public final class CitizenData {
    private final UUID id;
    private String name;
    private JobType job = JobType.UNEMPLOYED;
    private Optional<Integer> homeBuildingId = Optional.empty();
    private Optional<Integer> workBuildingId = Optional.empty();
    private Optional<UUID> entityUuid = Optional.empty();
    private float saturation = 10.0F;
    private boolean sleeping;
    private BlockPos lastPos = BlockPos.ZERO;

    public CitizenData(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JobType job() {
        return job;
    }

    public void setJob(JobType job) {
        this.job = job == null ? JobType.UNEMPLOYED : job;
    }

    public Optional<Integer> homeBuildingId() {
        return homeBuildingId;
    }

    public void setHomeBuildingId(Integer buildingId) {
        this.homeBuildingId = Optional.ofNullable(buildingId);
    }

    public Optional<Integer> workBuildingId() {
        return workBuildingId;
    }

    public void setWorkBuildingId(Integer buildingId) {
        this.workBuildingId = Optional.ofNullable(buildingId);
    }

    public Optional<UUID> entityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = Optional.ofNullable(entityUuid);
    }

    public float saturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = Math.max(0.0F, Math.min(20.0F, saturation));
    }

    public boolean sleeping() {
        return sleeping;
    }

    public void setSleeping(boolean sleeping) {
        this.sleeping = sleeping;
    }

    public BlockPos lastPos() {
        return lastPos;
    }

    public void setLastPos(BlockPos lastPos) {
        this.lastPos = lastPos.immutable();
    }

    public static final Codec<CitizenData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(CitizenData::id),
            Codec.STRING.fieldOf("name").forGetter(CitizenData::name),
            Codec.STRING.xmap(JobType::byId, JobType::id).optionalFieldOf("job", JobType.UNEMPLOYED).forGetter(CitizenData::job),
            Codec.INT.optionalFieldOf("homeBuildingId").forGetter(CitizenData::homeBuildingId),
            Codec.INT.optionalFieldOf("workBuildingId").forGetter(CitizenData::workBuildingId),
            UUIDUtil.CODEC.optionalFieldOf("entityUuid").forGetter(CitizenData::entityUuid),
            Codec.FLOAT.optionalFieldOf("saturation", 10.0F).forGetter(CitizenData::saturation),
            Codec.BOOL.optionalFieldOf("sleeping", false).forGetter(CitizenData::sleeping),
            BlockPos.CODEC.optionalFieldOf("lastPos", BlockPos.ZERO).forGetter(CitizenData::lastPos)
    ).apply(instance, CitizenData::fromCodec));

    private static CitizenData fromCodec(
            UUID id,
            String name,
            JobType job,
            Optional<Integer> home,
            Optional<Integer> work,
            Optional<UUID> entityUuid,
            float saturation,
            boolean sleeping,
            BlockPos lastPos) {
        CitizenData data = new CitizenData(id, name);
        data.setJob(job);
        home.ifPresent(data::setHomeBuildingId);
        work.ifPresent(data::setWorkBuildingId);
        entityUuid.ifPresent(data::setEntityUuid);
        data.setSaturation(saturation);
        data.setSleeping(sleeping);
        data.setLastPos(lastPos);
        return data;
    }
}

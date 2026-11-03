package com.quidoms.entity;

import java.util.UUID;

import com.quidoms.ai.CitizenAi;
import com.quidoms.tool.ColonistTools;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CitizenEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> COLONY_ID =
            SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);

    private UUID citizenId = UUID.randomUUID();
    private final CitizenAi ai = new CitizenAi(this);

    public CitizenEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLONY_ID, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public void bind(int colonyId, UUID citizenId) {
        this.entityData.set(COLONY_ID, colonyId);
        this.citizenId = citizenId;
    }

    public int colonyId() {
        return this.entityData.get(COLONY_ID);
    }

    public UUID citizenId() {
        return citizenId;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
            ColonistTools.enforceInventory(this);
            ai.tick(serverLevel);
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        ColonistTools.makeUnbreakable(stack);
        super.setItemSlot(slot, stack);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("ColonyId", colonyId());
        output.store("CitizenId", UUIDUtil.CODEC, citizenId);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(COLONY_ID, input.getIntOr("ColonyId", 0));
        input.read("CitizenId", UUIDUtil.CODEC).ifPresent(id -> citizenId = id);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}

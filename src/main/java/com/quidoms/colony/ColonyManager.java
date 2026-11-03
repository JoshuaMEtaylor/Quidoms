package com.quidoms.colony;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quidoms.QuidomsMod;
import com.quidoms.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ColonyManager extends SavedData {
    public static final String DATA_NAME = QuidomsMod.MOD_ID + "_colonies";

    public static final Codec<ColonyManager> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Colony.CODEC.listOf().fieldOf("colonies").forGetter(m -> new ArrayList<>(m.colonies.values())),
            Codec.INT.fieldOf("nextId").forGetter(m -> m.nextId)
    ).apply(instance, ColonyManager::fromCodec));

    public static final SavedDataType<ColonyManager> TYPE = new SavedDataType<>(
            DATA_NAME,
            ColonyManager::new,
            CODEC);

    private final Map<Integer, Colony> colonies = new HashMap<>();
    private int nextId = 1;

    public ColonyManager() {
    }

    private static ColonyManager fromCodec(List<Colony> colonies, int nextId) {
        ColonyManager manager = new ColonyManager();
        for (Colony colony : colonies) {
            manager.colonies.put(colony.id(), colony);
        }
        manager.nextId = nextId;
        return manager;
    }

    public static ColonyManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<Integer, Colony> colonies() {
        return colonies;
    }

    public Optional<Colony> byId(int id) {
        return Optional.ofNullable(colonies.get(id));
    }

    public Optional<Colony> byOwner(UUID owner) {
        for (Colony colony : colonies.values()) {
            if (colony.permissions().owner().equals(owner)) {
                return Optional.of(colony);
            }
        }
        return Optional.empty();
    }

    public Optional<Colony> byChunk(ChunkPos chunk) {
        long key = chunk.toLong();
        for (Colony colony : colonies.values()) {
            if (colony.claimedChunks().contains(key)) {
                return Optional.of(colony);
            }
        }
        return Optional.empty();
    }

    public Optional<Colony> byPos(BlockPos pos) {
        return byChunk(new ChunkPos(pos));
    }

    public Colony createColony(ServerLevel level, UUID owner, String name, BlockPos center, int claimRadius) {
        if (byOwner(owner).isPresent()) {
            throw new IllegalStateException("Player already owns a colony");
        }
        Colony colony = new Colony(nextId++, name, owner, center);
        colonies.put(colony.id(), colony);
        claimAround(level, colony, center, claimRadius);
        setDirty();
        return colony;
    }

    public void claimAround(ServerLevel level, Colony colony, BlockPos center, int radiusChunks) {
        ChunkPos centerChunk = new ChunkPos(center);
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                colony.claimChunk(chunkPos);
                if (level.hasChunk(chunkPos.x, chunkPos.z)) {
                    LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
                    chunk.setData(ModAttachments.CHUNK_COLONY_ID.get(), colony.id());
                    chunk.markUnsaved();
                }
            }
        }
        setDirty();
    }

    public void markChanged() {
        setDirty();
    }

    public static Optional<Colony> find(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        return get(serverLevel).byPos(pos);
    }
}

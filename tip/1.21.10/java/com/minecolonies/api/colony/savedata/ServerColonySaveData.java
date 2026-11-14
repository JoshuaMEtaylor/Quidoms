package com.minecolonies.api.colony.savedata;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.ColonyList;
import com.minecolonies.core.util.BackUpHelper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerColonySaveData extends SavedData implements IServerColonySaveData {
    public static final String NAME = ResourceLocation.fromNamespaceAndPath("minecolonies", "colony_manager").toDebugFileName();
    public static final SavedDataType<ServerColonySaveData> TYPE =
        new SavedDataType<>(NAME, ServerColonySaveData::new, ServerColonySaveData::makeCodec);

    @NotNull
    private final ColonyList<IColony> colonies = new ColonyList<>();
    private boolean overworld;
    @Nullable
    private ServerLevel level;

    private ServerColonySaveData() {
    }

    private ServerColonySaveData(SavedData.Context context) {
        this.level = context.level();
    }

    private static Codec<ServerColonySaveData> makeCodec(SavedData.Context context) {
        return CompoundTag.CODEC.flatXmap(tag -> {
            ColonySaveDataLoadGuard.enter();
            try {
                ServerColonySaveData data = new ServerColonySaveData(context);
                data.readNBT(context.levelOrThrow().registryAccess(), tag);
                return DataResult.success(data);
            } finally {
                ColonySaveDataLoadGuard.exit();
            }
        }, data -> DataResult.success(data.writeNBT(
            data.level != null ? data.level.registryAccess() : RegistryAccess.EMPTY,
            new CompoundTag())));
    }

    @Override
    public IColony createColony(@NotNull ServerLevel w, @NotNull String name, @NotNull BlockPos pos) {
        this.level = w;
        return this.colonies.create(w, name, pos);
    }

    @Override
    public void deleteColony(int id) {
        this.colonies.remove(id);
    }

    @Override
    public IColony getColony(int id) {
        return this.colonies.get(id);
    }

    @Override
    public List<IColony> getColonies() {
        return this.colonies.getCopyAsList();
    }

    @Override
    public void addColony(IColony colony) {
        this.colonies.add(colony);
    }

    @Override
    public int getTopID() {
        return this.colonies.getTopID();
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    private CompoundTag writeNBT(@NotNull HolderLookup.Provider provider, CompoundTag inputTag) {
        CompoundTag compound = new CompoundTag();
        ListTag colonyTag = new ListTag();
        for (IColony colony : this.colonies) {
            try {
                colonyTag.add(colony.getColonyTag());
            } catch (Exception e) {
                Log.getLogger().error("Colony: " + colony.getName() + " id:" + colony.getID()
                    + " owner:" + colony.getPermissions().getOwnerName() + " could not be saved! Error:", e);
            }
        }
        compound.put("colonies", colonyTag);
        if (this.overworld) {
            CompoundTag managerCompound = new CompoundTag();
            IColonyManager.getInstance().write(provider, managerCompound);
            compound.put("colonymanager", managerCompound);
        }
        inputTag.put("minecolonies", compound);
        return inputTag;
    }

    @Override
    public IServerColonySaveData setOverworld(boolean overworld) {
        this.overworld = overworld;
        return this;
    }

    private void readNBT(@NotNull HolderLookup.Provider provider, CompoundTag inputTag) {
        CompoundTag compound = inputTag.getCompoundOrEmpty("minecolonies");
        if (!compound.contains("colonies")) {
            BackUpHelper.loadManagerBackup(provider);
            return;
        }
        Multimap<BlockPos, IColony> tempColonies = ArrayListMultimap.create();
        for (Tag tag : compound.getListOrEmpty("colonies")) {
            Colony colony = Colony.loadColony((CompoundTag) tag, this.level, provider);
            if (colony == null) {
                continue;
            }
            tempColonies.put(colony.getCenter(), colony);
            this.colonies.add(colony);
        }
        if (compound.contains("colonymanager")) {
            IColonyManager.getInstance().read(provider, compound.getCompoundOrEmpty("colonymanager"));
            this.overworld = true;
        }
        for (BlockPos pos : tempColonies.keySet()) {
            if (tempColonies.get(pos).size() <= 1) {
                continue;
            }
            Log.getLogger().warn("Detected duplicate colonies which are at the same position:");
            for (IColony colony : tempColonies.get(pos)) {
                Log.getLogger().warn("ID: " + colony.getID() + " name:" + colony.getName()
                    + " citizens:" + colony.getCitizenManager().getCitizens().size()
                    + " building count:" + colony.getCommonBuildingManager().getBuildings().size());
            }
            Log.getLogger().warn("Check and remove all except one of the duplicated colonies above!");
        }
    }
}

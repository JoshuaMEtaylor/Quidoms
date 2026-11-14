package com.minecolonies.api.colony.savedata;

import com.minecolonies.api.colony.IColony;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public interface IServerColonySaveData {
    IColony createColony(@NotNull ServerLevel w, @NotNull String name, @NotNull BlockPos pos);
    void deleteColony(int id);
    @Nullable IColony getColony(int id);
    List<IColony> getColonies();
    void addColony(IColony colony);
    int getTopID();
    IServerColonySaveData setOverworld(boolean overworld);

    static IServerColonySaveData getOrComputeSaveData(ServerLevel level) {
        IServerColonySaveData existing = getSaveData(level);
        if (existing != null) {
            return existing.setOverworld(level.dimension() == Level.OVERWORLD);
        }
        if (ColonySaveDataLoadGuard.isLoading()) {
            return null;
        }
        return level.getDataStorage().computeIfAbsent(ServerColonySaveData.TYPE)
            .setOverworld(level.dimension() == Level.OVERWORLD);
    }

    static IServerColonySaveData getSaveData(ServerLevel level) {
        return level.getDataStorage().get(ServerColonySaveData.TYPE);
    }
}

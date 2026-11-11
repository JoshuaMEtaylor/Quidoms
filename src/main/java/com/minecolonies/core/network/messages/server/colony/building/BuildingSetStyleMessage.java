package com.minecolonies.core.network.messages.server.colony.building;

import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import com.minecolonies.core.util.HutBlueprintResolver;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Message to set the style (and blueprint path) of a building before Build Building creates a work order.
 */
public class BuildingSetStyleMessage extends AbstractBuildingServerMessage<IBuilding>
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "building_set_style", BuildingSetStyleMessage::new);

    private final String structurePack;
    private final String blueprintPath;

    public BuildingSetStyleMessage(@NotNull final IBuildingView building, final String structurePack)
    {
        this(building, structurePack, "");
    }

    public BuildingSetStyleMessage(@NotNull final IBuildingView building, final String structurePack, @Nullable final String blueprintPath)
    {
        super(TYPE, building);
        this.structurePack = structurePack == null ? "" : structurePack;
        this.blueprintPath = blueprintPath == null ? "" : blueprintPath;
    }

    protected BuildingSetStyleMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        structurePack = buf.readUtf(32767);
        blueprintPath = buf.readableBytes() > 0 ? buf.readUtf(32767) : "";
    }

    @Override
    protected void toBytes(@NotNull final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeUtf(structurePack);
        buf.writeUtf(blueprintPath);
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony, final IBuilding building)
    {
        if (building.getBuildingLevel() > 0 && !building.isDeconstructed())
        {
            return;
        }

        final String hut = building.getBuildingType() == null || building.getBuildingType().getRegistryName() == null
                             ? "builder"
                             : building.getBuildingType().getRegistryName().getPath();
        final int level = Math.max(1, building.getBuildingLevel() == 0 ? 1 : building.getBuildingLevel());

        // Prefer the client-chosen pack+path when it actually exists in StructurePacks.
        final HutBlueprintResolver.Resolved preferred =
          HutBlueprintResolver.resolve(
            hut,
            structurePack.isEmpty() ? building.getStructurePack() : structurePack,
            blueprintPath.isBlank() ? building.getBlueprintPath() : blueprintPath,
            level);

        if (preferred != null)
        {
            HutBlueprintResolver.apply(building, preferred);
            return;
        }

        // Fallback: any pack that has this hut schematic.
        HutBlueprintResolver.ensure(building, level);
    }
}

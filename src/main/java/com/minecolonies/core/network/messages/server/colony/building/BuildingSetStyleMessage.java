package com.minecolonies.core.network.messages.server.colony.building;

import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.compatibility.newstruct.BlueprintMapping;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
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

    /**
     * The style to set.
     */
    private final String structurePack;

    /**
     * Blueprint sub-path for this hut (may be empty; server will derive if needed).
     */
    private final String blueprintPath;

    /**
     * Creates object for the style of a building.
     *
     * @param building      View of the building to read data from.
     * @param structurePack style of the building.
     */
    public BuildingSetStyleMessage(@NotNull final IBuildingView building, final String structurePack)
    {
        this(building, structurePack, "");
    }

    /**
     * Creates object for the style + blueprint path of a building.
     *
     * @param building      View of the building to read data from.
     * @param structurePack style of the building.
     * @param blueprintPath schematic path within the pack (e.g. fundamentals/townhall1.blueprint).
     */
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
        // Older clients only sent pack — keep path optional for wire safety.
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

        if (!structurePack.isEmpty())
        {
            building.setStructurePack(structurePack);
        }

        String path = blueprintPath;
        if (path == null || path.isBlank())
        {
            path = building.getBlueprintPath();
        }
        if (path == null || path.isBlank())
        {
            path = deriveDefaultBlueprintPath(building);
        }
        if (path != null && !path.isBlank())
        {
            building.setBlueprintPath(path);
        }
    }

    @Nullable
    private static String deriveDefaultBlueprintPath(@NotNull final IBuilding building)
    {
        try
        {
            if (building.getBuildingType() == null || building.getBuildingType().getRegistryName() == null)
            {
                return null;
            }
            final String hut = building.getBuildingType().getRegistryName().getPath();
            if (hut.isEmpty())
            {
                return null;
            }
            String mapped = BlueprintMapping.getPathMapping("", hut);
            if (mapped == null || mapped.isEmpty())
            {
                mapped = "fundamentals/" + hut;
            }
            final int level = Math.max(1, building.getBuildingLevel() == 0 ? 1 : building.getBuildingLevel());
            return mapped + level + ".blueprint";
        }
        catch (final Exception e)
        {
            return null;
        }
    }
}

package com.minecolonies.core.network.messages.server;

import com.ldtteam.common.network.AbstractServerPlayMessage;
import com.ldtteam.common.network.PlayMessageType;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.eventbus.events.colony.ColonyCreatedModEvent;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.MessageUtils.MessagePriority;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.MineColonies;
import com.minecolonies.core.compatibility.quidcraft.QuidCraftKingdomBridge;
import com.minecolonies.core.network.messages.client.colony.OpenBuildingUIMessage;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.minecolonies.api.util.constant.BuildingConstants.DEACTIVATED;
import static com.minecolonies.api.util.constant.TranslationConstants.*;

/**
 * Tip 1.21.10 CreateColonyMessage — {@code ServerPlayer.level()} + {@code getRespawnData()}.
 * Always sends player feedback on success or failure (including founding fee).
 */
public class CreateColonyMessage extends AbstractServerPlayMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "create_colony", CreateColonyMessage::new);

    private final BlockPos townHall;
    private final boolean claim;
    private final String colonyName;
    private final String packName;
    private final String pathName;

    public CreateColonyMessage(final BlockPos townHall, final boolean claim, final String colonyName, final String packName, final String pathName)
    {
        super(TYPE);
        this.townHall = townHall;
        this.claim = claim;
        this.colonyName = colonyName;
        this.packName = packName;
        this.pathName = pathName;
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        buf.writeBlockPos(townHall);
        buf.writeBoolean(claim);
        buf.writeUtf(colonyName);
        buf.writeUtf(packName);
        buf.writeUtf(pathName);
    }

    protected CreateColonyMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        townHall = buf.readBlockPos();
        claim = buf.readBoolean();
        colonyName = buf.readUtf(32767);
        packName = buf.readUtf(32767);
        pathName = buf.readUtf(32767);
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer sender)
    {
        try
        {
            onExecuteSafe(sender);
        }
        catch (final Throwable ex)
        {
            System.err.println("CreateColonyMessage failed: " + ex);
            ex.printStackTrace();
            try
            {
                // Never send WARNING_COLONY_FOUNDING_FAILED / “already got a kingdom” on unexpected errors —
                // that message is only for the ownedColony != null path below.
                final String detail = ex.getClass().getSimpleName()
                  + (ex.getMessage() != null && !ex.getMessage().isEmpty() ? ": " + ex.getMessage() : "");
                final Component real = Component.literal("Founding failed: " + detail);
                sender.sendSystemMessage(real);
                sender.displayClientMessage(real, true);
            }
            catch (final Throwable ignored)
            {
            }
        }
    }

    private void onExecuteSafe(final ServerPlayer sender)
    {
        // Tip 1.21.10: ServerPlayer.level() returns ServerLevel (serverLevel() does not exist).
        final ServerLevel world = sender.level();

        final IColony colony = IColonyManager.getInstance().getClosestColony(world, townHall);

        String pack = packName == null ? "" : packName;
        final BlockEntity tileEntity = world.getBlockEntity(townHall);

        if (!(tileEntity instanceof final TileEntityColonyBuilding hut))
        {
            MessageUtils.format(WARNING_TOWN_HALL_NO_TILE_ENTITY)
              .withPriority(MessagePriority.DANGER)
              .sendTo(sender);
            sender.displayClientMessage(Component.literal("Founding failed: town hall has no tile entity."), true);
            return;
        }

        // Prefer hut pack whenever present — client selectedPack is often unset after supply placement.
        if (hut.getStructurePack() != null && (claim || pack.isEmpty()))
        {
            pack = hut.getStructurePack().getName();
        }

        final boolean reactivate = hut.getPositionedTags().getOrDefault(BlockPos.ZERO, new ArrayList<>()).contains(DEACTIVATED);
        if (reactivate)
        {
            hut.reactivate();
            if (hut.getStructurePack() != null)
            {
                pack = hut.getStructurePack().getName();
            }
        }

        hut.setStructurePack(StructurePacks.getStructurePack(pack));
        hut.setBlueprintPath(pathName);

        final double spawnDistance = Math.sqrt(BlockPosUtil.getDistanceSquared2D(townHall, world.getRespawnData().pos()));
        // NeoForge 21.10: IntValue exposes getAsInt() (IntSupplier). Do not call get():Integer —
        // tip-javac against a flat stub emitted that and caused NoSuchMethodError at runtime.
        final int minSpawnDist = MineColonies.getConfig().getServer().minDistanceFromWorldSpawn.getAsInt();
        final int maxSpawnDist = MineColonies.getConfig().getServer().maxDistanceFromWorldSpawn.getAsInt();
        if (spawnDistance < minSpawnDist)
        {
            if (!world.isClientSide())
            {
                MessageUtils.format(CANT_PLACE_COLONY_TOO_CLOSE_TO_SPAWN, minSpawnDist - spawnDistance).sendTo(sender);
            }
            return;
        }
        else if (spawnDistance > maxSpawnDist)
        {
            if (!world.isClientSide())
            {
                MessageUtils.format(CANT_PLACE_COLONY_TOO_FAR_FROM_SPAWN, spawnDistance - maxSpawnDist).sendTo(sender);
            }
            return;
        }

        if (colony != null && !IColonyManager.getInstance().isFarEnoughFromColonies(world, townHall))
        {
            MessageUtils.format(MESSAGE_COLONY_CREATE_DENIED_TOO_CLOSE, colony.getName()).sendTo(sender);
            return;
        }

        final IColony ownedColony = IColonyManager.getInstance().getIColonyByOwner(world, sender);

        if (ownedColony == null)
        {
            if (!QuidCraftKingdomBridge.tryFundKingdomFounding(sender, colonyName))
            {
                // Afford failure: QuidCraft HUD toast only (bridge). No chat / action-bar spam.
                return;
            }

            final IColony createdColony = IColonyManager.getInstance().createColony(world, townHall, sender, colonyName, pack);
            final IBuilding building = createdColony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding) tileEntity, world);

            if (reactivate)
            {
                MessageUtils.format(MESSAGE_COLONY_REACTIVATED, colonyName)
                  .withPriority(MessagePriority.IMPORTANT)
                  .sendTo(sender);
            }
            else
            {
                MessageUtils.format(MESSAGE_COLONY_FOUNDED)
                  .withPriority(MessagePriority.IMPORTANT)
                  .sendTo(sender);
                sender.displayClientMessage(Component.translatable(MESSAGE_COLONY_FOUNDED).withStyle(ChatFormatting.GREEN), true);
                sender.displayClientMessage(Component.literal(
                  "Treasury funded with " + QuidCraftKingdomBridge.formatFoundingCostPretty() + "."), false);
            }

            IMinecoloniesAPI.getInstance().getEventBus().post(new ColonyCreatedModEvent(createdColony));
            new OpenBuildingUIMessage(building).sendToPlayer(sender);
            return;
        }

        ownedColony.getPackageManager().sendColonyViewPackets();
        ownedColony.getPackageManager().sendPermissionsPackets();
        MessageUtils.format(WARNING_COLONY_FOUNDING_FAILED)
          .withPriority(MessagePriority.DANGER)
          .sendTo(sender);
        sender.displayClientMessage(Component.translatable(WARNING_COLONY_FOUNDING_FAILED), true);
    }
}

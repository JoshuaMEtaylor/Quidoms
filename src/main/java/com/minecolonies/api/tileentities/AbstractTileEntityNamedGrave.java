package com.minecolonies.api.tileentities;

import com.minecolonies.api.util.WorldUtil;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_CONTENT;

public class AbstractTileEntityNamedGrave extends BlockEntity
{
    /**
     * The position it faces.
     */
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /**
     * The text displayed on the name plate
     */
    private ArrayList<String> textLines = new ArrayList<>();

    public AbstractTileEntityNamedGrave(BlockEntityType<?> tileEntityTypeIn, final BlockPos pos, final BlockState state)
    {
        super(tileEntityTypeIn, pos, state);
        textLines.add("Unknown Citizen");
    }

    public ArrayList<String> getTextLines()
    {
        return textLines;
    }

    public void setTextLines(final ArrayList<String> content)
    {
        this.textLines = content;
        setChanged();
    }

    @Override
    public void loadAdditional(final ValueInput input)
    {
        super.loadAdditional(input);

        textLines.clear();
        for (final String line : input.listOrEmpty(TAG_CONTENT, Codec.STRING))
        {
            textLines.add(line);
        }
    }

    @Override
    public void saveAdditional(final ValueOutput output)
    {
        super.saveAdditional(output);

        final ValueOutput.TypedOutputList<String> lines = output.list(TAG_CONTENT, Codec.STRING);
        for (@NotNull final String line : textLines)
        {
            lines.add(line);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag(@NotNull final HolderLookup.Provider provider)
    {
        return this.saveWithoutMetadata(provider);
    }

    @Override
    public void onDataPacket(final Connection net, final ValueInput input)
    {
        this.loadAdditional(input);
    }

    @Override
    public void handleUpdateTag(final ValueInput input)
    {
        this.loadAdditional(input);
    }

    @Override
    public void setChanged()
    {
        if (level != null)
        {
            WorldUtil.markChunkDirty(level, worldPosition);
        }
    }
}

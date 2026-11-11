package com.ldtteam.domumornamentum.client.event.handlers;

import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import com.ldtteam.domumornamentum.client.model.MateriallyTexturedBlockStateModel;
import com.ldtteam.domumornamentum.util.Constants;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

/**
 * After baking, wrap Domum materially-textured block-state models so
 * {@link com.ldtteam.domumornamentum.client.model.data.MaterialTextureData}
 * from block entities remaps quads at render time.
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class MateriallyTexturedModelEventHandler
{
    private MateriallyTexturedModelEventHandler()
    {
    }

    @SubscribeEvent
    public static void onModifyBakingResult(final ModelEvent.ModifyBakingResult event)
    {
        final Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        for (final Map.Entry<BlockState, BlockStateModel> entry : models.entrySet())
        {
            final Block block = entry.getKey().getBlock();
            if (!(block instanceof IMateriallyTexturedBlock))
            {
                continue;
            }
            final BlockStateModel current = entry.getValue();
            if (current instanceof MateriallyTexturedBlockStateModel)
            {
                continue;
            }
            entry.setValue(new MateriallyTexturedBlockStateModel(current));
        }
    }
}

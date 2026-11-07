package com.ldtteam.domumornamentum.client.model;

import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.client.model.properties.ModProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wraps a baked Domum block-state model and remaps quads using
 * {@link MaterialTextureData} from NeoForge model data when present.
 */
@OnlyIn(Dist.CLIENT)
public class MateriallyTexturedBlockStateModel extends DelegateBlockStateModel
{
    public MateriallyTexturedBlockStateModel(final BlockStateModel delegate)
    {
        super(delegate);
    }

    @Override
    public void collectParts(final BlockAndTintGetter level, final BlockPos pos, final BlockState state, final RandomSource random, final List<BlockModelPart> parts)
    {
        final ModelData modelData = level.getModelData(pos);
        final MaterialTextureData textureData = modelData.get(ModProperties.MATERIAL_TEXTURE_PROPERTY);

        final List<BlockModelPart> inner = new ArrayList<>();
        this.delegate.collectParts(level, pos, state, random, inner);

        if (textureData == null || textureData.isEmpty())
        {
            parts.addAll(inner);
            return;
        }

        for (final BlockModelPart part : inner)
        {
            parts.add(new RemappedPart(part, textureData, level, pos));
        }
    }

    @Override
    public TextureAtlasSprite particleIcon(final BlockAndTintGetter level, final BlockPos pos, final BlockState state)
    {
        final ModelData modelData = level.getModelData(pos);
        final MaterialTextureData textureData = modelData.get(ModProperties.MATERIAL_TEXTURE_PROPERTY);
        final TextureAtlasSprite base = this.delegate.particleIcon(level, pos, state);
        if (textureData == null || textureData.isEmpty())
        {
            return base;
        }

        final ResourceLocation name = base.contents().name();
        final Block replacement = textureData.getTexturedComponents().get(name);
        if (replacement != null)
        {
            return Minecraft.getInstance().getBlockRenderer().getBlockModel(replacement.defaultBlockState())
              .particleIcon(level, pos, replacement.defaultBlockState());
        }
        for (final Block block : textureData.getTexturedComponents().values())
        {
            return Minecraft.getInstance().getBlockRenderer().getBlockModel(block.defaultBlockState())
              .particleIcon(level, pos, block.defaultBlockState());
        }
        return base;
    }

    private static final class RemappedPart implements BlockModelPart
    {
        private final BlockModelPart inner;
        private final MaterialTextureData textureData;
        private final BlockAndTintGetter level;
        private final BlockPos pos;

        private RemappedPart(final BlockModelPart inner, final MaterialTextureData textureData, final BlockAndTintGetter level, final BlockPos pos)
        {
            this.inner = inner;
            this.textureData = textureData;
            this.level = level;
            this.pos = pos;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable final Direction direction)
        {
            final List<BakedQuad> source = this.inner.getQuads(direction);
            if (source.isEmpty())
            {
                return source;
            }
            final List<BakedQuad> out = new ArrayList<>(source.size());
            for (final BakedQuad quad : source)
            {
                out.add(remapQuad(quad));
            }
            return out;
        }

        private BakedQuad remapQuad(final BakedQuad quad)
        {
            final ResourceLocation spriteName = quad.sprite().contents().name();
            Block replacement = this.textureData.getTexturedComponents().get(spriteName);
            if (replacement == null)
            {
                for (final Map.Entry<ResourceLocation, Block> entry : this.textureData.getTexturedComponents().entrySet())
                {
                    final String keyPath = entry.getKey().getPath();
                    final String spritePath = spriteName.getPath();
                    if (spritePath.endsWith(keyPath) || keyPath.endsWith(spritePath) || spritePath.contains(keyPath) || keyPath.contains(spritePath))
                    {
                        replacement = entry.getValue();
                        break;
                    }
                }
            }
            if (replacement == null)
            {
                return quad;
            }

            final TextureAtlasSprite targetSprite = Minecraft.getInstance().getBlockRenderer()
              .getBlockModel(replacement.defaultBlockState())
              .particleIcon(this.level, this.pos, replacement.defaultBlockState());

            final TextureAtlasSprite from = quad.sprite();
            final float minU = from.getU0();
            final float uDelta = from.getU1() - minU;
            final float minV = from.getV0();
            final float vDelta = from.getV1() - minV;

            final int[] verts = quad.vertices().clone();
            final int stride = verts.length / 4;
            for (int i = 0; i < 4; i++)
            {
                final int base = i * stride;
                final float u = Float.intBitsToFloat(verts[base + 4]);
                final float v = Float.intBitsToFloat(verts[base + 5]);
                final float nu = uDelta == 0f ? 0f : (u - minU) / uDelta;
                final float nv = vDelta == 0f ? 0f : (v - minV) / vDelta;
                verts[base + 4] = Float.floatToRawIntBits(targetSprite.getU(nu));
                verts[base + 5] = Float.floatToRawIntBits(targetSprite.getV(nv));
            }

            return new BakedQuad(verts, quad.tintIndex(), quad.direction(), targetSprite, quad.shade(), quad.lightEmission());
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean useAmbientOcclusion()
        {
            return this.inner.useAmbientOcclusion();
        }

        @Override
        public TextureAtlasSprite particleIcon()
        {
            return this.inner.particleIcon();
        }
    }
}

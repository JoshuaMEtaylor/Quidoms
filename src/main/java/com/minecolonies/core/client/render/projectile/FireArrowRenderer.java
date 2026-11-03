package com.minecolonies.core.client.render.projectile;

import com.minecolonies.api.util.constant.Constants;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class FireArrowRenderer extends ArrowRenderer<AbstractArrow, ArrowRenderState>
{
    private static final ResourceLocation[] RES = new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow1.png"),
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow2.png"),
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow3.png"),
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow4.png"),
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow5.png"),
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow6.png"),
    };

    private int frame;

    public FireArrowRenderer(final EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public void extractRenderState(final AbstractArrow entity, final ArrowRenderState state, final float partialTick)
    {
        super.extractRenderState(entity, state, partialTick);
        this.frame = entity.tickCount % 6;
    }

    @NotNull
    @Override
    protected ResourceLocation getTextureLocation(@NotNull final ArrowRenderState state)
    {
        return RES[Math.floorMod(this.frame, RES.length)];
    }

    @Override
    public ArrowRenderState createRenderState()
    {
        return new ArrowRenderState();
    }
}

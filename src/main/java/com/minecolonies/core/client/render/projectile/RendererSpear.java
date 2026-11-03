package com.minecolonies.core.client.render.projectile;

import com.minecolonies.core.client.render.QuidomsNoopEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RendererSpear extends QuidomsNoopEntityRenderer<ThrownTrident>
{
    public RendererSpear(final EntityRendererProvider.Context context)
    {
        super(context);
    }
}

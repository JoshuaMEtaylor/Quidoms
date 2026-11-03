package com.minecolonies.core.client.particles;

import com.minecolonies.api.util.constant.Constants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;

/**
 * Custom particle for sleeping (MC 1.21.10 SingleQuadParticle).
 */
public class SleepingParticle extends SingleQuadParticle
{
    private final double coordX;
    private final double coordY;
    private final double coordZ;

    private static final int LIGHT_LEVEL = 15 << 20 | 15 << 4;

    public static final ResourceLocation SLEEPING_TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "particle/sleeping");

    public SleepingParticle(SpriteSet spriteSet, ClientLevel worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn)
    {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, spriteSet.get(0, 1));
        setSpriteFromAge(spriteSet);
        this.xd = xSpeedIn * 0.5;
        this.yd = ySpeedIn;
        this.zd = zSpeedIn;
        this.coordX = xCoordIn;
        this.coordY = yCoordIn;
        this.coordZ = zCoordIn;
        this.xo = xCoordIn;
        this.yo = yCoordIn;
        this.zo = zCoordIn;
        this.x = this.xo;
        this.y = this.yo;
        this.z = this.zo;
        float f = this.random.nextFloat() * 0.6F + 0.4F;
        this.rCol = 0.9F * f;
        this.gCol = 0.9F * f;
        this.bCol = f;
        this.lifetime = (int) (Math.random() * 30.0D) + 40;
        this.scale((float) ((0.8 * Math.sin(0) + 1.3) * 0.1));
    }

    @Override
    public void tick()
    {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        float f = (float) this.age / (float) this.lifetime;
        this.scale((float) ((0.8 * Math.sin(f * 4) + 1.3) * 0.1));

        this.x = this.coordX + this.xd * f;
        this.y = this.coordY + this.yd * f;
        this.z = this.coordZ + this.zd * f;

        if (this.age++ >= this.lifetime)
        {
            this.remove();
        }
    }

    @Override
    public int getLightColor(float partialTick)
    {
        return LIGHT_LEVEL;
    }

    @Override
    public ParticleRenderType getGroup()
    {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType>
    {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet)
        {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType particleType, ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
        {
            return new SleepingParticle(spriteSet, world, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}

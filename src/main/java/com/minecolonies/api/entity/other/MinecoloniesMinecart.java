package com.minecolonies.api.entity.other;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Citizen transport cart. Custom 1.20-era rail physics removed for NeoForge 1.21.10
 * (AbstractMinecart now uses a behavior controller); keep collision/interaction locks
 * and auto-despawn when empty.
 */
public class MinecoloniesMinecart extends AbstractMinecart
{
    private static final Vec3 LOWERED_PASSENGER_ATTACHMENT = new Vec3(0.0D, 0.0D, 0.0D);

    public MinecoloniesMinecart(final EntityType<?> type, final Level world)
    {
        super(type, world);
    }

    @Override
    protected Item getDropItem()
    {
        return Items.MINECART;
    }

    @Override
    public ItemStack getPickResult()
    {
        return new ItemStack(Items.MINECART);
    }

    @Override
    protected void destroy(final ServerLevel level, final DamageSource source)
    {
        this.kill(level);
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand)
    {
        return InteractionResult.FAIL;
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    public void push(@NotNull final Entity entityIn)
    {
        // Citizens own the cart; do not shove.
    }

    @Override
    public void playerTouch(final Player entityIn)
    {
        // Noop
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean canCollideWith(final Entity entity)
    {
        return false;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 20 == 19 && this.getPassengers().isEmpty())
        {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    @NotNull
    protected Vec3 getPassengerAttachmentPoint(@NotNull final Entity passenger, @NotNull final EntityDimensions dimensions, final float partialTick)
    {
        return LOWERED_PASSENGER_ATTACHMENT;
    }
}

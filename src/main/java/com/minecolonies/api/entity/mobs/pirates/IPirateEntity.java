package com.minecolonies.api.entity.mobs.pirates;

import net.neoforged.neoforge.capabilities.Capabilities;

import com.minecolonies.api.util.IItemHandlerCapProvider;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public interface IPirateEntity extends Enemy, CommandSource, IItemHandlerCapProvider
{
    @Override
    @Nullable
    default IItemHandler getItemHandlerCap(final Direction direction)
    {
        // LivingEntities have cap registered by forge
        final var handler = Capabilities.Item.ENTITY.getCapability((LivingEntity) this, null);
        return handler == null ? null : IItemHandler.of(handler);
    }
}

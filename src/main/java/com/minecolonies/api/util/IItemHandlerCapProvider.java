package com.minecolonies.api.util;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

/**
 * Our class for to join {@link IItemHandler} providers, so we can have type independent code.
 */
@FunctionalInterface
public interface IItemHandlerCapProvider
{
    /**
     * For EntityCap register only
     */
    @Nullable
    default IItemHandler getItemHandlerCap(final Void nothing)
    {
        return getItemHandlerCap();
    }

    /**
     * @return direction-unaware itemHandler
     */
    @Nullable
    default IItemHandler getItemHandlerCap()
    {
        return getItemHandlerCap((Direction) null);
    }

    /**
     * @return direction-aware itemHandler
     */
    @Nullable
    IItemHandler getItemHandlerCap(final Direction direction);

    /**
     * ResourceHandler view for NeoForge {@code Capabilities.Item} registration.
     */
    @Nullable
    default ResourceHandler<ItemResource> getItemResourceHandler(@Nullable final Direction direction)
    {
        return LegacyItemHandlerAdapter.of(getItemHandlerCap(direction));
    }

    @Nullable
    default ResourceHandler<ItemResource> getItemResourceHandler(final Void nothing)
    {
        return LegacyItemHandlerAdapter.of(getItemHandlerCap(nothing));
    }

    public static IItemHandlerCapProvider wrap(final BlockEntity blockEntity)
    {
        return direction -> {
            final ResourceHandler<ItemResource> handler = Capabilities.Item.BLOCK.getCapability(
                blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
            return handler == null ? null : IItemHandler.of(handler);
        };
    }

    /**
     * @param sided if true then will use Direction aware capability, roughly should be true for machine-entities and false for mobs
     */
    public static IItemHandlerCapProvider wrap(final Entity entity, final boolean sided)
    {
        return sided
            ? direction -> {
                final ResourceHandler<ItemResource> handler = Capabilities.Item.ENTITY_AUTOMATION.getCapability(entity, direction);
                return handler == null ? null : IItemHandler.of(handler);
            }
            : direction -> {
                final ResourceHandler<ItemResource> handler = Capabilities.Item.ENTITY.getCapability(entity, null);
                return handler == null ? null : IItemHandler.of(handler);
            };
    }

    public static IItemHandlerCapProvider wrap(final ItemStack itemStack)
    {
        return direction -> {
            final ResourceHandler<ItemResource> handler = Capabilities.Item.ITEM.getCapability(itemStack, ItemAccess.forStack(itemStack));
            return handler == null ? null : IItemHandler.of(handler);
        };
    }
}

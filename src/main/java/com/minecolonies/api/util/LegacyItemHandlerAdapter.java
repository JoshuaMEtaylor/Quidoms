package com.minecolonies.api.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Bridges legacy {@link IItemHandler} inventories to NeoForge 1.21.9+ {@link ResourceHandler}
 * so existing MineColonies handlers can still be registered on {@code Capabilities.Item}.
 */
public final class LegacyItemHandlerAdapter extends SnapshotJournal<NonNullList<ItemStack>> implements ResourceHandler<ItemResource>
{
    private final IItemHandler handler;

    private LegacyItemHandlerAdapter(final IItemHandler handler)
    {
        this.handler = handler;
    }

    @Nullable
    public static ResourceHandler<ItemResource> of(@Nullable final IItemHandler handler)
    {
        return handler == null ? null : new LegacyItemHandlerAdapter(handler);
    }

    @Override
    public int size()
    {
        return handler.getSlots();
    }

    @Override
    public ItemResource getResource(final int index)
    {
        return ItemResource.of(handler.getStackInSlot(index));
    }

    @Override
    public long getAmountAsLong(final int index)
    {
        return handler.getStackInSlot(index).getCount();
    }

    @Override
    public long getCapacityAsLong(final int index, final ItemResource resource)
    {
        return handler.getSlotLimit(index);
    }

    @Override
    public boolean isValid(final int index, final ItemResource resource)
    {
        return resource.isEmpty() || handler.isItemValid(index, resource.toStack());
    }

    @Override
    public int insert(final int index, final ItemResource resource, final int amount, final TransactionContext transaction)
    {
        if (amount <= 0 || resource.isEmpty())
        {
            return 0;
        }
        final ItemStack simulatedRemainder = handler.insertItem(index, resource.toStack(amount), true);
        final int inserted = amount - simulatedRemainder.getCount();
        if (inserted <= 0)
        {
            return 0;
        }
        updateSnapshots(transaction);
        handler.insertItem(index, resource.toStack(inserted), false);
        return inserted;
    }

    @Override
    public int extract(final int index, final ItemResource resource, final int amount, final TransactionContext transaction)
    {
        if (amount <= 0 || resource.isEmpty())
        {
            return 0;
        }
        final ItemStack current = handler.getStackInSlot(index);
        if (current.isEmpty() || !ItemResource.of(current).equals(resource))
        {
            return 0;
        }
        final ItemStack simulated = handler.extractItem(index, amount, true);
        if (simulated.isEmpty())
        {
            return 0;
        }
        updateSnapshots(transaction);
        return handler.extractItem(index, simulated.getCount(), false).getCount();
    }

    @Override
    protected NonNullList<ItemStack> createSnapshot()
    {
        final int slots = handler.getSlots();
        final NonNullList<ItemStack> copy = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int i = 0; i < slots; i++)
        {
            copy.set(i, handler.getStackInSlot(i).copy());
        }
        return copy;
    }

    @Override
    protected void revertToSnapshot(final NonNullList<ItemStack> snapshot)
    {
        if (handler instanceof IItemHandlerModifiable modifiable)
        {
            for (int i = 0; i < snapshot.size(); i++)
            {
                modifiable.setStackInSlot(i, snapshot.get(i).copy());
            }
            return;
        }

        // Best-effort for read-only handlers: drain then re-insert.
        for (int i = 0; i < handler.getSlots(); i++)
        {
            final ItemStack cur = handler.getStackInSlot(i);
            if (!cur.isEmpty())
            {
                handler.extractItem(i, cur.getCount(), false);
            }
            final ItemStack want = snapshot.get(i);
            if (!want.isEmpty())
            {
                handler.insertItem(i, want.copy(), false);
            }
        }
    }
}

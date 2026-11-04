package com.minecolonies.api.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

/**
 * Compat shim: NeoForge removed {@code com.minecolonies.api.compat.INBTSerializable} after 1.21.1.
 * Must live under a Quidoms package — shipping it under {@code net.neoforged.*} breaks JPMS module exports.
 */
public interface INBTSerializable<T extends Tag> {
    T serializeNBT(HolderLookup.Provider provider);

    void deserializeNBT(HolderLookup.Provider provider, T nbt);
}

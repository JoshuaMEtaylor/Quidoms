package net.neoforged.neoforge.common.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

/** Compat shim: NeoForge removed INBTSerializable after 1.21.1. */
public interface INBTSerializable<T extends Tag> {
    T serializeNBT(HolderLookup.Provider provider);
    void deserializeNBT(HolderLookup.Provider provider, T nbt);
}

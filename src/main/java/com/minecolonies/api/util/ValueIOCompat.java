package com.minecolonies.api.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Bridges CompoundTag ↔ ValueInput/ValueOutput for NeoForge 1.21.10.
 */
public final class ValueIOCompat
{
    private ValueIOCompat() {}

    public static ValueInput input(final CompoundTag tag, final HolderLookup.Provider lookup)
    {
        return TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag);
    }

    public static ValueInput input(final CompoundTag tag)
    {
        return TagValueInput.create(ProblemReporter.DISCARDING, HolderLookup.Provider.create(java.util.stream.Stream.empty()), tag);
    }

    public static TagValueOutput output(final HolderLookup.Provider lookup)
    {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup);
    }

    public static TagValueOutput output()
    {
        return TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
    }

    public static CompoundTag toTag(final TagValueOutput output)
    {
        return output.buildResult();
    }
}

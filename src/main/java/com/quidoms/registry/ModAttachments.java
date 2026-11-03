package com.quidoms.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.quidoms.QuidomsMod;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, QuidomsMod.MOD_ID);

    /** Owning colony id on a chunk; 0 means unclaimed. */
    public static final Supplier<AttachmentType<Integer>> CHUNK_COLONY_ID = ATTACHMENT_TYPES.register(
            "colony_id",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("colony_id")).build());

    private ModAttachments() {
    }
}

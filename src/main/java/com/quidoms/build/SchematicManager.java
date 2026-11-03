package com.quidoms.build;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.quidoms.QuidomsMod;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads {@code data/<ns>/quidoms/schematics/*.json} on server reload.
 */
public final class SchematicManager extends SimpleJsonResourceReloadListener<Schematic> {
    public static final SchematicManager INSTANCE = new SchematicManager();

    private Map<ResourceLocation, Schematic> schematics = Map.of();

    private SchematicManager() {
        super(Schematic.CODEC, FileToIdConverter.json("quidoms/schematics"));
    }

    @Override
    protected void apply(Map<ResourceLocation, Schematic> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, Schematic> next = new HashMap<>();
        for (Map.Entry<ResourceLocation, Schematic> entry : prepared.entrySet()) {
            Schematic schematic = entry.getValue();
            // Prefer embedded id when present so start_build can use schematic.id from JSON.
            ResourceLocation key = schematic.id() != null ? schematic.id() : entry.getKey();
            next.put(key, schematic);
            if (!key.equals(entry.getKey())) {
                next.putIfAbsent(entry.getKey(), schematic);
            }
        }
        this.schematics = Collections.unmodifiableMap(next);
        QuidomsMod.LOGGER.info("Loaded {} Quidoms schematics", this.schematics.size());
    }

    public Optional<Schematic> get(ResourceLocation id) {
        return Optional.ofNullable(schematics.get(id));
    }

    public Map<ResourceLocation, Schematic> all() {
        return schematics;
    }
}

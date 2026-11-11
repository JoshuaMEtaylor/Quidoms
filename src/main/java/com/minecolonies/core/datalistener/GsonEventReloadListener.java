package com.minecolonies.core.datalistener;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.minecolonies.api.util.Log;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Drop-in replacement for pre-1.21.10 {@code SimpleJsonResourceReloadListener(Gson, String)}
 * which now requires a Codec. Loads JSON under a directory into a map for {@link #apply}.
 */
public abstract class GsonEventReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>>
{
    private final Gson gson;
    private final String directory;

    protected GsonEventReloadListener(final Gson gson, final String directory)
    {
        this.gson = gson;
        this.directory = directory;
    }

    @NotNull
    @Override
    protected Map<ResourceLocation, JsonElement> prepare(@NotNull final ResourceManager resourceManager, @NotNull final ProfilerFiller profiler)
    {
        final Map<ResourceLocation, JsonElement> map = new HashMap<>();
        final var resources = resourceManager.listResources(directory, rl -> rl.getPath().endsWith(".json"));
        for (final var entry : resources.entrySet())
        {
            final ResourceLocation fileId = entry.getKey();
            final String path = fileId.getPath();
            final String relative = path.startsWith(directory + "/")
                ? path.substring(directory.length() + 1, path.length() - ".json".length())
                : path.replace(".json", "");
            final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), relative);
            try (final BufferedReader reader = entry.getValue().openAsReader())
            {
                map.put(id, JsonParser.parseReader(reader));
            }
            catch (final Exception e)
            {
                Log.getLogger().error("Failed to load json {}: {}", fileId, e.getMessage());
            }
        }
        return map;
    }

    @Override
    protected abstract void apply(
        @NotNull Map<ResourceLocation, JsonElement> object,
        @NotNull ResourceManager resourceManager,
        @NotNull ProfilerFiller profiler);
}

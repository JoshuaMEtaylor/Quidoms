package com.minecolonies.core.datalistener;

import com.google.gson.*;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import com.minecolonies.core.datalistener.GsonEventReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Loads and listens to get custom nbt matching rules.
 */
public class ItemNbtListener extends GsonEventReloadListener
{
    /**
     * Gson instance
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * Create a new listener.
     */
    public ItemNbtListener()
    {
        super(GSON, "compatibility");
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> jsonElementMap, final @NotNull ResourceManager resourceManager, final @NotNull ProfilerFiller profiler)
    {
        ItemStackUtils.CHECKED_NBT_KEYS.clear();
        for (final Map.Entry<ResourceLocation, JsonElement> entry : jsonElementMap.entrySet())
        {
            tryParse(this.getRegistryLookup(), entry);
        }
    }

    /**
     * Tries to parse the entry
     *
     * @param entry
     */
    private void tryParse(@NotNull final HolderLookup.Provider provider, final Map.Entry<ResourceLocation, JsonElement> entry)
    {
        for (final JsonElement element : entry.getValue().getAsJsonArray())
        {
            try
            {
                final JsonObject jsonObj = element.getAsJsonObject();
                final ResourceLocation itemLoc = ResourceLocation.parse(jsonObj.get("item").getAsString());
                if (jsonObj.has("checkednbtkeys"))
                {
                    final HashSet<DataComponentType<?>> set = new HashSet<>();
                    final JsonArray jsonArray = jsonObj.getAsJsonArray("checkednbtkeys");
                    for (final JsonElement subElement : jsonArray)
                    {
                        final ResourceLocation keyId = migrateComponentId(ResourceLocation.parse(subElement.getAsString()));
                        final DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(keyId);
                        // Unknown/legacy component ids resolve to null — never store nulls (breaks login sync).
                        if (type != null)
                        {
                            set.add(type);
                        }
                        else
                        {
                            Log.getLogger().debug("Skipping unknown data component '{}' for item {}", keyId, itemLoc);
                        }
                    }

                    ItemStackUtils.CHECKED_NBT_KEYS.put(BuiltInRegistries.ITEM.getValue(itemLoc), set);
                }
                else
                {
                    ItemStackUtils.CHECKED_NBT_KEYS.put(BuiltInRegistries.ITEM.getValue(itemLoc), new HashSet<>());
                }
            }
            catch (Exception e)
            {
                Log.getLogger().warn("Could not nbt comparator for:" + entry.getKey(), e);
            }
        }
        Log.getLogger().warn("Read " + ItemStackUtils.CHECKED_NBT_KEYS.size() + " items with their nbt keys for compatibility.");
    }

    /**
     * Map removed/renamed 1.20–1.21 component ids onto 1.21.10 equivalents.
     */
    @NotNull
    private static ResourceLocation migrateComponentId(@NotNull final ResourceLocation id)
    {
        if ("minecraft".equals(id.getNamespace()))
        {
            return switch (id.getPath())
            {
                case "fire_resistant" -> ResourceLocation.withDefaultNamespace("damage_resistant");
                case "hide_additional_tooltip", "hide_tooltip" -> ResourceLocation.withDefaultNamespace("tooltip_display");
                default -> id;
            };
        }
        return id;
    }
}

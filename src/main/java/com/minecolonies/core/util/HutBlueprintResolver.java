package com.minecolonies.core.util;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.compatibility.newstruct.BlueprintMapping;
import com.minecolonies.api.util.Log;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Resolve a real StructurePacks pack+path for a hut (builder, townhall, …).
 * Orphan/empty/"Decorations" style names must never leave Build/WO/tape with a null blueprint.
 */
public final class HutBlueprintResolver
{
    public static final List<String> PREFERRED_PACKS = List.of(
      "Medieval Oak",
      "Minecolonies Original",
      "Colonial",
      "Nordic Spruce",
      "Desert Oasis",
      "Caledonia",
      "Fortress");

    private HutBlueprintResolver()
    {
    }

    public record Resolved(@NotNull String pack, @NotNull String path)
    {
    }

    /**
     * Resolve pack+path for a building that actually exists on disk in StructurePacks.
     */
    @Nullable
    public static Resolved resolve(@NotNull final IBuilding building, final int schematicLevel)
    {
        final String hut = hutId(building);
        final String preferred = building.getStructurePack();
        final String existingPath = building.getBlueprintPath();
        return resolve(hut, preferred, existingPath, Math.max(1, schematicLevel));
    }

    /**
     * Resolve pack+path for a hut id (e.g. {@code builder}, {@code townhall}).
     */
    @Nullable
    public static Resolved resolve(
      @Nullable final String hutId,
      @Nullable final String preferredPack,
      @Nullable final String existingPath,
      final int schematicLevel)
    {
        final String hut = (hutId == null || hutId.isBlank()) ? "builder" : hutId.trim();
        final int level = Math.max(1, schematicLevel);
        final List<String> pathCandidates = pathCandidates(hut, existingPath, level);

        // 1) Preferred / stored pack if it actually has one of the candidates.
        for (final String tryPack : preferredPackAliases(preferredPack))
        {
            final StructurePackMeta preferredMeta = StructurePacks.getStructurePack(tryPack);
            if (preferredMeta == null)
            {
                continue;
            }
            final List<String> tries = new ArrayList<>(pathCandidates);
            if (tryPack.equalsIgnoreCase(preferredPack) && preferredPack != null
                  && preferredPack.toLowerCase().contains("alternative"))
            {
                // Medieval Oak Alternative → prefer altbuilder / alttownhall paths first.
                tries.addAll(0, altPathCandidates(hut, level));
            }
            else if (!tryPack.equals(preferredPack) && preferredPack != null
                       && preferredPack.toLowerCase().contains("alternative"))
            {
                tries.addAll(0, altPathCandidates(hut, level));
            }
            final String hit = firstExistingPath(preferredMeta, tries);
            if (hit != null)
            {
                return new Resolved(preferredMeta.getName(), hit);
            }
        }

        // 2) Preferred style list (Medieval Oak, Original, …).
        for (final String name : PREFERRED_PACKS)
        {
            final StructurePackMeta pack = StructurePacks.getStructurePack(name);
            if (pack == null)
            {
                continue;
            }
            final String hit = firstExistingPath(pack, pathCandidates);
            if (hit != null)
            {
                return new Resolved(pack.getName(), hit);
            }
        }

        // 3) Any registered pack that has the hut schematic.
        for (final StructurePackMeta pack : StructurePacks.getPackMetas())
        {
            if (pack == null || isDecorationOnlyName(pack.getName()))
            {
                continue;
            }
            final String hit = firstExistingPath(pack, pathCandidates);
            if (hit != null)
            {
                return new Resolved(pack.getName(), hit);
            }
        }

        Log.getLogger().warn("HutBlueprintResolver: no pack has hut '{}' candidates {}", hut, pathCandidates);
        return null;
    }

    /**
     * Apply a resolved pack+path onto the building (server).
     */
    public static void apply(@NotNull final IBuilding building, @Nullable final Resolved resolved)
    {
        if (resolved == null)
        {
            return;
        }
        if (resolved.pack() != null && !resolved.pack().isBlank())
        {
            building.setStructurePack(resolved.pack());
        }
        if (resolved.path() != null && !resolved.path().isBlank())
        {
            building.setBlueprintPath(resolved.path());
        }
    }

    /**
     * Ensure building has a loadable pack+path; returns the resolution used.
     */
    @Nullable
    public static Resolved ensure(@NotNull final IBuilding building, final int schematicLevel)
    {
        final Resolved resolved = resolve(building, schematicLevel);
        apply(building, resolved);
        return resolved;
    }

    /**
     * Load a blueprint, searching packs if the stored pack/path is bad.
     */
    @Nullable
    public static Blueprint loadBlueprint(
      @Nullable final String pack,
      @Nullable final String path,
      @Nullable final String hutId,
      final int level,
      @NotNull final HolderLookup.Provider provider)
    {
        if (path != null && !path.isBlank() && pack != null && !pack.isBlank())
        {
            final Blueprint direct = StructurePacks.getBlueprint(pack, path, true, provider);
            if (direct != null)
            {
                return direct;
            }
        }
        final Resolved resolved = resolve(hutId, pack, path, level);
        if (resolved == null)
        {
            return null;
        }
        return StructurePacks.getBlueprint(resolved.pack(), resolved.path(), true, provider);
    }

    @NotNull
    public static List<String> pathCandidates(@NotNull final String hut, @Nullable final String existingPath, final int level)
    {
        final List<String> candidates = new ArrayList<>();
        if (existingPath != null && !existingPath.isBlank())
        {
            candidates.add(existingPath.trim());
            addLeveled(candidates, existingPath.trim(), level);
        }
        String mapped = BlueprintMapping.getPathMapping("", hut);
        if (mapped == null || mapped.isBlank())
        {
            mapped = "fundamentals/" + hut;
        }
        candidates.add(mapped + level + ".blueprint");
        candidates.add(mapped + "1.blueprint");
        // alt variants (Medieval Oak castle/altbuilder)
        candidates.add(mapped.replace("/" + hut, "/alt" + hut) + level + ".blueprint");
        candidates.add(mapped.replace("/" + hut, "/alt" + hut) + "1.blueprint");
        if (mapped.contains("/"))
        {
            final String folder = mapped.substring(0, mapped.lastIndexOf('/') + 1);
            candidates.add(folder + hut + level + ".blueprint");
            candidates.add(folder + hut + "1.blueprint");
        }
        return candidates.stream().filter(p -> p != null && !p.isBlank()).distinct().toList();
    }

    private static void addLeveled(final List<String> candidates, final String structurePath, final int level)
    {
        String name = structurePath.replace(".blueprint", "");
        if (name.isEmpty())
        {
            return;
        }
        final char last = name.charAt(name.length() - 1);
        final String base = Character.isDigit(last) ? name.substring(0, name.length() - 1) : name;
        candidates.add(base + level + ".blueprint");
        candidates.add(base + "1.blueprint");
    }

    @Nullable
    private static String firstExistingPath(@NotNull final StructurePackMeta pack, @NotNull final Collection<String> paths)
    {
        for (final String path : paths)
        {
            if (pathExists(pack, path))
            {
                return path;
            }
        }
        return null;
    }

    private static boolean pathExists(@NotNull final StructurePackMeta pack, @Nullable final String structurePath)
    {
        if (structurePath == null || structurePath.isBlank())
        {
            return false;
        }
        try
        {
            return Files.exists(pack.getPath().resolve(pack.getNormalizedSubPath(structurePath)));
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    @NotNull
    private static String hutId(@NotNull final IBuilding building)
    {
        try
        {
            if (building.getBuildingType() != null && building.getBuildingType().getRegistryName() != null)
            {
                final String path = building.getBuildingType().getRegistryName().getPath();
                if (path != null && !path.isBlank())
                {
                    return path;
                }
            }
        }
        catch (final Exception ignored)
        {
        }
        return "builder";
    }

    private static boolean isDecorationOnlyName(@Nullable final String name)
    {
        if (name == null)
        {
            return true;
        }
        final String n = name.trim();
        return n.equalsIgnoreCase("Decorations")
                 || n.equalsIgnoreCase("Decoration")
                 || n.equalsIgnoreCase("decos");
    }

    @NotNull
    private static List<String> preferredPackAliases(@Nullable final String preferredPack)
    {
        final List<String> names = new ArrayList<>();
        if (preferredPack != null && !preferredPack.isBlank())
        {
            names.add(preferredPack.trim());
            final String lower = preferredPack.toLowerCase();
            if (lower.endsWith(" alternative") || lower.endsWith(" alt"))
            {
                final String base = preferredPack.replaceAll("(?i)\\s+(alternative|alt)\\s*$", "").trim();
                if (!base.isBlank())
                {
                    names.add(base);
                }
            }
        }
        return names;
    }

    @NotNull
    private static List<String> altPathCandidates(@NotNull final String hut, final int level)
    {
        final List<String> alts = new ArrayList<>();
        alts.add("fundamentals/alt" + hut + level + ".blueprint");
        alts.add("fundamentals/alt" + hut + "1.blueprint");
        return alts;
    }
}

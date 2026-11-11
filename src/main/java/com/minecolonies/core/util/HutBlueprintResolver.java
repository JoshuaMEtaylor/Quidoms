package com.minecolonies.core.util;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.ServerFutureProcessor;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.compatibility.newstruct.BlueprintMapping;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.api.util.ColonyUtils;
import com.minecolonies.api.util.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolve a real StructurePacks pack+path for a hut (builder, townhall, …).
 * Orphan/empty/"Decorations" style names must never leave Build/WO/tape with a null blueprint.
 * <p>
 * Blueprint <em>bytes</em> are never read on the server tick — only lightweight {@code Files.exists}
 * probes for path resolution, and async {@link StructurePacks#getBlueprintFuture} for corner sync.
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

    /** Cap how many arbitrary packs we probe after preferred styles. */
    private static final int MAX_FALLBACK_PACK_PROBES = 8;

    private static final Set<Long> PENDING_CORNER_SYNC = ConcurrentHashMap.newKeySet();

    private HutBlueprintResolver()
    {
    }

    public record Resolved(@NotNull String pack, @NotNull String path)
    {
    }

    /**
     * Resolve pack+path for a building that actually exists on disk in StructurePacks.
     * Lightweight: no blueprint NBT load, no {@code waitUntilFinishedLoading}.
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
            if (preferredPack != null && preferredPack.toLowerCase().contains("alternative"))
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

        // 3) Capped scan of other packs (never unbounded on a tick).
        int probed = 0;
        for (final StructurePackMeta pack : StructurePacks.getPackMetas())
        {
            if (pack == null || isDecorationOnlyName(pack.getName()))
            {
                continue;
            }
            if (PREFERRED_PACKS.contains(pack.getName()))
            {
                continue;
            }
            if (probed++ >= MAX_FALLBACK_PACK_PROBES)
            {
                break;
            }
            final String hit = firstExistingPath(pack, pathCandidates);
            if (hit != null)
            {
                return new Resolved(pack.getName(), hit);
            }
        }

        // Last resort: derive fundamentals/<hut>N without existence probe so WO still has a path.
        String mapped = BlueprintMapping.getPathMapping("", hut);
        if (mapped == null || mapped.isBlank())
        {
            mapped = "fundamentals/" + hut;
        }
        final String derived = mapped + level + ".blueprint";
        final String packGuess = preferredPack != null && !preferredPack.isBlank()
                                   ? StructurePacks.aliasPackName(preferredPack)
                                   : (PREFERRED_PACKS.isEmpty() ? "" : PREFERRED_PACKS.get(0));
        if (!packGuess.isBlank() && StructurePacks.getStructurePack(packGuess) != null)
        {
            return new Resolved(StructurePacks.getStructurePack(packGuess).getName(), derived);
        }
        Log.getLogger().warn("HutBlueprintResolver: no pack has hut '{}' candidates {}", hut, pathCandidates);
        return null;
    }

    /**
     * Apply a resolved pack+path onto the building (server). No I/O.
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
     * Ensure building has a pack+path string (existence probes only — never loads NBT).
     */
    @Nullable
    public static Resolved ensure(@NotNull final IBuilding building, final int schematicLevel)
    {
        final Resolved resolved = resolve(building, schematicLevel);
        apply(building, resolved);
        return resolved;
    }

    /**
     * Apply corners from an already-loaded blueprint (no disk I/O). Safe on server tick.
     */
    @Nullable
    public static Tuple<BlockPos, BlockPos> applyCorners(
      @NotNull final IBuilding building,
      @Nullable final Blueprint blueprint,
      @Nullable final Level world,
      @Nullable final RotationMirror rotMir)
    {
        if (blueprint == null || world == null)
        {
            return null;
        }
        final RotationMirror rotation = rotMir != null
                                          ? rotMir
                                          : (building.getRotationMirror() != null
                                               ? building.getRotationMirror()
                                               : RotationMirror.NONE);
        final Tuple<BlockPos, BlockPos> corners =
          ColonyUtils.calculateCorners(building.getPosition(), world, blueprint, rotation);
        building.setCorners(corners.getA(), corners.getB());

        try
        {
            final AbstractTileEntityColonyBuilding te = building.getTileEntity();
            if (te != null)
            {
                te.setSchematicCorners(corners.getA().subtract(building.getPosition()),
                  corners.getB().subtract(building.getPosition()));
                final String pack = building.getStructurePack();
                final String path = building.getBlueprintPath();
                if (pack != null && !pack.isBlank())
                {
                    final StructurePackMeta meta = StructurePacks.getStructurePack(pack);
                    if (meta != null)
                    {
                        te.setStructurePack(meta);
                    }
                }
                if (path != null && !path.isBlank())
                {
                    te.setBlueprintPath(path);
                }
            }
        }
        catch (final Exception e)
        {
            Log.getLogger().debug("HutBlueprintResolver.applyCorners: TE update skipped", e);
        }
        return corners;
    }

    /**
     * Queue async blueprint load + corner sync. Never blocks the server tick.
     * Debounced per building position.
     */
    public static void queueCornerSync(
      @NotNull final IBuilding building,
      @Nullable final Level world,
      @Nullable final String pack,
      @Nullable final String path,
      @Nullable final RotationMirror rotMir)
    {
        if (world == null || world.isClientSide() || building.getColony() == null)
        {
            return;
        }
        final BlockPos id = building.getID();
        final long key = id.asLong();
        if (!PENDING_CORNER_SYNC.add(key))
        {
            return;
        }

        final String usePack = pack == null || pack.isBlank() ? building.getStructurePack() : pack;
        final String usePath = path == null || path.isBlank() ? building.getBlueprintPath() : path;
        if (usePack == null || usePack.isBlank() || usePath == null || usePath.isBlank())
        {
            PENDING_CORNER_SYNC.remove(key);
            return;
        }

        ServerFutureProcessor.queueBlueprint(new ServerFutureProcessor.BlueprintProcessingData(
          StructurePacks.getBlueprintFuture(usePack, usePath, true, world.registryAccess()),
          world,
          blueprint -> {
              try
              {
                  if (blueprint != null)
                  {
                      applyCorners(building, blueprint, world, rotMir);
                  }
              }
              finally
              {
                  PENDING_CORNER_SYNC.remove(key);
              }
          }));
    }

    /**
     * Ensure pack/path (existence only) then queue async corner sync.
     */
    public static void ensureAndQueueCornerSync(
      @NotNull final IBuilding building,
      final int schematicLevel,
      @Nullable final Level world)
    {
        final Resolved resolved = ensure(building, schematicLevel);
        if (resolved != null)
        {
            queueCornerSync(building, world, resolved.pack(), resolved.path(), building.getRotationMirror());
        }
        else
        {
            queueCornerSync(building, world, building.getStructurePack(), building.getBlueprintPath(),
              building.getRotationMirror());
        }
    }

    /**
     * @deprecated Sync NBT load — blocks on {@code waitUntilFinishedLoading}. Use
     * {@link #queueCornerSync} / {@link #applyCorners} instead. Kept for IO-pool callers only.
     */
    @Deprecated
    @Nullable
    public static Tuple<BlockPos, BlockPos> syncCornersFromBlueprint(
      @NotNull final IBuilding building,
      @Nullable final Level world,
      @Nullable final String pack,
      @Nullable final String path,
      @Nullable final RotationMirror rotMir)
    {
        // Do not sync-load on callers that may be on the server thread — queue instead.
        queueCornerSync(building, world, pack, path, rotMir);
        return null;
    }

    /**
     * @deprecated Prefer {@link #ensureAndQueueCornerSync}.
     */
    @Deprecated
    @Nullable
    public static Tuple<BlockPos, BlockPos> ensureAndSyncCorners(
      @NotNull final IBuilding building,
      final int schematicLevel,
      @Nullable final Level world)
    {
        ensureAndQueueCornerSync(building, schematicLevel, world);
        return null;
    }

    /**
     * Load a blueprint on an IO thread (StructurePacks soft-fail). Not for server tick.
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

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
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Resolves hut schematic pack/path for a target building level.
 * <p>
 * Quidoms: path candidates must prefer the <em>requested</em> level. Preferring the
 * building's current path first made L1→L2 upgrades load the L1 blueprint, so
 * Structurize saw zero shortage → progress 100% and instant completion.
 */
public final class HutBlueprintResolver
{
    public static final List<String> PREFERRED_PACKS = List.of(
      "Medieval Oak", "Minecolonies Original", "Colonial", "Nordic Spruce",
      "Desert Oasis", "Caledonia", "Fortress");

    private static final int MAX_FALLBACK_PACK_PROBES = 8;
    private static final Set<Long> PENDING_CORNER_SYNC = ConcurrentHashMap.newKeySet();

    private HutBlueprintResolver()
    {
    }

    @Nullable
    public static Resolved resolve(@NotNull final IBuilding building, final int level)
    {
        return resolve(hutId(building), building.getStructurePack(), building.getBlueprintPath(), Math.max(1, level));
    }

    @Nullable
    public static Resolved resolve(
      @Nullable final String hutId,
      @Nullable final String packName,
      @Nullable final String currentPath,
      final int level)
    {
        final String hut = (hutId == null || hutId.isBlank()) ? "builder" : hutId.trim();
        final int targetLevel = Math.max(1, level);
        final List<String> candidates = pathCandidates(hut, currentPath, targetLevel);

        for (final String packAlias : preferredPackAliases(packName))
        {
            final StructurePackMeta pack = StructurePacks.getStructurePack(packAlias);
            if (pack == null)
            {
                continue;
            }
            final List<String> ordered = new ArrayList<>(candidates);
            if (packName != null && packName.toLowerCase().contains("alternative"))
            {
                ordered.addAll(0, altPathCandidates(hut, targetLevel));
            }
            final String found = firstExistingPath(pack, ordered);
            if (found != null)
            {
                return new Resolved(pack.getName(), found);
            }
        }

        for (final String preferred : PREFERRED_PACKS)
        {
            final StructurePackMeta pack = StructurePacks.getStructurePack(preferred);
            if (pack == null)
            {
                continue;
            }
            final String found = firstExistingPath(pack, candidates);
            if (found != null)
            {
                return new Resolved(pack.getName(), found);
            }
        }

        int probed = 0;
        for (final StructurePackMeta pack : StructurePacks.getPackMetas())
        {
            if (pack == null || isDecorationOnlyName(pack.getName()) || PREFERRED_PACKS.contains(pack.getName()))
            {
                continue;
            }
            if (probed++ >= MAX_FALLBACK_PACK_PROBES)
            {
                break;
            }
            final String found = firstExistingPath(pack, candidates);
            if (found != null)
            {
                return new Resolved(pack.getName(), found);
            }
        }

        // Last resort: hutId+level only — never fall back to level-1 when a higher level was requested
        // (that caused upgrade WOs to bind the already-built schematic and autocomplete).
        final List<String> lastResort = List.of(hut + targetLevel + ".blueprint");
        for (final String packAlias : preferredPackAliases(packName))
        {
            final StructurePackMeta pack = StructurePacks.getStructurePack(packAlias);
            if (pack == null)
            {
                continue;
            }
            final String found = firstExistingPath(pack, lastResort);
            if (found != null)
            {
                return new Resolved(pack.getName(), found);
            }
        }
        for (final String preferred : PREFERRED_PACKS)
        {
            final StructurePackMeta pack = StructurePacks.getStructurePack(preferred);
            if (pack == null)
            {
                continue;
            }
            final String found = firstExistingPath(pack, lastResort);
            if (found != null)
            {
                return new Resolved(pack.getName(), found);
            }
        }

        Log.getLogger().warn("HutBlueprintResolver: no pack has hut '{}' level {} candidates {}", hut, targetLevel, candidates);
        return null;
    }

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

    @Nullable
    public static Resolved ensure(@NotNull final IBuilding building, final int level)
    {
        final Resolved resolved = resolve(building, level);
        apply(building, resolved);
        return resolved;
    }

    @Nullable
    public static Tuple<BlockPos, BlockPos> applyCorners(
      @NotNull final IBuilding building,
      @Nullable final Blueprint blueprint,
      @Nullable final Level level,
      @Nullable final RotationMirror rotationMirror)
    {
        if (blueprint == null || level == null)
        {
            return null;
        }
        final RotationMirror rot = rotationMirror != null
                                     ? rotationMirror
                                     : (building.getRotationMirror() != null ? building.getRotationMirror() : RotationMirror.NONE);
        final Tuple<BlockPos, BlockPos> corners =
          ColonyUtils.calculateCorners(building.getPosition(), level, blueprint, rot);
        building.setCorners(corners.getA(), corners.getB());
        try
        {
            final AbstractTileEntityColonyBuilding te = building.getTileEntity();
            if (te != null)
            {
                te.setSchematicCorners(
                  corners.getA().subtract(building.getPosition()),
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
        catch (final Exception ex)
        {
            Log.getLogger().debug("HutBlueprintResolver.applyCorners: TE update skipped", ex);
        }
        return corners;
    }

    public static void queueCornerSync(
      @NotNull final IBuilding building,
      @Nullable final Level level,
      @Nullable final String packName,
      @Nullable final String path,
      @Nullable final RotationMirror rotationMirror)
    {
        if (level == null || level.isClientSide() || building.getColony() == null)
        {
            return;
        }
        final BlockPos id = building.getID();
        final long key = id.asLong();
        if (!PENDING_CORNER_SYNC.add(key))
        {
            return;
        }
        final String pack = (packName == null || packName.isBlank()) ? building.getStructurePack() : packName;
        final String bp = (path == null || path.isBlank()) ? building.getBlueprintPath() : path;
        if (pack == null || pack.isBlank() || bp == null || bp.isBlank())
        {
            PENDING_CORNER_SYNC.remove(key);
            return;
        }
        ServerFutureProcessor.queueBlueprint(new ServerFutureProcessor.BlueprintProcessingData(
          (Future) StructurePacks.getBlueprintFuture(pack, bp, true, level.registryAccess()),
          level,
          blueprint -> {
              try
              {
                  if (blueprint != null)
                  {
                      applyCorners(building, blueprint, level, rotationMirror);
                  }
              }
              finally
              {
                  PENDING_CORNER_SYNC.remove(key);
              }
          }));
    }

    public static void ensureAndQueueCornerSync(@NotNull final IBuilding building, final int level, @Nullable final Level world)
    {
        final Resolved resolved = ensure(building, level);
        if (resolved != null)
        {
            queueCornerSync(building, world, resolved.pack(), resolved.path(), building.getRotationMirror());
        }
        else
        {
            queueCornerSync(building, world, building.getStructurePack(), building.getBlueprintPath(), building.getRotationMirror());
        }
    }

    @Deprecated
    @Nullable
    public static Tuple<BlockPos, BlockPos> syncCornersFromBlueprint(
      @NotNull final IBuilding building,
      @Nullable final Level level,
      @Nullable final String packName,
      @Nullable final String path,
      @Nullable final RotationMirror rotationMirror)
    {
        queueCornerSync(building, level, packName, path, rotationMirror);
        return null;
    }

    @Deprecated
    @Nullable
    public static Tuple<BlockPos, BlockPos> ensureAndSyncCorners(
      @NotNull final IBuilding building,
      final int level,
      @Nullable final Level world)
    {
        ensureAndQueueCornerSync(building, level, world);
        return null;
    }

    @Nullable
    public static Blueprint loadBlueprint(
      @Nullable final String packName,
      @Nullable final String path,
      @Nullable final String hutId,
      final int level,
      @NotNull final HolderLookup.Provider provider)
    {
        if (packName != null && !packName.isBlank() && path != null && !path.isBlank())
        {
            final Blueprint direct = StructurePacks.getBlueprint(packName, path, true, provider);
            if (direct != null)
            {
                return direct;
            }
        }
        final Resolved resolved = resolve(hutId, packName, path, level);
        if (resolved == null)
        {
            return null;
        }
        return StructurePacks.getBlueprint(resolved.pack(), resolved.path(), true, provider);
    }

    /**
     * Build path candidates for {@code targetLevel}. Never put the building's current
     * (possibly lower-level) path ahead of the leveled target path.
     */
    @NotNull
    public static List<String> pathCandidates(@NotNull final String hutId, @Nullable final String currentPath, final int level)
    {
        final ArrayList<String> out = new ArrayList<>();
        final int targetLevel = Math.max(1, level);

        if (currentPath != null && !currentPath.isBlank())
        {
            final String trimmed = currentPath.trim();
            // Target-level variants of the current path first
            addLeveled(out, trimmed, targetLevel);
            // Keep exact current path only if it already is the target level
            if (pathLevelSuffix(trimmed) == targetLevel)
            {
                out.add(0, trimmed);
            }
        }

        String mapped = BlueprintMapping.getPathMapping("", hutId);
        if (mapped == null || mapped.isBlank())
        {
            mapped = "fundamentals/" + hutId;
        }
        out.add(mapped + targetLevel + ".blueprint");
        out.add(mapped.replace("/" + hutId, "/alt" + hutId) + targetLevel + ".blueprint");
        if (mapped.contains("/"))
        {
            final String dir = mapped.substring(0, mapped.lastIndexOf('/') + 1);
            out.add(dir + hutId + targetLevel + ".blueprint");
            out.add(hutId + targetLevel + ".blueprint");
        }
        else
        {
            out.add(hutId + targetLevel + ".blueprint");
        }

        if (currentPath != null && currentPath.contains("/"))
        {
            final String leaf = currentPath.substring(currentPath.lastIndexOf('/') + 1).trim();
            if (!leaf.isBlank())
            {
                addLeveled(out, leaf, targetLevel);
            }
        }

        // Level-1 fallbacks last (only useful when target is 1, or as missing-schematic probe)
        if (targetLevel == 1)
        {
            out.add(mapped + "1.blueprint");
            out.add(mapped.replace("/" + hutId, "/alt" + hutId) + "1.blueprint");
            out.add(hutId + "1.blueprint");
        }

        return out.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
    }

    private static void addLeveled(final List<String> list, final String path, final int level)
    {
        String base = path.replace(".blueprint", "");
        if (base.isEmpty())
        {
            return;
        }
        final char last = base.charAt(base.length() - 1);
        if (Character.isDigit(last))
        {
            base = base.substring(0, base.length() - 1);
        }
        list.add(base + level + ".blueprint");
    }

    /** @return trailing schematic level digit, or -1 if absent */
    private static int pathLevelSuffix(@NotNull final String path)
    {
        String base = path.replace(".blueprint", "");
        if (base.isEmpty())
        {
            return -1;
        }
        final char last = base.charAt(base.length() - 1);
        return Character.isDigit(last) ? (last - '0') : -1;
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

    private static boolean pathExists(@NotNull final StructurePackMeta pack, @Nullable final String path)
    {
        if (path == null || path.isBlank())
        {
            return false;
        }
        try
        {
            return Files.exists(pack.getPath().resolve(pack.getNormalizedSubPath(path)), new LinkOption[0]);
        }
        catch (final Exception ex)
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
        return n.equalsIgnoreCase("Decorations") || n.equalsIgnoreCase("Decoration") || n.equalsIgnoreCase("decos");
    }

    @NotNull
    private static List<String> preferredPackAliases(@Nullable final String packName)
    {
        final ArrayList<String> out = new ArrayList<>();
        if (packName == null || packName.isBlank())
        {
            return out;
        }
        out.add(packName.trim());
        final String lower = packName.toLowerCase();
        if (lower.endsWith(" alternative") || lower.endsWith(" alt"))
        {
            final String base = packName.replaceAll("(?i)\\s+(alternative|alt)\\s*$", "").trim();
            if (!base.isBlank())
            {
                out.add(base);
            }
        }
        final String style = BlueprintMapping.getStyleMapping(packName.trim());
        if (style != null && !style.isBlank() && out.stream().noneMatch(style::equalsIgnoreCase))
        {
            out.add(style);
        }
        // Note: StructurePacks has no aliasPackName API on current Structurize — style mapping only.
        return out;
    }

    @NotNull
    private static List<String> altPathCandidates(@NotNull final String hutId, final int level)
    {
        return List.of(
          "fundamentals/alt" + hutId + level + ".blueprint");
    }

    public record Resolved(@NotNull String pack, @NotNull String path)
    {
    }
}

package com.minecolonies.core.util;

import com.minecolonies.api.util.Log;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * NeoForge 1.21.10 jar content-roots often fail {@code Files.list} for {@code blueprints/&lt;modid&gt;}.
 * Extract Quidoms style packs into the game {@code blueprints/} folder so Structurize can discover them locally.
 */
public final class BlueprintPackBootstrap
{
    private static final String MARKER = ".quidoms-blueprints-0.0.22-1.21.10";

    private BlueprintPackBootstrap()
    {
    }

    public static void ensureExtracted()
    {
        try
        {
            final Path gameBlueprints = FMLPaths.GAMEDIR.get().resolve("blueprints");
            Files.createDirectories(gameBlueprints);
            final Path marker = gameBlueprints.resolve(MARKER);
            if (Files.exists(marker))
            {
                return;
            }

            try (Stream<Path> existing = Files.list(gameBlueprints))
            {
                existing.filter(p -> p.getFileName().toString().startsWith(".quidoms-blueprints-"))
                  .forEach(p -> {
                      try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                  });
            }

            final int copied = extractViaJarWalk(gameBlueprints);
            if (copied > 0)
            {
                Files.writeString(marker, "extracted=" + copied);
                Log.getLogger().info("Extracted {} Quidoms blueprint files into {}", copied, gameBlueprints);
            }
            else
            {
                Log.getLogger().warn("Could not extract Quidoms blueprints into {}; jar discovery may still fail", gameBlueprints);
            }
        }
        catch (Exception ex)
        {
            Log.getLogger().warn("Blueprint pack bootstrap failed", ex);
        }
    }

    private static int extractViaJarWalk(final Path gameBlueprints) throws Exception
    {
        final URL codeSource = BlueprintPackBootstrap.class.getProtectionDomain().getCodeSource() != null
          ? BlueprintPackBootstrap.class.getProtectionDomain().getCodeSource().getLocation()
          : null;
        if (codeSource == null)
        {
            return 0;
        }

        int copied = 0;
        Path jarOrDir = Path.of(codeSource.toURI());
        if (Files.isRegularFile(jarOrDir) && jarOrDir.getFileName().toString().endsWith(".jar"))
        {
            try (FileSystem fs = FileSystems.newFileSystem(jarOrDir, Collections.emptyMap()))
            {
                final Path root = fs.getPath("blueprints", "minecolonies");
                if (!Files.isDirectory(root))
                {
                    return 0;
                }
                try (Stream<Path> styles = Files.list(root))
                {
                    for (final Path style : styles.toList())
                    {
                        if (!Files.isDirectory(style))
                        {
                            continue;
                        }
                        copied += copyTree(style, gameBlueprints.resolve(style.getFileName().toString()));
                    }
                }
            }
        }
        else
        {
            // Dev / exploded resources: <root>/blueprints/minecolonies
            Path root = jarOrDir;
            if (!Files.isDirectory(root.resolve("blueprints").resolve("minecolonies")))
            {
                // classes dir — walk up to resources
                Path probe = jarOrDir;
                for (int i = 0; i < 6 && probe != null; i++)
                {
                    if (Files.isDirectory(probe.resolve("blueprints").resolve("minecolonies")))
                    {
                        root = probe;
                        break;
                    }
                    probe = probe.getParent();
                }
            }
            final Path stylesRoot = root.resolve("blueprints").resolve("minecolonies");
            if (Files.isDirectory(stylesRoot))
            {
                try (Stream<Path> styles = Files.list(stylesRoot))
                {
                    for (final Path style : styles.toList())
                    {
                        if (!Files.isDirectory(style))
                        {
                            continue;
                        }
                        copied += copyTree(style, gameBlueprints.resolve(style.getFileName().toString()));
                    }
                }
            }
        }
        return copied;
    }

    private static int copyTree(final Path from, final Path to) throws IOException
    {
        final int[] count = {0};
        Files.walk(from).forEach(src -> {
            try
            {
                final Path rel = from.relativize(src);
                final Path dest = to.resolve(rel.toString());
                if (Files.isDirectory(src))
                {
                    Files.createDirectories(dest);
                }
                else
                {
                    Files.createDirectories(dest.getParent());
                    try (InputStream in = Files.newInputStream(src))
                    {
                        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                        count[0]++;
                    }
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });
        return count[0];
    }
}

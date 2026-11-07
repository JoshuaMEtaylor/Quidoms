package com.minecolonies.core.util;

import com.minecolonies.api.util.Log;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * NeoForge 1.21.10 jar content-roots often fail {@code Files.list} for {@code blueprints/&lt;modid&gt;}.
 * Extract Quidoms style packs into the game {@code blueprints/} folder so Structurize can discover them locally
 * (ClientStructurePackLoader lists {@code gamedir/blueprints/<style>/pack.json}).
 */
public final class BlueprintPackBootstrap
{
    private static final String MARKER = ".quidoms-blueprints-0.0.23-1.21.10";
    private static final String JAR_PREFIX = "blueprints/minecolonies/";

    private BlueprintPackBootstrap()
    {
    }

    public static void ensureExtracted()
    {
        try
        {
            final Path gameBlueprints = FMLPaths.GAMEDIR.get().resolve("blueprints");
            Files.createDirectories(gameBlueprints);

            final int expectedStyles = countJarStyles();
            final int presentStyles = countExtractedStyles(gameBlueprints);
            final Path marker = gameBlueprints.resolve(MARKER);

            final boolean markerOk = Files.exists(marker);
            final boolean complete = markerOk && expectedStyles > 0 && presentStyles >= expectedStyles;
            if (complete)
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

            final int copied = extractViaZipEntries(gameBlueprints);
            if (copied > 0)
            {
                Files.writeString(marker, "extracted=" + copied + " styles=" + countExtractedStyles(gameBlueprints));
                Log.getLogger().info("Extracted {} Quidoms blueprint files ({} styles) into {}",
                  copied, countExtractedStyles(gameBlueprints), gameBlueprints);
            }
            else
            {
                // Fall back to jar filesystem walk
                final int fallback = extractViaJarWalk(gameBlueprints);
                if (fallback > 0)
                {
                    Files.writeString(marker, "extracted=" + fallback + " styles=" + countExtractedStyles(gameBlueprints));
                    Log.getLogger().info("Extracted {} Quidoms blueprint files via jar FS into {}", fallback, gameBlueprints);
                }
                else
                {
                    Log.getLogger().warn("Could not extract Quidoms blueprints into {}; jar discovery may still fail", gameBlueprints);
                }
            }
        }
        catch (Exception ex)
        {
            Log.getLogger().warn("Blueprint pack bootstrap failed", ex);
        }
    }

    private static int countJarStyles()
    {
        try
        {
            final Path jar = resolveOwnJar();
            if (jar == null || !Files.isRegularFile(jar))
            {
                return 0;
            }
            final Set<String> styles = new HashSet<>();
            try (JarFile jf = new JarFile(jar.toFile()))
            {
                final Enumeration<JarEntry> en = jf.entries();
                while (en.hasMoreElements())
                {
                    final String name = en.nextElement().getName();
                    if (!name.startsWith(JAR_PREFIX) || name.length() <= JAR_PREFIX.length())
                    {
                        continue;
                    }
                    final String rest = name.substring(JAR_PREFIX.length());
                    final int slash = rest.indexOf('/');
                    if (slash > 0)
                    {
                        styles.add(rest.substring(0, slash));
                    }
                }
            }
            return styles.size();
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    private static int countExtractedStyles(final Path gameBlueprints) throws IOException
    {
        if (!Files.isDirectory(gameBlueprints))
        {
            return 0;
        }
        int n = 0;
        try (Stream<Path> styles = Files.list(gameBlueprints))
        {
            for (final Path style : styles.toList())
            {
                if (Files.isDirectory(style) && Files.isRegularFile(style.resolve("pack.json")))
                {
                    n++;
                }
            }
        }
        return n;
    }

    private static Path resolveOwnJar() throws Exception
    {
        final URL codeSource = BlueprintPackBootstrap.class.getProtectionDomain().getCodeSource() != null
          ? BlueprintPackBootstrap.class.getProtectionDomain().getCodeSource().getLocation()
          : null;
        if (codeSource == null)
        {
            return null;
        }
        final Path jarOrDir = Path.of(codeSource.toURI());
        if (Files.isRegularFile(jarOrDir) && jarOrDir.getFileName().toString().endsWith(".jar"))
        {
            return jarOrDir;
        }
        return null;
    }

    /**
     * Reliable extract: enumerate jar zip entries (does not depend on {@code Files.list} on jar FS).
     */
    private static int extractViaZipEntries(final Path gameBlueprints) throws Exception
    {
        final Path jar = resolveOwnJar();
        if (jar == null)
        {
            return 0;
        }
        int copied = 0;
        try (JarFile jf = new JarFile(jar.toFile()))
        {
            final Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements())
            {
                final JarEntry entry = en.nextElement();
                final String name = entry.getName();
                if (!name.startsWith(JAR_PREFIX) || entry.isDirectory())
                {
                    continue;
                }
                final String rest = name.substring(JAR_PREFIX.length());
                // skip junk at style root that isn't a style file
                if (rest.isEmpty() || rest.equals("not_converted.txt") || rest.equals("template.zip"))
                {
                    continue;
                }
                final Path dest = gameBlueprints.resolve(rest);
                Files.createDirectories(dest.getParent());
                try (InputStream in = jf.getInputStream(entry))
                {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                    copied++;
                }
            }
        }
        return copied;
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
            try (FileSystem fs = FileSystems.newFileSystem(jarOrDir, Collections.<String, Object>emptyMap()))
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
            Path root = jarOrDir;
            if (!Files.isDirectory(root.resolve("blueprints").resolve("minecolonies")))
            {
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

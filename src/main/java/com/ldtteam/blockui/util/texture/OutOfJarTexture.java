package com.ldtteam.blockui.util.texture;

import com.ldtteam.blockui.util.resloc.OutOfJarResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge 1.21.10 port: load textures from an absolute filesystem path carried by
 * {@link OutOfJarResourceLocation} (blueprint pack category icons, etc.).
 * <p>
 * Vanilla {@link TextureManager#getTexture(ResourceLocation)} always materializes a
 * {@code SimpleTexture} for unknown ids (logging "Missing resource" and caching the
 * missing texture). Out-of-jar icons must therefore be registered via
 * {@link TextureManager#registerAndLoad} <em>before</em> any vanilla getTexture call.
 */
public class OutOfJarTexture extends ReloadableTexture
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OutOfJarTexture.class);

    /** Ids we successfully registered so assertLoaded can short-circuit without reloading. */
    private static final Set<ResourceLocation> LOADED = ConcurrentHashMap.newKeySet();

    private final OutOfJarResourceLocation resourceLocation;

    public OutOfJarTexture(final OutOfJarResourceLocation resourceLocation)
    {
        super(resourceLocation);
        this.resourceLocation = resourceLocation;
    }

    @Override
    public TextureContents loadContents(final ResourceManager resourceManager) throws IOException
    {
        if (!Files.isRegularFile(resourceLocation.getNioPath()))
        {
            throw new IOException("Out-of-jar texture missing on disk: " + resourceLocation.getNioPath());
        }
        try (InputStream is = Files.newInputStream(resourceLocation.getNioPath()))
        {
            final NativeImage image = NativeImage.read(is);
            return new TextureContents(image, null);
        }
    }

    public static AbstractTexture assertLoadedDefaultManagers(final ResourceLocation resLoc)
    {
        return assertLoaded(resLoc, Minecraft.getInstance().getTextureManager(), Minecraft.getInstance().getResourceManager());
    }

    /**
     * Ensure {@code resLoc} is bound to a usable texture. Out-of-jar locations are loaded from
     * {@link OutOfJarResourceLocation#getNioPath()}; everything else uses vanilla systems.
     */
    public static AbstractTexture assertLoaded(final ResourceLocation resLoc,
        final TextureManager textureManager,
        final ResourceManager resourceManager)
    {
        if (!(resLoc instanceof final OutOfJarResourceLocation outOfJarResLoc))
        {
            return textureManager.getTexture(resLoc);
        }

        if (LOADED.contains(outOfJarResLoc))
        {
            final AbstractTexture existing = peekRegistered(textureManager, outOfJarResLoc);
            if (IsOurTexture.isOur(existing))
            {
                return existing;
            }
            // Stale entry (reload) — fall through and re-register.
            LOADED.remove(outOfJarResLoc);
        }

        // Drop any vanilla SimpleTexture that may have been cached from a prior failed lookup.
        textureManager.release(outOfJarResLoc);

        final OutOfJarTexture texture = new OutOfJarTexture(outOfJarResLoc);
        try
        {
            textureManager.registerAndLoad(outOfJarResLoc, texture);
            LOADED.add(outOfJarResLoc);
            return texture;
        }
        catch (final Exception ex)
        {
            LOGGER.warn("Failed to load out-of-jar texture {} from {}", outOfJarResLoc, outOfJarResLoc.getNioPath(), ex);
            return textureManager.getTexture(resLoc); // missing texture fallback
        }
    }

    /**
     * Peek without triggering vanilla SimpleTexture creation. Uses release/re-register path when
     * unknown; here we only read via getTexture after we know LOADED contains the id.
     */
    @Nullable
    private static AbstractTexture peekRegistered(final TextureManager textureManager, final ResourceLocation id)
    {
        try
        {
            return textureManager.getTexture(id);
        }
        catch (final Exception ignored)
        {
            return null;
        }
    }
}

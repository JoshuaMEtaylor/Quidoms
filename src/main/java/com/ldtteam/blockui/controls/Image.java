package com.ldtteam.blockui.controls;

import com.ldtteam.blockui.BOGuiGraphics;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneParams;
import com.ldtteam.blockui.Parsers;
import com.ldtteam.blockui.UiRenderMacros.ResolvedBlit;
import com.ldtteam.blockui.util.records.SizeI;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import java.util.Objects;

import static com.ldtteam.blockui.UiRenderMacros.blit;

/**
 * Simple image element (NeoForge 21.10 / MC 1.21.10).
 * Uses GuiGraphics blit / blitSprite; preserves XML source/offset/size attributes.
 */
public class Image extends Pane
{
    protected ResourceLocation resourceLocation = null;
    protected int u = 0;
    protected int v = 0;
    protected int uWidth = 0;
    protected int vHeight = 0;
    protected ResolvedBlit resolvedBlit = null;

    public Image()
    {
        super();
    }

    public Image(final PaneParams params)
    {
        super(params);

        params.applyShorthand("imageoffset", Parsers.INT, 2, a -> {
            u = a.get(0);
            v = a.get(1);
        });

        params.applyShorthand("imagesize", Parsers.INT, 2, a -> {
            uWidth = a.get(0);
            vHeight = a.get(1);
        });

        resourceLocation = params.getResource("source");
    }

    public static SizeI getImageDimensions(final ResourceLocation resourceLocation)
    {
        // Size probing via ImageIO/out-of-jar deferred; callers that need UV mapping
        // should set explicit imagesize in XML (Structurize/BlockUI layouts do).
        return new SizeI(256, 256);
    }

    public void setImage(final ResourceLocation rl, final int u, final int v, final int uWidth, final int vHeight)
    {
        if (Objects.equals(rl, resourceLocation) && this.u == u && this.v == v && this.uWidth == uWidth && this.vHeight == vHeight)
        {
            return;
        }

        this.resourceLocation = rl;
        this.u = u;
        this.v = v;
        this.uWidth = uWidth;
        this.vHeight = vHeight;
        this.resolvedBlit = null;
    }

    public void setImage(final ResourceLocation rl, final boolean keepUv)
    {
        if (keepUv)
        {
            setImage(rl, u, v, uWidth, vHeight);
        }
        else
        {
            setImage(rl, 0, 0, 0, 0);
        }
    }

    @Override
    public void drawSelf(final BOGuiGraphics target, final double mx, final double my)
    {
        if (!FMLEnvironment.isProduction())
        {
            Objects.requireNonNull(resourceLocation, () -> "Missing image source: " + id + " | " + window.getXmlResourceLocation());
        }

        if (resolvedBlit == null)
        {
            resolvedBlit = resolveBlit(resourceLocation, u, v, uWidth, vHeight);
        }

        resolvedBlit.blit(target, x, y, width, height);
    }

    public static ResolvedBlit resolveBlit(final ResourceLocation resLoc)
    {
        return resolveBlit(resLoc, 0, 0, 0, 0);
    }

    public static ResolvedBlit resolveBlit(final ResourceLocation resLoc, final int u, final int v, final int uWidth, final int vHeight)
    {
        if (resLoc == null || resLoc == MissingTextureAtlasSprite.getLocation())
        {
            return (gui, x, y, w, h) -> blit(gui, MissingTextureAtlasSprite.getLocation(), x, y, w, h);
        }

        // Prefer vanilla GUI atlas sprite path (AtlasManager is stubbed on 1.21.10).
        if (u == 0 && v == 0 && uWidth == 0 && vHeight == 0)
        {
            return (gui, x, y, w, h) -> {
                try
                {
                    gui.blitSprite(RenderPipelines.GUI_TEXTURED, resLoc, x, y, w, h);
                }
                catch (final RuntimeException ex)
                {
                    blit(gui, resLoc, x, y, w, h);
                }
            };
        }

        final int texW = uWidth == 0 ? 256 : Math.max(uWidth, u + uWidth);
        final int texH = vHeight == 0 ? 256 : Math.max(vHeight, v + vHeight);
        final int drawUW = uWidth == 0 ? texW : uWidth;
        final int drawVH = vHeight == 0 ? texH : vHeight;
        return (gui, x, y, w, h) -> gui.blit(
            net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            resLoc, x, y, (float) u, (float) v, w, h, drawUW, drawVH, texW, texH);
    }
}

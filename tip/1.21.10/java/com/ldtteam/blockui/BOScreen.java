package com.ldtteam.blockui;

import com.ldtteam.blockui.util.cursor.CursorUtils;
import com.ldtteam.blockui.views.BOWindow;
import java.util.Objects;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Quidoms tip overlay of BlockUI {@code BOScreen}.
 *
 * <p><b>QD-148 ghost frame fix (0.0.59):</b> NeoForge 1.21 {@code Screen.renderWithTooltipAndSubtitles}
 * always calls {@link #renderBackground} <em>before</em> {@link #render}. Vanilla
 * {@code renderBackground} draws {@code textures/gui/menu_background.png} (or in-world variant)
 * at {@code (0,0)} using {@link Screen#width}/{@link Screen#height}.
 * {@link #render} then overwrites width/height to the BlockUI window size and <em>centers</em>
 * the real UI — so a darkened menu-sized panel is left stuck in the screen top-left on every
 * Quidoms BlockUI menu. Empty {@link #renderBackground} permanently removes that ghost; BlockUI's
 * own lightbox gradient in {@link #render} still dims the world when {@code lightbox=true}.
 */
public class BOScreen extends Screen
{
    protected double renderScale = 1.0;
    protected double mcScale = 1.0;
    protected BOWindow window;
    protected double x = 0.0;
    protected double y = 0.0;
    public static boolean isMouseLeftDown = false;
    protected boolean isOpen = false;
    protected int framebufferWidth;
    protected int framebufferHeight;
    protected int absoluteMouseX;
    protected int absoluteMouseY;

    public BOScreen(final BOWindow window)
    {
        super(Component.literal("Blockout GUI"));
        this.window = window;
    }

    /**
     * Suppress vanilla menu / blur / transparent backgrounds. BlockUI owns chrome via XML shells
     * and optional lightbox fill in {@link #render}.
     */
    @Override
    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick)
    {
        // Intentionally empty — see class javadoc (top-left ghost menu_background frame).
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick)
    {
        if (this.minecraft == null || !this.isOpen)
        {
            return;
        }

        this.absoluteMouseX = mouseX;
        this.absoluteMouseY = mouseY;
        this.framebufferWidth = this.minecraft.getWindow().getWidth();
        this.framebufferHeight = this.minecraft.getWindow().getHeight();
        final int guiW = Math.max(this.minecraft.getWindow().getGuiScaledWidth(), 320);
        final int guiH = Math.max(this.minecraft.getWindow().getGuiScaledHeight(), 240);
        this.mcScale = Math.max((double) this.minecraft.getWindow().getGuiScale(), 1.0);
        final double fbScale = this.window.getRenderType().calcRenderScale(this.minecraft.getWindow(), this.window);
        this.renderScale = fbScale / this.mcScale;

        if (this.window.hasLightbox() && this.minecraft.screen == this)
        {
            graphics.fillGradient(0, 0, guiW, guiH, -1072689136, -804253680);
        }

        this.width = this.window.getWidth();
        this.height = this.window.getHeight();
        this.x = Math.floor(((double) guiW - (double) this.width * this.renderScale) / 2.0);
        this.y = Math.floor(((double) guiH - (double) this.height * this.renderScale) / 2.0);

        try
        {
            final BOGuiGraphics boGraphics = new BOGuiGraphics(graphics);
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) this.x, (float) this.y);
            graphics.pose().scale((float) this.renderScale, (float) this.renderScale);
            this.window.draw(boGraphics, this.calcRelativeX((double) mouseX), this.calcRelativeY((double) mouseY));
            if (this.minecraft.screen == this && Pane.debugging)
            {
                boGraphics.drawString(
                    "XML: %s Scaling: %s (vanilla: %.2f our: %.2f) "
                        .formatted(this.window.getXmlResourceLocation(), this.window.getRenderType().name(), this.mcScale, this.renderScale),
                    (float) (-this.x / this.renderScale) + 3.0F,
                    (float) (-9),
                    Color.getByName("white"));
            }

            this.window.drawLast(boGraphics, this.calcRelativeX((double) mouseX), this.calcRelativeY((double) mouseY));
            graphics.pose().popMatrix();
        }
        catch (final Exception ex)
        {
            final CrashReport report = CrashReport.forThrowable(ex, "Rendering BO screen");
            final CrashReportCategory category = report.addCategory("BO screen rendering details");
            category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
            category.setDetail("Scaling mode (window render type)", () -> this.window.getRenderType().name());
            category.setDetail("Vanilla gui scale", () -> Double.toString(this.mcScale));
            category.setDetail("BO gui scale", () -> Double.toString(this.renderScale));
            throw new ReportedException(report);
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event)
    {
        if (event.key() >= 0 && event.key() <= 348)
        {
            try
            {
                return this.window.onKeyTyped('\u0000', event.key());
            }
            catch (final Exception ex)
            {
                final CrashReport report = CrashReport.forThrowable(ex, "KeyPressed event for BO screen");
                final CrashReportCategory category = report.addCategory("BO screen key event details");
                category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
                category.setDetail("GLFW key value", () -> Integer.toString(event.key()));
                throw new ReportedException(report);
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(final CharacterEvent event)
    {
        try
        {
            return this.window.onKeyTyped(event.codepointAsString().charAt(0), event.codepoint());
        }
        catch (final Exception ex)
        {
            final CrashReport report = CrashReport.forThrowable(ex, "CharTyped event for BO screen");
            final CrashReportCategory category = report.addCategory("BO screen char event details");
            category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
            category.setDetail("Char value", event::codepointAsString);
            throw new ReportedException(report);
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick)
    {
        final double relX = this.calcRelativeX(event.x());
        final double relY = this.calcRelativeY(event.y());

        try
        {
            if (event.button() == 0)
            {
                isMouseLeftDown = true;
                return this.window.click(relX, relY);
            }
            return event.button() == 1 && this.window.rightClick(relX, relY);
        }
        catch (final Exception ex)
        {
            final CrashReport report = CrashReport.forThrowable(ex, "MousePressed event for BO screen");
            final CrashReportCategory category = report.addCategory("BO screen mouse event details");
            category.setDetail("XML res loc", () -> Objects.toString(this.window.getXmlResourceLocation()));
            category.setDetail("GLFW mouse key value", () -> Integer.toString(event.button()));
            throw new ReportedException(report);
        }
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY)
    {
        if (scrollY != 0.0)
        {
            try
            {
                return this.window.scrollInput(scrollX * 10.0, scrollY * 10.0, this.calcRelativeX(mouseX), this.calcRelativeY(mouseY));
            }
            catch (final Exception ex)
            {
                final CrashReport report = CrashReport.forThrowable(ex, "MouseScroll event for BO screen");
                final CrashReportCategory category = report.addCategory("BO screen scroll event details");
                category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
                category.setDetail("Scroll value", () -> Double.toString(scrollY));
                throw new ReportedException(report);
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY)
    {
        try
        {
            return this.window.onMouseDrag(this.calcRelativeX(event.x()), this.calcRelativeY(event.y()), event.button(), dragX, dragY);
        }
        catch (final Exception ex)
        {
            final CrashReport report = CrashReport.forThrowable(ex, "MouseDragged event for BO screen");
            final CrashReportCategory category = report.addCategory("BO screen mouse event details");
            category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
            throw new ReportedException(report);
        }
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event)
    {
        if (event.button() == 0)
        {
            isMouseLeftDown = false;
            try
            {
                return this.window.onMouseReleased(this.calcRelativeX(event.x()), this.calcRelativeY(event.y()));
            }
            catch (final Exception ex)
            {
                final CrashReport report = CrashReport.forThrowable(ex, "MouseReleased event for BO screen");
                final CrashReportCategory category = report.addCategory("BO screen mouse event details");
                category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
                category.setDetail("GLFW mouse key value", () -> Integer.toString(event.button()));
                throw new ReportedException(report);
            }
        }
        return false;
    }

    public BOWindow getWindow()
    {
        return this.window;
    }

    @Override
    public void tick()
    {
        try
        {
            if (this.minecraft != null)
            {
                if (!this.isOpen)
                {
                    this.window.onOpened();
                    this.isOpen = true;
                }
                else
                {
                    this.window.onUpdate();
                    final LocalPlayer player = this.minecraft.player;
                    if (player != null && !player.isAlive())
                    {
                        player.closeContainer();
                    }
                }
            }
        }
        catch (final Exception ex)
        {
            final CrashReport report = CrashReport.forThrowable(ex, "Ticking/Updating BO screen");
            final CrashReportCategory category = report.addCategory("BO screen update details");
            category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
            category.setDetail("Is opened", () -> Boolean.toString(this.isOpen));
            throw new ReportedException(report);
        }
    }

    @Override
    public void removed()
    {
        try
        {
            this.window.onClosed();
        }
        catch (final Exception ex)
        {
            final CrashReport report = CrashReport.forThrowable(ex, "Closing BO screen");
            final CrashReportCategory category = report.addCategory("BO screen closing details");
            category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
            category.setDetail("Is opened", () -> Boolean.toString(this.isOpen));
            throw new ReportedException(report);
        }
        finally
        {
            BOWindow.clearFocus();
            CursorUtils.resetCursor();
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return this.window.doesWindowPauseGame();
    }

    private double calcRelativeX(final double absoluteX)
    {
        return (absoluteX - this.x) / this.renderScale;
    }

    private double calcRelativeY(final double absoluteY)
    {
        return (absoluteY - this.y) / this.renderScale;
    }

    public double getRenderScale()
    {
        return this.renderScale;
    }

    public double getVanillaGuiScale()
    {
        return this.mcScale;
    }

    public int getFramebufferWidth()
    {
        return this.framebufferWidth;
    }

    public int getFramebufferHeight()
    {
        return this.framebufferHeight;
    }

    public int getAbsoluteMouseX()
    {
        return this.absoluteMouseX;
    }

    public int getAbsoluteMouseY()
    {
        return this.absoluteMouseY;
    }
}

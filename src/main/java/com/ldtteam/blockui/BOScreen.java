package com.ldtteam.blockui;
import com.ldtteam.blockui.util.cursor.CursorUtils;

import com.ldtteam.blockui.views.BOWindow;
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
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

/**
 * Wraps MineCrafts GuiScreen for BlockOut's Window.
 */
public class BOScreen extends Screen
{
    protected double renderScale = 1.0d;
    protected double mcScale = 1.0d;
    protected BOWindow window;
    protected double x = 0;
    protected double y = 0;
    public static boolean isMouseLeftDown = false;
    protected boolean isOpen = false;
    protected int framebufferWidth;
    protected int framebufferHeight;
    protected int absoluteMouseX;
    protected int absoluteMouseY;

    public BOScreen(final BOWindow w)
    {
        super(Component.literal("Blockout GUI"));
        window = w;
    }

    @Override
    public void render(final GuiGraphics ms, final int mx, final int my, final float f)
    {
        if (minecraft == null || !isOpen)
        {
            return;
        }

        absoluteMouseX = mx;
        absoluteMouseY = my;
        // Framebuffer (pixel) size — used by WindowRenderType scale calculators.
        framebufferWidth = minecraft.getWindow().getWidth();
        framebufferHeight = minecraft.getWindow().getHeight();
        // GuiGraphics / Screen mouse coords are in GUI-scaled space on 1.21.x.
        final int guiScaledWidth = Math.max(minecraft.getWindow().getGuiScaledWidth(), 320);
        final int guiScaledHeight = Math.max(minecraft.getWindow().getGuiScaledHeight(), 240);

        mcScale = Math.max(minecraft.getWindow().getGuiScale(), 1.0d);
        // calcRenderScale returns a framebuffer-pixel scale; convert to GUI-space scale so
        // windows fit modern GUI scale instead of drawing huge/clipped in scaled coords.
        final double pixelScale = window.getRenderType().calcRenderScale(minecraft.getWindow(), window);
        renderScale = pixelScale / mcScale;

        if (window.hasLightbox() && minecraft.screen == this)
        {
            ms.fillGradient(0, 0, guiScaledWidth, guiScaledHeight, -1072689136, -804253680);
        }

        width = window.getWidth();
        height = window.getHeight();
        x = Math.floor((guiScaledWidth - width * renderScale) / 2.0d);
        y = Math.floor((guiScaledHeight - height * renderScale) / 2.0d);

        try
        {
            final BOGuiGraphics target = new BOGuiGraphics(ms);
            ms.pose().pushMatrix();
            ms.pose().translate((float) x, (float) y);
            ms.pose().scale((float) renderScale, (float) renderScale);

            window.draw(target, calcRelativeX(mx), calcRelativeY(my));

            if (minecraft.screen == this && Pane.debugging)
            {
                target.drawString(
                    "XML: %s Scaling: %s (vanilla: %.2f our: %.2f) "
                        .formatted(window.getXmlResourceLocation(), window.getRenderType().name(), mcScale, renderScale),
                    (float) (-x / renderScale) + 3,
                    -minecraft.font.lineHeight,
                    Color.getByName("white"));
            }

            window.drawLast(target, calcRelativeX(mx), calcRelativeY(my));
            ms.pose().popMatrix();
        }
        catch (final Exception e)
        {
            final CrashReport crashReport = CrashReport.forThrowable(e, "Rendering BO screen");
            final CrashReportCategory category = crashReport.addCategory("BO screen rendering details");
            category.setDetail("XML res loc", () -> window.getXmlResourceLocation().toString());
            category.setDetail("Scaling mode (window render type)", () -> window.getRenderType().name());
            category.setDetail("Vanilla gui scale", () -> Double.toString(mcScale));
            category.setDetail("BO gui scale", () -> Double.toString(renderScale));
            throw new ReportedException(crashReport);
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event)
    {
        if (event.key() >= 0 && event.key() <= GLFW.GLFW_KEY_LAST)
        {
            try
            {
                return window.onKeyTyped('\0', event.key());
            }
            catch (final Exception e)
            {
                final CrashReport crashReport = CrashReport.forThrowable(e, "KeyPressed event for BO screen");
                final CrashReportCategory category = crashReport.addCategory("BO screen key event details");
                category.setDetail("XML res loc", () -> window.getXmlResourceLocation().toString());
                category.setDetail("GLFW key value", () -> Integer.toString(event.key()));
                throw new ReportedException(crashReport);
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(final CharacterEvent event)
    {
        try
        {
            return window.onKeyTyped(event.codepointAsString().charAt(0), event.codepoint());
        }
        catch (final Exception e)
        {
            final CrashReport crashReport = CrashReport.forThrowable(e, "CharTyped event for BO screen");
            final CrashReportCategory category = crashReport.addCategory("BO screen char event details");
            category.setDetail("XML res loc", () -> window.getXmlResourceLocation().toString());
            category.setDetail("Char value", event::codepointAsString);
            throw new ReportedException(crashReport);
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick)
    {
        final double mx = calcRelativeX(event.x());
        final double my = calcRelativeY(event.y());
        try
        {
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT)
            {
                isMouseLeftDown = true;
                return window.click(mx, my);
            }
            else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            {
                return window.rightClick(mx, my);
            }
        }
        catch (final Exception e)
        {
            final CrashReport crashReport = CrashReport.forThrowable(e, "MousePressed event for BO screen");
            final CrashReportCategory category = crashReport.addCategory("BO screen mouse event details");
            category.setDetail("XML res loc", () -> Objects.toString(window.getXmlResourceLocation()));
            category.setDetail("GLFW mouse key value", () -> Integer.toString(event.button()));
            throw new ReportedException(crashReport);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double mx, final double my, final double scrollHorizontalDiff, final double scrollVerticalDiff)
    {
        if (scrollVerticalDiff != 0)
        {
            try
            {
                return window.scrollInput(scrollHorizontalDiff * 10, scrollVerticalDiff * 10, calcRelativeX(mx), calcRelativeY(my));
            }
            catch (final Exception e)
            {
                final CrashReport crashReport = CrashReport.forThrowable(e, "MouseScroll event for BO screen");
                final CrashReportCategory category = crashReport.addCategory("BO screen scroll event details");
                category.setDetail("XML res loc", () -> window.getXmlResourceLocation().toString());
                category.setDetail("Scroll value", () -> Double.toString(scrollVerticalDiff));
                throw new ReportedException(crashReport);
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy)
    {
        try
        {
            return window.onMouseDrag(calcRelativeX(event.x()), calcRelativeY(event.y()), event.button(), dx, dy);
        }
        catch (final Exception e)
        {
            final CrashReport crashReport = CrashReport.forThrowable(e, "MouseDragged event for BO screen");
            final CrashReportCategory category = crashReport.addCategory("BO screen mouse event details");
            category.setDetail("XML res loc", () -> window.getXmlResourceLocation().toString());
            throw new ReportedException(crashReport);
        }
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event)
    {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT)
        {
            isMouseLeftDown = false;
            try
            {
                return window.onMouseReleased(calcRelativeX(event.x()), calcRelativeY(event.y()));
            }
            catch (final Exception e)
            {
                final CrashReport crashReport = CrashReport.forThrowable(e, "MouseReleased event for BO screen");
                final CrashReportCategory category = crashReport.addCategory("BO screen mouse event details");
                category.setDetail("XML res loc", () -> window.getXmlResourceLocation().toString());
                category.setDetail("GLFW mouse key value", () -> Integer.toString(event.button()));
                throw new ReportedException(crashReport);
            }
        }
        return false;
    }

    public BOWindow getWindow()
    {
        return window;
    }

    @Override
    public void tick()
    {
        try
        {
            if (minecraft != null)
            {
                if (!isOpen)
                {
                    window.onOpened();
                    isOpen = true;
                }
                else
                {
                    window.onUpdate();

                    final LocalPlayer player = minecraft.player;
                    if (player != null && !player.isAlive())
                    {
                        player.closeContainer();
                    }
                }
            }
        }
        catch (final Exception e)
        {
            final CrashReport crashReport = CrashReport.forThrowable(e, "Ticking/Updating BO screen");
            final CrashReportCategory category = crashReport.addCategory("BO screen update details");
            category.setDetail("XML res loc", () -> window.getXmlResourceLocation().toString());
            category.setDetail("Is opened", () -> Boolean.toString(isOpen));
            throw new ReportedException(crashReport);
        }
    }

    @Override
    public void removed()
    {
        try
        {
            window.onClosed();
        }
        catch (final Exception e)
        {
            final CrashReport crashReport = CrashReport.forThrowable(e, "Closing BO screen");
            final CrashReportCategory category = crashReport.addCategory("BO screen closing details");
            category.setDetail("XML res loc", () -> window.getXmlResourceLocation().toString());
            category.setDetail("Is opened", () -> Boolean.toString(isOpen));
            throw new ReportedException(crashReport);
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
        return window.doesWindowPauseGame();
    }

    private double calcRelativeX(final double xIn)
    {
        // mx/my from Screen are already GUI-scaled; x/renderScale are GUI-space too.
        return (xIn - x) / renderScale;
    }

    private double calcRelativeY(final double yIn)
    {
        return (yIn - y) / renderScale;
    }

    public double getRenderScale()
    {
        return renderScale;
    }

    public double getVanillaGuiScale()
    {
        return mcScale;
    }

    public int getFramebufferWidth()
    {
        return framebufferWidth;
    }

    public int getFramebufferHeight()
    {
        return framebufferHeight;
    }

    public int getAbsoluteMouseX()
    {
        return absoluteMouseX;
    }

    public int getAbsoluteMouseY()
    {
        return absoluteMouseY;
    }
}

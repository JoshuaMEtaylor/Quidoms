package com.ldtteam.blockui.views;

import com.ldtteam.blockui.BOGuiGraphics;
import com.ldtteam.blockui.MouseEventCallback;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneParams;
import com.ldtteam.blockui.util.records.Pos2i.MutablePos2i;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

/**
 * Quidoms 1.21.10 fix: GuiGraphics.enableScissor transforms via the current pose.
 * Passing pre-transformed coords (old Pane.scissorsStart) double-applies BOScreen
 * centering/scale and left-clips ScrollingList rows. Compute absolute corners from
 * the pose, then enableScissor under an identity pose so the clip matches drawing.
 */
public class ScrollingContainer extends View
{
    private static final int PERCENT_90 = 90;
    private static final int PERCENT_FULL = 100;

    protected ScrollingView owner;
    protected double scrollY = 0;
    protected int contentHeight = 0;

    ScrollingContainer(final ScrollingView owner)
    {
        super();
        this.owner = owner;
    }

    @Override
    public void parseChildren(final PaneParams params)
    {
        super.parseChildren(params);
        computeContentHeight();
    }

    public void computeContentHeight()
    {
        contentHeight = 0;
        for (final Pane child : children)
        {
            if (child != null)
            {
                contentHeight = Math.max(contentHeight, child.getY() + child.getHeight());
            }
        }
        setScrollY(scrollY);
    }

    public void setContentHeight(final int size)
    {
        contentHeight = size;
        setScrollY(scrollY);
    }

    public int getMaxScrollY()
    {
        return Math.max(0, contentHeight - getHeight());
    }

    @Override
    public void drawSelf(final BOGuiGraphics target, final double mx, final double my)
    {
        final Matrix3x2fStack ms = target.pose();
        final Vector2f tl = ms.transformPosition(this.x, this.y, new Vector2f());
        final Vector2f br = ms.transformPosition(this.x + this.width, this.y + this.height, new Vector2f());
        final int x0 = (int) Math.floor(Math.min(tl.x, br.x));
        final int y0 = (int) Math.floor(Math.min(tl.y, br.y));
        final int x1 = (int) Math.ceil(Math.max(tl.x, br.x));
        final int y1 = (int) Math.ceil(Math.max(tl.y, br.y));

        final int oldW = this.window.getScreen().width;
        final int oldH = this.window.getScreen().height;
        this.window.getScreen().width = this.width;
        this.window.getScreen().height = this.contentHeight;

        ms.pushMatrix();
        ms.identity();
        target.enableScissor(x0, y0, x1, y1);
        ms.popMatrix();

        ms.pushMatrix();
        ms.translate(0.0F, (float) (-scrollY));
        super.drawSelf(target, mx, my + scrollY);
        ms.popMatrix();

        target.disableScissor();
        this.window.getScreen().width = oldW;
        this.window.getScreen().height = oldH;
    }

    @Override
    public void drawSelfLast(final BOGuiGraphics target, final double mx, final double my)
    {
        final Matrix3x2fStack ms = target.pose();
        ms.pushMatrix();
        ms.translate(0.0F, (float) (-scrollY));
        super.drawSelfLast(target, mx, my + scrollY);
        ms.popMatrix();
    }

    @Override
    protected boolean childIsVisible(final Pane child)
    {
        return child.getX() < getWidth() && child.getY() < getHeight() + scrollY && (child.getX() + child.getWidth()) >= 0 &&
            (child.getY() + child.getHeight()) >= scrollY;
    }

    public double getScrollY()
    {
        return scrollY;
    }

    public boolean setScrollY(final double scroll)
    {
        final double oldScroll = scrollY;
        final double maxScrollY = getMaxScrollY();
        scrollY = scroll;
        if (scrollY > maxScrollY)
        {
            scrollY = maxScrollY;
        }
        else if (scrollY < 0)
        {
            scrollY = 0;
        }
        return oldScroll != scrollY;
    }

    public int getContentHeight()
    {
        return contentHeight;
    }

    public int getScrollPageSize()
    {
        return getHeight() * PERCENT_90 / PERCENT_FULL;
    }

    public void scrollBy(final double deltaY)
    {
        setScrollY(scrollY + deltaY);
    }

    protected MutablePos2i accumulatePosition(final MutablePos2i accumulator)
    {
        if (parent != null)
        {
            parent.accumulatePosition(accumulator);
        }
        accumulator.x += x;
        accumulator.y += (int) (y - scrollY);
        return accumulator;
    }

    @Override
    public boolean mouseEventProcessor(final double mx,
        final double my,
        final MouseEventCallback panePredicate,
        final MouseEventCallback eventCallbackPositive,
        final MouseEventCallback eventCallbackNegative)
    {
        return super.mouseEventProcessor(mx, my + scrollY, panePredicate, eventCallbackPositive, eventCallbackNegative);
    }
}

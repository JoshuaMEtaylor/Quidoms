package com.minecolonies.core.client.gui.modules;

import com.ldtteam.blockui.Alignment;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.ButtonHandler;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.blockui.views.View;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.BiFunction;

/**
 * Quidoms: QuidCraft sidebar module tabs — full 84×22 chips with always-visible cream labels.
 * Label is a dedicated {@link Text} child so it cannot render invisible on ButtonImage.
 */
public class TabsWindowModule implements IWindowModule
{
    private static final int TAB_X_OFFSET      = 6;
    private static final int TAB_Y_OFFSET      = 36;
    private static final int TAB_WIDTH         = 84;
    private static final int TAB_HEIGHT        = 22;
    private static final int TAB_Y_SPACING     = 4;
    private static final int TAB_ICON_WIDTH    = 14;
    private static final int TAB_ICON_HEIGHT   = 14;
    private static final int TAB_ICON_OFFSET_X = 4;
    private static final int TAB_ICON_OFFSET_Y = 4;
    private static final int TAB_LABEL_X       = 22;
    private static final int TAB_LABEL_W       = 58;
    private static final int CREAM             = 0xFFECE8E0;
    private static final int CREAM_MUTED       = 0xFFB7B1A8;

    private final AbstractWindowSkeleton parent;
    private final Random iconRandom;

    private int tabXOffset = TAB_X_OFFSET;
    private int tabYOffset = TAB_Y_OFFSET;
    private int tabYSpacing = TAB_Y_SPACING;

    public TabsWindowModule(final AbstractWindowSkeleton parent, final Random iconRandom)
    {
        this.parent = parent;
        this.iconRandom = iconRandom;
    }

    public void setTabXOffset(final int tabXOffset)
    {
        this.tabXOffset = tabXOffset;
    }

    public void setTabYOffset(final int tabYOffset)
    {
        this.tabYOffset = tabYOffset;
    }

    public void setTabYSpacing(final int tabYSpacing)
    {
        this.tabYSpacing = tabYSpacing;
    }

    public void renderTabButton(
      final int index,
      final TabImageSide side,
      final ResourceLocation icon,
      @Nullable final MutableComponent hoverText,
      final ButtonHandler handler)
    {
        final View view = new View();
        view.setID(icon.getPath() + "_view");
        view.setPosition(side.getXPosition.apply(parent, tabXOffset), tabYOffset + ((TAB_HEIGHT + tabYSpacing) * index));
        view.setSize(TAB_WIDTH, TAB_HEIGHT);

        final ButtonImage chip = new ButtonImage();
        chip.setImage(side.getImage(iconRandom));
        chip.setSize(TAB_WIDTH, TAB_HEIGHT);
        chip.setHandler(handler);
        chip.setColors(CREAM, CREAM, CREAM_MUTED);
        chip.setTextColor(CREAM);
        chip.setTextHoverColor(CREAM);
        chip.setTextDisabledColor(CREAM_MUTED);

        final MutableComponent labelText = hoverText != null
                                             ? hoverText
                                             : Component.literal(fallbackLabel(icon));
        chip.setText(labelText);
        chip.setTextAlignment(Alignment.MIDDLE_LEFT);
        chip.setTextOffset(TAB_LABEL_X, 0);
        chip.setTextRenderBox(TAB_LABEL_W, TAB_HEIGHT);

        final ButtonImage iconImage = new ButtonImage();
        iconImage.setID(icon.getPath());
        iconImage.setImage(icon);
        iconImage.setSize(TAB_ICON_WIDTH, TAB_ICON_HEIGHT);
        iconImage.setPosition(TAB_ICON_OFFSET_X, TAB_ICON_OFFSET_Y);
        iconImage.setHandler(handler);

        final Text label = new Text();
        label.setSize(TAB_LABEL_W, TAB_HEIGHT);
        label.setPosition(TAB_LABEL_X, 0);
        label.setTextAlignment(Alignment.MIDDLE_LEFT);
        label.setColors(CREAM, CREAM, CREAM_MUTED);
        label.setTextColor(CREAM);
        label.setText(labelText);
        label.setTextScale(0.85);

        parent.addChild(view);
        view.addChild(chip);
        view.addChild(iconImage);
        view.addChild(label);

        PaneBuilders.tooltipBuilder().append(labelText).hoverPane(view).build();
    }

    private static String fallbackLabel(final ResourceLocation icon)
    {
        final String path = icon.getPath();
        final int slash = path.lastIndexOf('/');
        final String leaf = slash >= 0 ? path.substring(slash + 1) : path;
        final int dot = leaf.lastIndexOf('.');
        final String name = dot > 0 ? leaf.substring(0, dot) : leaf;
        if (name.isEmpty())
        {
            return "Tab";
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public enum TabImageSide
    {
        LEFT("left", 4, (v, tabXOffset) -> tabXOffset),
        RIGHT("right", 4, (v, tabXOffset) -> Math.max(0, v.getWidth() - tabXOffset - TAB_WIDTH));

        private final String side;
        private final int imageCount;
        private final BiFunction<BOWindow, Integer, Integer> getXPosition;

        TabImageSide(final String side, final int imageCount, final BiFunction<BOWindow, Integer, Integer> getXPosition)
        {
            this.side = side;
            this.imageCount = imageCount;
            this.getXPosition = getXPosition;
        }

        public ResourceLocation getImage(final Random random)
        {
            return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/quidcraft/nav_84x22.png");
        }
    }
}

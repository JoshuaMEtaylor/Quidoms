package com.minecolonies.core.client.gui.modules;

import com.ldtteam.blockui.Alignment;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.ButtonHandler;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.blockui.views.View;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.BiFunction;

/**
 * Quidoms: QuidCraft sidebar module tabs — full 84×22 chips with always-visible cream labels.
 */
public class TabsWindowModule implements IWindowModule
{
    private static final int TAB_X_OFFSET      = 6;  // inside QuidCraft sidebar
    private static final int TAB_Y_OFFSET      = 36; // below chrome header
    private static final int TAB_WIDTH         = 84;
    private static final int TAB_HEIGHT        = 22;
    private static final int TAB_Y_SPACING     = 4;
    private static final int TAB_ICON_WIDTH    = 14;
    private static final int TAB_ICON_HEIGHT   = 14;
    private static final int TAB_ICON_OFFSET_X = 4;
    private static final int TAB_ICON_OFFSET_Y = 4;
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

    public void renderTabButton(int index, final TabImageSide side, final ResourceLocation icon, @Nullable final MutableComponent hoverText, final ButtonHandler handler)
    {
        final View view = new View();
        view.setID(icon.getPath() + "_view");
        view.setPosition(side.getXPosition.apply(parent, tabXOffset), tabYOffset + ((TAB_HEIGHT + tabYSpacing) * index));
        view.setSize(TAB_WIDTH, TAB_HEIGHT);

        final ButtonImage chip = new ButtonImage();
        chip.setImage(side.getImage(iconRandom));
        chip.setSize(TAB_WIDTH, TAB_HEIGHT);
        chip.setHandler(handler);
        chip.setTextAlignment(Alignment.MIDDLE_LEFT);
        chip.setTextOffset(22, 0);
        chip.setColors(CREAM, CREAM, CREAM_MUTED);
        if (hoverText != null)
        {
            chip.setText(hoverText);
        }

        final ButtonImage iconImage = new ButtonImage();
        iconImage.setID(icon.getPath());
        iconImage.setImage(icon);
        iconImage.setSize(TAB_ICON_WIDTH, TAB_ICON_HEIGHT);
        iconImage.setPosition(TAB_ICON_OFFSET_X, TAB_ICON_OFFSET_Y);
        iconImage.setHandler(handler);

        parent.addChild(view);
        view.addChild(chip);
        view.addChild(iconImage);

        if (hoverText != null)
        {
            PaneBuilders.tooltipBuilder().append(hoverText).hoverPane(view).build();
        }
    }

    public enum TabImageSide
    {
        // Quidoms: LEFT tabs sit inside the 96px sidebar (absolute x = tabXOffset).
        LEFT("left", 4, (v, tabXOffset) -> tabXOffset),
        RIGHT("right", 4, (v, tabXOffset) -> v.getWidth() - tabXOffset);

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
            // QuidCraft nav chip (full hitbox); keep legacy wax as unused fallback path unused.
            return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/quidcraft/nav_84x22.png");
        }
    }
}

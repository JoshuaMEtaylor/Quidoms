package com.minecolonies.core.client.gui;

import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.Utils;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.townhall.AbstractWindowTownHall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.BannerFlagModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.minecolonies.api.util.constant.translation.BaseGameTranslationConstants.BASE_GUI_DONE;
import static net.minecraft.client.gui.components.Button.DEFAULT_NARRATION;

/**
 * A custom rendered Screen (i.e. not blockui) that renders a picker for the banners,
 * similar to a loom. The resulting banner cannot be extracted.
 */
@OnlyIn(Dist.CLIENT)
public class WindowBannerPicker extends Screen
{
    /** The Y position of the layers */
    private static final int GUI_Y = 30;

    /** The side length for layer and palette buttons */
    private static final int SIDE = 20;

    /** The height of the pattern buttons */
    private static final int PATTERN_HEIGHT = 30;

    /** The width of the pattern buttons */
    private static final int PATTERN_WIDTH = PATTERN_HEIGHT / 2;

    /** The margin after each pattern button */
    private static final int PATTERN_MARGIN = 3;

    /** The number of columns the patterns are arranged in */
    private static final int PATTERN_COLUMNS = 8;

    /** The number of rows the patterns are arranged in */
    private static final int PATTERN_ROWS = 4;


    /**
     * The list of patterns that usually require charges, or are to be made more valuable
     * by excluding them from lower TH levels. Sorted by the TH level they are first introduced at
     */
    private static final ResourceKey[][] EXCLUSION = {
            {    // 1
                BannerPatterns.GRADIENT,
                BannerPatterns.GRADIENT_UP
            }, { // 2
                BannerPatterns.BRICKS,
                BannerPatterns.FLOWER
            }, { // 3
                BannerPatterns.SKULL,
                BannerPatterns.CREEPER
            }, { // 4
                BannerPatterns.GLOBE,
                BannerPatterns.PIGLIN
            }, { // 5
                BannerPatterns.MOJANG
            }, { // Excluded completely
                BannerPatterns.BASE
            }
    };

    /** The list of banner patterns, to be excluded and cached */
    private final List<Holder<BannerPattern>> patterns;

    /** The final list of patterns and colors of the flag */
    private final List<BannerPatternLayers.Layer> layers;

    /** The colony this flag refers to */
    private final IColonyView colony;

    /** The town hall window that called this picker. Will be used to return to it. */
    private final AbstractWindowTownHall window;

    /** Flag model for the large preview (1.21.10 STANDING_BANNER_FLAG). */
    private final BannerFlagModel flagModel;

    /**
     * Local reference of feature unlocked flag.
     */
    private final AtomicBoolean isFeatureUnlocked;

    /** The currently selected palette color. */
    private ColorPalette colors;

    /** The currently selected layer. Zero is the base. */
    private int activeLayer = 0;

    /** Whether or not the player is dragging the scrollbar */
    private boolean scrolling = false;

    /** The number of rows scrolled past */
    private int scrollRow = 0;

    /**
     * @param colony            the colony to make the flag for
     * @param hallWindow        the calling town hall window to return to
     * @param isFeatureUnlocked
     */
    public WindowBannerPicker(IColonyView colony, AbstractWindowTownHall hallWindow, final AtomicBoolean isFeatureUnlocked)
    {
        super(Component.literal("Flag"));

        this.colony = colony;
        this.window = hallWindow;
        this.flagModel = new BannerFlagModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG));

        /* Get all patterns, then remove excluded and item-required patterns */
        List<Holder<BannerPattern>> exclusion = new ArrayList<>();
        for (int i = hallWindow.buildingView.getBuildingLevel(); i <= hallWindow.buildingView.getBuildingMaxLevel(); i++)
        {
            for (final ResourceKey key : EXCLUSION[i])
            {
                exclusion.add(Utils.getRegistryValue(key, colony.getWorld()));
            }
        }

        this.patterns = colony.getWorld().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).listElements().collect(Collectors.toCollection(LinkedList::new));
        this.patterns.removeAll(exclusion);
        this.isFeatureUnlocked = isFeatureUnlocked;

        // Fetch the patterns as a List and not ListNBT
        this.layers = new ArrayList<>(colony.getColonyFlag().layers());
        // Remove the extra base layer created by the above function
    }

    @Override
    protected void init()
    {
        int paletteX = center(this.width, PATTERN_COLUMNS, PATTERN_WIDTH, 0, 0) - 70;
        this.colors = new ColorPalette(paletteX, this.height/2, 2, this::addRenderableWidget);
        colors.onchange = color -> setLayer(null, color);

        createLayerButtons();
        createPatternButtons();
        createCloseButtons();
    }

    /**
     * Creates the buttons for banner layer selection; Base, 1-6, and the remove button
     */
    protected void createLayerButtons()
    {
        for (int layer = 0; layer <= 6; layer++)
        {
            int posX = (this.width - SIDE * 6) / 2 + layer * SIDE;

            this.addRenderableWidget(new LayerButton(posX, GUI_Y, SIDE, SIDE, layer));
        }

        this.addRenderableWidget(new Button(
                center(this.width, 6, SIDE, 7, 0), GUI_Y,
                SIDE, SIDE,
                Component.literal(ChatFormatting.RED + "X"),
                pressed -> layers.remove(activeLayer), DEFAULT_NARRATION)
        {
            @Override
            public void renderWidget(final GuiGraphics stack, int mouseX, int mouseY, float partialTicks)
            {
                this.active = activeLayer < layers.size() && activeLayer != 0; // TODO: port this last vital condition
                super.renderWidget(stack, mouseX, mouseY, partialTicks);
            }
        });
    }

    /**
     * Creates the buttons behind each pattern.
     */
    protected void createPatternButtons()
    {
        for (int i = 0; i < patterns.size(); i++)
        {
            int posX = center(this.width, PATTERN_COLUMNS, PATTERN_WIDTH, i % PATTERN_COLUMNS, PATTERN_MARGIN);
            int posY = center(this.height+30, PATTERN_ROWS, PATTERN_HEIGHT, Math.floorDiv(i, PATTERN_COLUMNS), PATTERN_MARGIN);

            final PatternButton button = new PatternButton(posX, posY, PATTERN_HEIGHT, patterns.get(i));
            this.addRenderableWidget(button);

            if (!isFeatureUnlocked.get() && patterns.get(i).unwrapKey().get().location().getNamespace().equals(Constants.MOD_ID))
            {
                button.setTooltip(Tooltip.create(Component.translatable("com.minecolonies.core.gui.banner.patreon")));
                button.blocked = true;
            }
        }
    }

    /**
     * Creates the Done and Cancel buttons, to return to the town hall window and save the banner or not, respectively.
     */
    protected void createCloseButtons()
    {
        this.addRenderableWidget(new Button(
                center(this.width, 2, 80, 1, 10),
                this.height - 40,
                80, SIDE,
                Component.translatableEscape(BASE_GUI_DONE),
                pressed -> {
                    BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
                    for (BannerPatternLayers.Layer layer : layers)
                        builder.add(layer);

                    colony.setColonyFlag(builder.build());
                    window.open();
                }, DEFAULT_NARRATION
        ));
        this.addRenderableWidget(new Button(
                center(this.width, 2, 80, 0, 10),
                this.height - 40,
                80, SIDE,
                Component.translatableEscape("gui.cancel"),
                pressed -> window.open(), DEFAULT_NARRATION
        ));
    }

    /**
     * Positions a button within a grid based on the center coordinates of that grid.
     * This method is Axis agnostic.
     * @param length the length of the grid
     * @param count the number of items along that length
     * @param side the side length of the items in the relevant axis
     * @param n the nth item we are positioning
     * @param margin the gap between elements, half of this gap length borders the hole grid
     * @return the coordinate along the relevant axis
     */
    public static int center(int length, int count, int side, int n, int margin)
    {
        return (length - count * (side + margin)) / 2 + n * (side + margin) + margin / 2;
    }

    /**
     * Tries to set the layer in the banner pattern list with the given information
     * @param pattern the pattern to set in the layer. Uses the existing or BASE if null
     * @param color the associated color for the pattern
     */
    public void setLayer(@Nullable Holder<BannerPattern> pattern, DyeColor color)
    {
        if (pattern == null)
        {
            // Drop out if only the color was selected.
            if (activeLayer == layers.size()) return;
            else if (activeLayer == 0) pattern = Utils.getRegistryValue(BannerPatterns.BASE, colony.getWorld());
            else pattern = layers.get(activeLayer).pattern();
        }

        if (activeLayer == layers.size())
            layers.add(new BannerPatternLayers.Layer(pattern, color));
        else
            layers.set(activeLayer, new BannerPatternLayers.Layer(pattern, color));
    }

    @Override
    public void render(final GuiGraphics stack, int mouseX, int mouseY, float partialTicks)
    {
        super.render(stack, mouseX, mouseY, partialTicks);
        drawFlag(stack);

        // Draw the scrollbar
        int scrollRows = (int) (Math.ceil(this.patterns.size() / (float) PATTERN_COLUMNS) - PATTERN_ROWS);
        if (scrollRows > 0 && activeLayer > 0)
        {
            int trackHeight = (PATTERN_HEIGHT + PATTERN_MARGIN) * PATTERN_ROWS;
            double barHeight = trackHeight * (PATTERN_ROWS / (float)(scrollRows + PATTERN_ROWS));
            int trackX = center(this.width, PATTERN_COLUMNS, PATTERN_WIDTH, PATTERN_COLUMNS, PATTERN_MARGIN);
            int trackY = (int) (center(this.height, PATTERN_ROWS, PATTERN_HEIGHT, 0, PATTERN_MARGIN)
                                + this.scrollRow * (trackHeight / (float)(scrollRows + PATTERN_ROWS)));

            stack.fill(trackX+2, trackY, trackX+6, trackY+ (int) barHeight, 0xBBFFFFFF);
        }


        // Render the instructions
        stack.drawCenteredString(this.font, Component.translatableEscape("com.minecolonies.coremod.gui.flag.choose").getString(), this.width / 2, 16, 0xFFFFFF);
        if (activeLayer == 0)
        {
            stack.drawCenteredString(this.font,
              Component.translatableEscape("com.minecolonies.core.gui.flag.pick_layer").getString(),
              this.width / 2, this.height / 2 - 10, 0xFFCCCCCC);
        }
    }

    /**
     * Sets the large final preview of the banner for rendering
     */
    private void drawFlag(final GuiGraphics stack)
    {
        final int posX = (int) ((this.width + PATTERN_HEIGHT / 2.0 * PATTERN_COLUMNS) / 2 + SIDE * 2);
        final int posY = this.height / 2 - 40;
        final DyeColor base = layers.isEmpty() ? DyeColor.WHITE : layers.get(0).color();
        final BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
        for (int i = 1; i < layers.size(); i++)
        {
            builder.add(layers.get(i));
        }
        stack.submitBannerPatternRenderState(flagModel, base, builder.build(), posX, posY, posX + 40, posY + 80);
    }

    /**
     * Draw a single banner pattern sprite into a pattern button cell (loom-style).
     */
    private void drawBannerPattern(Holder<BannerPattern> pattern, int x, int y, GuiGraphics stack)
    {
        final boolean locked = !isFeatureUnlocked.get() && pattern.unwrapKey().isPresent()
          && pattern.unwrapKey().get().location().getNamespace().equals(Constants.MOD_ID);
        stack.fill(x, y, x + PATTERN_WIDTH, y + PATTERN_HEIGHT, locked ? 0xFF222222 : 0xFF555555);
        try
        {
            final Material material = Sheets.getBannerMaterial(pattern);
            final TextureAtlasSprite sprite = stack.getSprite(material);
            // Tint: locked patterns stay dark; others use current palette / white.
            final int tint = locked ? 0xFF000000 : (0xFF000000 | colors.getSelected().getTextureDiffuseColor());
            stack.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 1, y + 1, PATTERN_WIDTH - 2, PATTERN_HEIGHT - 2, tint);
        }
        catch (final Exception ex)
        {
            // Fallback solid if atlas lookup fails
            stack.fill(x + 2, y + 2, x + PATTERN_WIDTH - 2, y + PATTERN_HEIGHT - 2, 0xFFFFFFFF);
            Log.getLogger().debug("Banner pattern sprite missing: {}", pattern.value().assetId(), ex);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (activeLayer > 0) {
            this.scrollRow = (int) Mth.clamp(
                    this.scrollRow - scrollY,
                    0,
                    Math.ceil(this.patterns.size() / PATTERN_COLUMNS) - PATTERN_ROWS + 1 // Extra 1 so it is inclusive
            );
        }

        return true;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean isDoubleClick)
    {
        this.scrolling = false;
        final double mouseX = event.x();
        final double mouseY = event.y();

        int trackX = center(this.width, PATTERN_COLUMNS, PATTERN_WIDTH, PATTERN_COLUMNS, PATTERN_MARGIN);
        int trackY = center(this.height, PATTERN_ROWS, PATTERN_HEIGHT, 0, PATTERN_MARGIN);
        int trackEnd = trackY + PATTERN_ROWS*(PATTERN_HEIGHT + PATTERN_MARGIN);
        if (mouseX > trackX + 2 && mouseX < trackX + 8 && mouseY > trackY && mouseY < trackEnd)
            this.scrolling = true;
        
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double deltaX, final double deltaY)
    {
        if (this.scrolling && this.activeLayer > 0) {

            int trackStart = center(this.height, PATTERN_ROWS, PATTERN_HEIGHT, 0, PATTERN_MARGIN);
            int trackLength = PATTERN_ROWS*(PATTERN_HEIGHT + PATTERN_MARGIN);

            double scrollRatio = Mth.clamp(
                    (event.y() - trackStart) / trackLength,
                    0, 1
            );
            this.scrollRow = (int) Math.round(scrollRatio * (Math.ceil(this.patterns.size() / PATTERN_COLUMNS) - PATTERN_ROWS + 1));

            return true;
        } else {
            return super.mouseDragged(event, deltaX, deltaY);
        }
    }

    /**
     * A custom button for each layer, to override click and render logic
     */
    public class LayerButton extends Button
    {
        private final int layer;

        /**
         * @param x the left x position of the button
         * @param y the top y position of the button
         * @param width the width of the button. Probably 20. Overridden if layer is 0.
         * @param height the height of the button. Probably 20.
         * @param layer the layer this button represents.
         */
        public LayerButton(int x, int y, int width, int height, int layer)
        {
            super(
                    x - (layer == 0 ? width*2 : 0), y,
                    width * (layer == 0 ? 3 : 1), height,
                    layer == 0
                            ? Component.translatableEscape("com.minecolonies.coremod.gui.flag.base_layer")
                            : Component.literal(String.valueOf(layer)),
                    pressed -> {},
                    DEFAULT_NARRATION
            );
            this.layer = layer;
        }

        @Override
        public void onPress(final InputWithModifiers input)
        {
            activeLayer = this.layer;

            if (this.layer >= layers.size())
                colors.setSelected(layers.get(0).color().equals(DyeColor.BLACK) ? DyeColor.WHITE : DyeColor.BLACK);
            else
                colors.setSelected(layers.get(activeLayer).color());
        }

        @Override
        public void renderWidget(final GuiGraphics stack, int p_render_1_, int p_render_2_, float p_render_3_)
        {
            this.active = this.layer <= layers.size();
            super.renderWidget(stack, p_render_1_, p_render_2_, p_render_3_);

            if (activeLayer == this.layer)
                stack.fill(this.getX(), this.getY(), this.getX()+this.width, this.getY()+this.height, 0x66DD99FF);
        }
    }

    /**
     * A custom button for each pattern, to override click and render logic
     */
    public class PatternButton extends Button
    {
        private final Holder<BannerPattern> pattern;
        private int index = -1;
        private boolean blocked = false;

        /**
         * @param x the left x position of the button
         * @param y the top y position of the button
         * @param height the height of the button. Twice the width, always
         * @param pattern the pattern this button represents
         */
        public PatternButton(int x, int y, int height, Holder<BannerPattern> pattern)
        {
            super(x, y, height/2, height, Component.literal(""), btn -> {}, DEFAULT_NARRATION);
            this.pattern = pattern;
            int tempIndex = 0;
            for (final Holder<BannerPattern> pat : WindowBannerPicker.this.patterns)
            {
                if (pat.value().assetId().equals(pattern.value().assetId()))
                {
                    this.index = tempIndex;
                    break;
                }
                tempIndex++;
            }
        }

        @Override
        public void onPress(final InputWithModifiers input)
        {
            if (!this.blocked)
            {
                setLayer(this.pattern, colors.getSelected());
            }
        }

        @Override
        public void renderWidget(final GuiGraphics stack, int mx, int my, float p_renderButton_3_)
        {
            boolean isVisible = scrollRow * PATTERN_COLUMNS <= this.index && this.index < PATTERN_COLUMNS * (scrollRow + PATTERN_ROWS);
            // Clickable only when editing a pattern layer (not Base).
            this.active = activeLayer != 0 && !this.blocked;

            if (!this.visible || !isVisible)
            {
                return;
            }

            int position = Math.floorDiv(this.index - scrollRow * PATTERN_COLUMNS, PATTERN_COLUMNS);
            this.setY(center(WindowBannerPicker.this.height, PATTERN_ROWS, PATTERN_HEIGHT, position, PATTERN_MARGIN));
            this.isHovered = mx >= this.getX() && my >= this.getY() && mx < this.getX() + this.width && my < this.getY() + this.height;

            // Always draw pattern sprites (even on Base layer) so the list is never an empty gray field.
            try
            {
                drawBannerPattern(this.pattern, this.getX(), this.getY(), stack);
            }
            catch (final Exception ex)
            {
                Log.getLogger().warn(pattern.value().translationKey());
                Log.getLogger().error(ex);
            }

            if (this.blocked)
            {
                stack.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x99000000);
            }
            else if (this.isHovered && this.active)
            {
                stack.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x66FFFFFF);
            }

            if (activeLayer > 0 && activeLayer < layers.size() && layers.get(activeLayer).pattern() == this.pattern)
            {
                stack.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x66DD88FF);
            }
        }
    }
}

package com.minecolonies.core.client.gui;

import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.Utils;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.townhall.AbstractWindowTownHall;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.BannerFlagModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
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
 * Loom-like colony flag picker. Layout matches upstream MineColonies:
 * Base + layers 1–6, dye palette, pattern grid (when a pattern layer is active),
 * live banner preview, Done/Cancel.
 */
@OnlyIn(Dist.CLIENT)
public class WindowBannerPicker extends Screen
{
    private static final int GUI_Y = 30;
    private static final int SIDE = 20;
    private static final int PATTERN_HEIGHT = 30;
    private static final int PATTERN_WIDTH = PATTERN_HEIGHT / 2;
    private static final int PATTERN_MARGIN = 3;
    private static final int PATTERN_COLUMNS = 8;
    private static final int PATTERN_ROWS = 4;

    /** Banner pattern atlas is 64×64; loom samples the flag face at 21×40. */
    private static final float BANNER_TEX = 64.0F;
    private static final float BANNER_FACE_W = 21.0F;
    private static final float BANNER_FACE_H = 40.0F;

    private static final ResourceKey[][] EXCLUSION = {
            { BannerPatterns.GRADIENT, BannerPatterns.GRADIENT_UP },
            { BannerPatterns.BRICKS, BannerPatterns.FLOWER },
            { BannerPatterns.SKULL, BannerPatterns.CREEPER },
            { BannerPatterns.GLOBE, BannerPatterns.PIGLIN },
            { BannerPatterns.MOJANG },
            { BannerPatterns.BASE }
    };

    private final List<Holder<BannerPattern>> patterns;
    private final List<BannerPatternLayers.Layer> layers;
    private final IColonyView colony;
    private final AbstractWindowTownHall window;
    private final BannerFlagModel flagModel;
    private final AtomicBoolean isFeatureUnlocked;

    private ColorPalette colors;
    private int activeLayer = 0;
    private boolean scrolling = false;
    private int scrollRow = 0;

    public WindowBannerPicker(IColonyView colony, AbstractWindowTownHall hallWindow, final AtomicBoolean isFeatureUnlocked)
    {
        super(Component.literal("Flag"));

        this.colony = colony;
        this.window = hallWindow;
        this.flagModel = new BannerFlagModel(
          Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG));

        final List<Holder<BannerPattern>> exclusion = new ArrayList<>();
        for (int i = hallWindow.buildingView.getBuildingLevel(); i <= hallWindow.buildingView.getBuildingMaxLevel(); i++)
        {
            for (final ResourceKey key : EXCLUSION[i])
            {
                exclusion.add(Utils.getRegistryValue(key, colony.getWorld()));
            }
        }

        this.patterns = colony.getWorld().registryAccess()
          .lookupOrThrow(Registries.BANNER_PATTERN)
          .listElements()
          .collect(Collectors.toCollection(LinkedList::new));
        this.patterns.removeAll(exclusion);
        this.isFeatureUnlocked = isFeatureUnlocked;

        this.layers = new ArrayList<>(colony.getColonyFlag().layers());
        if (this.layers.isEmpty())
        {
            this.layers.add(new BannerPatternLayers.Layer(
              Utils.getRegistryValue(BannerPatterns.BASE, colony.getWorld()), DyeColor.WHITE));
        }
    }

    @Override
    protected void init()
    {
        // ColorPalette treats (x,y) as the palette *center*.
        final int paletteX = center(this.width, PATTERN_COLUMNS, PATTERN_WIDTH, 0, 0) - 70;
        this.colors = new ColorPalette(paletteX, this.height / 2, 2, this::addRenderableWidget);
        colors.onchange = color -> setLayer(null, color);

        createLayerButtons();
        createPatternButtons();
        createCloseButtons();
    }

    protected void createLayerButtons()
    {
        for (int layer = 0; layer <= 6; layer++)
        {
            final int posX = (this.width - SIDE * 6) / 2 + layer * SIDE;
            this.addRenderableWidget(new LayerButton(posX, GUI_Y, SIDE, SIDE, layer));
        }

        // Layer-remove control (not window close — Done/Cancel close).
        this.addRenderableWidget(new Button(
          center(this.width, 6, SIDE, 7, 0), GUI_Y,
          SIDE, SIDE,
          Component.literal(ChatFormatting.RED + "X"),
          pressed -> {
              if (activeLayer > 0 && activeLayer < layers.size())
              {
                  layers.remove(activeLayer);
                  if (activeLayer >= layers.size())
                  {
                      activeLayer = Math.max(0, layers.size() - 1);
                  }
              }
          }, DEFAULT_NARRATION)
        {
            @Override
            public void renderWidget(final GuiGraphics stack, int mouseX, int mouseY, float partialTicks)
            {
                this.active = activeLayer > 0 && activeLayer < layers.size();
                super.renderWidget(stack, mouseX, mouseY, partialTicks);
            }
        });
    }

    protected void createPatternButtons()
    {
        for (int i = 0; i < patterns.size(); i++)
        {
            final int posX = center(this.width, PATTERN_COLUMNS, PATTERN_WIDTH, i % PATTERN_COLUMNS, PATTERN_MARGIN);
            final int posY = center(this.height + 30, PATTERN_ROWS, PATTERN_HEIGHT, Math.floorDiv(i, PATTERN_COLUMNS), PATTERN_MARGIN);

            final PatternButton button = new PatternButton(posX, posY, PATTERN_HEIGHT, patterns.get(i));
            this.addRenderableWidget(button);

            if (!isFeatureUnlocked.get() && patterns.get(i).unwrapKey().get().location().getNamespace().equals(Constants.MOD_ID))
            {
                button.setTooltip(Tooltip.create(Component.translatable("com.minecolonies.core.gui.banner.patreon")));
                button.blocked = true;
            }
        }
    }

    protected void createCloseButtons()
    {
        this.addRenderableWidget(new Button(
          center(this.width, 2, 80, 1, 10),
          this.height - 40,
          80, SIDE,
          Component.translatableEscape(BASE_GUI_DONE),
          pressed -> {
              final BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
              for (final BannerPatternLayers.Layer layer : layers)
              {
                  builder.add(layer);
              }
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

    public static int center(int length, int count, int side, int n, int margin)
    {
        return (length - count * (side + margin)) / 2 + n * (side + margin) + margin / 2;
    }

    public void setLayer(@Nullable Holder<BannerPattern> pattern, DyeColor color)
    {
        if (pattern == null)
        {
            if (activeLayer == layers.size())
            {
                return;
            }
            else if (activeLayer == 0)
            {
                pattern = Utils.getRegistryValue(BannerPatterns.BASE, colony.getWorld());
            }
            else
            {
                pattern = layers.get(activeLayer).pattern();
            }
        }

        // Base layer is always the BASE pattern; only its dye changes.
        if (activeLayer == 0)
        {
            pattern = Utils.getRegistryValue(BannerPatterns.BASE, colony.getWorld());
        }

        if (activeLayer == layers.size())
        {
            layers.add(new BannerPatternLayers.Layer(pattern, color));
        }
        else
        {
            layers.set(activeLayer, new BannerPatternLayers.Layer(pattern, color));
        }
    }

    @Override
    public void render(final GuiGraphics stack, int mouseX, int mouseY, float partialTicks)
    {
        super.render(stack, mouseX, mouseY, partialTicks);
        drawFlag(stack);

        final int scrollRows = (int) (Math.ceil(this.patterns.size() / (float) PATTERN_COLUMNS) - PATTERN_ROWS);
        if (scrollRows > 0 && activeLayer > 0)
        {
            final int trackHeight = (PATTERN_HEIGHT + PATTERN_MARGIN) * PATTERN_ROWS;
            final double barHeight = trackHeight * (PATTERN_ROWS / (float) (scrollRows + PATTERN_ROWS));
            final int trackX = center(this.width, PATTERN_COLUMNS, PATTERN_WIDTH, PATTERN_COLUMNS, PATTERN_MARGIN);
            final int trackY = (int) (center(this.height, PATTERN_ROWS, PATTERN_HEIGHT, 0, PATTERN_MARGIN)
              + this.scrollRow * (trackHeight / (float) (scrollRows + PATTERN_ROWS)));
            stack.fill(trackX + 2, trackY, trackX + 6, trackY + (int) barHeight, 0xBBFFFFFF);
        }

        // Title only — do not draw helper text over the pattern grid.
        stack.drawCenteredString(this.font,
          Component.translatableEscape("com.minecolonies.coremod.gui.flag.choose").getString(),
          this.width / 2, 16, 0xFFFFFF);
        if (activeLayer == 0)
        {
            // Sit just under the layer tabs, above the (hidden) pattern grid.
            stack.drawCenteredString(this.font,
              Component.translatableEscape("com.minecolonies.core.gui.flag.pick_layer").getString(),
              this.width / 2, GUI_Y + SIDE + 6, 0xFFAAAAAA);
        }
    }

    private void drawFlag(final GuiGraphics stack)
    {
        final int posX = (int) ((this.width + PATTERN_HEIGHT / 2.0 * PATTERN_COLUMNS) / 2 + SIDE * 2);
        final int posY = this.height / 2 - 40;
        final DyeColor base = layers.get(0).color();
        final BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
        for (int i = 1; i < layers.size(); i++)
        {
            builder.add(layers.get(i));
        }
        this.flagModel.setupAnim(0.0F);
        stack.submitBannerPatternRenderState(flagModel, base, builder.build(), posX, posY, posX + 40, posY + 80);
    }

    /**
     * Loom-style pattern cell: gray face + UV-cropped banner pattern sprite.
     * {@link GuiGraphics#blit(net.minecraft.resources.ResourceLocation, int, int, int, int, float, float, float, float)}
     * takes <b>corner</b> coords (x0,y0,x1,y1), not x/y/w/h.
     */
    private void drawBannerPattern(Holder<BannerPattern> pattern, int x, int y, GuiGraphics stack)
    {
        final boolean locked = !isFeatureUnlocked.get() && pattern.unwrapKey().isPresent()
          && pattern.unwrapKey().get().location().getNamespace().equals(Constants.MOD_ID);

        // Cell chrome (replaces the old 3D mini-banner frame).
        stack.fill(x, y, x + PATTERN_WIDTH, y + PATTERN_HEIGHT, 0xFF2A2A2A);
        stack.fill(x + 1, y + 1, x + PATTERN_WIDTH - 1, y + PATTERN_HEIGHT - 1,
          locked ? 0xFF111111 : (0xFF000000 | DyeColor.GRAY.getTextureDiffuseColor()));

        final int pad = 2;
        final int x0 = x + pad;
        final int y0 = y + pad;
        final int x1 = x + PATTERN_WIDTH - pad;
        final int y1 = y + PATTERN_HEIGHT - pad;

        try
        {
            final Material material = Sheets.getBannerMaterial(pattern);
            final TextureAtlasSprite sprite = stack.getSprite(material);
            final float u0 = sprite.getU0();
            final float u1 = u0 + (sprite.getU1() - u0) * (BANNER_FACE_W / BANNER_TEX);
            final float vSpan = sprite.getV1() - sprite.getV0();
            final float v0 = sprite.getV0() + vSpan / BANNER_TEX;
            final float v1 = v0 + vSpan * (BANNER_FACE_H / BANNER_TEX);

            // CRITICAL: blit corners, not width/height — wrong args painted a fullscreen white box.
            stack.blit(sprite.atlasLocation(), x0, y0, x1, y1, u0, u1, v0, v1);

            if (locked)
            {
                stack.fill(x0, y0, x1, y1, 0x99000000);
            }
        }
        catch (final Exception ex)
        {
            stack.fill(x0, y0, x1, y1, 0xFFFFFFFF);
            Log.getLogger().debug("Banner pattern sprite missing: {}", pattern.value().assetId(), ex);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (activeLayer > 0)
        {
            this.scrollRow = (int) Mth.clamp(
              this.scrollRow - scrollY,
              0,
              Math.ceil(this.patterns.size() / (double) PATTERN_COLUMNS) - PATTERN_ROWS + 1);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean isDoubleClick)
    {
        this.scrolling = false;
        final double mouseX = event.x();
        final double mouseY = event.y();

        final int trackX = center(this.width, PATTERN_COLUMNS, PATTERN_WIDTH, PATTERN_COLUMNS, PATTERN_MARGIN);
        final int trackY = center(this.height, PATTERN_ROWS, PATTERN_HEIGHT, 0, PATTERN_MARGIN);
        final int trackEnd = trackY + PATTERN_ROWS * (PATTERN_HEIGHT + PATTERN_MARGIN);
        if (mouseX > trackX + 2 && mouseX < trackX + 8 && mouseY > trackY && mouseY < trackEnd)
        {
            this.scrolling = true;
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double deltaX, final double deltaY)
    {
        if (this.scrolling && this.activeLayer > 0)
        {
            final int trackStart = center(this.height, PATTERN_ROWS, PATTERN_HEIGHT, 0, PATTERN_MARGIN);
            final int trackLength = PATTERN_ROWS * (PATTERN_HEIGHT + PATTERN_MARGIN);
            final double scrollRatio = Mth.clamp((event.y() - trackStart) / trackLength, 0, 1);
            this.scrollRow = (int) Math.round(scrollRatio * (Math.ceil(this.patterns.size() / (double) PATTERN_COLUMNS) - PATTERN_ROWS + 1));
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    public class LayerButton extends Button
    {
        private final int layer;

        public LayerButton(int x, int y, int width, int height, int layer)
        {
            super(
              x - (layer == 0 ? width * 2 : 0), y,
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
            {
                colors.setSelected(layers.get(0).color().equals(DyeColor.BLACK) ? DyeColor.WHITE : DyeColor.BLACK);
            }
            else
            {
                colors.setSelected(layers.get(activeLayer).color());
            }
        }

        @Override
        public void renderWidget(final GuiGraphics stack, int mouseX, int mouseY, float partial)
        {
            this.active = this.layer <= layers.size();
            super.renderWidget(stack, mouseX, mouseY, partial);
            if (activeLayer == this.layer)
            {
                stack.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x66DD99FF);
            }
        }
    }

    public class PatternButton extends Button
    {
        private final Holder<BannerPattern> pattern;
        private int index = -1;
        private boolean blocked = false;

        public PatternButton(int x, int y, int height, Holder<BannerPattern> pattern)
        {
            super(x, y, height / 2, height, Component.literal(""), btn -> {}, DEFAULT_NARRATION);
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
            if (!this.blocked && activeLayer > 0)
            {
                setLayer(this.pattern, colors.getSelected());
            }
        }

        @Override
        public void renderWidget(final GuiGraphics stack, int mx, int my, float partial)
        {
            final boolean isVisible = scrollRow * PATTERN_COLUMNS <= this.index
              && this.index < PATTERN_COLUMNS * (scrollRow + PATTERN_ROWS);
            // Upstream behavior: pattern grid is inactive/hidden on Base.
            this.active = activeLayer != 0 && !this.blocked;

            if (!this.active || !this.visible || !isVisible)
            {
                return;
            }

            final int position = Math.floorDiv(this.index - scrollRow * PATTERN_COLUMNS, PATTERN_COLUMNS);
            this.setY(center(WindowBannerPicker.this.height, PATTERN_ROWS, PATTERN_HEIGHT, position, PATTERN_MARGIN));
            this.isHovered = mx >= this.getX() && my >= this.getY()
              && mx < this.getX() + this.width && my < this.getY() + this.height;

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
                stack.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000);
            }
            else if (this.isHovered)
            {
                stack.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x66FFFFFF);
            }

            if (activeLayer < layers.size() && layers.get(activeLayer).pattern() == this.pattern)
            {
                stack.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x66DD88FF);
            }
        }
    }
}

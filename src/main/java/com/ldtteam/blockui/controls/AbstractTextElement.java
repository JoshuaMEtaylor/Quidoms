package com.ldtteam.blockui.controls;

import com.ldtteam.blockui.Alignment;
import com.ldtteam.blockui.BOGuiGraphics;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneParams;
import com.ldtteam.blockui.util.SpacerTextComponent;
import com.ldtteam.blockui.util.ToggleableTextComponent;
import com.ldtteam.blockui.util.SpacerTextComponent.FormattedSpacerComponent;
import com.ldtteam.blockui.util.ToggleableTextComponent.FormattedToggleableCharSequence;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

public abstract class AbstractTextElement extends Pane {
   public static final float FILTERING_MAX_SCALE = 2.0F;
   public static final float FILTERING_THRESHOLD = 0.02F;
   public static final double DEFAULT_TEXT_SCALE = 1.0;
   public static final Alignment DEFAULT_TEXT_ALIGNMENT = Alignment.MIDDLE_LEFT;
   public static final int DEFAULT_TEXT_COLOR = 0xffffffff;
   public static final boolean DEFAULT_TEXT_SHADOW = false;
   public static final boolean DEFAULT_TEXT_WRAP = false;
   public static final int DEFAULT_TEXT_LINESPACE = 0;
   public static final int SIZE_FOR_UNLIMITED_ELEMENTS = 1000000;
   protected double textScale = 1.0;
   protected Alignment textAlignment = DEFAULT_TEXT_ALIGNMENT;
   protected int textColor = DEFAULT_TEXT_COLOR;
   protected int textHoverColor = DEFAULT_TEXT_COLOR;
   protected int textDisabledColor = DEFAULT_TEXT_COLOR;
   protected boolean textShadow = false;
   protected boolean textWrap = false;
   protected int textLinespace = 0;
   protected List<MutableComponent> text;
   protected List<FormattedCharSequence> preparedText;
   protected int renderedTextWidth;
   protected int renderedTextHeight;
   protected int textOffsetX = 0;
   protected int textOffsetY = 0;
   protected int textWidth = this.width;
   protected int textHeight = this.height;

   public AbstractTextElement() {
      this(DEFAULT_TEXT_ALIGNMENT, DEFAULT_TEXT_COLOR, DEFAULT_TEXT_COLOR, DEFAULT_TEXT_COLOR, false, false);
   }

   public AbstractTextElement(
      Alignment defaultTextAlignment,
      int defaultTextColor,
      int defaultTextHoverColor,
      int defaultTextDisabledColor,
      boolean defaultTextShadow,
      boolean defaultTextWrap
   ) {
      this.textAlignment = defaultTextAlignment;
      this.textColor = ensureOpaque(defaultTextColor);
      this.textHoverColor = ensureOpaque(defaultTextHoverColor);
      this.textDisabledColor = ensureOpaque(defaultTextDisabledColor);
      this.textShadow = defaultTextShadow;
      this.textWrap = defaultTextWrap;
   }

   public AbstractTextElement(PaneParams params) {
      this(params, DEFAULT_TEXT_ALIGNMENT, DEFAULT_TEXT_COLOR, DEFAULT_TEXT_COLOR, DEFAULT_TEXT_COLOR, false, false);
   }

   public AbstractTextElement(
      PaneParams params,
      Alignment defaultTextAlignment,
      int defaultTextColor,
      int defaultTextHoverColor,
      int defaultTextDisabledColor,
      boolean defaultTextShadow,
      boolean defaultTextWrap
   ) {
      super(params);
      this.textAlignment = (Alignment)params.getEnum("textalign", Alignment.class, defaultTextAlignment);
      if (params.hasAttribute("color")) {
         this.setColors(params.getColor("color", defaultTextColor));
      } else {
         this.textColor = ensureOpaque(params.getColor("textcolor", defaultTextColor));
         this.textHoverColor = ensureOpaque(params.getColor("texthovercolor", defaultTextHoverColor));
         this.textDisabledColor = ensureOpaque(params.getColor("textdisabledcolor", defaultTextDisabledColor));
      }

      this.textShadow = params.getBoolean("shadow", defaultTextShadow);
      this.textWrap = params.getBoolean("wrap", defaultTextWrap);
      this.textScale = params.getDouble("textscale", this.textScale);
      this.textLinespace = params.getInteger("linespace", this.textLinespace);
      this.text = params.getMultilineText(params.hasAnyAttribute("label", new String[]{"text"}));
   }

   protected void recalcTextRendering() {
      if (!(this.textScale <= 0.0) && this.textWidth >= 1 && this.textHeight >= 1 && !this.isTextEmpty()) {
         int maxWidth = (int)(this.textWidth / this.textScale) - (this.textShadow ? 1 : 0);
         this.preparedText = this.text.stream().flatMap(textBlock -> this.toFormattedSequence(maxWidth, textBlock)).collect(Collectors.toList());
      } else {
         this.preparedText = Collections.emptyList();
      }
   }

   private Stream<? extends FormattedCharSequence> toFormattedSequence(int maxWidth, MutableComponent textBlock) {
      if (textBlock.getContents() instanceof SpacerTextComponent spacer) {
         return Stream.of(spacer.getVisualOrderText());
      } else if (textBlock.getContents() instanceof ToggleableTextComponent toggleable) {
         return this.toFormattedSequence(maxWidth, toggleable.data()).map(formatted -> new FormattedToggleableCharSequence(toggleable.condition(), formatted));
      } else {
         return textBlock.getContents() == Component.empty().getContents() && textBlock.getSiblings().isEmpty()
            ? Stream.of(textBlock.getVisualOrderText())
            : this.mc.font.split(textBlock, maxWidth).stream();
      }
   }

   public void recalcPreparedTextBox() {
      if (this.textWrap) {
         int maxHeight = (int)(this.textHeight / this.textScale) + 1;
         int lineHeight = 9 + this.textLinespace;
         this.preparedText = this.preparedText.subList(0, Math.min(this.preparedText.size(), maxHeight / lineHeight));
         int heightSum = 0;
         int widthMax = 0;

         for (FormattedCharSequence textBlock : this.preparedText) {
            if (textBlock instanceof FormattedSpacerComponent spacer) {
               heightSum += spacer.pixelHeight() + this.textLinespace;
            } else if (textBlock instanceof FormattedToggleableCharSequence toggleable) {
               if (toggleable.condition().getAsBoolean()) {
                  heightSum += lineHeight;
                  widthMax = Math.max(widthMax, this.mc.font.width(toggleable.data()));
               }
            } else {
               heightSum += lineHeight;
               widthMax = Math.max(widthMax, this.mc.font.width(textBlock));
            }
         }

         this.renderedTextWidth = (int)(widthMax * this.textScale);
         this.renderedTextHeight = (int)((Math.min(heightSum, maxHeight) - 1 - this.textLinespace) * this.textScale);
      } else {
         this.preparedText = this.preparedText.subList(0, 1);
         this.renderedTextWidth = (int)(this.mc.font.width(this.preparedText.get(0)) * this.textScale);
         this.renderedTextHeight = (int)((9 - 1) * this.textScale);
      }
   }

   public void drawSelf(BOGuiGraphics ms, double mx, double my) {
      if (!this.preparedText.isEmpty()) {
         this.recalcPreparedTextBox();
         this.innerDrawSelf(ms, mx, my);
      }
   }


   /**
    * NeoForge 1.21.10 GuiGraphics.drawString skips glyphs when ARGB alpha is 0.
    * Legacy BlockUI/Minecraft colors are often RGB-only (e.g. 0xFFFFFF, ChatFormatting).
    * Treat alpha 0 as fully opaque so upstream XML/Java keep working.
    */
   protected static int ensureOpaque(final int color)
   {
      return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
   }

   protected void innerDrawSelf(BOGuiGraphics target, double mx, double my) {
      Matrix3x2fStack ms = target.pose();
      int color = ensureOpaque(this.isEnabled() ? (this.wasCursorInPane ? this.textHoverColor : this.textColor) : this.textDisabledColor);
      int offsetX = this.textOffsetX;
      int offsetY = this.textOffsetY;
      if (this.textAlignment.isRightAligned()) {
         offsetX += this.textWidth - this.renderedTextWidth;
      } else if (this.textAlignment.isHorizontalCentered()) {
         offsetX += (this.textWidth - this.renderedTextWidth) / 2;
      }

      if (this.textAlignment.isBottomAligned()) {
         offsetY += this.textHeight - this.renderedTextHeight;
      } else if (this.textAlignment.isVerticalCentered()) {
         offsetY += (this.textHeight - this.renderedTextHeight) / 2;
      }

      ms.pushMatrix();
      ms.translate(this.x + offsetX, this.y + offsetY);
      ms.scale((float)this.textScale, (float)this.textScale);
      int lineShift = 0;

      for (FormattedCharSequence row : this.preparedText) {
         if (row == FormattedCharSequence.EMPTY) {
            lineShift += 9 + this.textLinespace;
         } else {
            if (row instanceof FormattedToggleableCharSequence toggleable) {
               if (!toggleable.condition().getAsBoolean()) {
                  continue;
               }

               row = toggleable.data();
            } else if (row instanceof FormattedSpacerComponent spacer) {
               lineShift += spacer.pixelHeight() + this.textLinespace;
               continue;
            }

            int xOffset;
            if (this.textAlignment.isRightAligned()) {
               xOffset = (int)((this.renderedTextWidth - this.mc.font.width(row) * this.textScale) / this.textScale);
            } else if (this.textAlignment.isHorizontalCentered()) {
               xOffset = (int)((this.renderedTextWidth - this.mc.font.width(row) * this.textScale) / 2.0 / this.textScale);
            } else {
               xOffset = 0;
            }

            target.drawString(this.mc.font, row, xOffset, lineShift, color, this.textShadow);
            lineShift += 9 + this.textLinespace;
         }
      }

      ms.popMatrix();
   }

   public Alignment getTextAlignment() {
      return this.textAlignment;
   }

   public void setTextAlignment(Alignment textAlignment) {
      this.textAlignment = textAlignment;
   }

   public double getTextScale() {
      return this.textScale;
   }

   public void setTextScale(double textScale) {
      this.textScale = textScale;
      this.recalcTextRendering();
   }

   public void setColors(int color) {
      this.setColors(color, color, color);
   }

   public void setColors(int textColor, int textDisabledColor, int textHoverColor) {
      this.textColor = ensureOpaque(textColor);
      this.textDisabledColor = ensureOpaque(textDisabledColor);
      this.textHoverColor = ensureOpaque(textHoverColor);
   }

   public int getTextColor() {
      return this.textColor;
   }

   public void setTextColor(int textColor) {
      this.textColor = ensureOpaque(textColor);
   }

   public int getTextHoverColor() {
      return this.textHoverColor;
   }

   public void setTextHoverColor(int textHoverColor) {
      this.textHoverColor = ensureOpaque(textHoverColor);
   }

   public int getTextDisabledColor() {
      return this.textDisabledColor;
   }

   public void setTextDisabledColor(int textDisabledColor) {
      this.textDisabledColor = ensureOpaque(textDisabledColor);
   }

   public int getTextLinespace() {
      return this.textLinespace;
   }

   public void setTextLinespace(int textLinespace) {
      this.textLinespace = textLinespace;
   }

   public boolean isTextShadow() {
      return this.textShadow;
   }

   public void setTextShadow(boolean textShadow) {
      this.textShadow = textShadow;
   }

   public boolean isTextWrap() {
      return this.textWrap;
   }

   public void setTextWrap(boolean textWrap) {
      this.textWrap = textWrap;
      this.recalcTextRendering();
   }

   @Nullable
   public List<MutableComponent> getTextAsList() {
      return this.text;
   }

   @Nullable
   public MutableComponent getText() {
      return this.isTextEmpty() ? null : this.text.get(0);
   }

   public void setTextOld(List<Component> text) {
      this.setText(text.stream().map(c -> c instanceof MutableComponent m ? m : c.copy()).toList());
   }

   public void setText(List<MutableComponent> text) {
      this.text = text;
      this.recalcTextRendering();
   }

   public void setText(Component text) {
      this.setText(text instanceof MutableComponent m ? m : text.copy());
   }

   public void setText(MutableComponent text) {
      this.setText(Collections.singletonList(text));
   }

   public void clearText() {
      this.setText(Collections.emptyList());
   }

   @Nullable
   public String getTextAsStringStrict() {
      return this.isTextEmpty() ? null : this.text.get(0).getString();
   }

   public String getTextAsString() {
      return this.isTextEmpty() ? "" : this.text.get(0).getString();
   }

   public boolean isTextEmpty() {
      return this.text == null || this.text.stream().allMatch(t -> t.getString().isEmpty());
   }

   public int getRenderedTextWidth() {
      return this.renderedTextWidth;
   }

   public int getRenderedTextHeight() {
      return this.renderedTextHeight;
   }

   public List<FormattedCharSequence> getPreparedText() {
      return this.preparedText;
   }

   public void setSize(int w, int h) {
      super.setSize(w, h);
      this.setTextSize(w, h);
   }

   public void setTextSize(int w, int h) {
      this.textWidth = this.width;
      this.textHeight = this.height;
      this.recalcTextRendering();
   }
}

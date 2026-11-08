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
import org.joml.Matrix3x2fStack;

/**
 * Quidoms 1.21.10 fix: GuiGraphics pose space is already GUI-scaled.
 * Older BlockUI treated pose as framebuffer pixels (correct for immediate-mode GL),
 * which made every BOWindow enormous and clipped off-screen.
 */
public class BOScreen extends Screen {
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

   public BOScreen(BOWindow w) {
      super(Component.literal("Blockout GUI"));
      this.window = w;
   }

   public void render(GuiGraphics ms, int mx, int my, float f) {
      if (this.minecraft != null && this.isOpen) {
         this.absoluteMouseX = mx;
         this.absoluteMouseY = my;
         this.framebufferWidth = ms.minecraft.getWindow().getWidth();
         this.framebufferHeight = ms.minecraft.getWindow().getHeight();
         this.mcScale = Math.max(1.0, (double) ms.minecraft.getWindow().getGuiScale());
         this.renderScale = this.window.getRenderType().calcRenderScale(ms.minecraft.getWindow(), this.window);

         // Layout math stays in framebuffer pixels (matches WindowRenderType calculators).
         final int fbW = Math.max(this.framebufferWidth, 320);
         final int fbH = Math.max(this.framebufferHeight, 240);
         this.width = this.window.getWidth();
         this.height = this.window.getHeight();
         this.x = Math.floor((fbW - this.width * this.renderScale) / 2.0);
         this.y = Math.floor((fbH - this.height * this.renderScale) / 2.0);

         // Convert framebuffer layout into GuiGraphics (GUI-scaled) space.
         final float guiX = (float) (this.x / this.mcScale);
         final float guiY = (float) (this.y / this.mcScale);
         final float guiRenderScale = (float) (this.renderScale / this.mcScale);

         if (this.window.hasLightbox() && ms.minecraft.screen == this) {
            final int guiScaledW = ms.minecraft.getWindow().getGuiScaledWidth();
            final int guiScaledH = ms.minecraft.getWindow().getGuiScaledHeight();
            ms.fillGradient(0, 0, guiScaledW, guiScaledH, -1072689136, -804253680);
         }

         Matrix3x2fStack pose = ms.pose();
         pose.pushMatrix();
         pose.translate(guiX, guiY);
         pose.scale(guiRenderScale, guiRenderScale);

         try {
            BOGuiGraphics target = new BOGuiGraphics(ms);
            this.window.draw(target, this.calcRelativeX(mx), this.calcRelativeY(my));
            if (ms.minecraft.screen == this) {
               int debugX = (int) (-this.x / this.renderScale) + 3;
               if (Pane.debugging) {
                  debugX = target.drawString(
                     "XML: %s Scaling: %s (vanilla: %.2f our: %.2f) "
                        .formatted(this.window.getXmlResourceLocation(), this.window.getRenderType().name(), this.mcScale, this.renderScale),
                     debugX,
                     -9,
                     Color.getByName("white")
                  );
               }

               target.applyCursor(debugX);
            }

            this.window.drawLast(target, this.calcRelativeX(mx), this.calcRelativeY(my));
         } catch (Exception var14) {
            CrashReport crashReport = CrashReport.forThrowable(var14, "Rendering BO screen");
            CrashReportCategory category = crashReport.addCategory("BO screen rendering details");
            category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
            category.setDetail("Scaling mode (window render type)", () -> this.window.getRenderType().name());
            category.setDetail("Vanilla gui scale", () -> Double.toString(this.mcScale));
            category.setDetail("BO gui scale", () -> Double.toString(this.renderScale));
            throw new ReportedException(crashReport);
         } finally {
            pose.popMatrix();
         }
      }
   }

   public boolean keyPressed(KeyEvent event) {
      int key = event.key();
      if (key >= 0 && key <= 348) {
         try {
            return this.window.onKeyTyped('\u0000', key);
         } catch (Exception var6) {
            CrashReport crashReport = CrashReport.forThrowable(var6, "KeyPressed event for BO screen");
            CrashReportCategory category = crashReport.addCategory("BO screen key event details");
            category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
            category.setDetail("GLFW key value", () -> Integer.toString(key));
            throw new ReportedException(crashReport);
         }
      } else {
         return false;
      }
   }

   public boolean charTyped(CharacterEvent event) {
      char ch = event.codepointAsString().isEmpty() ? 0 : event.codepointAsString().charAt(0);

      try {
         return this.window.onKeyTyped(ch, event.codepoint());
      } catch (Exception var6) {
         CrashReport crashReport = CrashReport.forThrowable(var6, "CharTyped event for BO screen");
         CrashReportCategory category = crashReport.addCategory("BO screen char event details");
         category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
         category.setDetail("Char value", () -> Character.toString(ch));
         throw new ReportedException(crashReport);
      }
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      double mx = this.calcRelativeX(event.x());
      double my = this.calcRelativeY(event.y());
      int keyCode = event.button();

      try {
         if (keyCode == 0) {
            isMouseLeftDown = true;
            return this.window.click(mx, my);
         } else {
            return keyCode == 1 ? this.window.rightClick(mx, my) : false;
         }
      } catch (Exception var11) {
         CrashReport crashReport = CrashReport.forThrowable(var11, "MousePressed event for BO screen");
         CrashReportCategory category = crashReport.addCategory("BO screen mouse event details");
         category.setDetail("XML res loc", () -> Objects.toString(this.window.getXmlResourceLocation()));
         category.setDetail("GLFW mouse key value", () -> Integer.toString(keyCode));
         throw new ReportedException(crashReport);
      }
   }

   public boolean mouseScrolled(double mx, double my, double scrollHorizontalDiff, double scrollVerticalDiff) {
      if (scrollVerticalDiff != 0.0) {
         try {
            return this.window.scrollInput(scrollHorizontalDiff * 10.0, scrollVerticalDiff * 10.0, this.calcRelativeX(mx), this.calcRelativeY(my));
         } catch (Exception var12) {
            CrashReport crashReport = CrashReport.forThrowable(var12, "MouseScroll event for BO screen");
            CrashReportCategory category = crashReport.addCategory("BO screen scroll event details");
            category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
            category.setDetail("Scroll value", () -> Double.toString(scrollVerticalDiff));
            throw new ReportedException(crashReport);
         }
      } else {
         return false;
      }
   }

   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
      try {
         return this.window.onMouseDrag(this.calcRelativeX(event.x()), this.calcRelativeY(event.y()), event.button(), deltaX, deltaY);
      } catch (Exception var9) {
         CrashReport crashReport = CrashReport.forThrowable(var9, "MouseDragged event for BO screen");
         CrashReportCategory category = crashReport.addCategory("BO screen mouse event details");
         category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
         throw new ReportedException(crashReport);
      }
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      int keyCode = event.button();
      if (keyCode == 0) {
         isMouseLeftDown = false;

         try {
            return this.window.onMouseReleased(this.calcRelativeX(event.x()), this.calcRelativeY(event.y()));
         } catch (Exception var6) {
            CrashReport crashReport = CrashReport.forThrowable(var6, "MouseReleased event for BO screen");
            CrashReportCategory category = crashReport.addCategory("BO screen mouse event details");
            category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
            category.setDetail("GLFW mouse key value", () -> Integer.toString(keyCode));
            throw new ReportedException(crashReport);
         }
      } else {
         return false;
      }
   }

   public BOWindow getWindow() {
      return this.window;
   }

   public void tick() {
      try {
         if (this.minecraft != null) {
            if (!this.isOpen) {
               this.window.onOpened();
               this.isOpen = true;
            } else {
               this.window.onUpdate();
               LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
               if (player != null && (!player.isAlive() || player.isDeadOrDying())) {
                  player.closeContainer();
               }
            }
         }
      } catch (Exception var4) {
         CrashReport crashReport = CrashReport.forThrowable(var4, "Ticking/Updating BO screen");
         CrashReportCategory category = crashReport.addCategory("BO screen update details");
         category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
         category.setDetail("Is opened", () -> Boolean.toString(this.isOpen));
         throw new ReportedException(crashReport);
      }
   }

   public void removed() {
      try {
         this.window.onClosed();
      } catch (Exception var7) {
         CrashReport crashReport = CrashReport.forThrowable(var7, "Closing BO screen");
         CrashReportCategory category = crashReport.addCategory("BO screen closing details");
         category.setDetail("XML res loc", () -> this.window.getXmlResourceLocation().toString());
         category.setDetail("Is opened", () -> Boolean.toString(this.isOpen));
         throw new ReportedException(crashReport);
      } finally {
         BOWindow.clearFocus();
         CursorUtils.resetCursor();
      }
   }

   public boolean isPauseScreen() {
      return this.window.doesWindowPauseGame();
   }

   /** Mouse events arrive in GUI-scaled coords; convert via framebuffer layout. */
   private double calcRelativeX(double xIn) {
      return (xIn * this.mcScale - this.x) / this.renderScale;
   }

   private double calcRelativeY(double yIn) {
      return (yIn * this.mcScale - this.y) / this.renderScale;
   }

   public double getRenderScale() {
      return this.renderScale;
   }

   public double getVanillaGuiScale() {
      return this.mcScale;
   }

   public int getFramebufferWidth() {
      return this.framebufferWidth;
   }

   public int getFramebufferHeight() {
      return this.framebufferHeight;
   }

   public int getAbsoluteMouseX() {
      return this.absoluteMouseX;
   }

   public int getAbsoluteMouseY() {
      return this.absoluteMouseY;
   }
}

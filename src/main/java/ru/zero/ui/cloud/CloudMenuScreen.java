package ru.zero.ui.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.zero.util.render.core.Renderer2D;

/** CloudVisuals: base for the alternative menus. Drawing happens through Zero's RenderEvent (see CloudMenus). */
@Environment(EnvType.CLIENT)
public abstract class CloudMenuScreen extends Screen {
   protected final MinecraftClient mc = MinecraftClient.getInstance();
   protected float mouseX;
   protected float mouseY;

   protected CloudMenuScreen(String title) {
      super(Text.literal(title));
   }

   public final void draw(Renderer2D r) {
      if (this.mc.getWindow() == null) {
         return;
      }
      int fbW = this.mc.getWindow().getFramebufferWidth();
      float sw = this.mc.getWindow().getScaledWidth();
      float sh = this.mc.getWindow().getScaledHeight();
      if (fbW <= 0 || sw <= 0.0F || sh <= 0.0F) {
         return;
      }
      double winW = Math.max(1, this.mc.getWindow().getWidth());
      double winH = Math.max(1, this.mc.getWindow().getHeight());
      this.mouseX = (float) (this.mc.mouse.getX() * sw / winW);
      this.mouseY = (float) (this.mc.mouse.getY() * sh / winH);
      r.pushScale(fbW / sw);
      try {
         this.drawMenu(r, sw, sh);
      } finally {
         r.popScale();
      }
   }

   protected abstract void drawMenu(Renderer2D r, float width, float height);

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   @Override
   public boolean shouldPause() {
      return false;
   }
}

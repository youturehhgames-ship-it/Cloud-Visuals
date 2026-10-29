package ru.zero.ui.cloud;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import ru.zero.Zero;
import ru.zero.module.api.Module;
import ru.zero.module.api.setting.Setting;
import ru.zero.module.api.setting.impl.BindSettings;
import ru.zero.module.api.setting.impl.BooleanSetting;
import ru.zero.module.api.setting.impl.ButtonSetting;
import ru.zero.module.api.setting.impl.HueSetting;
import ru.zero.module.api.setting.impl.ModeSetting;
import ru.zero.module.api.setting.impl.MultiBooleanSetting;
import ru.zero.module.api.setting.impl.SliderSetting;
import ru.zero.module.api.setting.impl.StringSetting;
import ru.zero.util.color.ColorUtil;
import ru.zero.util.keyboard.Keyboard;
import ru.zero.util.render.core.Renderer2D;
import ru.zero.util.render.text.FontRegistry;

/**
 * CloudVisuals: a compact settings list shared by the "Cheat Menu" and "GTA" menu styles.
 * Draw it every frame; it records hit areas used by the mouse handlers.
 */
@Environment(EnvType.CLIENT)
public final class SettingsPanel {
   public static final float ROW = 14.0F;
   private static final float TEXT = 8.5F;
   private static final float PAD = 5.0F;

   private final List<Hit> hits = new ArrayList<>();
   private SliderSetting dragSlider;
   private HueSetting dragHue;
   private float dragX;
   private float dragW;
   private BindSettings listenBind;
   private Module listenModule;

   private record Hit(float x, float y, float w, float h, Runnable left, Runnable right) {
   }

   public void beginFrame() {
      this.hits.clear();
   }

   public boolean isListening() {
      return this.listenBind != null || this.listenModule != null;
   }

   public float draw(Renderer2D r, Module module, float x, float y, float w, float mouseX, float mouseY) {
      float cy = y;
      cy += this.drawRow(r, x, cy, w, mouseX, mouseY, "Бинд", this.listenModule == module ? "..." : keyName(module.bind), () -> {
         this.listenModule = module;
         this.listenBind = null;
      }, null);
      for (Setting setting : module.getSettingsForGUI()) {
         cy += this.drawSetting(r, setting, x, cy, w, mouseX, mouseY);
      }
      return cy - y;
   }

   private float drawSetting(Renderer2D r, Setting setting, float x, float y, float w, float mouseX, float mouseY) {
      if (setting instanceof BooleanSetting b) {
         return this.drawToggle(r, x, y, w, mouseX, mouseY, b.name, b);
      }
      if (setting instanceof SliderSetting s) {
         return this.drawSlider(r, x, y, w, mouseX, mouseY, s.name, s.current, s.minimum, s.maximum, s.increment, () -> {
            this.dragSlider = s;
            this.dragHue = null;
         }, -1);
      }
      if (setting instanceof HueSetting hue) {
         float max = hue.maximum > hue.minimum ? hue.maximum : 360.0F;
         int swatch = java.awt.Color.HSBtoRGB((hue.current - hue.minimum) / Math.max(1.0E-4F, max - hue.minimum), hue.saturation, hue.brightness);
         return this.drawSlider(r, x, y, w, mouseX, mouseY, hue.name, hue.current, hue.minimum, max, hue.increment, () -> {
            this.dragHue = hue;
            this.dragSlider = null;
         }, swatch | 0xFF000000);
      }
      if (setting instanceof ModeSetting mode) {
         return this.drawRow(r, x, y, w, mouseX, mouseY, mode.name, "< " + mode.get() + " >", () -> cycle(mode, 1), () -> cycle(mode, -1));
      }
      if (setting instanceof MultiBooleanSetting multi) {
         float cy = y;
         cy += this.drawRow(r, x, cy, w, mouseX, mouseY, multi.name, "", null, null);
         for (BooleanSetting sub : multi.settings) {
            cy += this.drawToggle(r, x + 6.0F, cy, w - 6.0F, mouseX, mouseY, sub.name, sub);
         }
         return cy - y;
      }
      if (setting instanceof BindSettings bind) {
         return this.drawRow(r, x, y, w, mouseX, mouseY, bind.name, this.listenBind == bind ? "..." : keyName(bind.key), () -> {
            this.listenBind = bind;
            this.listenModule = null;
         }, null);
      }
      if (setting instanceof ButtonSetting button) {
         float h = ROW + 2.0F;
         bg(r, x, y, w, h);
         boolean hov = inside(mouseX, mouseY, x + 3.0F, y + 2.0F, w - 6.0F, h - 4.0F);
         r.rect(x + 3.0F, y + 2.0F, w - 6.0F, h - 4.0F, 3.0F, hov ? ColorUtil.replAlpha(main(), 200) : ColorUtil.replAlpha(main(), 120));
         float tw = r.measureText(FontRegistry.INTER_MEDIUM, button.name, TEXT).width;
         r.text(FontRegistry.INTER_MEDIUM, x + w / 2.0F - tw / 2.0F, y + h / 2.0F + TEXT * 0.35F, TEXT, button.name, 0xFFFFFFFF);
         this.hits.add(new Hit(x, y, w, h, () -> {
            if (button.action != null) {
               button.action.run();
            }
         }, null));
         return h;
      }
      if (setting instanceof StringSetting str) {
         return this.drawRow(r, x, y, w, mouseX, mouseY, str.name, str.get() == null ? "" : str.get(), null, null);
      }
      return this.drawRow(r, x, y, w, mouseX, mouseY, setting.name, "", null, null);
   }

   private float drawToggle(Renderer2D r, float x, float y, float w, float mouseX, float mouseY, String name, BooleanSetting b) {
      bg(r, x, y, w, ROW);
      label(r, x, y, name, ROW);
      float tw = 16.0F;
      float th = 8.0F;
      float tx = x + w - PAD - tw;
      float ty = y + (ROW - th) / 2.0F;
      boolean on = b.get();
      r.rect(tx, ty, tw, th, th / 2.0F, on ? main() : Renderer2D.ColorUtil.rgba(70, 70, 80, 255));
      float knob = th - 2.0F;
      r.rect(on ? tx + tw - knob - 1.0F : tx + 1.0F, ty + 1.0F, knob, knob, knob / 2.0F, 0xFFFFFFFF);
      this.hits.add(new Hit(x, y, w, ROW, () -> {
         b.set(!b.get());
         save();
      }, null));
      return ROW;
   }

   private float drawSlider(Renderer2D r, float x, float y, float w, float mouseX, float mouseY, String name, float value, float min, float max,
         float inc, Runnable startDrag, int swatch) {
      float h = ROW + 6.0F;
      bg(r, x, y, w, h);
      label(r, x, y, name, ROW);
      String valueText = inc >= 1.0F ? String.valueOf(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.2f", value);
      float vw = r.measureText(FontRegistry.INTER_MEDIUM, valueText, TEXT).width;
      r.text(FontRegistry.INTER_MEDIUM, x + w - PAD - vw, y + ROW / 2.0F + TEXT * 0.35F, TEXT, valueText, dim());
      float bx = x + PAD;
      float bw = w - PAD * 2.0F;
      float by = y + ROW + 1.0F;
      float t = max > min ? clamp01((value - min) / (max - min)) : 0.0F;
      r.rect(bx, by, bw, 3.0F, 1.5F, Renderer2D.ColorUtil.rgba(60, 60, 70, 255));
      int fill = swatch != -1 ? swatch : main();
      r.rect(bx, by, Math.max(3.0F, bw * t), 3.0F, 1.5F, fill);
      r.rect(bx + bw * t - 2.5F, by - 1.5F, 5.0F, 6.0F, 2.5F, 0xFFFFFFFF);
      this.hits.add(new Hit(x, y, w, h, () -> {
         startDrag.run();
         this.dragX = bx;
         this.dragW = bw;
      }, null));
      return h;
   }

   private float drawRow(Renderer2D r, float x, float y, float w, float mouseX, float mouseY, String name, String value, Runnable left, Runnable right) {
      bg(r, x, y, w, ROW);
      if (left != null && inside(mouseX, mouseY, x, y, w, ROW)) {
         r.rect(x, y, w, ROW, Renderer2D.ColorUtil.rgba(255, 255, 255, 12));
      }
      label(r, x, y, name, ROW);
      if (value != null && !value.isEmpty()) {
         float vw = r.measureText(FontRegistry.INTER_MEDIUM, value, TEXT).width;
         r.text(FontRegistry.INTER_MEDIUM, x + w - PAD - vw, y + ROW / 2.0F + TEXT * 0.35F, TEXT, value, left != null ? main() : dim());
      }
      if (left != null || right != null) {
         this.hits.add(new Hit(x, y, w, ROW, left, right));
      }
      return ROW;
   }

   public boolean mouseClicked(double mx, double my, int button) {
      for (int i = this.hits.size() - 1; i >= 0; i--) {
         Hit hit = this.hits.get(i);
         if (inside((float) mx, (float) my, hit.x, hit.y, hit.w, hit.h)) {
            Runnable action = button == 1 ? hit.right : hit.left;
            if (action != null) {
               action.run();
               if (this.dragSlider != null || this.dragHue != null) {
                  this.mouseDragged(mx);
               }
               return true;
            }
            return hit.left != null || hit.right != null;
         }
      }
      return false;
   }

   public boolean mouseDragged(double mx) {
      if (this.dragW <= 0.0F || this.dragSlider == null && this.dragHue == null) {
         return false;
      }
      float t = clamp01(((float) mx - this.dragX) / this.dragW);
      if (this.dragSlider != null) {
         SliderSetting s = this.dragSlider;
         s.current = snap(s.minimum + t * (s.maximum - s.minimum), s.minimum, s.maximum, s.increment);
      } else {
         HueSetting h = this.dragHue;
         float max = h.maximum > h.minimum ? h.maximum : 360.0F;
         h.current = snap(h.minimum + t * (max - h.minimum), h.minimum, max, h.increment);
      }
      return true;
   }

   public void mouseReleased() {
      if (this.dragSlider != null || this.dragHue != null) {
         save();
      }
      this.dragSlider = null;
      this.dragHue = null;
   }

   public boolean keyPressed(int key) {
      if (!this.isListening()) {
         return false;
      }
      int value = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE ? -1 : key;
      if (this.listenModule != null) {
         this.listenModule.bind = value;
      } else if (this.listenBind != null) {
         this.listenBind.key = value;
      }
      this.listenModule = null;
      this.listenBind = null;
      save();
      return true;
   }

   private static void cycle(ModeSetting mode, int dir) {
      if (mode.modes == null || mode.modes.isEmpty()) {
         return;
      }
      int n = mode.modes.size();
      int idx = Math.max(0, mode.modes.indexOf(mode.currentMode));
      int next = ((idx + dir) % n + n) % n;
      mode.currentMode = mode.modes.get(next);
      mode.index = next;
      save();
   }

   private static float snap(float v, float min, float max, float inc) {
      if (inc > 0.0F) {
         v = Math.round(v / inc) * inc;
      }
      return Math.max(min, Math.min(max, v));
   }

   private static void bg(Renderer2D r, float x, float y, float w, float h) {
      r.rect(x, y, w, h, Renderer2D.ColorUtil.rgba(12, 12, 16, 235));
   }

   private static void label(Renderer2D r, float x, float y, String name, float h) {
      r.text(FontRegistry.INTER_MEDIUM, x + PAD, y + h / 2.0F + TEXT * 0.35F, TEXT, name == null ? "" : name, Renderer2D.ColorUtil.getTextColor(1, 1));
   }

   static int main() {
      return Renderer2D.ColorUtil.getMainColor(1, 1);
   }

   static int dim() {
      return Renderer2D.ColorUtil.rgba(165, 165, 175, 255);
   }

   static String keyName(int key) {
      if (key <= 0) {
         return "Нет";
      }
      String name = Keyboard.keyName(key);
      return name == null || name.isBlank() ? String.valueOf(key) : name;
   }

   static void save() {
      try {
         if (Zero.get != null && Zero.get.configManager != null) {
            Zero.get.configManager.autoSave();
         }
      } catch (Exception ignored) {
      }
   }

   static boolean inside(float px, float py, float x, float y, float w, float h) {
      return px >= x && px <= x + w && py >= y && py <= y + h;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : Math.min(1.0F, v);
   }
}

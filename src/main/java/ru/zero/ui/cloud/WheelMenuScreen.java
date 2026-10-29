package ru.zero.ui.cloud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import ru.zero.Zero;
import ru.zero.module.api.Category;
import ru.zero.module.api.Module;
import ru.zero.ui.gui.GuiScreen;
import ru.zero.util.color.ColorUtil;
import ru.zero.util.render.core.Renderer2D;
import ru.zero.util.render.text.FontRegistry;

/**
 * CloudVisuals "GTA" menu: a weapon-wheel style radial menu.
 * First ring = categories, then modules of the chosen category (10 per page, wheel to flip).
 * LMB toggles, RMB opens settings on the side, click the center or Backspace to go back.
 */
@Environment(EnvType.CLIENT)
public final class WheelMenuScreen extends CloudMenuScreen {
   private static final int PAGE_SIZE = 10;
   private static final float START_DEG = 270.0F;
   private static Category lastCategory;

   private final SettingsPanel settings = new SettingsPanel();
   private final List<Object> items = new ArrayList<>();
   private Category category = lastCategory;
   private int page;
   private Module selected;
   private float cx;
   private float cy;
   private float outer;
   private float inner;
   private int hoverIndex = -1;
   private boolean hoverCenter;

   public WheelMenuScreen() {
      super("GTA Menu");
   }

   private List<Module> modulesOf(Category c) {
      List<Module> list = new ArrayList<>(Zero.get.manager.getType(c));
      list.sort(Comparator.comparing(m -> m.name == null ? "" : m.name.toLowerCase()));
      return list;
   }

   private int pageCount() {
      if (this.category == null) {
         return 1;
      }
      int n = this.modulesOf(this.category).size();
      return Math.max(1, (n + PAGE_SIZE - 1) / PAGE_SIZE);
   }

   private void rebuildItems() {
      this.items.clear();
      if (this.category == null) {
         for (Category c : GuiScreen.resolveCategories()) {
            this.items.add(c);
         }
      } else {
         List<Module> all = this.modulesOf(this.category);
         this.page = Math.max(0, Math.min(this.page, this.pageCount() - 1));
         int from = this.page * PAGE_SIZE;
         for (int i = from; i < Math.min(all.size(), from + PAGE_SIZE); i++) {
            this.items.add(all.get(i));
         }
      }
   }

   private void updateHover(float mx, float my) {
      float dx = mx - this.cx;
      float dy = my - this.cy;
      float dist = (float) Math.sqrt(dx * dx + dy * dy);
      this.hoverCenter = dist < this.inner;
      this.hoverIndex = -1;
      int n = this.items.size();
      if (n > 0 && dist >= this.inner && dist <= this.outer + 6.0F) {
         double ang = Math.toDegrees(Math.atan2(dy, dx));
         if (ang < 0.0) {
            ang += 360.0;
         }
         double rel = ((ang - START_DEG) % 360.0 + 360.0) % 360.0;
         this.hoverIndex = Math.min(n - 1, (int) (rel / (360.0 / n)));
      }
   }

   @Override
   protected void drawMenu(Renderer2D r, float width, float height) {
      this.settings.beginFrame();
      r.rect(0.0F, 0.0F, width, height, Renderer2D.ColorUtil.rgba(0, 0, 0, 125));
      if (Zero.get == null || Zero.get.manager == null) {
         return;
      }
      this.rebuildItems();
      int main = SettingsPanel.main();
      this.outer = Math.min(width, height) * 0.36F;
      this.inner = this.outer * 0.42F;
      this.cx = this.selected != null ? width * 0.36F : width / 2.0F;
      this.cy = height / 2.0F;
      this.updateHover(this.mouseX, this.mouseY);

      int n = this.items.size();
      r.circle(this.cx, this.cy, this.outer + 5.0F, 0.0F, 1.0F, Renderer2D.ColorUtil.rgba(8, 8, 12, 200));
      if (n > 0) {
         float seg = 360.0F / n;
         for (int i = 0; i < n; i++) {
            Object item = this.items.get(i);
            boolean enabled = item instanceof Module m && m.enable;
            boolean hov = i == this.hoverIndex;
            int color;
            if (enabled) {
               color = hov ? main : ColorUtil.replAlpha(main, 150);
            } else {
               color = hov ? Renderer2D.ColorUtil.rgba(64, 64, 78, 245) : Renderer2D.ColorUtil.rgba(26, 26, 33, 235);
            }
            if (item == this.selected) {
               color = ColorUtil.replAlpha(main, 215);
            }
            float start = (START_DEG + i * seg) % 360.0F;
            r.circle(this.cx, this.cy, hov ? this.outer + 4.0F : this.outer, start, seg / 360.0F, color);
         }
         for (int i = 0; i < n && n > 1; i++) {
            float edge = (START_DEG + i * seg - 0.9F + 360.0F) % 360.0F;
            r.circle(this.cx, this.cy, this.outer + 6.0F, edge, 1.8F / 360.0F, Renderer2D.ColorUtil.rgba(8, 8, 12, 255));
         }
      }
      r.circle(this.cx, this.cy, this.inner + 1.5F, 0.0F, 1.0F, ColorUtil.replAlpha(main, 170));
      r.circle(this.cx, this.cy, this.inner, 0.0F, 1.0F, this.hoverCenter && this.category != null
         ? Renderer2D.ColorUtil.rgba(24, 24, 30, 250) : Renderer2D.ColorUtil.rgba(12, 12, 16, 250));

      if (n > 0) {
         float seg = 360.0F / n;
         float labelRadius = (this.inner + this.outer) / 2.0F;
         float maxLabel = (this.outer - this.inner) * 0.95F;
         for (int i = 0; i < n; i++) {
            Object item = this.items.get(i);
            String label = item instanceof Category c ? c.name() : ((Module) item).name;
            float size = 9.0F;
            float tw = r.measureText(FontRegistry.INTER_SEMIBOLD, label, size).width;
            while (tw > maxLabel && size > 6.0F) {
               size -= 0.5F;
               tw = r.measureText(FontRegistry.INTER_SEMIBOLD, label, size).width;
            }
            double mid = Math.toRadians(START_DEG + (i + 0.5) * seg);
            float lx = this.cx + (float) Math.cos(mid) * labelRadius;
            float ly = this.cy + (float) Math.sin(mid) * labelRadius;
            boolean on = item instanceof Module m && m.enable;
            r.text(FontRegistry.INTER_SEMIBOLD, lx - tw / 2.0F, ly + size * 0.35F, size, label, on || i == this.hoverIndex ? 0xFFFFFFFF : Renderer2D.ColorUtil.rgba(200, 200, 208, 255));
            if (item instanceof Category cat) {
               String cnt = String.valueOf(this.modulesOf(cat).size());
               float cw = r.measureText(FontRegistry.INTER_MEDIUM, cnt, 7.5F).width;
               r.text(FontRegistry.INTER_MEDIUM, lx - cw / 2.0F, ly + size * 0.35F + 10.0F, 7.5F, cnt, SettingsPanel.dim());
            }
         }
      }

      String title;
      String sub;
      if (this.hoverIndex >= 0 && this.hoverIndex < n) {
         Object item = this.items.get(this.hoverIndex);
         if (item instanceof Module m) {
            title = m.name;
            sub = m.description == null ? "" : m.description.trim();
         } else {
            title = ((Category) item).name();
            sub = "Открыть категорию";
         }
      } else if (this.category != null) {
         title = this.category.name();
         sub = "Стр. " + (this.page + 1) + "/" + this.pageCount() + " · центр — назад";
      } else {
         title = "Cloud";
         sub = "Выбери категорию";
      }
      this.centerText(r, title, this.cy - 2.0F, 11.0F, FontRegistry.INTER_SEMIBOLD, 0xFFFFFFFF);
      this.centerWrapped(r, sub, this.cy + 11.0F, 7.5F, this.inner * 1.7F);

      if (this.selected != null) {
         float pw = 175.0F;
         float px = Math.min(width - pw - 10.0F, this.cx + this.outer + 30.0F);
         float py = Math.max(10.0F, this.cy - 110.0F);
         r.rect(px, py, pw, 18.0F, 3.0F, main);
         r.text(FontRegistry.INTER_SEMIBOLD, px + 6.0F, py + 12.5F, 10.0F, this.selected.name, 0xFFFFFFFF);
         this.settings.draw(r, this.selected, px, py + 18.0F, pw, this.mouseX, this.mouseY);
      }
      String hint = "ЛКМ — вкл/выкл   ПКМ — настройки   Колесо — страницы   Центр / Backspace — назад";
      r.text(FontRegistry.INTER_MEDIUM, 8.0F, height - 8.0F, 8.0F, hint, ColorUtil.replAlpha(0xFFFFFFFF, 120));
   }

   private void centerText(Renderer2D r, String text, float y, float size, ru.zero.util.render.text.FontObject font, int color) {
      float tw = r.measureText(font, text, size).width;
      r.text(font, this.cx - tw / 2.0F, y, size, text, color);
   }

   private void centerWrapped(Renderer2D r, String text, float y, float size, float maxWidth) {
      if (text == null || text.isEmpty()) {
         return;
      }
      String[] words = text.split(" ");
      StringBuilder line = new StringBuilder();
      float lineY = y;
      int lines = 0;
      for (String word : words) {
         String candidate = line.length() == 0 ? word : line + " " + word;
         if (r.measureText(FontRegistry.INTER_MEDIUM, candidate, size).width > maxWidth && line.length() > 0) {
            this.centerText(r, line.toString(), lineY, size, FontRegistry.INTER_MEDIUM, SettingsPanel.dim());
            lineY += size + 2.0F;
            line = new StringBuilder(word);
            if (++lines >= 3) {
               return;
            }
         } else {
            line = new StringBuilder(candidate);
         }
      }
      if (line.length() > 0) {
         this.centerText(r, line.toString(), lineY, size, FontRegistry.INTER_MEDIUM, SettingsPanel.dim());
      }
   }

   private void back() {
      this.category = null;
      lastCategory = null;
      this.page = 0;
      this.selected = null;
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      double mx = click.x();
      double my = click.y();
      int button = click.button();
      if (this.settings.mouseClicked(mx, my, button)) {
         return true;
      }
      this.updateHover((float) mx, (float) my);
      if (this.hoverCenter) {
         if (this.category != null) {
            this.back();
         }
         return true;
      }
      if (this.hoverIndex >= 0 && this.hoverIndex < this.items.size()) {
         Object item = this.items.get(this.hoverIndex);
         if (item instanceof Category c) {
            this.category = c;
            lastCategory = c;
            this.page = 0;
            this.selected = null;
         } else if (item instanceof Module m) {
            if (button == 0) {
               m.toggle();
            } else if (button == 1) {
               this.selected = this.selected == m ? null : m;
            }
         }
         return true;
      }
      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean mouseDragged(Click click, double deltaX, double deltaY) {
      return this.settings.mouseDragged(click.x()) || super.mouseDragged(click, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(Click click) {
      this.settings.mouseReleased();
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
      if (this.category != null && vertical != 0.0) {
         int pages = this.pageCount();
         this.page = ((this.page - (int) Math.signum(vertical)) % pages + pages) % pages;
      }
      return true;
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (this.settings.keyPressed(input.key())) {
         return true;
      }
      if (input.key() == GLFW.GLFW_KEY_BACKSPACE && this.category != null) {
         this.back();
         return true;
      }
      return super.keyPressed(input);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return !this.settings.isListening();
   }
}

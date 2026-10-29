package ru.zero.ui.cloud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import ru.zero.Zero;
import ru.zero.module.api.Category;
import ru.zero.module.api.Module;
import ru.zero.ui.gui.GuiScreen;
import ru.zero.util.color.ColorUtil;
import ru.zero.util.render.core.Renderer2D;
import ru.zero.util.render.text.FontRegistry;

/**
 * CloudVisuals "Cheat Menu": classic click-GUI with one draggable panel per category.
 * LMB toggles a module, RMB expands its settings, drag a header to move a panel, wheel scrolls.
 */
@Environment(EnvType.CLIENT)
public final class PanelMenuScreen extends CloudMenuScreen {
   private static final float PANEL_W = 120.0F;
   private static final float HEADER_H = 18.0F;
   private static final float ROW_H = 15.0F;
   private static final Map<Category, float[]> POSITIONS = new EnumMap<>(Category.class);
   private static final Set<String> EXPANDED = new HashSet<>();
   private static float scrollY;

   private final SettingsPanel settings = new SettingsPanel();
   private final List<Object[]> moduleHits = new ArrayList<>();
   private final List<Object[]> headerHits = new ArrayList<>();
   private Category dragging;
   private float dragOffsetX;
   private float dragOffsetY;
   private Module hovered;

   public PanelMenuScreen() {
      super("Cheat Menu");
   }

   @Override
   protected void drawMenu(Renderer2D r, float width, float height) {
      this.moduleHits.clear();
      this.headerHits.clear();
      this.settings.beginFrame();
      this.hovered = null;
      r.rect(0.0F, 0.0F, width, height, Renderer2D.ColorUtil.rgba(0, 0, 0, 110));
      if (Zero.get == null || Zero.get.manager == null) {
         return;
      }
      int main = SettingsPanel.main();
      Category[] categories = GuiScreen.resolveCategories();
      for (int i = 0; i < categories.length; i++) {
         Category category = categories[i];
         final int index = i;
         float[] pos = POSITIONS.computeIfAbsent(category, k -> new float[] { 14.0F + index * (PANEL_W + 12.0F), 14.0F });
         if (this.dragging == category) {
            pos[0] = this.mouseX - this.dragOffsetX;
            pos[1] = this.mouseY - this.dragOffsetY - scrollY;
         }
         float px = pos[0];
         float py = pos[1] + scrollY;
         List<Module> modules = new ArrayList<>(Zero.get.manager.getType(category));
         modules.sort(Comparator.comparing(m -> m.name == null ? "" : m.name.toLowerCase()));

         r.rect(px, py, PANEL_W, HEADER_H, 3.0F, main);
         r.text(FontRegistry.INTER_SEMIBOLD, px + 6.0F, py + HEADER_H / 2.0F + 3.5F, 10.0F, category.name(), 0xFFFFFFFF);
         String count = String.valueOf(modules.size());
         float cw = r.measureText(FontRegistry.INTER_MEDIUM, count, 8.5F).width;
         r.text(FontRegistry.INTER_MEDIUM, px + PANEL_W - 6.0F - cw, py + HEADER_H / 2.0F + 3.0F, 8.5F, count, ColorUtil.replAlpha(0xFFFFFFFF, 190));
         this.headerHits.add(new Object[] { px, py, PANEL_W, HEADER_H, category });

         float cy = py + HEADER_H;
         for (Module module : modules) {
            boolean hov = SettingsPanel.inside(this.mouseX, this.mouseY, px, cy, PANEL_W, ROW_H);
            r.rect(px, cy, PANEL_W, ROW_H, Renderer2D.ColorUtil.rgba(18, 18, 23, 232));
            if (hov) {
               r.rect(px, cy, PANEL_W, ROW_H, Renderer2D.ColorUtil.rgba(255, 255, 255, 14));
               this.hovered = module;
            }
            if (module.enable) {
               r.rect(px, cy, 2.0F, ROW_H, main);
            }
            r.text(FontRegistry.INTER_MEDIUM, px + 7.0F, cy + ROW_H / 2.0F + 3.0F, 9.0F, module.name,
               module.enable ? main : Renderer2D.ColorUtil.rgba(205, 205, 212, 255));
            boolean expanded = EXPANDED.contains(module.name);
            if (!module.getSettingsForGUI().isEmpty() || expanded) {
               String mark = expanded ? "-" : "+";
               r.text(FontRegistry.INTER_MEDIUM, px + PANEL_W - 10.0F, cy + ROW_H / 2.0F + 3.0F, 9.0F, mark, SettingsPanel.dim());
            }
            this.moduleHits.add(new Object[] { px, cy, PANEL_W, ROW_H, module });
            cy += ROW_H;
            if (expanded) {
               cy += this.settings.draw(r, module, px + 3.0F, cy, PANEL_W - 3.0F, this.mouseX, this.mouseY);
            }
         }
         r.rect(px, cy, PANEL_W, 3.0F, Renderer2D.ColorUtil.rgba(18, 18, 23, 232));
         r.rect(px, cy + 2.0F, PANEL_W, 1.0F, ColorUtil.replAlpha(main, 160));
      }

      if (this.hovered != null && this.hovered.description != null && !this.hovered.description.isBlank()) {
         String text = this.hovered.description.trim();
         float tw = r.measureText(FontRegistry.INTER_MEDIUM, text, 9.0F).width;
         float bx = width / 2.0F - tw / 2.0F - 8.0F;
         float by = height - 40.0F;
         r.rect(bx, by, tw + 16.0F, 16.0F, 4.0F, Renderer2D.ColorUtil.rgba(12, 12, 16, 235));
         r.text(FontRegistry.INTER_MEDIUM, bx + 8.0F, by + 11.0F, 9.0F, text, Renderer2D.ColorUtil.getTextColor(1, 1));
      }
      String hint = "ЛКМ — вкл/выкл   ПКМ — настройки   Колесо — прокрутка   Стиль меняется в Utils > Menu Style";
      r.text(FontRegistry.INTER_MEDIUM, 8.0F, height - 8.0F, 8.0F, hint, ColorUtil.replAlpha(0xFFFFFFFF, 120));
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      double mx = click.x();
      double my = click.y();
      int button = click.button();
      if (this.settings.mouseClicked(mx, my, button)) {
         return true;
      }
      for (int i = this.headerHits.size() - 1; i >= 0; i--) {
         Object[] hit = this.headerHits.get(i);
         if (inside(hit, mx, my) && button == 0) {
            Category category = (Category) hit[4];
            float[] pos = POSITIONS.get(category);
            this.dragging = category;
            this.dragOffsetX = (float) mx - pos[0];
            this.dragOffsetY = (float) my - (pos[1] + scrollY);
            return true;
         }
      }
      for (int i = this.moduleHits.size() - 1; i >= 0; i--) {
         Object[] hit = this.moduleHits.get(i);
         if (inside(hit, mx, my)) {
            Module module = (Module) hit[4];
            if (button == 0) {
               module.toggle();
            } else if (button == 1) {
               if (!EXPANDED.remove(module.name)) {
                  EXPANDED.add(module.name);
               }
            }
            return true;
         }
      }
      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean mouseDragged(Click click, double deltaX, double deltaY) {
      if (this.settings.mouseDragged(click.x())) {
         return true;
      }
      return this.dragging != null || super.mouseDragged(click, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(Click click) {
      this.dragging = null;
      this.settings.mouseReleased();
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
      scrollY += (float) vertical * 22.0F;
      return true;
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (this.settings.keyPressed(input.key())) {
         return true;
      }
      return super.keyPressed(input);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return !this.settings.isListening();
   }

   private static boolean inside(Object[] hit, double mx, double my) {
      return SettingsPanel.inside((float) mx, (float) my, (Float) hit[0], (Float) hit[1], (Float) hit[2], (Float) hit[3]);
   }
}

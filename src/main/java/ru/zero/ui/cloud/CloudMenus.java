package ru.zero.ui.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import ru.zero.event.EventInit;
import ru.zero.event.EventManager;
import ru.zero.event.render.RenderEvent;
import ru.zero.module.impl.cloud.MenuStyle;

/** CloudVisuals: picks the Right Shift menu according to Menu Style. */
@Environment(EnvType.CLIENT)
public final class CloudMenus {
   private static boolean registered;

   private CloudMenus() {
   }

   public static Screen choose(Screen zeroMenu) {
      register();
      String style = MenuStyle.style.get();
      if ("Cheat Menu".equals(style)) {
         return new PanelMenuScreen();
      }
      if ("GTA".equals(style)) {
         return new WheelMenuScreen();
      }
      return zeroMenu;
   }

   private static synchronized void register() {
      if (registered) {
         return;
      }
      registered = true;
      EventManager.register(new Object() {
         @EventInit
         public void onRender(RenderEvent event) {
            MinecraftClient client = event.client();
            if (client != null && client.currentScreen instanceof CloudMenuScreen screen && event.renderer() != null) {
               screen.draw(event.renderer());
            }
         }
      });
   }
}

package ru.zero.module.impl.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.zero.Zero;
import ru.zero.module.api.Category;
import ru.zero.module.api.IModule;
import ru.zero.module.api.Module;

// CloudVisuals (idea from LiquidBounce NoSwing): hides only your own swing animation.
// The swing packet is still sent, so the server and other players see a normal swing.
@IModule(name = "No Swing", description = "Убирает анимацию взмаха рукой у тебя на экране (сервер взмах видит)", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class NoSwing extends Module {
   public static boolean active() {
      if (Zero.get == null || Zero.get.manager == null) {
         return false;
      }
      NoSwing m = Zero.get.manager.get(NoSwing.class);
      return m != null && m.enable;
   }
}

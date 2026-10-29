package ru.zero.module.impl.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.zero.Zero;
import ru.zero.module.api.Category;
import ru.zero.module.api.IModule;
import ru.zero.module.api.Module;

// CloudVisuals (idea from LiquidBounce NoFov): FOV no longer changes with sprinting, speed or slowness.
@IModule(name = "Static FOV", description = "FOV не меняется от бега, скорости и эффектов", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class StaticFov extends Module {
   public static boolean active() {
      if (Zero.get == null || Zero.get.manager == null) {
         return false;
      }
      StaticFov m = Zero.get.manager.get(StaticFov.class);
      return m != null && m.enable;
   }
}

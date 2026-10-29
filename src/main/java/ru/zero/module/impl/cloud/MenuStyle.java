package ru.zero.module.impl.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.zero.module.api.Category;
import ru.zero.module.api.IModule;
import ru.zero.module.api.Module;
import ru.zero.module.api.setting.Setting;
import ru.zero.module.api.setting.impl.ModeSetting;

// CloudVisuals: chooses which menu opens on Right Shift. The setting works whether the module is on or off.
@IModule(name = "Menu Style", description = "Вид меню на Right Shift: Zero, Cheat Menu (панели) или GTA (колесо)", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class MenuStyle extends Module {
   public static final ModeSetting style = new ModeSetting("Стиль меню", "Zero", "Zero", "Cheat Menu", "GTA");

   public MenuStyle() {
      this.addSettings(new Setting[] { style });
   }
}

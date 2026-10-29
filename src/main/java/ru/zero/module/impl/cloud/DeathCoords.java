package ru.zero.module.impl.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.zero.module.api.Category;
import ru.zero.module.api.IModule;
import ru.zero.module.api.Module;
import ru.zero.module.api.setting.Setting;
import ru.zero.module.api.setting.impl.BooleanSetting;
import ru.zero.module.api.setting.impl.ModeSetting;
import ru.zero.module.api.setting.impl.SliderSetting;

// CloudVisuals: settings are pushed to the underlying feature by CloudBridge every tick.
@IModule(name = "Death Coords", description = "Координаты смерти в чате", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class DeathCoords extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Отдельно", "В сообщении", "Отдельно");
   public DeathCoords() {
      this.addSettings(new Setting[] { mode });
   }
}

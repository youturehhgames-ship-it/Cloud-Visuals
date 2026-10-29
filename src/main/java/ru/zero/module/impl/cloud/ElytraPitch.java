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
@IModule(name = "Elytra Pitch", description = "Линии-подсказки угла полёта на элитрах", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ElytraPitch extends Module {
   public final SliderSetting up = new SliderSetting("Угол вверх", 40.0F, 10.0F, 80.0F, 1.0F, false);
   public final SliderSetting down = new SliderSetting("Угол вниз", 40.0F, 10.0F, 80.0F, 1.0F, false);
   public final SliderSetting tolerance = new SliderSetting("Допуск", 10.0F, 1.0F, 30.0F, 1.0F, false);
   public ElytraPitch() {
      this.addSettings(new Setting[] { up, down, tolerance });
   }
}

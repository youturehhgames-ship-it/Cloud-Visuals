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
@IModule(name = "Speed Lines", description = "Аниме-линии по краям экрана на высокой скорости", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class SpeedLines extends Module {
   public final SliderSetting alpha = new SliderSetting("Прозрачность", 0.7F, 0.1F, 1.0F, 0.05F, false);
   public final SliderSetting amount = new SliderSetting("Количество", 1.0F, 0.2F, 3.0F, 0.1F, false);
   public final SliderSetting radius = new SliderSetting("Радиус", 0.7F, 0.3F, 1.0F, 0.05F, false);
   public final SliderSetting width = new SliderSetting("Толщина", 8.0F, 1.0F, 20.0F, 0.5F, false);
   public final SliderSetting speed = new SliderSetting("Скорость", 1.0F, 0.2F, 3.0F, 0.1F, false);
   public final SliderSetting threshold = new SliderSetting("Порог скорости", 0.6F, 0.1F, 2.0F, 0.05F, false);
   public SpeedLines() {
      this.addSettings(new Setting[] { alpha, amount, radius, width, speed, threshold });
   }
}

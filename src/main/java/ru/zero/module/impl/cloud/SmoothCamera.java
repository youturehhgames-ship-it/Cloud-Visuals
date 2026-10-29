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
@IModule(name = "Smooth Camera", description = "Плавное движение камеры", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class SmoothCamera extends Module {
   public final SliderSetting speed = new SliderSetting("Скорость", 0.25F, 0.02F, 1.0F, 0.01F, false);
   public final SliderSetting fpSpeed = new SliderSetting("Скорость от 1-го лица", 0.3F, 0.02F, 1.0F, 0.01F, false);
   public final SliderSetting maxOffset = new SliderSetting("Макс. отставание", 4.0F, 0.5F, 16.0F, 0.5F, false);
   public SmoothCamera() {
      this.addSettings(new Setting[] { speed, fpSpeed, maxOffset });
   }
}

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
@IModule(name = "Motion Blur", description = "Размытие движения. Сила = длина шлейфа, одинаково при любом FPS", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class MotionBlur extends Module {
   public final SliderSetting strength = new SliderSetting("Сила", 60.0F, 1.0F, 100.0F, 1.0F, false);
   public final BooleanSetting adaptive = new BooleanSetting("Не зависеть от FPS", true);
   public MotionBlur() {
      this.addSettings(new Setting[] { strength, adaptive });
   }
}

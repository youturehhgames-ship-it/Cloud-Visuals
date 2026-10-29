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
@IModule(name = "Damage Numbers", description = "Цифры урона над мобами и игроками", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class DamageNumbers extends Module {
   public final SliderSetting scale = new SliderSetting("Размер", 1.0F, 0.5F, 3.0F, 0.1F, false);
   public final BooleanSetting border = new BooleanSetting("Обводка", true);
   public DamageNumbers() {
      this.addSettings(new Setting[] { scale, border });
   }
}

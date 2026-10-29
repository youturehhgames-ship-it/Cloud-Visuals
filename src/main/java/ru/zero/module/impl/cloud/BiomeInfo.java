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
@IModule(name = "Biome Info", description = "Название биома при входе в новый биом", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class BiomeInfo extends Module {
   public final SliderSetting duration = new SliderSetting("Время показа (сек)", 3.0F, 1.0F, 10.0F, 0.5F, false);
   public BiomeInfo() {
      this.addSettings(new Setting[] { duration });
   }
}

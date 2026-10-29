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
@IModule(name = "Pickup Notifier", description = "Всплывашки о подобранных предметах и опыте", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class PickupNotifier extends Module {
   public final SliderSetting scale = new SliderSetting("Размер", 1.0F, 0.5F, 2.0F, 0.05F, false);
   public final SliderSetting duration = new SliderSetting("Время показа (сек)", 6.0F, 1.0F, 15.0F, 0.5F, false);
   public final BooleanSetting showXp = new BooleanSetting("Показывать опыт", true);
   public final ModeSetting side = new ModeSetting("Сторона", "Справа", "Слева", "Справа");
   public final BooleanSetting vanilla = new BooleanSetting("Ванильный стиль", true);
   public final SliderSetting bg = new SliderSetting("Фон", 1.0F, 0.0F, 1.0F, 0.05F, false);
   public PickupNotifier() {
      this.addSettings(new Setting[] { scale, duration, showXp, side, vanilla, bg });
   }
}

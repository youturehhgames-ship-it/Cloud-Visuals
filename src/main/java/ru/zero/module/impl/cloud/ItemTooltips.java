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
@IModule(name = "Item Tooltips", description = "Прочность и чары предмета в руке над хотбаром", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ItemTooltips extends Module {
   public final BooleanSetting durability = new BooleanSetting("Прочность", true);
   public final BooleanSetting enchants = new BooleanSetting("Чары", true);
   public final SliderSetting maxEnchants = new SliderSetting("Макс. чар", 3.0F, 1.0F, 10.0F, 1.0F, false);
   public ItemTooltips() {
      this.addSettings(new Setting[] { durability, enchants, maxEnchants });
   }
}

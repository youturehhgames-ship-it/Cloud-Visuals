package ru.zero.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.effect.StatusEffects;
import ru.zero.event.EventInit;
import ru.zero.event.impl.EventUpdate;
import ru.zero.module.api.Category;
import ru.zero.module.api.IModule;
import ru.zero.module.api.Module;
import ru.zero.module.api.setting.Setting;
import ru.zero.module.api.setting.impl.BooleanSetting;

@IModule(
   name = "No Render",
   description = " ",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class NoRender extends Module {
   public static final BooleanSetting fire = new BooleanSetting("Огонь", true);
   public static final BooleanSetting fog = new BooleanSetting("Туман", true);

   // CloudVisuals (ideas from LiquidBounce AntiBlind / NoHurtCam / NoBob): purely cosmetic overlays only
   public static final BooleanSetting inWall = new BooleanSetting("Блок в лицо", false);
   public static final BooleanSetting pumpkin = new BooleanSetting("Тыква", false);
   public static final BooleanSetting portal = new BooleanSetting("Портал", false);
   public static final BooleanSetting spyglass = new BooleanSetting("Подзорная труба", false);
   public static final BooleanSetting hurtCam = new BooleanSetting("Тряска от урона", false);
   public static final BooleanSetting bob = new BooleanSetting("Покачивание", false);

   public static boolean hides(BooleanSetting option) {
      if (ru.zero.Zero.get == null || ru.zero.Zero.get.manager == null) {
         return false;
      }
      NoRender m = ru.zero.Zero.get.manager.get(NoRender.class);
      return m != null && m.enable && option.get();
   }

   public NoRender() {
      this.addSettings(new Setting[]{fire, fog, inWall, pumpkin, portal, spyglass, hurtCam, bob});
   }

   @EventInit
   public void onUpdate(EventUpdate e) {
      if (mc.player != null) {
         if (mc.player.hasStatusEffect(StatusEffects.DARKNESS)) {
            mc.player.removeStatusEffect(StatusEffects.DARKNESS);
         }
      }
   }
}

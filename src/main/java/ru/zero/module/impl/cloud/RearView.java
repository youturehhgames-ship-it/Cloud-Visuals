package ru.zero.module.impl.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.zero.event.EventInit;
import ru.zero.event.lifecycle.ClientTickEvent;
import ru.zero.module.api.Category;
import ru.zero.module.api.IModule;
import ru.zero.module.api.Module;
import ru.zero.module.api.setting.Setting;
import ru.zero.module.api.setting.impl.BindSettings;
import ru.zero.module.api.setting.impl.ModeSetting;

// CloudVisuals (idea from LiquidBounce QuickPerspectiveSwap): hold a key to look at yourself / behind.
@IModule(name = "Rear View", description = "Пока держишь клавишу, камера переключается на вид от третьего лица", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class RearView extends Module {
   public final BindSettings key = new BindSettings("Клавиша", GLFW.GLFW_KEY_R);
   public final ModeSetting mode = new ModeSetting("Вид", "Спереди", "Спереди", "Сзади");

   private Perspective saved;
   private boolean active;

   public RearView() {
      this.addSettings(new Setting[] { key, mode });
   }

   @EventInit
   public void onClientTick(ClientTickEvent event) {
      if (mc == null || mc.getWindow() == null || mc.options == null) {
         return;
      }
      int k = key.key;
      boolean down = mc.currentScreen == null && k > 0 && InputUtil.isKeyPressed(mc.getWindow(), k);
      if (down && !active) {
         saved = mc.options.getPerspective();
         mc.options.setPerspective(mode.is("Сзади") ? Perspective.THIRD_PERSON_BACK : Perspective.THIRD_PERSON_FRONT);
         active = true;
      } else if (!down && active) {
         restore();
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      restore();
   }

   private void restore() {
      if (active && saved != null && mc != null && mc.options != null) {
         mc.options.setPerspective(saved);
      }
      active = false;
   }
}

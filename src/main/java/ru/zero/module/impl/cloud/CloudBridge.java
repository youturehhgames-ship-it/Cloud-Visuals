package ru.zero.module.impl.cloud;

import com.simplevisuals.Simplevisuals;
import com.simplevisuals.config.SimplevisualsConfig;
import dev.r3vy.motionsuite.config.Settings;
import java.util.function.DoubleConsumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import ru.zero.Zero;
import ru.zero.module.api.Module;

/**
 * CloudVisuals bridge: the Zero click-GUI is the single source of truth.
 * Every client tick the state of the wrapper modules is pushed into
 * SimpleVisuals' config and Motionsuite's settings.
 */
@Environment(EnvType.CLIENT)
public final class CloudBridge implements ClientModInitializer {
   private static boolean failed;

   @Override
   public void onInitializeClient() {
      ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
   }

   private static <T extends Module> T m(Class<T> type) {
      if (Zero.get == null || Zero.get.manager == null) {
         return null;
      }
      return Zero.get.manager.get(type);
   }

   private static boolean on(Module module) {
      return module != null && module.enable;
   }

   private static void tick() {
      if (failed) {
         return;
      }
      try {
         syncSimpleVisuals();
         syncMotion();
      } catch (Throwable t) {
         failed = true;
         System.err.println("[CloudVisuals] bridge disabled after error: " + t);
         t.printStackTrace();
      }
   }

   private static void syncSimpleVisuals() {
      SimplevisualsConfig cfg = Simplevisuals.getConfig();
      if (cfg == null || cfg.visuals == null) {
         return;
      }
      SimplevisualsConfig.Visuals v = cfg.visuals;

      v.enablePlayerLocator = on(m(PlayerLocator.class));
      v.enableChatHeads = on(m(ChatHeads.class));
      v.enableStatusEffectBars = on(m(EffectBars.class));
      v.enableMapTooltips = on(m(MapTooltips.class));
      v.enableAnvilFormatting = on(m(AnvilColors.class));
      v.enableRenamedItemTextures = on(m(RenamedTextures.class));

      ElytraPitch ep = m(ElytraPitch.class);
      v.enableElytraPitchHelper = on(ep);
      if (ep != null) {
         v.elytraTargetAngleUp = -ep.up.get();
         v.elytraTargetAngleDown = ep.down.get();
         v.elytraPitchTolerance = ep.tolerance.get();
      }

      SpeedLines sl = m(SpeedLines.class);
      v.speedLines.enableSpeedLines = on(sl);
      if (sl != null) {
         v.speedLines.speedLinesAlpha = sl.alpha.get();
         v.speedLines.speedLinesAmount = sl.amount.get();
         v.speedLines.speedLinesRadius = sl.radius.get();
         v.speedLines.speedLinesWidth = sl.width.get();
         v.speedLines.speedLinesSpeed = sl.speed.get();
         v.speedLines.speedThreshold = sl.threshold.get();
      }

      PickupNotifier pn = m(PickupNotifier.class);
      v.pickupNotifier.enablePickupNotifier = on(pn);
      if (pn != null) {
         v.pickupNotifier.pickupNotifierScale = pn.scale.get();
         v.pickupNotifier.pickupNotifierDuration = Math.round(pn.duration.get() * 20.0F);
         v.pickupNotifier.pickupNotifierShowXp = pn.showXp.get();
         v.pickupNotifier.pickupNotifierSide = pn.side.is("Слева")
            ? SimplevisualsConfig.PickupSide.LEFT
            : SimplevisualsConfig.PickupSide.RIGHT;
         v.pickupNotifier.pickupVanillaStyle = pn.vanilla.get();
         v.pickupNotifier.pickupBackgroundOpacity = pn.bg.get();
      }

      DamageNumbers dn = m(DamageNumbers.class);
      v.damageIndicators.enable = on(dn);
      if (dn != null) {
         v.damageIndicators.scale = dn.scale.get();
         v.damageIndicators.showBorder = dn.border.get();
      }

      BiomeInfo bi = m(BiomeInfo.class);
      v.biomeInfo.enable = on(bi);
      if (bi != null) {
         v.biomeInfo.displayDuration = Math.round(bi.duration.get() * 20.0F);
      }

      ItemTooltips it = m(ItemTooltips.class);
      v.heldItemTooltips.enable = on(it);
      if (it != null) {
         v.heldItemTooltips.showDurability = it.durability.get();
         v.heldItemTooltips.showEnchantments = it.enchants.get();
         v.heldItemTooltips.maxEnchantments = Math.round(it.maxEnchants.get());
      }

      DeathCoords dc = m(DeathCoords.class);
      v.enhanceDeathMessages = on(dc);
      if (!on(dc)) {
         v.deathCoordsMode = SimplevisualsConfig.Visuals.DeathCoordsMode.DISABLED;
      } else {
         v.deathCoordsMode = dc.mode.is("В сообщении")
            ? SimplevisualsConfig.Visuals.DeathCoordsMode.APPEND
            : SimplevisualsConfig.Visuals.DeathCoordsMode.SEPARATE;
      }
   }

   private static void syncMotion() {
      MotionBlur mb = m(MotionBlur.class);
      // UI 100% == internal 75%: above that the accumulation blur breaks down
      int amount = on(mb) ? Math.round(mb.strength.get() * 0.75F) : 0;
      if (Settings.BLUR.amount() != amount) {
         Settings.BLUR.setAmount(amount);
      }
      boolean adaptive = mb != null && mb.adaptive.get();
      if (Settings.BLUR.adaptive() != adaptive) {
         Settings.BLUR.setAdaptive(adaptive);
      }

      SmoothCamera sc = m(SmoothCamera.class);
      boolean cam = on(sc);
      if (Settings.CAMERA.enabled() != cam) {
         Settings.CAMERA.setEnabled(cam);
      }
      if (sc != null) {
         setIfChanged(Settings.CAMERA.speed(), sc.speed.get(), Settings.CAMERA::setSpeed);
         setIfChanged(Settings.CAMERA.firstPersonSpeed(), sc.fpSpeed.get(), Settings.CAMERA::setFirstPersonSpeed);
         setIfChanged(Settings.CAMERA.maxOffset(), sc.maxOffset.get(), Settings.CAMERA::setMaxOffset);
      }
   }

   private static void setIfChanged(double current, double wanted, DoubleConsumer setter) {
      if (Math.abs(current - wanted) > 1.0E-4) {
         setter.accept(wanted);
      }
   }
}

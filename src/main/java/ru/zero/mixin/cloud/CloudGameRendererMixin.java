package ru.zero.mixin.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zero.module.impl.visuals.NoRender;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class CloudGameRendererMixin {
   @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
   private void cloud$noHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      if (NoRender.hides(NoRender.hurtCam)) {
         ci.cancel();
      }
   }

   @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
   private void cloud$noBob(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      if (NoRender.hides(NoRender.bob)) {
         ci.cancel();
      }
   }
}

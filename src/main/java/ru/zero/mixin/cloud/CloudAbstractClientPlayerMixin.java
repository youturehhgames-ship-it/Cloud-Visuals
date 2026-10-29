package ru.zero.mixin.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zero.module.impl.cloud.StaticFov;

@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayerEntity.class)
public class CloudAbstractClientPlayerMixin {
   @Inject(method = "getFovMultiplier", at = @At("HEAD"), cancellable = true)
   private void cloud$staticFov(boolean firstPerson, float fovEffectScale, CallbackInfoReturnable<Float> cir) {
      if (StaticFov.active()) {
         cir.setReturnValue(1.0F);
      }
   }
}

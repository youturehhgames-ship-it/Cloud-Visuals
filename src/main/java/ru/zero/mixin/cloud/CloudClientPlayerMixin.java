package ru.zero.mixin.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zero.module.impl.cloud.NoSwing;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public class CloudClientPlayerMixin {
   // Skip only the local swing animation; still tell the server we swung.
   @Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At("HEAD"), cancellable = true)
   private void cloud$noSwing(Hand hand, CallbackInfo ci) {
      if (NoSwing.active()) {
         ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
         if (self.networkHandler != null) {
            self.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
         }
         ci.cancel();
      }
   }
}

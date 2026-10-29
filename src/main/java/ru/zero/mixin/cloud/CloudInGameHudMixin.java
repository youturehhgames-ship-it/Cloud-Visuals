package ru.zero.mixin.cloud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zero.module.impl.visuals.NoRender;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class CloudInGameHudMixin {
   @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
   private void cloud$hidePumpkin(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
      if (texture != null && "textures/misc/pumpkinblur.png".equals(texture.getPath()) && NoRender.hides(NoRender.pumpkin)) {
         ci.cancel();
      }
   }

   @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void cloud$hidePortal(DrawContext context, float strength, CallbackInfo ci) {
      if (NoRender.hides(NoRender.portal)) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
   private void cloud$hideSpyglass(DrawContext context, float scale, CallbackInfo ci) {
      if (NoRender.hides(NoRender.spyglass)) {
         ci.cancel();
      }
   }
}

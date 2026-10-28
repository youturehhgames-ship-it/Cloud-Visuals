package dev.r3vy.motionsuite.mixin;

import dev.r3vy.motionsuite.camera.CameraSmoother;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArgs(
            method = {"update", "method_19321"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void motionsuite$smoothPosition(Args args) {
        try {
            double x = args.get(0);
            double y = args.get(1);
            double z = args.get(2);
            double[] smoothed = CameraSmoother.apply(MinecraftClient.getInstance(), x, y, z);
            if (smoothed == null || Double.isNaN(smoothed[0]) || Double.isNaN(smoothed[1]) || Double.isNaN(smoothed[2])) {
                return;
            }
            args.set(0, smoothed[0]);
            args.set(1, smoothed[1]);
            args.set(2, smoothed[2]);
        } catch (RuntimeException error) {
            CameraSmoother.fail("camera", error);
        }
    }
}

package dev.r3vy.motionsuite.mixin.vk;

import dev.r3vy.motionsuite.VkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.vulkanmod.vulkan.Renderer")
public abstract class VkRsMixin {
    @Inject(method = "initRenderer", at = @At("TAIL"), remap = false)
    private static void motionsuite$afterVkInit(CallbackInfo ci) {
        VkLoader.call("install");
    }
}

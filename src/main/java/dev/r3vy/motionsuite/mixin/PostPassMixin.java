package dev.r3vy.motionsuite.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import dev.r3vy.motionsuite.blur.BlendUniform;
import dev.r3vy.motionsuite.blur.BlurGate;
import dev.r3vy.motionsuite.blur.FrameBlend;
import net.minecraft.client.gl.PostEffectPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(PostEffectPass.class)
public abstract class PostPassMixin {
    @Shadow(aliases = {"customUniforms", "uniformBuffers", "field_60123"})
    @Final
    private Map<String, GpuBuffer> customUniformBuffers;

    @Unique
    private GpuBuffer motionsuite$owned;

    @Unique
    private float motionsuite$ownedBlend = -1.0f;

    @Inject(method = {"method_62255", "render", "addToFrame"}, at = @At("HEAD"))
    private void motionsuite$refreshBlend(CallbackInfo ci) {
        if (!BlurGate.shouldRun()) {
            return;
        }
        GpuBuffer current = customUniformBuffers.get(BlendUniform.NAME);
        if (current == null) {
            return;
        }
        float blend = FrameBlend.next();
        if (current == motionsuite$owned && blend == motionsuite$ownedBlend) {
            return;
        }
        GpuBuffer replacement = BlendUniform.create(blend);
        if (replacement == null) {
            return;
        }
        customUniformBuffers.put(BlendUniform.NAME, replacement);
        motionsuite$owned = replacement;
        motionsuite$ownedBlend = blend;
        current.close();
    }
}

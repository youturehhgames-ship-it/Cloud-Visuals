package com.simplevisuals.mixin.client;

import com.simplevisuals.Simplevisuals;
import com.simplevisuals.client.EffectDurationTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.Iterator;

@Mixin(InGameHud.class)
public abstract class StatusEffectBarMixin {

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        EffectDurationTracker.cleanup(MinecraftClient.getInstance().player);
    }

    @Inject(
            method = "renderStatusEffectOverlay",
            at = @At(
                    value = "INVOKE",
                    // Ziel: drawGuiTexture(Pipeline, ID, x, y, w, h, color) -> 5 Ints am Ende
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void renderEffectBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci,
                                 Collection<?> collection,
                                 int i, int j,
                                 Iterator<?> iterator,
                                 StatusEffectInstance instance,
                                 RegistryEntry<StatusEffect> effectEntry,
                                 int k, int l,
                                 float f) {

        if (!Simplevisuals.getConfig().visuals.enableStatusEffectBars) return;
        if (instance.isInfinite()) return;

        float percent = EffectDurationTracker.getProgress(instance);
        percent = MathHelper.clamp(percent, 0.0f, 1.0f);

        int barWidth = 18;
        int barX = k + 3;
        int barY = l + 3 + 18;
        int barHeight = 1;

        int color = instance.getEffectType().value().getColor() | 0xFF000000;

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF000000);

        int progressWidth = (int) (barWidth * percent);
        if (progressWidth > 0) {
            context.fill(barX, barY, barX + progressWidth, barY + barHeight, color);
        }
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void clearDurations(CallbackInfo ci) {
        EffectDurationTracker.clear();
    }
}
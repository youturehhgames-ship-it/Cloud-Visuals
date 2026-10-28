package com.simplevisuals.mixin.client;

import com.simplevisuals.Simplevisuals;
import com.simplevisuals.client.EffectDurationTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;

@Mixin(targets = "net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay")
public abstract class InventoryEffectBarMixin {

    // 1. Cleanup auch beim Öffnen des Inventars durchführen
    @Inject(method = "drawStatusEffects", at = @At("HEAD"))
    private void onRenderStart(DrawContext context, Collection<StatusEffectInstance> effects, int x, int height, int mouseX, int mouseY, int width, CallbackInfo ci) {
        EffectDurationTracker.cleanup(MinecraftClient.getInstance().player);
    }

    // 2. Rendering mit zentralem Tracker
    @Inject(
            method = "drawStatusEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void renderInventoryBar(DrawContext context, Collection<StatusEffectInstance> effects, int x, int height, int mouseX, int mouseY, int width, CallbackInfo ci,
                                    Iterable<?> iterable,
                                    int yPos,
                                    TextRenderer textRenderer,
                                    java.util.Iterator<?> iterator,
                                    StatusEffectInstance instance,
                                    boolean bl,
                                    Text text,
                                    Text text2,
                                    int j
    ) {

        if (!Simplevisuals.getConfig().visuals.enableStatusEffectBars) return;
        if (instance.isInfinite()) return;

        // --- FIX: Nutzung des zentralen Trackers statt lokaler Map ---
        // Das holt sich den global gespeicherten Max-Wert (vom HUD oder vorherigen Öffnen)
        float percent = EffectDurationTracker.getProgress(instance);
        percent = MathHelper.clamp(percent, 0.0f, 1.0f);
        // -------------------------------------------------------------

        // Deine angepassten Koordinaten
        int barWidth = 24;
        int barX = x + 4;
        int barY = yPos + 7 + 18 + 3;
        int barHeight = 1;

        int color = instance.getEffectType().value().getColor() | 0xFF000000;

        // Hintergrund
        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF000000);

        // Fortschritt
        int progressWidth = (int) (barWidth * percent);
        if (progressWidth > 0) {
            context.fill(barX, barY, barX + progressWidth, barY + barHeight, color);
        }
    }
}
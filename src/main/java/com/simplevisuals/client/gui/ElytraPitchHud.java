package com.simplevisuals.client.gui;


import com.simplevisuals.Simplevisuals;
import com.simplevisuals.config.SimplevisualsConfig;
import com.simplevisuals.mixin.EntityAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class ElytraPitchHud implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        SimplevisualsConfig.Visuals config = Simplevisuals.getConfig().visuals;
        if (!config.enableElytraPitchHelper) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (player == null || !player.isGliding()) return;

        int cx = context.getScaledWindowWidth() / 2;
        int cy = context.getScaledWindowHeight() / 2;

        float tickDelta = tickCounter.getTickProgress(true);

        // FIX: Zugriff über Accessor (und lastPitch statt prevPitch)
        float prevPitch = ((EntityAccessor) player).getLastPitch();
        float pitch = MathHelper.lerp(tickDelta, prevPitch, player.getPitch());

        float[] targets = {config.elytraTargetAngleUp, config.elytraTargetAngleDown};

        for (float target : targets) {
            float diff = target - pitch;

            if (Math.abs(diff) <= config.elytraPitchTolerance) {
                float offset = diff * config.elytraSensitivity;
                int lineY = cy + (int) offset;

                float alphaPct = 1.0f - (Math.abs(diff) / config.elytraPitchTolerance);
                alphaPct = MathHelper.clamp(alphaPct, 0.0f, 1.0f);

                int alpha = (int) (alphaPct * 220);
                int color = (alpha << 24) | 0xFFFFFF;

                int width = 10;
                int thickness = 1;
                int shadowAlpha = (int) (alphaPct * 128);
                int shadowColor = (shadowAlpha << 24) | 0x000000;

                if (alpha > 5) {
                    context.fill(cx - width + 1, lineY + 1, cx + width + 1, lineY + thickness + 1, shadowColor);
                    context.fill(cx - width, lineY, cx + width, lineY + thickness, color);
                    context.fill(cx - 2, lineY - 1, cx + 1, lineY + 2, color);
                }
            }
        }
    }
}
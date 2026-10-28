package com.simplevisuals.client.gui;

import com.simplevisuals.Simplevisuals;
import com.simplevisuals.config.SimplevisualsConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class SpeedLinesOverlay implements HudRenderCallback {

    private final MinecraftClient client;
    private final Random random = Random.create();

    private double lastSpeed = 0;
    private float smoothedOpacity = 0.0f;
    private float animationTime = 0.0f;

    public SpeedLinesOverlay() {
        this.client = MinecraftClient.getInstance();
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        SimplevisualsConfig.Visuals config = Simplevisuals.getConfig().visuals;

        if (!config.speedLines.enableSpeedLines) return;
        if (client.player == null || client.options.hudHidden || client.options.getPerspective().isFrontView()) return;

        PlayerEntity player = client.player;

        // --- PHYSIK ---
        Vec3d velocity = player.getVelocity();
        // Horizontale Geschwindigkeit als Basis für Laufen/Rennen
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double totalSpeed = velocity.length();
        boolean isFlying = player.isGliding(); // 1.21 Name

        double activeSpeed = isFlying ? totalSpeed : horizontalSpeed;

        double acceleration = activeSpeed - lastSpeed;
        lastSpeed = activeSpeed;

        // Schwellenwert: 0.25 (Rennen ist ca 0.28, Gehen 0.21) -> Effekt nur beim Rennen/Fliegen
        float speedThreshold = config.speedLines.speedThreshold;

        // Faktor berechnen (0.0 bis 1.0)
        float speedFactor = (float) MathHelper.clamp((activeSpeed - speedThreshold) / (isFlying ? 1.0 : 0.5), 0.0, 1.0);
        float accelFactor = (float) MathHelper.clamp(acceleration * 15.0, 0.0, 1.0);

        float targetOpacity = Math.max(speedFactor * 0.6f, accelFactor * 0.9f);
        smoothedOpacity = MathHelper.lerp(0.1f, smoothedOpacity, targetOpacity);

        float finalOpacity = smoothedOpacity * config.speedLines.speedLinesAlpha;
        if (finalOpacity < 0.02f) return;

        // Animation Time (config.speedLinesSpeed steuert Basis-Tempo)
        animationTime += (0.2f + speedFactor * 1.0f) * config.speedLines.speedLinesSpeed;

        renderTunnelLines(context, finalOpacity, velocity, player, config);
    }

    private void renderTunnelLines(DrawContext context, float intensity, Vec3d velocity, PlayerEntity player, SimplevisualsConfig.Visuals config) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        // --- FLUCHTPUNKT (Verschiebung durch Bewegung) ---
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = look.crossProduct(up).normalize();
        Vec3d relativeUp = right.crossProduct(look).normalize();

        double sideSpeed = velocity.dotProduct(right);
        double vertSpeed = velocity.dotProduct(relativeUp);

        // Dezente Verschiebung
        float shiftX = (float)(sideSpeed * 100.0);
        float shiftY = (float)(vertSpeed * 80.0);

        float vanishX = centerX - MathHelper.clamp(shiftX, -width * 0.3f, width * 0.3f);
        float vanishY = centerY - MathHelper.clamp(shiftY, -height * 0.3f, height * 0.3f);

        // --- FARBE ---
        int c = config.speedLines.speedLinesColor;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;

        // --- PARAMETER ---
        int lineCount = (int) (30 * config.speedLines.speedLinesAmount);

        // MaxDist etwas größer als Bildschirm, damit Linien sauber verschwinden
        float maxDist = (float) Math.sqrt(width * width + height * height) * 0.7f;

        // Innerer Radius (Loch in der Mitte), konfigurierbar
        float minRadiusBase = height * Math.max(0.2f, config.speedLines.speedLinesRadius);

        // 1.21+ Matrix Methoden
        context.getMatrices().pushMatrix();

        for (int i = 0; i < lineCount; i++) {
            // Seed pro Linie für Konsistenz im Tunnel
            random.setSeed(i * 12345L);

            float angle = random.nextFloat() * (float) Math.PI * 2.0f;
            float lineSpeedVar = 10.0f + random.nextFloat() * 20.0f;
            float offset = random.nextFloat() * maxDist;

            // --- ANIMATION ---
            // Position der Linie im Tunnel (0 bis maxDist)
            float pos = (animationTime * lineSpeedVar + offset) % maxDist;

            // Tatsächlicher Startabstand vom Fluchtpunkt
            float distStart = minRadiusBase + pos;

            // Wenn zu weit draußen, überspringen (Modulo fängt eh wieder bei 0 an)
            if (distStart > maxDist) continue;

            // Länge wächst mit Config Scale und Geschwindigkeit
            float length = (50.0f + pos * 0.2f) * (0.8f + intensity) * config.speedLines.speedLinesScale;

            // Breite wächst, je näher die Linie dem Bildschirmrand kommt (Perspektive)
            float thickness = (1.5f + (pos / maxDist) * 3.0f) * config.speedLines.speedLinesWidth;

            // Fade-In am Anfang (Loch) und Fade-Out am Ende
            float fadeIn = MathHelper.clamp((distStart - minRadiusBase) / 50.0f, 0.0f, 1.0f);
            float fadeOut = 1.0f - MathHelper.clamp((distStart + length - maxDist * 0.8f) / (maxDist * 0.2f), 0.0f, 1.0f);

            int a = (int) (150 * intensity * fadeIn * fadeOut); // Max Alpha 150
            int color = (a << 24) | (r << 16) | (g << 8) | b;

            if (a < 3) continue;

            // --- ZEICHNEN ---
            context.getMatrices().pushMatrix();

            // 1. Zum Fluchtpunkt
            // translate(x, y) reicht für 2D
            context.getMatrices().translate(vanishX, vanishY);

            // 2. Rotieren in Strahlrichtung
            // rotate() nimmt radians
            context.getMatrices().rotate(angle);

            // 3. Nach außen zum Start der Linie schieben
            // Da wir rotiert haben, ist "außen" einfach X-Achse (oder Y, je nach Konvention)
            // Hier nutzen wir X
            context.getMatrices().translate(distStart, 0);

            // 4. Rechteck zeichnen (als Segment des Strahls)
            // fill(x1, y1, x2, y2, color)
            // Wir zeichnen von 0 bis length
            // thickness zentriert um die Achse
            context.fill(0, (int)(-thickness/2), (int)length, (int)(thickness/2), color);

            context.getMatrices().popMatrix();
        }

        context.getMatrices().popMatrix();
    }
}
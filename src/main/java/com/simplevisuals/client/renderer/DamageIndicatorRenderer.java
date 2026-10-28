package com.simplevisuals.client.renderer;

import com.simplevisuals.Simplevisuals;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DamageIndicatorRenderer {
    private static final List<Indicator> indicators = new ArrayList<>();

    // OPTIMIERUNG: Allocator statisch wiederverwenden, statt jeden Frame neu zu erstellen.
    private static final BufferAllocator allocator = new BufferAllocator(1024);

    public static void add(Vec3d pos, float amount, boolean isSpecial) {
        if (!Simplevisuals.getConfig().visuals.damageIndicators.enable) return;
        indicators.add(new Indicator(pos, amount, isSpecial));
    }

    public static void render(WorldRenderContext context) {
        if (indicators.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        Quaternionf cameraRotation = camera.getRotation();

        MatrixStack matrices = context.matrices();
        TextRenderer textRenderer = client.textRenderer;

        // Wir nutzen den statischen Allocator
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);

        var config = Simplevisuals.getConfig().visuals.damageIndicators;
        // Caching von Config-Werten für Schleife
        int colorNormal = config.colorNormal;
        int colorSpecial = config.colorSpecial;
        boolean showBorder = config.showBorder;
        float baseScale = 0.03f * config.scale;

        // Farben vorberechnen
        int borderArgbTemplate = 0xFF000000; // Alpha wird unten gesetzt

        Iterator<Indicator> it = indicators.iterator();
        while (it.hasNext()) {
            Indicator ind = it.next();
            ind.age++;

            if (ind.age > 40) {
                it.remove();
                continue;
            }

            double yOffset = MathHelper.lerp(ind.age / 40.0, 0.0, 1.2);

            matrices.push();
            matrices.translate(ind.pos.x - cameraPos.x, (ind.pos.y + 0.5 + yOffset) - cameraPos.y, ind.pos.z - cameraPos.z);
            matrices.multiply(cameraRotation);
            matrices.scale(-baseScale, -baseScale, baseScale);

            // OPTIMIERUNG: String-Konvertierung ist teuer.
            // Hier okay, aber bei tausenden Entities könnte man cachen.
            String textStr = String.valueOf((int)Math.ceil(ind.damage));
            Text text = Text.literal(textStr);

            float x = -textRenderer.getWidth(text) / 2.0f;
            int color = ind.isSpecial ? colorSpecial : colorNormal;

            int alpha = 255;
            if (ind.age > 25) {
                alpha = (int) (255 * (1.0f - (ind.age - 25) / 15.0f));
            }
            int argb = (alpha << 24) | (color & 0x00FFFFFF);
            int borderArgb = (alpha << 24) | 0x000000;

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            if (showBorder) {
                textRenderer.draw(text, x + 1, 1, borderArgb, false, matrix, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
                textRenderer.draw(text, x - 1, 1, borderArgb, false, matrix, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
                textRenderer.draw(text, x, 0, borderArgb, false, matrix, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
                textRenderer.draw(text, x, 2, borderArgb, false, matrix, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
            }

            textRenderer.draw(text, x, 1, argb, false, matrix, immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);

            matrices.pop();
        }

        immediate.draw();
        // Der Allocator wird von 'immediate.draw()' resettet, also ist er bereit für den nächsten Frame.
    }

    private static class Indicator {
        Vec3d pos;
        float damage;
        boolean isSpecial;
        int age;

        public Indicator(Vec3d pos, float damage, boolean isSpecial) {
            this.pos = pos;
            this.damage = damage;
            this.isSpecial = isSpecial;
            this.age = 0;
        }
    }
}
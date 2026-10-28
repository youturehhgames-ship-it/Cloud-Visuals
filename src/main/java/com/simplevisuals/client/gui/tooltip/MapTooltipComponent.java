package com.simplevisuals.client.gui.tooltip;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.util.Identifier;

public class MapTooltipComponent implements TooltipComponent {
    private final MapTooltipData data;

    public MapTooltipComponent(MapTooltipData data) {
        this.data = data;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return 128 + 4; // Map size (128) + padding
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 128; // Map size
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        if (this.data.mapState() == null) return;

        MinecraftClient client = MinecraftClient.getInstance();

        // 1. Hole die Textur-ID für den aktuellen MapState
        Identifier textureId = client.getMapTextureManager().getTextureId(this.data.mapId(), this.data.mapState());

        // 2. Zeichne die Karte direkt an Position x, y
        // Wir nutzen drawTexture statt drawGuiTexture, da Maps dynamische Texturen sind
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                textureId,
                x, y,          // Position im Tooltip
                0.0F, 0.0F,    // u, v Startkoordinaten
                128, 128,      // Breite, Höhe auf dem Bildschirm
                128, 128,      // Breite, Höhe der Region in der Textur
                128, 128,      // Gesamtbreite, Gesamthöhe der Textur
                -1             // Farbe (Weiß/Kein Tint)
        );

        // Optional: Einen Rahmen zeichnen, damit es wie ein Item aussieht
        // context.drawBorder(x - 1, y - 1, 128 + 2, 128 + 2, 0xFF000000);
    }
}
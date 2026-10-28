package com.simplevisuals.client.gui;

import com.simplevisuals.Simplevisuals;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.Map;

public class BiomeNotificationHud implements HudRenderCallback {

    private final MinecraftClient client = MinecraftClient.getInstance();
    
    // Status
    private RegistryKey<Biome> currentBiomeKey = null;
    private Text currentBiomeName = null;
    private int displayTimer = 0;
    private BlockPos lastPos = null;
    
    // Cooldown Speicher: BiomeID -> Zeitstempel (in ms) wann wir es zuletzt verlassen haben
    private final Map<Identifier, Long> biomeCooldowns = new HashMap<>();

    public void tick() {
        if (client.player == null || client.world == null) return;
        if (!Simplevisuals.getConfig().visuals.biomeInfo.enable) return;

        BlockPos currentPos = client.player.getBlockPos();

        // OPTIMIERUNG: Nur prüfen, wenn sich die Position geändert hat
        if (lastPos != null && currentPos.equals(lastPos)) {
            // Timer Logik muss trotzdem laufen
            if (displayTimer > 0) displayTimer--;
            return;
        }
        lastPos = currentPos;

        RegistryEntry<Biome> biomeEntry = client.world.getBiome(currentPos);
        // Versuchen den Key zu bekommen (1.21 Style)
        biomeEntry.getKey().ifPresent(key -> {
            if (key != currentBiomeKey) {
                long now = System.currentTimeMillis();
                long cooldownMs = Simplevisuals.getConfig().visuals.biomeInfo.cooldownSeconds * 1000L;
                Identifier biomeId = key.getValue();

                // Wenn wir das Biom gewechselt haben
                // 1. Speichere Zeit für das alte Biom (Start Cooldown)
                if (currentBiomeKey != null) {
                    biomeCooldowns.put(currentBiomeKey.getValue(), now);
                }

                // 2. Prüfe ob das neue Biom bereit ist
                long lastVisit = biomeCooldowns.getOrDefault(biomeId, 0L);
                if (now - lastVisit > cooldownMs) {
                    // Zeige Nachricht
                    currentBiomeName = Text.translatable("biome." + biomeId.getNamespace() + "." + biomeId.getPath());
                    displayTimer = Simplevisuals.getConfig().visuals.biomeInfo.displayDuration + 20; // +20 für Fade
                } else {
                    // Kein Pop-Up (Cooldown aktiv), aber wir merken uns, dass wir hier sind
                    // damit es nicht triggert, während wir drin stehen.
                }
                
                currentBiomeKey = key;
            }
        });

        if (displayTimer > 0) {
            displayTimer--;
        }
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (displayTimer <= 0 || currentBiomeName == null) return;
        if (client.options.hudHidden) return;

        TextRenderer tr = client.textRenderer;
        int width = context.getScaledWindowWidth();
        int y = Simplevisuals.getConfig().visuals.biomeInfo.yOffset;

        // Fade In / Out Logic
        float maxTime = Simplevisuals.getConfig().visuals.biomeInfo.displayDuration + 20;
        float alpha = 1.0f;
        
        // Fade In (erste 10 Ticks)
        if (displayTimer > maxTime - 10) {
            alpha = (maxTime - displayTimer) / 10.0f;
        } 
        // Fade Out (letzte 10 Ticks)
        else if (displayTimer < 10) {
            alpha = displayTimer / 10.0f;
        }

        int alphaInt = (int) (MathHelper.clamp(alpha, 0.0f, 1.0f) * 255);
        if (alphaInt < 5) return;

        int color = (alphaInt << 24) | 0xFFFFFF;
        
        int textWidth = tr.getWidth(currentBiomeName);
        int x = (width - textWidth) / 2;

        context.getMatrices().pushMatrix();
        // Leichtes Vergrößern beim Erscheinen
        float scale = 1.0f + (1.0f - alpha) * 0.5f; 
        // Skalierung um die Mitte
        context.getMatrices().translate(width / 2.0f, y + 4.0f);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-width / 2.0f, -(y + 4.0f));

        context.drawTextWithShadow(tr, currentBiomeName, x, y, color);
        
        // Untertitel "Biome" (optional, kleiner darunter)
        Text sub = Text.literal("Biome").formatted(net.minecraft.util.Formatting.GRAY);
        int subWidth = tr.getWidth(sub);
        context.drawTextWithShadow(tr, sub, (width - subWidth) / 2, y - 10, (alphaInt << 24) | 0xAAAAAA);

        context.getMatrices().popMatrix();
    }
}
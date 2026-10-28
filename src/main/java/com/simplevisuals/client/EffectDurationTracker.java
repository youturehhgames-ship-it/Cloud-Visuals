package com.simplevisuals.client;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;

public class EffectDurationTracker {
    private static final Map<StatusEffect, Integer> maxDurations = new HashMap<>();

    public static float getProgress(StatusEffectInstance instance) {
        StatusEffect effect = instance.getEffectType().value();
        int current = instance.getDuration();
        int max = maxDurations.getOrDefault(effect, 0);

        if (current > max) {
            max = current;
            maxDurations.put(effect, max);
        }

        return (float) current / (float) max;
    }

    public static void cleanup(PlayerEntity player) {
        if (player == null) return;

        // FIX: Wir prüfen manuell gegen die Liste der aktiven Effekte,
        // um den "RegistryEntry vs StatusEffect" Fehler zu umgehen.
        maxDurations.keySet().removeIf(effect -> {
            for (StatusEffectInstance active : player.getStatusEffects()) {
                if (active.getEffectType().value() == effect) {
                    return false; // Spieler hat den Effekt noch -> behalten
                }
            }
            return true; // Effekt nicht gefunden -> löschen
        });
    }

    public static void clear() {
        maxDurations.clear();
    }
}
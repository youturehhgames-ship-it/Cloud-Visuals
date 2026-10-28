package com.simplevisuals.mixin.client;

import com.simplevisuals.client.renderer.DamageIndicatorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealthMixin extends Entity {

    public LivingEntityHealthMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow public abstract float getHealth();

    @Unique private float lastHealth = -1;

    @Inject(method = "setHealth", at = @At("HEAD"))
    private void detectDamage(float health, CallbackInfo ci) {
        World world = this.getEntityWorld();
        // Sicherstellen, dass wir Client-seitig sind
        if (world != null && world.isClient()) {

            if (lastHealth == -1) {
                lastHealth = this.getHealth();
                return;
            }

            // Floating Point Fehler vermeiden
            if (Math.abs(lastHealth - health) < 0.01) return;

            float diff = lastHealth - health;

            // LOGGING AKTIVIERT: Prüfe die Konsole, ob Zeilen auftauchen, wenn du Mobs schlägst!
            if (diff > 0) {

                boolean isSpecial = false;
                if (diff > 8.0f) isSpecial = true; // Kritischer Treffer Simulation

                // Indikator hinzufügen - Höhe leicht variieren
                DamageIndicatorRenderer.add(this.getEntityPos().add(0, this.getHeight() * 0.5 + 0.5, 0), diff, isSpecial);
            }

            lastHealth = health;
        }
    }
}
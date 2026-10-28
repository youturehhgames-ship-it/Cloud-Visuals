package com.simplevisuals.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    // In deinen Mappings scheint es "lastPitch" zu heißen.
    // Falls das Spiel crasht mit "Mixin apply failed", ändere "lastPitch" zu "prevPitch".
    @Accessor("lastPitch")
    float getLastPitch();
}
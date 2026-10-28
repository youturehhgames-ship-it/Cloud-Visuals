package dev.r3vy.motionsuite;

import net.minecraft.util.Identifier;

public interface PostEffectControl {
    Identifier motionsuite$getEffectId();

    void motionsuite$setEffectId(Identifier id);

    void motionsuite$setEffectActive(boolean active);
}

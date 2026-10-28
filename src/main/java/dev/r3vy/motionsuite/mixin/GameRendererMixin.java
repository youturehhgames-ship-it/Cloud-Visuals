package dev.r3vy.motionsuite.mixin;

import dev.r3vy.motionsuite.PostEffectControl;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements PostEffectControl {
    @Shadow(aliases = {"postProcessorId", "postEffectId", "field_53898"})
    private Identifier effectId;

    @Shadow(aliases = {"postProcessorEnabled", "effectActive", "field_4013"})
    private boolean effectEnabled;

    @Override
    public Identifier motionsuite$getEffectId() {
        return effectId;
    }

    @Override
    public void motionsuite$setEffectId(Identifier id) {
        effectId = id;
    }

    @Override
    public void motionsuite$setEffectActive(boolean active) {
        effectEnabled = active;
    }
}

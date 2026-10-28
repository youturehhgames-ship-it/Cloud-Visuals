package dev.r3vy.motionsuite.blur;

import dev.r3vy.motionsuite.PostEffectControl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;

public final class GlBlur {
    private static final Identifier CHAIN_ID = Identifier.of("motionsuite", "motion_blur");

    private GlBlur() {
    }

    public static void tick() {
        GameRenderer renderer = MinecraftClient.getInstance().gameRenderer;
        if (!(renderer instanceof PostEffectControl control)) {
            return;
        }
        Identifier current = control.motionsuite$getEffectId();

        if (!BlurGate.shouldRun()) {
            if (CHAIN_ID.equals(current)) {
                control.motionsuite$setEffectActive(false);
                control.motionsuite$setEffectId(null);
            }
            FrameBlend.reset();
            return;
        }
        if (current != null && !CHAIN_ID.equals(current)) {
            return;
        }
        if (current == null) {
            control.motionsuite$setEffectId(CHAIN_ID);
        }
        control.motionsuite$setEffectActive(true);
    }
}

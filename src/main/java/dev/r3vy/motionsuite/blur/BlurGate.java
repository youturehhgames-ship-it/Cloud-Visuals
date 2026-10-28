package dev.r3vy.motionsuite.blur;

import dev.r3vy.motionsuite.config.Settings;
import net.minecraft.client.MinecraftClient;

public final class BlurGate {
    private BlurGate() {
    }

    public static boolean shouldRun() {
        return Settings.BLUR.amount() > 0 && inWorld() && MinecraftClient.getInstance().isWindowFocused();
    }

    public static boolean inWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null && client.player != null && client.currentScreen == null;
    }
}

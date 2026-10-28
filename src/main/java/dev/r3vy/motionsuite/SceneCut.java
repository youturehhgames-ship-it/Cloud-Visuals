package dev.r3vy.motionsuite;

import dev.r3vy.motionsuite.blur.FrameBlend;
import dev.r3vy.motionsuite.camera.CameraSmoother;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public final class SceneCut {
    private static final double TELEPORT_DIST_SQ = 64.0;

    private static double lastX;
    private static double lastY;
    private static double lastZ;
    private static boolean tracking;

    private SceneCut() {
    }

    public static void observe(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            tracking = false;
            return;
        }
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        if (tracking && !CameraSmoother.isSmoothing()) {
            double dx = x - lastX;
            double dy = y - lastY;
            double dz = z - lastZ;
            if (dx * dx + dy * dy + dz * dz > TELEPORT_DIST_SQ) {
                FrameBlend.cut();
            }
        }
        lastX = x;
        lastY = y;
        lastZ = z;
        tracking = true;
    }
}

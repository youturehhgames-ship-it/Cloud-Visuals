package dev.r3vy.motionsuite.blur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlendUniform {
    public static final String NAME = "MotionCfg";

    private static final Logger LOGGER = LoggerFactory.getLogger("motionsuite");
    private static final int BLOCK_SIZE = 16;
    private static final int USAGE_UNIFORM = 128;
    private static boolean failureReported;

    private BlendUniform() {
    }

    public static GpuBuffer create(float blend) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, BLOCK_SIZE);
            builder.putFloat(blend);
            return RenderSystem.getDevice().createBuffer(() -> "motionsuite blend", USAGE_UNIFORM, builder.get());
        } catch (RuntimeException e) {
            if (!failureReported) {
                failureReported = true;
                LOGGER.error("Could not allocate the blend uniform buffer", e);
            }
            return null;
        }
    }
}

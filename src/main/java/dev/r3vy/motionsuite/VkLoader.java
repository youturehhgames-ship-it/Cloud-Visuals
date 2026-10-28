package dev.r3vy.motionsuite;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VkLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("motionsuite");
    private static final String SETUP_CLASS = "dev.r3vy.motionsuite.vk.VkSetup";
    private static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("vulkanmod");
    private static final Map<String, MethodHandle> HANDLES = new ConcurrentHashMap<>();

    private static volatile boolean broken;

    private VkLoader() {
    }

    public static boolean present() {
        return PRESENT;
    }

    public static void call(String action) {
        if (!PRESENT || broken) {
            return;
        }
        try {
            MethodHandle handle = HANDLES.get(action);
            if (handle == null) {
                handle = resolve(action);
                HANDLES.put(action, handle);
            }
            handle.invokeExact();
        } catch (Throwable error) {
            broken = true;
            LOGGER.error("VulkanMod integration failed on '{}' and was disabled", action, error);
        }
    }

    private static MethodHandle resolve(String action) throws ReflectiveOperationException {
        Class<?> setup = Class.forName(SETUP_CLASS, true, VkLoader.class.getClassLoader());
        return MethodHandles.publicLookup().findStatic(setup, action, MethodType.methodType(void.class));
    }
}

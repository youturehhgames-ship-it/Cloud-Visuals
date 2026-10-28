package dev.r3vy.motionsuite;

import dev.r3vy.motionsuite.blur.GlBlur;
import dev.r3vy.motionsuite.camera.CameraSmoother;
import dev.r3vy.motionsuite.command.Commands;
import dev.r3vy.motionsuite.config.Settings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

public final class MbClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Settings.load();
        Commands.register();
        ClientTickEvents.END_CLIENT_TICK.register(MbClient::onEndTick);
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> CameraSmoother.onEntityLoad(entity));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> CameraSmoother.onEntityUnload(entity));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> VkLoader.call("disconnect"));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Settings.flush());
    }

    private static void onEndTick(MinecraftClient client) {
        CameraSmoother.tick(client);
        SceneCut.observe(client);
        if (VkLoader.present()) {
            VkLoader.call("tick");
        } else {
            GlBlur.tick();
        }
    }
}

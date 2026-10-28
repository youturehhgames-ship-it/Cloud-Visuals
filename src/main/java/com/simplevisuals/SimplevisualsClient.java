package com.simplevisuals;

import com.simplevisuals.client.ModelKeyRegistry; // Import der neuen Klasse
import com.simplevisuals.client.cit.CitReloadListener;
import com.simplevisuals.client.gui.BiomeNotificationHud;
import com.simplevisuals.client.gui.ElytraPitchHud;
import com.simplevisuals.client.gui.PickupNotifierHud;
import com.simplevisuals.client.gui.SpeedLinesOverlay;
import com.simplevisuals.client.gui.tooltip.MapTooltipComponent;
import com.simplevisuals.client.gui.tooltip.MapTooltipData;
import com.simplevisuals.client.renderer.DamageIndicatorRenderer;
import com.simplevisuals.client.resources.SimpleVisualsResourcePackProvider;
import com.simplevisuals.mixin.client.ResourcePackManagerAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class SimplevisualsClient implements ClientModInitializer {

    // Status, ob wir schon einmal neu geladen haben
    private static boolean hasReloadedResources = false;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new ElytraPitchHud());
        HudRenderCallback.EVENT.register(new SpeedLinesOverlay());

        BiomeNotificationHud biomeHud = new BiomeNotificationHud();
        HudRenderCallback.EVENT.register(biomeHud);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!client.isPaused()) {
                PickupNotifierHud.tick();
                biomeHud.tick();
            }
        });

        WorldRenderEvents.END_MAIN.register(DamageIndicatorRenderer::render);

        TooltipComponentCallback.EVENT.register(data -> {
            if (Simplevisuals.getConfig().visuals.enableMapTooltips && data instanceof MapTooltipData mapData) {
                return new MapTooltipComponent(mapData);
            }
            return null;
        });

        // ---------------------------------------------------------------
        // --- CUSTOM TEXTURES / CIT SYSTEM (Nur wenn in Config aktiv) ---
        // ---------------------------------------------------------------
        if (Simplevisuals.getConfig().visuals.enableRenamedItemTextures) {

            System.out.println("[SimpleVisuals] Custom Renamed Item Textures are ENABLED.");

            // 1. CIT Reload Listener
            ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                    .registerReloadListener(new CitReloadListener());

            // 2. Model Loading Plugin (Extra Modelle registrieren)
            ModelLoadingPlugin.register(ctx -> {
                Path textureDir = FabricLoader.getInstance().getConfigDir().resolve("simplevisuals/textures");
                if (Files.exists(textureDir)) {
                    try (Stream<Path> stream = Files.list(textureDir)) {
                        stream.filter(p -> p.toString().toLowerCase().endsWith(".png")).forEach(path -> {
                            String filename = path.getFileName().toString().toLowerCase();
                            String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));

                            Identifier modelId = Identifier.of("simplevisuals", "item/" + nameWithoutExt);
                            var key = ModelKeyRegistry.getKey(modelId);

                            // Registrieren als BlockStateModel
                            ctx.addModel(key, SimpleUnbakedExtraModel.blockStateModel(modelId));
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            // 3. Resource Pack Provider & Auto-Reload beim Start
            ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
                Path textureDir = FabricLoader.getInstance().getConfigDir().resolve("simplevisuals/textures");

                // Provider hinzufügen
                var manager = (ResourcePackManagerAccessor) client.getResourcePackManager();
                manager.getProviders().add(new SimpleVisualsResourcePackProvider(textureDir));

                // Scannen
                client.getResourcePackManager().scanPacks();

                // Zwangsladen beim ersten Start
                if (!hasReloadedResources) {
                    hasReloadedResources = true;
                    System.out.println("[SimpleVisuals] Triggering initial resource reload...");
                    client.reloadResources();
                }
            });
        } else {
            System.out.println("[SimpleVisuals] Custom Renamed Item Textures are DISABLED via config.");
        }
    }
}
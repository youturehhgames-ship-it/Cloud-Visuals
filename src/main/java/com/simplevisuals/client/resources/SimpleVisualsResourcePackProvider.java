package com.simplevisuals.client.resources;

import net.minecraft.resource.*;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class SimpleVisualsResourcePackProvider implements ResourcePackProvider {
    private final Path rootPath; // Wir speichern nur den Pfad, nicht das Pack!

    public SimpleVisualsResourcePackProvider(Path rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public void register(Consumer<ResourcePackProfile> profileAdder) {
        ResourcePackInfo info = new ResourcePackInfo(
                "simplevisuals_dynamic",
                Text.literal("Simple Visuals Assets"),
                ResourcePackSource.BUILTIN,
                Optional.empty()
        );

        // FIX: Erstelle bei jedem Aufruf ein NEUES Pack -> Scant Ordner neu bei F3+T
        ResourcePackProfile.PackFactory factory = new ResourcePackProfile.PackFactory() {
            @Override
            public ResourcePack open(ResourcePackInfo info) {
                return new DynamicResourcePack("simplevisuals_dynamic", rootPath);
            }

            @Override
            public ResourcePack openWithOverlays(ResourcePackInfo info, ResourcePackProfile.Metadata metadata) {
                return new DynamicResourcePack("simplevisuals_dynamic", rootPath);
            }
        };

        ResourcePackProfile profile = ResourcePackProfile.create(
                info,
                factory,
                ResourceType.CLIENT_RESOURCES,
                new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, false)
        );

        if (profile != null) {
            profileAdder.accept(profile);
        }
    }
}
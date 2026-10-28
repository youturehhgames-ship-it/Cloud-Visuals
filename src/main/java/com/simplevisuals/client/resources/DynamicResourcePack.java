package com.simplevisuals.client.resources;

import com.google.common.base.Charsets;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.PackVersion;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Range;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class DynamicResourcePack implements ResourcePack {

    private final String id;
    private final Path rootPath;
    private final Map<Identifier, String> generatedFiles = new HashMap<>();
    private final Map<Identifier, Path> externalTextures = new HashMap<>();

    public DynamicResourcePack(String id, Path rootPath) {
        this.id = id;
        this.rootPath = rootPath;
        scanForTextures();
    }

    private void scanForTextures() {
        generatedFiles.clear();
        externalTextures.clear();

        if (!Files.exists(rootPath)) {
            try { Files.createDirectories(rootPath); } catch (IOException e) { e.printStackTrace(); }
            return;
        }

        try (Stream<Path> stream = Files.list(rootPath)) {
            stream.filter(p -> p.toString().toLowerCase().endsWith(".png")).forEach(path -> {
                String filename = path.getFileName().toString();
                String lowerFilename = filename.toLowerCase();
                String nameWithoutExt = lowerFilename.substring(0, lowerFilename.lastIndexOf('.'));

                // 1. Textur registrieren
                Identifier textureId = Identifier.of("simplevisuals", "textures/item/" + lowerFilename);
                externalTextures.put(textureId, path);

                // --- 2. Modell generieren (FIX: Explizites 3D-Modell statt 'generated') ---
                // Wir bauen ein flaches Quadrat (0-16 Pixel), damit es immer sichtbar ist.
                String texturePath = "simplevisuals:item/" + nameWithoutExt;

                String jsonModel = "{\n" +
                        "  \"textures\": {\n" +
                        "    \"layer0\": \"" + texturePath + "\",\n" +
                        "    \"particle\": \"" + texturePath + "\"\n" +
                        "  },\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"from\": [ 0, 0, 7.5 ],\n" +
                        "      \"to\": [ 16, 16, 8.5 ],\n" +
                        "      \"faces\": {\n" +
                        "        \"north\": { \"uv\": [ 16, 0, 0, 16 ], \"texture\": \"#layer0\" },\n" +
                        "        \"south\": { \"uv\": [ 0, 0, 16, 16 ], \"texture\": \"#layer0\" }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"display\": {\n" +
                        "        \"thirdperson_righthand\": {\n" +
                        "            \"rotation\": [ 0, 0, 0 ],\n" +
                        "            \"translation\": [ 0, 3, 1 ],\n" +
                        "            \"scale\": [ 0.55, 0.55, 0.55 ]\n" +
                        "        },\n" +
                        "        \"firstperson_righthand\": {\n" +
                        "            \"rotation\": [ 0, -90, 25 ],\n" +
                        "            \"translation\": [ 1.13, 3.2, 1.13 ],\n" +
                        "            \"scale\": [ 0.68, 0.68, 0.68 ]\n" +
                        "        },\n" +
                        "        \"gui\": {\n" +
                        "            \"rotation\": [ 0, 0, 0 ],\n" +
                        "            \"translation\": [ 0, 0, 0 ],\n" +
                        "            \"scale\": [ 1, 1, 1 ]\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

                Identifier modelId = Identifier.of("simplevisuals", "models/item/" + nameWithoutExt + ".json");
                generatedFiles.put(modelId, jsonModel);

                // 3. CIT Regel generieren
                String targetItem = "minecraft:stick";
                String targetName = nameWithoutExt;
                if (nameWithoutExt.startsWith("stick_")) {
                    targetName = nameWithoutExt.substring(6);
                }

                String citJson = "{\n" +
                        "  \"item\": \"" + targetItem + "\",\n" +
                        "  \"name\": \"" + targetName + "\",\n" +
                        "  \"model\": \"simplevisuals:item/" + nameWithoutExt + "\"\n" +
                        "}";

                Identifier citId = Identifier.of("simplevisuals", "cit/" + nameWithoutExt + ".json");
                generatedFiles.put(citId, citJson);

                System.out.println("[SimpleVisuals] DEBUG: Registered " + nameWithoutExt);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @Nullable InputSupplier<InputStream> openRoot(String... segments) { return null; }

    @Override
    public @Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        if (type != ResourceType.CLIENT_RESOURCES) return null;

        if (generatedFiles.containsKey(id)) {
            return () -> new ByteArrayInputStream(generatedFiles.get(id).getBytes(Charsets.UTF_8));
        }

        if (externalTextures.containsKey(id)) {
            Path path = externalTextures.get(id);
            return () -> {
                System.out.println("[SimpleVisuals] READ STREAM: " + id);
                return Files.newInputStream(path);
            };
        }
        return null;
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResourcePack.ResultConsumer consumer) {
        if (type != ResourceType.CLIENT_RESOURCES || !"simplevisuals".equals(namespace)) return;

        for (Identifier id : generatedFiles.keySet()) {
            if (id.getPath().startsWith(prefix)) {
                consumer.accept(id, open(type, id));
            }
        }
        for (Identifier id : externalTextures.keySet()) {
            if (id.getPath().startsWith(prefix)) {
                consumer.accept(id, open(type, id));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) { return Set.of("simplevisuals"); }

    @Override
    public <T> @Nullable T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer) throws IOException {
        if (metadataSerializer == PackResourceMetadata.CLIENT_RESOURCES_SERIALIZER) {
            return (T) new PackResourceMetadata(Text.literal("Simple Visuals Auto-Gen"), new Range<>(PackVersion.of(42)));
        }
        return null;
    }

    @Override
    public ResourcePackInfo getInfo() {
        return new ResourcePackInfo(id, Text.literal("Simple Visuals Generated"), net.minecraft.resource.ResourcePackSource.BUILTIN, Optional.empty());
    }

    @Override
    public void close() { }
}
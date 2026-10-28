package com.simplevisuals.client.cit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CitReloadListener implements SimpleSynchronousResourceReloadListener {

    private static final Identifier ID = Identifier.of("simplevisuals", "cit_loader");
    private static final Gson GSON = new Gson();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        CitRegistry.clear();
        int count = 0;

        ResourceFinder finder = ResourceFinder.json("cit");

        for (Map.Entry<Identifier, Resource> entry : finder.findResources(manager).entrySet()) {
            Identifier fileId = entry.getKey();

            try (Reader reader = entry.getValue().getReader()) {
                JsonObject json = JsonHelper.deserialize(GSON, reader, JsonObject.class);

                if (json.has("item") && json.has("name") && json.has("model")) {
                    // Item
                    String itemIdStr = json.get("item").getAsString();
                    Item item = Registries.ITEM.get(Identifier.of(itemIdStr));

                    // Name
                    String name = json.get("name").getAsString();

                    // Model ID (Intelligente Korrektur)
                    String modelStr = json.get("model").getAsString();
                    Identifier modelId;

                    // Fall 1: Benutzer gibt kompletten Pfad an (namespace:path)
                    if (modelStr.contains(":")) {
                        modelId = Identifier.of(modelStr);
                    } else {
                        // Fall 2: Benutzer gibt nur "diamond" an -> wir machen "minecraft:item/diamond" draus
                        // oder zumindest "minecraft:diamond" und lassen Minecraft den Rest machen (in 1.21 oft "item/" nötig)
                        if (!modelStr.startsWith("item/")) {
                            modelStr = "item/" + modelStr;
                        }
                        modelId = Identifier.of("minecraft", modelStr);
                    }

                    // Tags laden
                    List<String> tags = new ArrayList<>();
                    if (json.has("tags")) {
                        JsonArray tagArray = json.getAsJsonArray("tags");
                        for (JsonElement e : tagArray) {
                            tags.add(e.getAsString());
                        }
                    }

                    // ID des Eintrags
                    Identifier entryId = json.has("id")
                            ? Identifier.of(json.get("id").getAsString())
                            : fileId;

                    float weight = json.has("weight") ? json.get("weight").getAsFloat() : 0.0f;

                    // Registrieren
                    CitEntry citEntry = new CitEntry(entryId, name, modelId, tags, weight);
                    CitRegistry.register(item, citEntry);
                    count++;
                }

            } catch (Exception e) {
                System.err.println("[SimpleVisuals] Fehler beim Laden von CIT " + fileId + ": " + e.getMessage());
            }
        }

        System.out.println("[SimpleVisuals] " + count + " CIT-Regeln geladen.");
    }
}
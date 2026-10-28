package com.simplevisuals.client.cit;

import com.simplevisuals.client.ModelKeyRegistry;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BlockStateModel; // Import
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CitRegistry {

    private static final Map<Item, Map<String, List<CitEntry>>> ENTRIES = new HashMap<>();

    public static void register(Item item, CitEntry entry) {
        ENTRIES.computeIfAbsent(item, k -> new HashMap<>())
               .computeIfAbsent(entry.name(), k -> new ArrayList<>())
               .add(entry);
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static List<CitEntry> getEntries(Item item) {
        List<CitEntry> allEntries = new ArrayList<>();
        Map<String, List<CitEntry>> namedEntries = ENTRIES.get(item);
        if (namedEntries != null) {
            for (List<CitEntry> list : namedEntries.values()) {
                allEntries.addAll(list);
            }
        }
        return allEntries;
    }

    // FIX: Rückgabetyp ist jetzt BlockStateModel, nicht ResolvableModel
    public static BlockStateModel getModel(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return null;

        String name = stack.getName().getString();
        var namesMap = ENTRIES.get(stack.getItem());
        if (namesMap == null) return null;

        List<CitEntry> entries = namesMap.get(name);
        if (entries == null || entries.isEmpty()) return null;

        CitEntry bestMatch = entries.get(0);

        if (bestMatch.modelId() != null) {
            BakedModelManager modelManager = MinecraftClient.getInstance().getBakedModelManager();
            FabricBakedModelManager fabricManager = (FabricBakedModelManager) modelManager;

            var key = ModelKeyRegistry.getKey(bestMatch.modelId());

            // HIER IST DER FIX: Wir casten zu BlockStateModel.
            // Der Crash sagte, es ist ein 'SimpleBlockStateModel', also klappt dieser Cast.
            return (BlockStateModel) fabricManager.getModel(key);
        }

        return null;
    }
}
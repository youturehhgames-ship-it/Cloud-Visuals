package com.simplevisuals.client.cit;

import net.minecraft.util.Identifier;
import java.util.List;

public record CitEntry(
        Identifier id,          // Eindeutige ID (z.B. "mypack:emerald_stick")
        String name,            // Der Name für den Amboss (z.B. "JsonTest")
        Identifier modelId,     // Das Model (z.B. "minecraft:item/emerald")
        List<String> tags,      // Tags für die Suche (z.B. ["grün", "edelstein"])
        float weight            // Sortierung
) {}
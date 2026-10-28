package com.simplevisuals.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class ModelKeyRegistry {
    // Speichert Identifier -> Key, damit wir später darauf zugreifen können
    private static final Map<Identifier, ExtraModelKey<BlockStateModel>> KEYS = new HashMap<>();

    public static ExtraModelKey<BlockStateModel> getKey(Identifier id) {
        return KEYS.computeIfAbsent(id, k -> ExtraModelKey.create(k::toString));
    }
}
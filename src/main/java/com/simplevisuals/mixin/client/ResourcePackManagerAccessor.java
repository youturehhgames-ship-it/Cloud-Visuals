package com.simplevisuals.mixin.client;

import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ResourcePackManager.class)
public interface ResourcePackManagerAccessor {
    // Macht das private Feld 'providers' zugänglich
    @Accessor("providers")
    Set<ResourcePackProvider> getProviders();
}
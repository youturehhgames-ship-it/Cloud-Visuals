package com.simplevisuals.mixin.client;

import com.simplevisuals.client.gui.tooltip.MapTooltipData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

// Erbt jetzt direkt von Item, da NetworkSyncedItem entfernt wurde
@Mixin(FilledMapItem.class)
public abstract class FilledMapItemMixin extends Item {

    // Konstruktor muss mit Item übereinstimmen
    public FilledMapItemMixin(Settings settings) {
        super(settings);
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        // Prüfen, ob eine Map ID existiert
        if (stack.contains(DataComponentTypes.MAP_ID)) {
            MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);

            // Client-Welt holen
            World world = MinecraftClient.getInstance().world;
            if (world != null) {
                // MapState abrufen (statische Methode in FilledMapItem)
                MapState state = FilledMapItem.getMapState(mapId, world);

                if (state != null) {
                    return Optional.of(new MapTooltipData(mapId, state));
                }
            }
        }
        return Optional.empty();
    }
}
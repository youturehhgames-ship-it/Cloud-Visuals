package com.simplevisuals.mixin.client;

import com.simplevisuals.client.gui.PickupNotifierHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Shadow private ClientWorld world;

    @Unique private int simplevisuals$lastEntityId = -1;
    @Unique private long simplevisuals$lastPickupTime = 0;

    @Inject(method = "onEntitiesDestroy", at = @At("HEAD"))
    private void onEntitiesDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        if (this.world == null) return;

        for (int id : packet.getEntityIds()) {
            Entity entity = this.world.getEntityById(id);
            if (entity instanceof ItemEntity itemEntity) {
                // Stack kopieren bevor er gelöscht wird.
                // Wichtig: !isEmpty checken, damit wir keine leeren Stacks in den Cache ballern.
                ItemStack stack = itemEntity.getStack();
                if (!stack.isEmpty()) {
                    PickupNotifierHud.cacheEntity(id, stack.copy());
                }
            } else if (entity instanceof ExperienceOrbEntity xpOrb) {
                PickupNotifierHud.cacheXp(id, xpOrb.getValue());
            }
        }
    }

    @Inject(method = "onItemPickupAnimation", at = @At("HEAD"))
    private void onPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || this.world == null) return;

        Entity collector = this.world.getEntityById(packet.getCollectorEntityId());
        if (collector != client.player) return;

        long currentTime = System.currentTimeMillis();
        if (packet.getEntityId() == simplevisuals$lastEntityId && (currentTime - simplevisuals$lastPickupTime) < 150) {
            return;
        }
        simplevisuals$lastEntityId = packet.getEntityId();
        simplevisuals$lastPickupTime = currentTime;

        Entity pickedUpEntity = this.world.getEntityById(packet.getEntityId());

        ItemStack stack = ItemStack.EMPTY;
        int xpValue = 0;
        int count = packet.getStackAmount();

        // 1. Entity in Welt suchen
        if (pickedUpEntity != null) {
            if (pickedUpEntity instanceof ItemEntity itemEntity) {
                stack = itemEntity.getStack();
                if (count <= 0) count = stack.getCount();
            } else if (pickedUpEntity instanceof ExperienceOrbEntity xpOrb) {
                xpValue = xpOrb.getValue();
            }
        }
        // 2. Cache suchen (falls schon gelöscht)
        else {
            PickupNotifierHud.CachedPickup cached = PickupNotifierHud.getCachedPickup(packet.getEntityId());
            if (cached != null) {
                stack = cached.stack();
                xpValue = cached.xpValue();
                if (count <= 0 && !stack.isEmpty()) {
                    count = stack.getCount();
                }
            }
        }

        // 3. Ergebnis
        if (!stack.isEmpty()) {
            if (count <= 0) count = 1;
            PickupNotifierHud.addNotification(stack, count);
        }
        else if (xpValue > 0) {
            PickupNotifierHud.addXpNotification(xpValue);
        }
        else {
            // FIX: Wenn weder Welt noch Cache das Item haben (oder es leer ist),
            // merken wir uns die ID und versuchen es in den nächsten Ticks erneut.
            // (Datenpaket kommt wahrscheinlich gleich an).
            if (count <= 0) count = 1;
            PickupNotifierHud.schedulePendingPickup(packet.getEntityId(), count);
        }
    }
}
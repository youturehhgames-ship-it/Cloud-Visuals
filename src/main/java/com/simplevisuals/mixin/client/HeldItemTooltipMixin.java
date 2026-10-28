package com.simplevisuals.mixin.client;

import com.simplevisuals.Simplevisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(InGameHud.class)
public abstract class HeldItemTooltipMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow private int heldItemTooltipFade;
    @Shadow private ItemStack currentStack;

    // FIX 2: Trigger-Logik für Verzauberungen
    // Minecraft prüft standardmäßig nur Name & Item-Typ. Wir prüfen zusätzlich auf Enchantment-Änderungen.
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.client.player == null) return;
        if (!Simplevisuals.getConfig().visuals.heldItemTooltips.enable) return;

        ItemStack newStack = this.client.player.getMainHandStack();

        // Wenn wir bereits einen Stack haben und das neue Item nicht leer ist
        if (this.currentStack != null && !this.currentStack.isEmpty() && !newStack.isEmpty()) {

            // Wenn Vanilla sagen würde "Das ist das gleiche Item" (gleiches Item + gleicher Name)
            if (newStack.isOf(this.currentStack.getItem()) &&
                    newStack.getName().equals(this.currentStack.getName())) {

                // ... aber die Verzauberungen anders sind
                if (!Objects.equals(newStack.get(DataComponentTypes.ENCHANTMENTS),
                        this.currentStack.get(DataComponentTypes.ENCHANTMENTS))) {

                    // Dann erzwingen wir den Reset des Tooltips (40 Ticks * Multiplier)
                    this.heldItemTooltipFade = (int)(40.0 * this.client.options.getNotificationDisplayTime().getValue());
                    this.currentStack = newStack; // Stack aktualisieren, damit es nicht loopt
                }
            }
        }
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void renderEnhancedTooltip(DrawContext context, CallbackInfo ci) {
        if (!Simplevisuals.getConfig().visuals.heldItemTooltips.enable) return;

        int fade = this.heldItemTooltipFade;
        if (fade > 0 && this.currentStack != null && !this.currentStack.isEmpty()) {
            ci.cancel();

            int screenWidth = context.getScaledWindowWidth();
            int screenHeight = context.getScaledWindowHeight();
            int alpha = (int)((float)fade * 256.0F / 10.0F);
            if (alpha > 255) alpha = 255;

            if (alpha > 0) {
                List<Text> lines = new ArrayList<>();

                // 1. Item Name
                MutableText name = Text.empty().append(this.currentStack.getName()).formatted(this.currentStack.getRarity().getFormatting());
                if (this.currentStack.contains(DataComponentTypes.CUSTOM_NAME)) {
                    name.formatted(Formatting.ITALIC);
                }
                lines.add(name);

                // 2. Haltbarkeit
                if (Simplevisuals.getConfig().visuals.heldItemTooltips.showDurability && this.currentStack.isDamageable()) {
                    int max = this.currentStack.getMaxDamage();
                    int current = max - this.currentStack.getDamage();
                    int percent = (int) ((double) current / max * 100);

                    Formatting color = Formatting.GREEN;
                    if (percent < 60) color = Formatting.YELLOW;
                    if (percent < 25) color = Formatting.RED;

                    lines.add(Text.literal("Durability: " + current + " / " + max + " (" + percent + "%)").formatted(color));
                }

                // 3. Verzauberungen
                if (Simplevisuals.getConfig().visuals.heldItemTooltips.showEnchantments) {
                    ItemEnchantmentsComponent enchantments = this.currentStack.get(DataComponentTypes.ENCHANTMENTS);
                    if (enchantments != null && !enchantments.isEmpty()) {
                        int count = 0;
                        int maxShow = Simplevisuals.getConfig().visuals.heldItemTooltips.maxEnchantments;

                        for (var entry : enchantments.getEnchantmentEntries()) {
                            if (count >= maxShow) {
                                lines.add(Text.literal("... and " + (enchantments.getSize() - count) + " more").formatted(Formatting.GRAY, Formatting.ITALIC));
                                break;
                            }
                            RegistryEntry<Enchantment> enchant = entry.getKey();
                            int level = entry.getIntValue();

                            // FIX 1: .copy() nutzen!
                            // Verhindert "UnsupportedOperationException", da wir eine Kopie bearbeiten statt das Original.
                            MutableText enchText = enchant.value().description().copy();

                            if (level != 1 || enchant.value().getMaxLevel() != 1) {
                                enchText.append(" ").append(Text.translatable("enchantment.level." + level));
                            }
                            lines.add(enchText.formatted(Formatting.GRAY));
                            count++;
                        }
                    }
                }

                // RENDERING
                TextRenderer textRenderer = this.client.textRenderer;
                int y = screenHeight - 59;

                if (!this.client.interactionManager.hasStatusBars()) {
                    y += 14;
                }

                for (int i = lines.size() - 1; i >= 0; i--) {
                    Text line = lines.get(i);
                    int x = (screenWidth - textRenderer.getWidth(line)) / 2;

                    int colorWithAlpha = (alpha << 24) | 0xFFFFFF;

                    context.drawTextWithShadow(textRenderer, line, x, y, colorWithAlpha);

                    y -= 10;
                }
            }
        }
    }
}
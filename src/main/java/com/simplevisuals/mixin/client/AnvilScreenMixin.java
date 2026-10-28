package com.simplevisuals.mixin.client;

import com.simplevisuals.client.cit.CitRegistry;
import com.simplevisuals.client.gui.VisualsScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends HandledScreen<AnvilScreenHandler> {

    @Unique
    private ButtonWidget visualsButton;

    public AnvilScreenMixin(AnvilScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    // FEHLERBEHEBUNG: "setup" statt "init" nutzen
    @Inject(method = "setup", at = @At("TAIL"))
    private void setupVisualsButton(CallbackInfo ci) {
        // Position: Rechts neben dem Amboss-GUI
        int x = this.x + this.backgroundWidth + 5;
        int y = this.y + 20;

        // Button erstellen
        this.visualsButton = ButtonWidget.builder(Text.literal("V"), button -> {
                    // Rufe den neuen Screen auf
                    // "this" ist hier der AnvilScreen (durch Mixin), "handler.getSlot(0)..." ist das Item
                    ItemStack stack = this.handler.getSlot(0).getStack();
                    if (!stack.isEmpty()) {
                        this.client.setScreen(new VisualsScreen(this, stack.getItem()));
                    }
                })
        .dimensions(x, y, 20, 20)
        .build();

        this.visualsButton.visible = false;
        this.addDrawableChild(this.visualsButton);
    }

    // Button-Sichtbarkeit jeden Tick prüfen
    @Inject(method = "handledScreenTick", at = @At("TAIL"))
    private void updateVisualsButton(CallbackInfo ci) {
        if (this.visualsButton != null) {
            ItemStack stack = this.handler.getSlot(0).getStack();

            // Zeige Button nur, wenn Items im Slot liegen UND es CIT-Regeln dafür gibt
            boolean hasVisuals = !stack.isEmpty() && !CitRegistry.getEntries(stack.getItem()).isEmpty();

            this.visualsButton.visible = hasVisuals;

            // Position aktualisieren, falls sich das Fenster ändert
            this.visualsButton.setX(this.x + this.backgroundWidth + 5);
            this.visualsButton.setY(this.y + 20);
        }
    }

    // 3. Deine bestehende Render-Logik (Repair Cost)
    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void renderAnvilUses(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        // Slot 0: Linker Input
        ItemStack inputStack = this.handler.getSlot(0).getStack();
        // Slot 2: Output (Ergebnis)
        ItemStack outputStack = this.handler.getSlot(2).getStack();

        // Bedingung:
        // 1. Input muss da sein (!isEmpty)
        // 2. Output muss LEER sein (outputStack.isEmpty())
        //    -> Denn wenn der Output voll ist, rendert Minecraft dort bereits die "Level Cost".
        if (!inputStack.isEmpty() && outputStack.isEmpty()) {

            int repairCost = inputStack.getOrDefault(DataComponentTypes.REPAIR_COST, 0);
            int uses = 0;

            if (repairCost > 0) {
                // Berechnung der Uses: log2(repairCost + 1)
                uses = (int) (Math.log(repairCost + 1) / Math.log(2));
            }

            if (uses > 0) {
                // Standard GUI Textfarbe in Minecraft ist meist 4210752 (0x404040),
                // aber Schwarz (0xFF000000) ist auch okay.
                int color = 0xDD404040;

                Text text = Text.translatable("simplevisuals.screen.anvil_uses", uses);

                // drawText ohne Shadow (false), da es im GUI-Fenster ist
                context.drawText(this.textRenderer, text, 80, 68, color, false);
            }
        }
    }


    @org.spongepowered.asm.mixin.injection.ModifyVariable(
            method = "onRenamed",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
            argsOnly = true
    )
    private String injectColorCodes(String name) {
        if (com.simplevisuals.Simplevisuals.getConfig().visuals.enableAnvilFormatting && name != null) {
            // Ersetzt z.B. "&cTest" durch "§cTest" (Rot) oder "&kTest" durch "§kTest" (Magisch)
            return name.replace('&', '§');
        }
        return name;
    }
}
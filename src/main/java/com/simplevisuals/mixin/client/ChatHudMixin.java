package com.simplevisuals.mixin.client;

import com.simplevisuals.Simplevisuals;
import com.simplevisuals.config.SimplevisualsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private List<ChatHudLine> messages;
    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
    @Shadow private int scrolledLines;

    @Shadow public abstract double getChatScale();
    @Shadow public abstract int getVisibleLineCount();
    @Shadow protected abstract boolean isChatHidden();

    // 1. Verschiebung: Wir injecten VOR dem Aufruf der internen render-Methode.
    // Das ist sicher, da dieser Aufruf NACH pushMatrix() passiert.
    @Inject(
            method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
            at = @At(
                    value = "INVOKE",
                    // Ziel: Die private render Methode (Backend, int, int, boolean)
                    target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V"
            )
    )
    private void shiftChatBeforeInternalRender(DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean interactable, boolean bl, CallbackInfo ci) {
        if (!this.isChatHidden() && Simplevisuals.getConfig().visuals.enableChatHeads) {
            // Verschiebe den Kontext um 12 Pixel nach rechts
            context.getMatrices().translate(12.0f, 0.0f);
        }
    }

    // 2. Köpfe rendern (Am Ende der Methode)
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("TAIL"))
    private void renderChatHeads(DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean interactable, boolean bl, CallbackInfo ci) {
        if (this.isChatHidden() || !Simplevisuals.getConfig().visuals.enableChatHeads) return;
        if (this.visibleMessages.isEmpty()) return;

        float scale = (float) this.getChatScale();

        context.getMatrices().pushMatrix();

        // WICHTIG: Die Verschiebung auch hier anwenden, da TAIL nach popMatrix() liegt
        context.getMatrices().translate(12.0f, 0.0f);

        // Standard Chat-Transformationen nachbauen
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(4.0f, 0.0f);

        int windowHeight = context.getScaledWindowHeight();
        int startY = MathHelper.floor((float)(windowHeight - 40) / scale);

        double lineSpacing = this.client.options.getChatLineSpacing().getValue();
        int lineHeight = (int)((double)9 * (lineSpacing + 1.0));
        int textOffset = (int)Math.round(8.0 * (lineSpacing + 1.0) - 4.0 * lineSpacing);

        int visibleCount = this.getVisibleLineCount();
        int lineCount = this.visibleMessages.size();
        int startLine = Math.min(lineCount - this.scrolledLines, visibleCount) - 1;

        for (int i = startLine; i >= 0; --i) {
            int listIndex = i + this.scrolledLines;
            if (listIndex >= this.visibleMessages.size()) continue;

            ChatHudLine.Visible visibleLine = this.visibleMessages.get(listIndex);

            float opacity = 1.0f;
            if (!interactable) {
                int age = currentTick - visibleLine.addedTime();
                double d = (double)age / 200.0;
                d = 1.0 - d;
                d *= 10.0;
                d = MathHelper.clamp(d, 0.0, 1.0);
                d *= d;
                opacity = (float)d;
            }

            if (opacity > 0.005f) {
                int lineY = startY - i * lineHeight;
                int drawY = lineY - textOffset - 1;
                simplevisuals$drawHeadForLine(context, visibleLine, drawY, opacity);
            }
        }

        context.getMatrices().popMatrix();
    }

    @Unique
    private void simplevisuals$drawHeadForLine(DrawContext context, ChatHudLine.Visible visibleLine, int y, float opacity) {
        ChatHudLine rawLine = null;
        for (ChatHudLine line : this.messages) {
            if (line.creationTick() == visibleLine.addedTime()) {
                rawLine = line;
                break;
            }
        }
        if (rawLine == null) return;

        String senderName = simplevisuals$getSenderName(rawLine.content());
        if (senderName == null) return;

        PlayerListEntry playerEntry = this.client.getNetworkHandler().getPlayerListEntry(senderName);
        if (playerEntry == null) return;

        Identifier skin = playerEntry.getSkinTextures().body().texturePath();

        int headSize = 9;
        // Positionierung: Links vom Text (x=0)
        int x = -headSize - 2;

        int alphaInt = (int)(opacity * 255.0f);
        if (alphaInt < 4) return;
        int color = alphaInt << 24 | 0xFFFFFF;

        // Köpfe zeichnen (RenderPipelines importieren nicht vergessen!)
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, x, y, 8.0F, 8.0F, headSize, headSize, 8, 8, 64, 64, color);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, x, y, 40.0F, 8.0F, headSize, headSize, 8, 8, 64, 64, color);
    }

    @Unique
    private String simplevisuals$getSenderName(Text text) {
        if (text.getContent() instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey();
            if (key.startsWith("chat.type.")) {
                Object[] args = translatable.getArgs();
                if (args.length > 0 && args[0] instanceof Text senderText) {
                    String name = senderText.getString();
                    name = name.replaceAll("[<>]", "");
                    return name.trim();
                }
            }
        }
        return null;
    }

    // --- LOGIK 1: APPEND MODE (Public Style) ---
    // Verändert die Nachricht, BEVOR sie dem Chat hinzugefügt wird.
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text appendCoordinatesToDeathMessage(Text message) {
        // Nur aktiv, wenn Modus auf APPEND steht
        if (Simplevisuals.getConfig().visuals.deathCoordsMode != SimplevisualsConfig.Visuals.DeathCoordsMode.APPEND) {
            return message;
        }

        return simplevisuals$processDeathMessage(message, true);
    }

    // --- LOGIK 2: SEPARATE MODE (Private Style) ---
    // Fügt eine ZWEITE Nachricht hinzu, NACHDEM die erste verarbeitet wurde.
    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("TAIL")
    )
    private void sendSeparateCoordinates(Text message, net.minecraft.network.message.MessageSignatureData signature, net.minecraft.client.gui.hud.MessageIndicator indicator, CallbackInfo ci) {
        // Nur aktiv, wenn Modus auf SEPARATE steht
        if (Simplevisuals.getConfig().visuals.deathCoordsMode != SimplevisualsConfig.Visuals.DeathCoordsMode.SEPARATE) {
            return;
        }

        // Wir rufen die Logik auf, aber sagen "nicht anhängen" (append=false),
        // sondern geben nur den Koordinaten-Text zurück.
        Text coordsOnly = simplevisuals$processDeathMessage(message, false);

        // Wenn coordsOnly != message, heißt das, wir haben Koordinaten gefunden.
        // Da processDeathMessage im "false"-Modus nur die Coords zurückgibt, prüfen wir:
        if (coordsOnly != null && coordsOnly != message) {
            // Wir fügen die Koordinaten als neue Nachricht hinzu.
            // WICHTIG: Damit wir keine Endlosschleife erzeugen, muss processDeathMessage sicherstellen,
            // dass diese neue Nachricht nicht wieder als "death." erkannt wird. (Ist sie nicht, da LiteralText).
            ((ChatHud)(Object)this).addMessage(coordsOnly);
        }
    }

    // --- HILFSMETHODE (Core Logic) ---
    @Unique
    private Text simplevisuals$processDeathMessage(Text message, boolean appendMode) {
        if (message.getContent() instanceof TranslatableTextContent content) {
            String key = content.getKey();

            // Prüfen ob es eine Todesnachricht ist
            if (key.startsWith("death.")) {
                Object[] args = content.getArgs();
                if (args.length > 0 && args[0] instanceof Text textArg) {
                    String victimName = textArg.getString();

                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.world != null) {
                        AbstractClientPlayerEntity victim = null;

                        // Ist es der Spieler selbst?
                        if (client.player != null && client.player.getName().getString().equals(victimName)) {
                            victim = client.player;
                        } else {
                            // Suche in der Spielerliste
                            List<AbstractClientPlayerEntity> players = client.world.getPlayers();
                            for (AbstractClientPlayerEntity p : players) {
                                if (p.getName().getString().equals(victimName)) {
                                    victim = p;
                                    break;
                                }
                            }
                        }

                        if (victim != null) {
                            int x = victim.getBlockX();
                            int y = victim.getBlockY();
                            int z = victim.getBlockZ();

                            // Formatierung der Koordinaten
                            MutableText coordText = Text.literal(String.format(" [%d, %d, %d]", x, y, z))
                                    .formatted(Formatting.GRAY)
                                    .formatted(Formatting.ITALIC);

                            if (appendMode) {
                                // Modus APPEND: Original + Coords
                                return Text.empty().append(message).append(coordText);
                            } else {
                                // Modus SEPARATE: Nur Coords (mit Label "Location:")
                                return Text.literal("Location: ").formatted(Formatting.GRAY).append(coordText);
                            }
                        }
                    }
                }
            }
        }
        // Wenn keine Todesnachricht oder Spieler nicht gefunden -> Original zurückgeben
        return message;
    }
}
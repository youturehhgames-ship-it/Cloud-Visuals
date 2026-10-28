package com.simplevisuals.client.gui;

import com.simplevisuals.Simplevisuals;
import com.simplevisuals.config.SimplevisualsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PickupNotifierHud {

    // FIX: Use thread-safe collections to prevent ConcurrentModificationException
    // when accessed from Network thread (via Mixin) and Render thread (via Tick) simultaneously.
    private static final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private static final MinecraftClient client = MinecraftClient.getInstance();

    // Cache for deleted entities
    private static final Map<Integer, CachedPickup> deadEntityCache = new ConcurrentHashMap<>();

    // Queue for pending pickups
    private static final List<PendingPickup> pendingPickups = new CopyOnWriteArrayList<>();

    public record CachedPickup(ItemStack stack, int xpValue, long timestamp) {}
    private record PendingPickup(int entityId, int amount, long timestamp) {}

    // --- CACHE METHODS ---
    public static void cacheEntity(int entityId, ItemStack stack) {
        if (!stack.isEmpty()) {
            deadEntityCache.put(entityId, new CachedPickup(stack, 0, System.currentTimeMillis()));
        }
    }

    public static void cacheXp(int entityId, int value) {
        deadEntityCache.put(entityId, new CachedPickup(ItemStack.EMPTY, value, System.currentTimeMillis()));
    }

    public static CachedPickup getCachedPickup(int entityId) {
        return deadEntityCache.get(entityId);
    }

    // --- PENDING METHODS (Dropper Fix) ---
    public static void schedulePendingPickup(int entityId, int amount) {
        pendingPickups.add(new PendingPickup(entityId, amount, System.currentTimeMillis()));
    }

    public static boolean hasPending() {
        return !pendingPickups.isEmpty();
    }

    /**
     * Tries to match a pending pickup with an inventory update.
     */
    public static void resolvePendingWithInventory(ItemStack stack, int changeAmount) {
        if (pendingPickups.isEmpty()) return;

        long now = System.currentTimeMillis();

        // Iterating CopyOnWriteArrayList is safe (snapshot iterator)
        for (PendingPickup pending : pendingPickups) {
            // If event is too old (> 500ms), ignore here (cleaned up in tick)
            if (now - pending.timestamp > 500) continue;

            // Match found (simple FIFO heuristic)
            addNotification(stack, changeAmount);
            pendingPickups.remove(pending); // Safe removal
            return;
        }
    }
    // ----------------------------------------

    public static void addNotification(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) return;

        ItemStack displayStack = stack.copy();

        for (Notification notification : notifications) {
            if (!notification.isXp && ItemStack.areItemsEqual(notification.stack, displayStack) && notification.age < 60) {
                notification.count += count;
                notification.age = 0;
                notification.popScale = 1.3f;
                return;
            }
        }

        notifications.add(new Notification(displayStack, count));
    }

    public static void addXpNotification(int amount) {
        if (!Simplevisuals.getConfig().visuals.pickupNotifier.pickupNotifierShowXp || amount <= 0) return;

        for (Notification notification : notifications) {
            if (notification.isXp && notification.age < 60) {
                notification.count += amount;
                notification.age = 0;
                notification.popScale = 1.3f;
                return;
            }
        }

        ItemStack xpIcon = new ItemStack(Items.EXPERIENCE_BOTTLE);
        Notification xpNotif = new Notification(xpIcon, amount);
        xpNotif.isXp = true;
        notifications.add(xpNotif);
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        var config = Simplevisuals.getConfig().visuals;
        if (!config.pickupNotifier.enablePickupNotifier || notifications.isEmpty() || client.options.hudHidden) return;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        TextRenderer textRenderer = client.textRenderer;

        boolean isRight = (config.pickupNotifier.pickupNotifierSide == SimplevisualsConfig.PickupSide.RIGHT);
        int startX = isRight ? (screenWidth - config.pickupNotifier.pickupNotifierOffsetX) : config.pickupNotifier.pickupNotifierOffsetX;
        int startY = screenHeight - config.pickupNotifier.pickupNotifierOffsetY;

        float scale = config.pickupNotifier.pickupNotifierScale;
        int maxAge = config.pickupNotifier.pickupNotifierDuration;

        context.getMatrices().pushMatrix();

        if (scale != 1.0f) {
            context.getMatrices().scale(scale, scale);
            startX = (int) (startX / scale);
            startY = (int) (startY / scale);
        }

        int yOffset = 0;

        for (int i = 0; i < notifications.size(); i++) {
            Notification n = notifications.get(i);
            float life = (float) n.age + tickCounter.getTickProgress(true);

            float opacity = 1.0f;
            if (life < 5) opacity = MathHelper.clamp(life / 5.0f, 0.0f, 1.0f);
            else if (life > maxAge - 20) opacity = MathHelper.clamp((maxAge - life) / 20.0f, 0.0f, 1.0f);

            if (opacity <= 0) continue;

            int alpha = (int) (255 * opacity);
            int textColor = (alpha << 24) | 0xFFFFFF;
            int countColor = n.isXp ? ((alpha << 24) | 0x80FF20) : ((alpha << 24) | 0xAAAAAA);

            Text nameText;
            if (n.isXp) {
                nameText = Text.literal("Experience").formatted(Formatting.GREEN);
            } else {
                nameText = n.stack.getName();
                if (config.pickupNotifier.pickupUseRarityColor) {
                    Rarity rarity = n.stack.getRarity();
                    if (rarity != Rarity.COMMON) {
                        nameText = nameText.copy().formatted(rarity.getFormatting());
                    }
                }
            }

            String countText = "+" + n.count;
            if (n.isXp) countText += " XP";

            int padding = 4;
            int iconWidth = 16;
            int nameWidth = textRenderer.getWidth(nameText);
            int countWidth = textRenderer.getWidth(countText);

            boolean showIcon = config.pickupNotifier.pickupShowItem;
            boolean showName = config.pickupNotifier.pickupShowName;
            boolean showCount = config.pickupNotifier.pickupShowCount;

            int contentWidth = 0;
            if (showIcon) contentWidth += iconWidth + padding;
            if (showName) contentWidth += nameWidth + padding;
            if (showCount) contentWidth += countWidth + padding;
            if (contentWidth > 0) contentWidth -= padding;

            int boxWidth = contentWidth + (padding * 2);
            int boxHeight = 20;

            int renderX = isRight ? startX - boxWidth : startX;
            int renderY = startY - yOffset - boxHeight;

            float currentScale = 1.0f;
            if (n.popScale > 1.0f) {
                currentScale = n.popScale;
                n.popScale = MathHelper.lerp(0.2f, n.popScale, 1.0f);
            }

            context.getMatrices().pushMatrix();

            if (currentScale > 1.0f) {
                float centerX = renderX + boxWidth / 2.0f;
                float centerY = renderY + boxHeight / 2.0f;
                context.getMatrices().translate(centerX, centerY);
                context.getMatrices().scale(currentScale, currentScale);
                context.getMatrices().translate(-centerX, -centerY);
            }

            if (config.pickupNotifier.pickupVanillaStyle) {
                drawVanillaBox(context, renderX, renderY, boxWidth, boxHeight, alpha, config.pickupNotifier.pickupBackgroundOpacity);
            } else {
                int bgAlphaValue = (int) ((Math.max(0, alpha - 180)) * config.pickupNotifier.pickupBackgroundOpacity);
                int bgColor = (bgAlphaValue << 24) | 0x000000;
                context.fill(renderX, renderY, renderX + boxWidth, renderY + boxHeight, bgColor);
            }

            int currentX = renderX + padding;
            int centerY = renderY + (boxHeight / 2);

            SimplevisualsConfig.PickupLayout layout = config.pickupNotifier.pickupNotifierLayout;

            if (layout == SimplevisualsConfig.PickupLayout.ICON_NAME_COUNT) {
                if (showIcon) currentX = drawIcon(context, n.stack, currentX, centerY, padding);
                if (showName) currentX = drawString(context, textRenderer, nameText, currentX, centerY, textColor, padding);
                if (showCount) drawString(context, textRenderer, countText, currentX, centerY, countColor, padding);
            } else if (layout == SimplevisualsConfig.PickupLayout.COUNT_ICON_NAME) {
                if (showCount) currentX = drawString(context, textRenderer, countText, currentX, centerY, countColor, padding);
                if (showIcon) currentX = drawIcon(context, n.stack, currentX, centerY, padding);
                if (showName) drawString(context, textRenderer, nameText, currentX, centerY, textColor, padding);
            } else if (layout == SimplevisualsConfig.PickupLayout.NAME_ICON_COUNT) {
                if (showName) currentX = drawString(context, textRenderer, nameText, currentX, centerY, textColor, padding);
                if (showIcon) currentX = drawIcon(context, n.stack, currentX, centerY, padding);
                if (showCount) drawString(context, textRenderer, countText, currentX, centerY, countColor, padding);
            } else if (layout == SimplevisualsConfig.PickupLayout.ICON_COUNT_NAME) {
                if (showIcon) currentX = drawIcon(context, n.stack, currentX, centerY, padding);
                if (showCount) currentX = drawString(context, textRenderer, countText, currentX, centerY, countColor, padding);
                if (showName) drawString(context, textRenderer, nameText, currentX, centerY, textColor, padding);
            }

            context.getMatrices().popMatrix();

            float slideProgress = MathHelper.clamp(life / 3.0f, 0.0f, 1.0f);
            yOffset += (int) ((boxHeight + 2) * slideProgress);
        }

        context.getMatrices().popMatrix();
    }

    private static int drawIcon(DrawContext context, ItemStack stack, int x, int centerY, int padding) {
        context.drawItem(stack, x, centerY - 8);
        return x + 16 + padding;
    }

    private static int drawString(DrawContext context, TextRenderer textRenderer, Text text, int x, int centerY, int color, int padding) {
        context.drawText(textRenderer, text, x, centerY - 4, color, true);
        return x + textRenderer.getWidth(text) + padding;
    }

    private static int drawString(DrawContext context, TextRenderer textRenderer, String text, int x, int centerY, int color, int padding) {
        context.drawText(textRenderer, text, x, centerY - 4, color, true);
        return x + textRenderer.getWidth(text) + padding;
    }

    private static void drawVanillaBox(DrawContext context, int x, int y, int w, int h, int globalAlpha, float bgOpacityFactor) {
        int bg = 0xF0100010;
        int borderStart = 0x505000FF;
        int borderEnd = 0x5028007F;

        int alpha = globalAlpha;
        int bgAlpha = (int) (0xF0 * bgOpacityFactor * (globalAlpha / 255.0f));

        if (alpha != 255 || bgOpacityFactor != 1.0f) {
            bg = (bgAlpha << 24) | (bg & 0x00FFFFFF);
            borderStart = (alpha << 24) | (borderStart & 0x00FFFFFF);
            borderEnd = (alpha << 24) | (borderEnd & 0x00FFFFFF);
        }

        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        context.fillGradient(x + 1, y, x + w - 1, y + 1, borderStart, borderStart);
        context.fillGradient(x + 1, y + h - 1, x + w - 1, y + h, borderEnd, borderEnd);
        context.fillGradient(x, y + 1, x + 1, y + h - 1, borderStart, borderEnd);
        context.fillGradient(x + w - 1, y + 1, x + w, y + h - 1, borderStart, borderEnd);
    }

    public static void tick() {
        long now = System.currentTimeMillis();

        // 1. Check Pending Pickups (snapshot iterator safe for removal via object)
        if (!pendingPickups.isEmpty() && client.world != null) {
            for (PendingPickup pending : pendingPickups) {
                // Timeout (1 second)
                if (now - pending.timestamp > 1000) {
                    pendingPickups.remove(pending);
                    continue;
                }

                // Check A: Entity exists and has data now
                Entity entity = client.world.getEntityById(pending.entityId);
                if (entity instanceof ItemEntity itemEntity && !itemEntity.getStack().isEmpty()) {
                    addNotification(itemEntity.getStack(), pending.amount);
                    pendingPickups.remove(pending);
                    continue;
                }

                // Check B: Entity is in dead cache
                CachedPickup cached = deadEntityCache.get(pending.entityId);
                if (cached != null && !cached.stack().isEmpty()) {
                    addNotification(cached.stack(), pending.amount);
                    pendingPickups.remove(pending);
                    continue;
                }
            }
        }

        if (notifications.isEmpty() && deadEntityCache.isEmpty()) return;

        var config = Simplevisuals.getConfig().visuals;
        int maxAge = config.pickupNotifier.pickupNotifierDuration;

        // Ageing notifications
        for (Notification n : notifications) {
            n.age++;
        }
        // Removing old ones (safe with CopyOnWriteArrayList)
        notifications.removeIf(n -> n.age > maxAge);

        // Clean dead cache
        // Safe with ConcurrentHashMap
        deadEntityCache.values().removeIf(c -> (now - c.timestamp) > 1000);
    }

    private static class Notification {
        ItemStack stack;
        int count;
        int age;
        boolean isXp;
        float popScale = 1.0f;

        public Notification(ItemStack stack, int count) {
            this.stack = stack.copy();
            this.count = count;
            this.age = 0;
            this.isXp = false;
        }
    }
}
package com.simplevisuals.mixin.client;

import com.simplevisuals.Simplevisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.gui.hud.bar.LocatorBar;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.resource.waypoint.WaypointStyleAsset;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickManager;
import net.minecraft.world.waypoint.EntityTickProgress;
import net.minecraft.world.waypoint.TrackedWaypoint;
import net.minecraft.world.waypoint.Waypoint;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.UUID;

@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin implements Bar {

    @Shadow @Final private MinecraftClient client;
    @Shadow private static Identifier ARROW_UP;
    @Shadow private static Identifier ARROW_DOWN;

    /**
     * @author Simplevisuals
     * @reason Ersetzt die Icons durch Spielerköpfe
     */
    @Overwrite
    public void renderAddons(DrawContext context, RenderTickCounter tickCounter) {
        int centerY = this.getCenterY(this.client.getWindow());
        Entity cameraEntity = this.client.getCameraEntity();

        if (cameraEntity != null) {
            World world = cameraEntity.getEntityWorld();
            TickManager tickManager = world.getTickManager();

            // Lambda für Tick-Progress
            EntityTickProgress entityTickProgress = (entity) -> tickCounter.getTickProgress(!tickManager.shouldSkipTick(entity));

            // Zugriff auf Waypoints (Pfad kann je nach Mappings variieren, hier basierend auf deinem Source)
            this.client.player.networkHandler.getWaypointHandler().forEachWaypoint(cameraEntity, (waypoint) -> {

                // Prüfen, ob der Waypoint man selbst ist (Self-Check)
                boolean isSelf = waypoint.getSource().left()
                        .map((uuid) -> uuid.equals(cameraEntity.getUuid()))
                        .orElse(false);

                if (!isSelf) {
                    double relYaw = waypoint.getRelativeYaw(world, this.client.gameRenderer.getCamera(), entityTickProgress);

                    // Sichtbarkeitsbereich (-60 bis +60 Grad)
                    if (relYaw > -60.0 && relYaw <= 60.0) {
                        int centerX = MathHelper.ceil((float)(context.getScaledWindowWidth() - 9) / 2.0F);

                        // X-Position berechnen
                        int xOffset = MathHelper.floor(relYaw * 173.0 / 120.0);
                        int x = centerX + xOffset;
                        int y = centerY - 2;

                        boolean renderedHead = false;

                        // --- CUSTOM LOGIC: Spielerkopf rendern ---
                        if (Simplevisuals.getConfig().visuals.enablePlayerLocator) {
                            Optional<UUID> sourceUuid = waypoint.getSource().left();
                            if (sourceUuid.isPresent()) {
                                PlayerListEntry playerEntry = this.client.getNetworkHandler().getPlayerListEntry(sourceUuid.get());

                                if (playerEntry != null) {
                                    Identifier skin = playerEntry.getSkinTextures().body().texturePath();

                                    // Wir rendern den Kopf (8x8 Textur) skaliert auf 9x9 oder zentriert
                                    // Hier rendern wir ihn 9x9 groß, damit er das Icon-Feld füllt

                                    // Layer 1 (Gesicht)
                                    context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, x, y, 8.0F, 8.0F, 9, 9, 8, 8, 64, 64);

                                    // Layer 2 (Hat/Overlay)
                                    context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, x, y, 40.0F, 8.0F, 9, 9, 8, 8, 64, 64);

                                    renderedHead = true;
                                }
                            }
                        }
                        // -----------------------------------------

                        // Fallback: Normales Icon rendern (wenn kein Kopf gerendert wurde)
                        if (!renderedHead) {
                            Waypoint.Config config = waypoint.getConfig();
                            WaypointStyleAsset style = this.client.getWaypointStyleAssetManager().get(config.style);
                            float distance = MathHelper.sqrt((float)waypoint.squaredDistanceTo(cameraEntity));
                            Identifier sprite = style.getSpriteForDistance(distance);

                            int color = config.color.orElseGet(() ->
                                    waypoint.getSource().map(
                                            (uuid) -> ColorHelper.withBrightness(ColorHelper.withAlpha(255, uuid.hashCode()), 0.9F),
                                            (name) -> ColorHelper.withBrightness(ColorHelper.withAlpha(255, name.hashCode()), 0.9F)
                                    )
                            );

                            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, x, y, 9, 9, color);
                        }

                        // Pfeile rendern (Höhenunterschied)
                        TrackedWaypoint.Pitch pitch = waypoint.getPitch(world, this.client.gameRenderer, entityTickProgress);
                        if (pitch != TrackedWaypoint.Pitch.NONE) {
                            int arrowYOffset;
                            Identifier arrowSprite;
                            if (pitch == TrackedWaypoint.Pitch.DOWN) {
                                arrowYOffset = 6;
                                arrowSprite = ARROW_DOWN;
                            } else {
                                arrowYOffset = -6;
                                arrowSprite = ARROW_UP;
                            }
                            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, arrowSprite, x + 1, centerY + arrowYOffset, 7, 5);
                        }
                    }
                }
            });
        }
    }
}
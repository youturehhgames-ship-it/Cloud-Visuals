package com.simplevisuals.config;

import com.simplevisuals.Simplevisuals;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;


@Config(name = Simplevisuals.MOD_ID)
public class SimplevisualsConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public Visuals visuals = new Visuals();

    public enum SlideActivationMode {
        CAMERA, ALWAYS
    }

    public enum PickupLayout {
        ICON_NAME_COUNT,
        COUNT_ICON_NAME,
        NAME_ICON_COUNT,
        ICON_COUNT_NAME
    }

    public enum PickupSide {
        LEFT, RIGHT
    }



    public static class Visuals {
        @ConfigEntry.Gui.Tooltip
        public boolean enablePlayerLocator = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableChatHeads = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableStatusEffectBars = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableElytraPitchHelper = true;
        @ConfigEntry.Gui.Tooltip
        public float elytraTargetAngleUp = -40.0f;
        @ConfigEntry.Gui.Tooltip
        public float elytraTargetAngleDown = 40.0f;
        @ConfigEntry.Gui.Tooltip
        public float elytraPitchTolerance = 10.0f;
        @ConfigEntry.Gui.Tooltip
        public float elytraSensitivity = 4.0f;
        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean enableRenamedItemTextures = false;
        @ConfigEntry.Gui.Tooltip
        public boolean enableAnvilFormatting = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enhanceDeathMessages = true;

        public enum DeathCoordsMode {
            DISABLED,
            APPEND,    // "Public" Style: Wird direkt an die Nachricht angehängt
            SEPARATE   // "Private" Style: Eigene Chat-Nachricht nur für dich
        }

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DeathCoordsMode deathCoordsMode = DeathCoordsMode.SEPARATE;

        @ConfigEntry.Gui.Tooltip
        public boolean enableMapTooltips = true;

        @ConfigEntry.Gui.CollapsibleObject
        public SpeedLines speedLines = new SpeedLines();

        // Unterkategorie: Pickup Notifier
        @ConfigEntry.Gui.CollapsibleObject
        public PickupNotifier pickupNotifier = new PickupNotifier();

        @ConfigEntry.Gui.CollapsibleObject
        public DamageIndicators damageIndicators = new DamageIndicators();

        @ConfigEntry.Gui.CollapsibleObject
        public BiomeInfo biomeInfo = new BiomeInfo();

        public static class DamageIndicators {
            @ConfigEntry.Gui.Tooltip
            public boolean enable = true;
            @ConfigEntry.Gui.Tooltip
            public float scale = 1.0f;
            @ConfigEntry.Gui.Tooltip
            public int colorNormal = 0xFFFFFF; // Weiß
            @ConfigEntry.Gui.Tooltip
            public int colorSpecial = 0xFFD700; // Gold (für Smite/Arthropods)
            @ConfigEntry.Gui.Tooltip
            public boolean showBorder = true;
        }

        public static class BiomeInfo {
            @ConfigEntry.Gui.Tooltip
            public boolean enable = false;
            @ConfigEntry.Gui.Tooltip
            public int displayDuration = 60; // Wie lange die Nachricht bleibt (Ticks)
            @ConfigEntry.Gui.Tooltip
            public int cooldownSeconds = 60; // Wie lange ein Biom ignoriert wird nach Verlassen
            @ConfigEntry.Gui.Tooltip
            public int yOffset = 50; // Versatz von oben
        }

        public static class SpeedLines {
            @ConfigEntry.Gui.Tooltip
            public boolean enableSpeedLines = true;

            @ConfigEntry.Gui.Tooltip
            public int speedLinesColor = 0xFFFFFF;

            @ConfigEntry.Gui.Tooltip
            public float speedLinesAlpha = 0.7f;

            @ConfigEntry.Gui.Tooltip
            public float speedLinesAmount = 1.0f;

            @ConfigEntry.Gui.Tooltip
            public float speedLinesRadius = 0.7f;

            @ConfigEntry.Gui.Tooltip
            public float speedLinesWidth = 8.0f;

            @ConfigEntry.Gui.Tooltip
            public float speedLinesSpeed = 1.0f;

            @ConfigEntry.Gui.Tooltip
            public float speedLinesScale = 4.0f;

            @ConfigEntry.Gui.Tooltip
            public float speedThreshold = 0.6f;
        }
        @ConfigEntry.Gui.CollapsibleObject
        public HeldItemTooltips heldItemTooltips = new HeldItemTooltips();

        public static class HeldItemTooltips {
            @ConfigEntry.Gui.Tooltip
            public boolean enable = true;

            @ConfigEntry.Gui.Tooltip
            public boolean showDurability = true;

            @ConfigEntry.Gui.Tooltip
            public boolean showEnchantments = true;

            @ConfigEntry.Gui.Tooltip
            public int maxEnchantments = 3; // Begrenzung, damit der Bildschirm nicht vollgespammt wird
        }

        public static class PickupNotifier {
            @ConfigEntry.Gui.Tooltip
            public boolean enablePickupNotifier = true;

            @ConfigEntry.Gui.Tooltip
            public int pickupNotifierOffsetX = 10;

            @ConfigEntry.Gui.Tooltip
            public int pickupNotifierOffsetY = 10;

            @ConfigEntry.Gui.Tooltip
            public float pickupNotifierScale = 1.0f;

            @ConfigEntry.Gui.Tooltip
            public int pickupNotifierDuration = 120; // Dauer Einstellung

            @ConfigEntry.Gui.Tooltip
            public boolean pickupNotifierShowXp = true;

            // NEU: Seite
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
            public PickupSide pickupNotifierSide = PickupSide.RIGHT;

            // Anordnung
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
            public PickupLayout pickupNotifierLayout = PickupLayout.COUNT_ICON_NAME;

            @ConfigEntry.Gui.Tooltip
            public boolean pickupShowItem = true;
            @ConfigEntry.Gui.Tooltip
            public boolean pickupShowName = true;
            @ConfigEntry.Gui.Tooltip
            public boolean pickupShowCount = true;

            // Stil
            @ConfigEntry.Gui.Tooltip
            public boolean pickupUseRarityColor = true;

            @ConfigEntry.Gui.Tooltip
            public boolean pickupVanillaStyle = true;

            // NEU: Hintergrund Opazität
            @ConfigEntry.Gui.Tooltip
            public float pickupBackgroundOpacity = 1.0f;
        }

    }
}
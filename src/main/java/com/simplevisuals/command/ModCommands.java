package com.simplevisuals.command;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.simplevisuals.Simplevisuals;
import com.simplevisuals.config.SimplevisualsConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.vehicle.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // --- CONFIG COMMANDS (simplevisuals) ---
            dispatcher.register(CommandManager.literal("simplevisuals")
                    // Level 4 für Admin-Befehle (Config Änderungen)
                    .requires(source -> checkPermission(source, 4))
                            // NEU: Player Locator
                            .then(CommandManager.literal("locator")
                                    .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                            .executes(ctx -> {
                                                boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                                Simplevisuals.getConfig().visuals.enablePlayerLocator = val;
                                                saveConfig();
                                                ctx.getSource().sendFeedback(() -> Text.literal("Player Locator enabled: " + val), true);
                                                return 1;
                                            })))
                            // NEU: Status Effect Bars
                            .then(CommandManager.literal("statusBars")
                                    .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                            .executes(ctx -> {
                                                boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                                Simplevisuals.getConfig().visuals.enableStatusEffectBars = val;
                                                saveConfig();
                                                ctx.getSource().sendFeedback(() -> Text.literal("Status Effect Bars enabled: " + val), true);
                                                return 1;
                                            })))
                            // NEU: Chat Heads
                            .then(CommandManager.literal("chatHeads")
                                    .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                            .executes(ctx -> {
                                                boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                                Simplevisuals.getConfig().visuals.enableChatHeads = val;
                                                saveConfig();
                                                ctx.getSource().sendFeedback(() -> Text.literal("Chat Heads enabled: " + val), true);
                                                return 1;
                                            })))
                            // NEU: Elytra Pitch Helper
                            .then(CommandManager.literal("elytraHelper")
                                    .then(CommandManager.literal("enable")
                                            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                                    .executes(ctx -> {
                                                        boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                                        Simplevisuals.getConfig().visuals.enableElytraPitchHelper = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("Elytra Pitch Helper enabled: " + val), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("angles")
                                            .then(CommandManager.argument("up", FloatArgumentType.floatArg())
                                                    .then(CommandManager.argument("down", FloatArgumentType.floatArg())
                                                            .executes(ctx -> {
                                                                float up = FloatArgumentType.getFloat(ctx, "up");
                                                                float down = FloatArgumentType.getFloat(ctx, "down");
                                                                Simplevisuals.getConfig().visuals.elytraTargetAngleUp = up;
                                                                Simplevisuals.getConfig().visuals.elytraTargetAngleDown = down;
                                                                saveConfig();
                                                                ctx.getSource().sendFeedback(() -> Text.literal("Elytra Helper Targets set to Up:" + up + " Down:" + down), true);
                                                                return 1;
                                                            }))))
                                    .then(CommandManager.literal("tolerance")
                                            .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.0f))
                                                    .executes(ctx -> {
                                                        float val = FloatArgumentType.getFloat(ctx, "value");
                                                        Simplevisuals.getConfig().visuals.elytraPitchTolerance = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("Elytra Helper Tolerance set to: " + val), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("sensitivity")
                                            .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.0f))
                                                    .executes(ctx -> {
                                                        float val = FloatArgumentType.getFloat(ctx, "value");
                                                        Simplevisuals.getConfig().visuals.elytraSensitivity = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("Elytra Helper Sensitivity set to: " + val), true);
                                                        return 1;
                                                    })))
                            )
                    // 9. Visual Settings
                    .then(CommandManager.literal("visuals")
                            // --- SPEED LINES ---
                            .then(CommandManager.literal("speedLines")
                                    .then(CommandManager.literal("enable")
                                            .then(CommandManager.argument("val", BoolArgumentType.bool())
                                                    .executes(ctx -> {
                                                        boolean val = BoolArgumentType.getBool(ctx, "val");
                                                        Simplevisuals.getConfig().visuals.speedLines.enableSpeedLines = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("SpeedLines enabled: " + val), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("color")
                                            .then(CommandManager.argument("hex", IntegerArgumentType.integer())
                                                    .executes(ctx -> {
                                                        int val = IntegerArgumentType.getInteger(ctx, "hex");
                                                        Simplevisuals.getConfig().visuals.speedLines.speedLinesColor = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("SpeedLines color set to: " + Integer.toHexString(val)), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("alpha")
                                            .then(CommandManager.argument("val", FloatArgumentType.floatArg(0, 1))
                                                    .executes(ctx -> {
                                                        float val = FloatArgumentType.getFloat(ctx, "val");
                                                        Simplevisuals.getConfig().visuals.speedLines.speedLinesAlpha = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("SpeedLines alpha set to: " + val), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("amount")
                                            .then(CommandManager.argument("val", FloatArgumentType.floatArg(0))
                                                    .executes(ctx -> {
                                                        float val = FloatArgumentType.getFloat(ctx, "val");
                                                        Simplevisuals.getConfig().visuals.speedLines.speedLinesAmount = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("SpeedLines amount set to: " + val), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("threshold")
                                            .then(CommandManager.argument("val", FloatArgumentType.floatArg(0))
                                                    .executes(ctx -> {
                                                        float val = FloatArgumentType.getFloat(ctx, "val");
                                                        Simplevisuals.getConfig().visuals.speedLines.speedThreshold = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("SpeedLines threshold set to: " + val), true);
                                                        return 1;
                                                    })))
                            )

                            // --- PICKUP NOTIFIER ---
                            .then(CommandManager.literal("pickupNotifier")
                                    .then(CommandManager.literal("enable")
                                            .then(CommandManager.argument("val", BoolArgumentType.bool())
                                                    .executes(ctx -> {
                                                        boolean val = BoolArgumentType.getBool(ctx, "val");
                                                        Simplevisuals.getConfig().visuals.pickupNotifier.enablePickupNotifier = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("PickupNotifier enabled: " + val), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("offset")
                                            .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                                    .then(CommandManager.argument("y", IntegerArgumentType.integer())
                                                            .executes(ctx -> {
                                                                int x = IntegerArgumentType.getInteger(ctx, "x");
                                                                int y = IntegerArgumentType.getInteger(ctx, "y");
                                                                Simplevisuals.getConfig().visuals.pickupNotifier.pickupNotifierOffsetX = x;
                                                                Simplevisuals.getConfig().visuals.pickupNotifier.pickupNotifierOffsetY = y;
                                                                saveConfig();
                                                                ctx.getSource().sendFeedback(() -> Text.literal("PickupNotifier Offset set to X:" + x + " Y:" + y), true);
                                                                return 1;
                                                            }))))
                                    .then(CommandManager.literal("scale")
                                            .then(CommandManager.argument("val", FloatArgumentType.floatArg(0))
                                                    .executes(ctx -> {
                                                        float val = FloatArgumentType.getFloat(ctx, "val");
                                                        Simplevisuals.getConfig().visuals.pickupNotifier.pickupNotifierScale = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("PickupNotifier scale set to: " + val), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("duration")
                                            .then(CommandManager.argument("ticks", IntegerArgumentType.integer(0))
                                                    .executes(ctx -> {
                                                        int val = IntegerArgumentType.getInteger(ctx, "ticks");
                                                        Simplevisuals.getConfig().visuals.pickupNotifier.pickupNotifierDuration = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("PickupNotifier duration set to: " + val), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("showXp")
                                            .then(CommandManager.argument("val", BoolArgumentType.bool())
                                                    .executes(ctx -> {
                                                        boolean val = BoolArgumentType.getBool(ctx, "val");
                                                        Simplevisuals.getConfig().visuals.pickupNotifier.pickupNotifierShowXp = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("PickupNotifier Show XP: " + val), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("vanillaStyle")
                                            .then(CommandManager.argument("val", BoolArgumentType.bool())
                                                    .executes(ctx -> {
                                                        boolean val = BoolArgumentType.getBool(ctx, "val");
                                                        Simplevisuals.getConfig().visuals.pickupNotifier.pickupVanillaStyle = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("PickupNotifier Vanilla Style: " + val), true);
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("opacity")
                                            .then(CommandManager.argument("val", FloatArgumentType.floatArg(0, 1))
                                                    .executes(ctx -> {
                                                        float val = FloatArgumentType.getFloat(ctx, "val");
                                                        Simplevisuals.getConfig().visuals.pickupNotifier.pickupBackgroundOpacity = val;
                                                        saveConfig();
                                                        ctx.getSource().sendFeedback(() -> Text.literal("PickupNotifier Bg Opacity: " + val), true);
                                                        return 1;
                                                    })))
                                    // ENUMS (Side & Layout)
                                    .then(CommandManager.literal("side")
                                            .then(CommandManager.argument("side", StringArgumentType.word())
                                                    .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"left", "right"}, builder))
                                                    .executes(ctx -> {
                                                        String s = StringArgumentType.getString(ctx, "side");
                                                        try {
                                                            SimplevisualsConfig.PickupSide side = SimplevisualsConfig.PickupSide.valueOf(s.toUpperCase());
                                                            Simplevisuals.getConfig().visuals.pickupNotifier.pickupNotifierSide = side;
                                                            saveConfig();
                                                            ctx.getSource().sendFeedback(() -> Text.literal("PickupNotifier Side set to: " + side), true);
                                                        } catch (IllegalArgumentException e) {
                                                            ctx.getSource().sendError(Text.literal("Invalid side. Use 'left' or 'right'."));
                                                        }
                                                        return 1;
                                                    })))
                                    .then(CommandManager.literal("layout")
                                            .then(CommandManager.argument("layout", StringArgumentType.word())
                                                    .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"icon_name_count", "count_icon_name", "name_icon_count", "icon_count_name"}, builder))
                                                    .executes(ctx -> {
                                                        String s = StringArgumentType.getString(ctx, "layout");
                                                        try {
                                                            SimplevisualsConfig.PickupLayout layout = SimplevisualsConfig.PickupLayout.valueOf(s.toUpperCase());
                                                            Simplevisuals.getConfig().visuals.pickupNotifier.pickupNotifierLayout = layout;
                                                            saveConfig();
                                                            ctx.getSource().sendFeedback(() -> Text.literal("PickupNotifier Layout set to: " + layout), true);
                                                        } catch (IllegalArgumentException e) {
                                                            ctx.getSource().sendError(Text.literal("Invalid layout."));
                                                        }
                                                        return 1;
                                                    })))
                                    // Toggles für Elemente
                                    .then(CommandManager.literal("elements")
                                            .then(CommandManager.literal("item").then(CommandManager.argument("val", BoolArgumentType.bool()).executes(ctx -> {
                                                Simplevisuals.getConfig().visuals.pickupNotifier.pickupShowItem = BoolArgumentType.getBool(ctx, "val"); saveConfig(); return 1;
                                            })))
                                            .then(CommandManager.literal("name").then(CommandManager.argument("val", BoolArgumentType.bool()).executes(ctx -> {
                                                Simplevisuals.getConfig().visuals.pickupNotifier.pickupShowName = BoolArgumentType.getBool(ctx, "val"); saveConfig(); return 1;
                                            })))
                                            .then(CommandManager.literal("count").then(CommandManager.argument("val", BoolArgumentType.bool()).executes(ctx -> {
                                                Simplevisuals.getConfig().visuals.pickupNotifier.pickupShowCount = BoolArgumentType.getBool(ctx, "val"); saveConfig(); return 1;
                                            })))
                                    )
                            )
                    )
            );
        });
    }

    private static void saveConfig() {
        AutoConfig.getConfigHolder(SimplevisualsConfig.class).save();
    }

    private static boolean checkPermission(ServerCommandSource source, int level) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            return source.getServer().getPlayerManager().isOperator(player.getPlayerConfigEntry());
        }
        return true;
    }
}
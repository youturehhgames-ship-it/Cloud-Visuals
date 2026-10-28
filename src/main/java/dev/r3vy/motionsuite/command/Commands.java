package dev.r3vy.motionsuite.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.r3vy.motionsuite.blur.FrameBlend;
import dev.r3vy.motionsuite.camera.CameraSmoother;
import dev.r3vy.motionsuite.config.BlurSettings;
import dev.r3vy.motionsuite.config.CameraSettings;
import dev.r3vy.motionsuite.config.Settings;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class Commands {
    private static final String VALUE = "value";
    private static final String BLUR_PREFIX = "Motion Blur: ";
    private static final String CAMERA_PREFIX = "Motion Camera: ";

    private Commands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(blurRoot());
            dispatcher.register(cameraRoot());
            dispatcher.register(suiteRoot());
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> blurRoot() {
        BlurSettings blur = Settings.BLUR;
        return ClientCommandManager.literal("motionblur")
                .executes(context -> send(context.getSource(), blurSummary()))
                .then(ClientCommandManager.literal("adaptive")
                        .executes(context -> send(context.getSource(), BLUR_PREFIX + "adaptive " + onOff(blur.adaptive())))
                        .then(ClientCommandManager.literal("on")
                                .executes(context -> setAdaptive(context.getSource(), true)))
                        .then(ClientCommandManager.literal("off")
                                .executes(context -> setAdaptive(context.getSource(), false))))
                .then(intOption("guardcut", 0, 100, blur::setGuardCut, blur::guardCut, BLUR_PREFIX))
                .then(ClientCommandManager.argument(VALUE, IntegerArgumentType.integer(0, 100))
                        .executes(context -> {
                            blur.setAmount(IntegerArgumentType.getInteger(context, VALUE));
                            Settings.save();
                            return send(context.getSource(), BLUR_PREFIX + blur.amount() + "%");
                        }));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> cameraRoot() {
        CameraSettings camera = Settings.CAMERA;
        return ClientCommandManager.literal("motioncamera")
                .executes(context -> send(context.getSource(), cameraSummary()))
                .then(ClientCommandManager.literal("toggle")
                        .executes(context -> {
                            camera.setEnabled(!camera.enabled());
                            Settings.save();
                            return send(context.getSource(), CAMERA_PREFIX + onOff(camera.enabled()));
                        }))
                .then(ClientCommandManager.literal("guard")
                        .executes(context -> {
                            camera.setCrystalGuard(!camera.crystalGuard());
                            Settings.save();
                            return send(context.getSource(), CAMERA_PREFIX + "crystal guard " + onOff(camera.crystalGuard()));
                        }))
                .then(ClientCommandManager.literal("status")
                        .executes(context -> send(context.getSource(), cameraSummary())))
                .then(ClientCommandManager.literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(doubleOption("speed", 0.02, 1.0, camera::setSpeed, camera::speed))
                .then(doubleOption("fpspeed", 0.02, 1.0, camera::setFirstPersonSpeed, camera::firstPersonSpeed))
                .then(doubleOption("maxoffset", 0.0, 64.0, camera::setMaxOffset, camera::maxOffset))
                .then(doubleOption("guardoffset", 0.1, 8.0, camera::setGuardMaxOffset, camera::guardMaxOffset))
                .then(doubleOption("guardspeed", 0.02, 1.0, camera::setGuardSpeed, camera::guardSpeed))
                .then(intOption("duration", 100, 10000, camera::setExceptionDuration, camera::exceptionDuration, CAMERA_PREFIX));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> suiteRoot() {
        return ClientCommandManager.literal("motionsuite")
                .executes(context -> send(context.getSource(), blurSummary() + "\n" + cameraSummary()))
                .then(ClientCommandManager.literal("status")
                        .executes(context -> send(context.getSource(), blurSummary() + "\n" + cameraSummary())))
                .then(ClientCommandManager.literal("reload")
                        .executes(context -> reload(context.getSource())));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> doubleOption(
            String name, double min, double max, DoubleConsumer setter, DoubleSupplier getter) {
        return ClientCommandManager.literal(name)
                .executes(context -> send(context.getSource(), CAMERA_PREFIX + name + " = " + decimal(getter.getAsDouble())))
                .then(ClientCommandManager.argument(VALUE, DoubleArgumentType.doubleArg(min, max))
                        .executes(context -> {
                            setter.accept(DoubleArgumentType.getDouble(context, VALUE));
                            Settings.save();
                            return send(context.getSource(), CAMERA_PREFIX + name + " = " + decimal(getter.getAsDouble()));
                        }));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> intOption(
            String name, int min, int max, IntConsumer setter, IntSupplier getter, String prefix) {
        return ClientCommandManager.literal(name)
                .executes(context -> send(context.getSource(), prefix + name + " = " + getter.getAsInt()))
                .then(ClientCommandManager.argument(VALUE, IntegerArgumentType.integer(min, max))
                        .executes(context -> {
                            setter.accept(IntegerArgumentType.getInteger(context, VALUE));
                            Settings.save();
                            return send(context.getSource(), prefix + name + " = " + getter.getAsInt());
                        }));
    }

    private static int setAdaptive(FabricClientCommandSource source, boolean enabled) {
        Settings.BLUR.setAdaptive(enabled);
        Settings.save();
        FrameBlend.reset();
        return send(source, BLUR_PREFIX + "adaptive " + onOff(enabled));
    }

    private static int reload(FabricClientCommandSource source) {
        Settings.reload();
        FrameBlend.reset();
        return send(source, "Motion Suite: đã tải lại config");
    }

    private static String blurSummary() {
        BlurSettings blur = Settings.BLUR;
        return BLUR_PREFIX + blur.amount() + "% | adaptive " + onOff(blur.adaptive()) + " | guardcut " + blur.guardCut() + "%";
    }

    private static String cameraSummary() {
        CameraSettings camera = Settings.CAMERA;
        String state = CameraSmoother.isBroken() ? "DISABLED (error)" : onOff(camera.enabled());
        return String.format(Locale.ROOT,
                "%s%s | speed %.2f | fpspeed %.2f | maxoffset %.1f | duration %dms | guard %s (offset %.1f, speed %.2f)",
                CAMERA_PREFIX, state, camera.speed(), camera.firstPersonSpeed(), camera.maxOffset(),
                camera.exceptionDuration(), onOff(camera.crystalGuard()), camera.guardMaxOffset(), camera.guardSpeed());
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static int send(FabricClientCommandSource source, String message) {
        source.sendFeedback(Text.literal(message));
        return Command.SINGLE_SUCCESS;
    }
}

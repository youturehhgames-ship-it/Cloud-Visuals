package dev.r3vy.motionsuite.camera;

import dev.r3vy.motionsuite.blur.FrameBlend;
import dev.r3vy.motionsuite.config.CameraSettings;
import dev.r3vy.motionsuite.config.Settings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class CameraSmoother {
    private static final Logger LOGGER = LoggerFactory.getLogger("motionsuite");

    private static final long NEVER = Long.MIN_VALUE / 4;
    private static final long FADE_TICKS = 8L;
    private static final double SNAP_DIST_SQ = 65536.0;
    private static final int MAX_ERRORS = 5;
    private static final int EARLY_END_BASE_TICKS = 3;
    private static final int EARLY_END_MIN_TICKS = 5;
    private static final int EARLY_END_MAX_TICKS = 14;
    private static final double EARLY_END_LAG_SQ = 0.0004;
    private static final double GUARD_RISE = 0.34;
    private static final double GUARD_FALL = 0.1;
    private static final double MAX_FRAME_DT = 0.1;
    private static final double FULL_SPEED = 0.999;
    private static final double TICKS_PER_SECOND = 20.0;
    private static final double NANOS_TO_SECONDS = 1.0E-9;
    private static final double UNLIMITED_OFFSET = 64.0;
    private static final long MILLIS_PER_TICK = 50L;

    private static final double[] OUT = new double[3];
    private static final List<Entity> pearls = new ArrayList<>(4);

    private static double cx;
    private static double cy;
    private static double cz;
    private static long lastFrameNanos;
    private static boolean frameInit;
    private static long tickCounter;
    private static long lastPearlTick = NEVER;
    private static boolean smoothing;
    private static double fade;
    private static double guardBlend;
    private static boolean needSnap = true;
    private static ClientPlayerEntity lastPlayer;
    private static int errors;
    private static boolean broken;

    private CameraSmoother() {
    }

    public static boolean isSmoothing() {
        return smoothing && !broken;
    }

    public static double guardLevel() {
        return broken ? 0.0 : guardBlend;
    }

    public static boolean isBroken() {
        return broken;
    }

    public static void onEntityLoad(Entity entity) {
        if (entity.getType() == EntityType.ENDER_PEARL && !pearls.contains(entity)) {
            pearls.add(entity);
        }
    }

    public static void onEntityUnload(Entity entity) {
        pearls.remove(entity);
    }

    public static void tick(MinecraftClient client) {
        if (broken) {
            return;
        }
        try {
            doTick(client);
        } catch (RuntimeException error) {
            fail("tick", error);
        }
    }

    public static double[] apply(MinecraftClient client, double x, double y, double z) {
        long now = System.nanoTime();
        double dt = frameInit
                ? Math.min(Math.max((now - lastFrameNanos) * NANOS_TO_SECONDS, 0.0), MAX_FRAME_DT)
                : 0.0;
        lastFrameNanos = now;
        frameInit = true;

        if (!isActive(client)) {
            followTarget(x, y, z);
            needSnap = false;
            return null;
        }

        double dx = x - cx;
        double dy = y - cy;
        double dz = z - cz;
        if (needSnap || dx * dx + dy * dy + dz * dz > SNAP_DIST_SQ) {
            followTarget(x, y, z);
            needSnap = false;
            FrameBlend.cut();
            return null;
        }

        CameraSettings cfg = Settings.CAMERA;
        double baseSpeed = client.options.getPerspective().isFirstPerson() ? cfg.firstPersonSpeed() : cfg.speed();
        double guardedSpeed = Math.max(baseSpeed, cfg.guardSpeed());
        double speed = baseSpeed + (guardedSpeed - baseSpeed) * guardBlend;
        speed = speed + (1.0 - speed) * fade;

        double step;
        if (dt <= 0.0) {
            step = 0.0;
        } else if (speed >= FULL_SPEED) {
            step = 1.0;
        } else {
            step = 1.0 - Math.pow(1.0 - speed, dt * TICKS_PER_SECOND);
        }
        cx += dx * step;
        cy += dy * step;
        cz += dz * step;

        double maxOffset = cfg.maxOffset() > 0.0 ? cfg.maxOffset() : UNLIMITED_OFFSET;
        double limit = maxOffset + (Math.min(maxOffset, cfg.guardMaxOffset()) - maxOffset) * guardBlend;
        double lagX = x - cx;
        double lagY = y - cy;
        double lagZ = z - cz;
        double lagSq = lagX * lagX + lagY * lagY + lagZ * lagZ;
        if (lagSq > limit * limit) {
            double scale = limit / Math.sqrt(lagSq);
            cx = x - lagX * scale;
            cy = y - lagY * scale;
            cz = z - lagZ * scale;
        }

        OUT[0] = cx;
        OUT[1] = cy;
        OUT[2] = cz;
        return OUT;
    }

    public static void fail(String stage, Throwable error) {
        errors++;
        LOGGER.error("Motion Camera error in {} ({}/{})", stage, errors, MAX_ERRORS, error);
        if (errors >= MAX_ERRORS) {
            broken = true;
            smoothing = false;
            LOGGER.error("Motion Camera disabled itself to protect the game. Please send this log to the developer.");
        }
    }

    private static void followTarget(double x, double y, double z) {
        cx = x;
        cy = y;
        cz = z;
    }

    private static void doTick(MinecraftClient client) {
        CameraSettings cfg = Settings.CAMERA;
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;

        if (player == null || world == null) {
            smoothing = false;
            needSnap = true;
            lastPlayer = null;
            lastPearlTick = NEVER;
            guardBlend = 0.0;
            pearls.clear();
            return;
        }
        if (player != lastPlayer) {
            lastPlayer = player;
            lastPearlTick = NEVER;
            guardBlend = 0.0;
            needSnap = true;
            pearls.clear();
            FrameBlend.cut();
        }

        tickCounter++;
        if (cfg.enabled()) {
            trackOwnPearls(player);
        }
        updateGuard(client, player, world, cfg);

        double eyeX = player.getX();
        double eyeY = player.getY() + player.getStandingEyeHeight();
        double eyeZ = player.getZ();
        double dx = eyeX - cx;
        double dy = eyeY - cy;
        double dz = eyeZ - cz;
        double lagSq = dx * dx + dy * dy + dz * dz;
        if (lagSq > SNAP_DIST_SQ) {
            needSnap = true;
        }

        boolean pearlTracked = lastPearlTick != NEVER;
        if (pearlTracked
                && tickCounter - lastPearlTick >= earlyEndTicks(client, player)
                && lagSq < EARLY_END_LAG_SQ) {
            lastPearlTick = NEVER;
        }

        long holdTicks = (cfg.exceptionDuration() + MILLIS_PER_TICK - 1L) / MILLIS_PER_TICK;
        long overdue = tickCounter - lastPearlTick - holdTicks;
        fade = overdue <= 0L ? 0.0 : Math.min(1.0, overdue / (double) FADE_TICKS);
        smoothing = cfg.enabled() && fade < 1.0;
    }

    private static void trackOwnPearls(ClientPlayerEntity player) {
        for (int index = pearls.size() - 1; index >= 0; index--) {
            Entity pearl = pearls.get(index);
            if (pearl.isRemoved()) {
                pearls.remove(index);
            } else if (pearl instanceof ProjectileEntity projectile && projectile.getOwner() == player) {
                lastPearlTick = tickCounter;
            }
        }
    }

    private static int earlyEndTicks(MinecraftClient client, ClientPlayerEntity player) {
        int latency = 0;
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler != null) {
            PlayerListEntry entry = handler.getPlayerListEntry(player.getUuid());
            if (entry != null) {
                latency = entry.getLatency();
            }
        }
        long ticks = EARLY_END_BASE_TICKS + (latency + MILLIS_PER_TICK - 1L) / MILLIS_PER_TICK;
        return Math.clamp(ticks, EARLY_END_MIN_TICKS, EARLY_END_MAX_TICKS);
    }

    private static void updateGuard(MinecraftClient client, ClientPlayerEntity player, ClientWorld world, CameraSettings cfg) {
        boolean guarding = cfg.crystalGuard() && isCrystalSituation(client, player, world);
        guardBlend = guarding ? Math.min(1.0, guardBlend + GUARD_RISE) : Math.max(0.0, guardBlend - GUARD_FALL);
    }

    private static boolean isCrystalSituation(MinecraftClient client, ClientPlayerEntity player, ClientWorld world) {
        if (player.getMainHandStack().isOf(Items.END_CRYSTAL) || player.getOffHandStack().isOf(Items.END_CRYSTAL)) {
            return true;
        }
        HitResult target = client.crosshairTarget;
        if (target instanceof EntityHitResult entityHit) {
            return entityHit.getEntity().getType() == EntityType.END_CRYSTAL;
        }
        if (target instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = world.getBlockState(blockHit.getBlockPos());
            return state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
        }
        return false;
    }

    private static boolean isActive(MinecraftClient client) {
        Entity cameraEntity = client.getCameraEntity();
        return smoothing
                && !broken
                && cameraEntity != null
                && cameraEntity == client.player
                && cameraEntity == lastPlayer;
    }
}

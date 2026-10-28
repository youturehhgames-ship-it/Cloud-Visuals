package dev.r3vy.motionsuite.blur;

import dev.r3vy.motionsuite.camera.CameraSmoother;
import dev.r3vy.motionsuite.config.BlurSettings;
import dev.r3vy.motionsuite.config.Settings;

import java.util.Arrays;

public final class FrameBlend {
    private static final int WINDOW = 24;
    private static final double STABLE_SPREAD = 2.2;
    private static final double SLOW_RATE = 0.02;
    private static final double SLOW_FLOOR = 0.8;
    private static final double SLOW_CEILING = 1.25;
    private static final double MAX_DT = 0.1;
    private static final double MIN_RATIO = 0.5;
    private static final double MAX_RATIO = 2.0;
    private static final double OFF_LOW = 0.7;
    private static final double OFF_HIGH = 1.45;
    private static final int OFF_FRAMES = 16;
    private static final double RATIO_SMOOTHING = 0.5;
    private static final double RELEARNING = -1.0;
    private static final double NANOS_PER_SECOND = 1.0E9;
    private static final float DEADBAND = 0.01f;
    private static final float QUANTUM = 128.0f;
    private static final float MAX_BLEND = 0.99f;

    private static final double[] window = new double[WINDOW];
    private static boolean pendingSeed = true;
    private static long last;
    private static double slow;
    private static double smoothedRatio = 1.0;
    private static int count;
    private static int head;
    private static int off;

    private FrameBlend() {
    }

    public static void reset() {
        pendingSeed = true;
        last = 0L;
        relearn();
    }

    public static void cut() {
        pendingSeed = true;
    }

    public static float next() {
        long now = System.nanoTime();
        if (pendingSeed) {
            pendingSeed = false;
            last = now;
            return 0.0f;
        }
        double dt = last == 0L ? 0.0 : (now - last) / NANOS_PER_SECOND;
        last = now;

        float base = guardedBase();
        if (!Settings.BLUR.adaptive() || base <= 0.0f) {
            return quantize(base);
        }
        if (slow == 0.0) {
            learn(dt);
            return quantize(base);
        }

        double ratio = measureRatio(dt);
        if (ratio == RELEARNING) {
            return quantize(base);
        }
        smoothedRatio += (ratio - smoothedRatio) * RATIO_SMOOTHING;
        double exponent = Math.max(MIN_RATIO, Math.min(MAX_RATIO, smoothedRatio));
        float scaled = (float) Math.pow(base, exponent);
        return quantize(Math.abs(scaled - base) < DEADBAND ? base : scaled);
    }

    private static float guardedBase() {
        BlurSettings blur = Settings.BLUR;
        double cut = CameraSmoother.guardLevel() * blur.guardCut() / 100.0;
        return (float) (blur.blend() * (1.0 - cut));
    }

    private static float quantize(float value) {
        return Math.min(Math.round(value * QUANTUM) / QUANTUM, MAX_BLEND);
    }

    private static double measureRatio(double dt) {
        if (dt >= MAX_DT) {
            return MAX_RATIO;
        }
        if (dt <= 0.0) {
            return 1.0;
        }
        double ratio = dt / slow;
        if (ratio < OFF_LOW || ratio > OFF_HIGH) {
            off++;
            if (off >= OFF_FRAMES) {
                relearn();
                return RELEARNING;
            }
        } else {
            off = 0;
        }
        double target = Math.max(SLOW_FLOOR * slow, Math.min(SLOW_CEILING * slow, dt));
        slow += (target - slow) * SLOW_RATE;
        return ratio;
    }

    private static void relearn() {
        slow = 0.0;
        smoothedRatio = 1.0;
        count = 0;
        head = 0;
        off = 0;
    }

    private static void learn(double dt) {
        if (dt <= 0.0 || dt >= MAX_DT) {
            count = 0;
            head = 0;
            return;
        }
        window[head] = dt;
        head = (head + 1) % WINDOW;
        if (count < WINDOW) {
            count++;
        }
        if (count < WINDOW) {
            return;
        }
        double[] sorted = window.clone();
        Arrays.sort(sorted);
        if (sorted[WINDOW - 1] <= STABLE_SPREAD * sorted[0]) {
            int middle = WINDOW / 2;
            slow = (sorted[middle - 1] + sorted[middle]) / 2.0;
            off = 0;
        }
    }
}

package dev.r3vy.motionsuite.config;

import com.google.gson.JsonObject;

public final class CameraSettings {
    private static final double DEFAULT_SPEED = 0.25;
    private static final double DEFAULT_FIRST_PERSON_SPEED = 0.3;
    private static final double DEFAULT_MAX_OFFSET = 4.0;
    private static final double DEFAULT_GUARD_MAX_OFFSET = 1.0;
    private static final double DEFAULT_GUARD_SPEED = 0.7;
    private static final int DEFAULT_DURATION = 1200;

    private static final double MIN_SPEED = 0.02;
    private static final double MAX_SPEED = 1.0;
    private static final double MAX_OFFSET_LIMIT = 64.0;
    private static final double MIN_GUARD_OFFSET = 0.1;
    private static final double MAX_GUARD_OFFSET = 8.0;
    private static final int MIN_DURATION = 100;
    private static final int MAX_DURATION = 10000;

    private volatile boolean enabled = true;
    private volatile double speed = DEFAULT_SPEED;
    private volatile double firstPersonSpeed = DEFAULT_FIRST_PERSON_SPEED;
    private volatile int exceptionDuration = DEFAULT_DURATION;
    private volatile double maxOffset = DEFAULT_MAX_OFFSET;
    private volatile boolean crystalGuard = false;
    private volatile double guardMaxOffset = DEFAULT_GUARD_MAX_OFFSET;
    private volatile double guardSpeed = DEFAULT_GUARD_SPEED;

    CameraSettings() {
    }

    public boolean enabled() {
        return enabled;
    }

    public double speed() {
        return speed;
    }

    public double firstPersonSpeed() {
        return firstPersonSpeed;
    }

    public int exceptionDuration() {
        return exceptionDuration;
    }

    public double maxOffset() {
        return maxOffset;
    }

    public boolean crystalGuard() {
        return crystalGuard;
    }

    public double guardMaxOffset() {
        return guardMaxOffset;
    }

    public double guardSpeed() {
        return guardSpeed;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public void setSpeed(double value) {
        speed = clamp(value, MIN_SPEED, MAX_SPEED, DEFAULT_SPEED);
    }

    public void setFirstPersonSpeed(double value) {
        firstPersonSpeed = clamp(value, MIN_SPEED, MAX_SPEED, DEFAULT_FIRST_PERSON_SPEED);
    }

    public void setExceptionDuration(int value) {
        exceptionDuration = Math.max(MIN_DURATION, Math.min(MAX_DURATION, value));
    }

    public void setMaxOffset(double value) {
        maxOffset = clamp(value, 0.0, MAX_OFFSET_LIMIT, DEFAULT_MAX_OFFSET);
    }

    public void setCrystalGuard(boolean value) {
        crystalGuard = value;
    }

    public void setGuardMaxOffset(double value) {
        guardMaxOffset = clamp(value, MIN_GUARD_OFFSET, MAX_GUARD_OFFSET, DEFAULT_GUARD_MAX_OFFSET);
    }

    public void setGuardSpeed(double value) {
        guardSpeed = clamp(value, MIN_SPEED, MAX_SPEED, DEFAULT_GUARD_SPEED);
    }

    void reset() {
        enabled = true;
        speed = DEFAULT_SPEED;
        firstPersonSpeed = DEFAULT_FIRST_PERSON_SPEED;
        exceptionDuration = DEFAULT_DURATION;
        maxOffset = DEFAULT_MAX_OFFSET;
        crystalGuard = false;
        guardMaxOffset = DEFAULT_GUARD_MAX_OFFSET;
        guardSpeed = DEFAULT_GUARD_SPEED;
    }

    void read(JsonObject json) {
        if (json == null) {
            return;
        }
        enabled = Settings.flag(json, "enabled", enabled);
        setSpeed(Settings.number(json, "speed", speed));
        setFirstPersonSpeed(Settings.number(json, "firstPersonSpeed", firstPersonSpeed));
        setExceptionDuration((int) Math.round(Settings.number(json, "exceptionDuration", exceptionDuration)));
        setMaxOffset(Settings.number(json, "maxOffset", maxOffset));
        crystalGuard = Settings.flag(json, "crystalGuard", crystalGuard);
        setGuardMaxOffset(Settings.number(json, "guardMaxOffset", guardMaxOffset));
        setGuardSpeed(Settings.number(json, "guardSpeed", guardSpeed));
    }

    JsonObject write() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("speed", speed);
        json.addProperty("firstPersonSpeed", firstPersonSpeed);
        json.addProperty("exceptionDuration", exceptionDuration);
        json.addProperty("maxOffset", maxOffset);
        json.addProperty("crystalGuard", crystalGuard);
        json.addProperty("guardMaxOffset", guardMaxOffset);
        json.addProperty("guardSpeed", guardSpeed);
        return json;
    }

    private static double clamp(double value, double min, double max, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}

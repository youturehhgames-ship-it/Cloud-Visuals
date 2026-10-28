package dev.r3vy.motionsuite.config;

import com.google.gson.JsonObject;

public final class BlurSettings {
    private static final int DEFAULT_AMOUNT = 50;
    private static final int DEFAULT_GUARD_CUT = 60;
    private static final int MAX_PERCENT = 100;
    private static final int MAX_BLEND_PERCENT = 99;

    private volatile int amount = DEFAULT_AMOUNT;
    private volatile boolean adaptive;
    private volatile int guardCut = DEFAULT_GUARD_CUT;

    BlurSettings() {
    }

    public int amount() {
        return amount;
    }

    public boolean adaptive() {
        return adaptive;
    }

    public int guardCut() {
        return guardCut;
    }

    public float blend() {
        return Math.min(amount, MAX_BLEND_PERCENT) / 100.0f;
    }

    public void setAmount(int value) {
        amount = clampPercent(value);
    }

    public void setAdaptive(boolean value) {
        adaptive = value;
    }

    public void setGuardCut(int value) {
        guardCut = clampPercent(value);
    }

    void reset() {
        amount = DEFAULT_AMOUNT;
        adaptive = false;
        guardCut = DEFAULT_GUARD_CUT;
    }

    void read(JsonObject json) {
        if (json == null) {
            return;
        }
        amount = clampPercent(Math.round(Settings.number(json, "amount", amount)));
        adaptive = Settings.flag(json, "adaptive", adaptive);
        guardCut = clampPercent(Math.round(Settings.number(json, "guardCut", guardCut)));
    }

    JsonObject write() {
        JsonObject json = new JsonObject();
        json.addProperty("amount", amount);
        json.addProperty("adaptive", adaptive);
        json.addProperty("guardCut", guardCut);
        return json;
    }

    private static int clampPercent(long value) {
        return (int) Math.max(0L, Math.min(MAX_PERCENT, value));
    }
}

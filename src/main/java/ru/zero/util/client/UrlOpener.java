package ru.zero.util.client;

import net.minecraft.util.Util;

public final class UrlOpener {
    private UrlOpener() {}

    public static void open(String url) {
        try {
            Util.getOperatingSystem().open(url);
        } catch (Throwable ignored) {
        }
    }
}

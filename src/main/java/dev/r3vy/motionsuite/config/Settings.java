package dev.r3vy.motionsuite.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Settings {
    public static final BlurSettings BLUR = new BlurSettings();
    public static final CameraSettings CAMERA = new CameraSettings();

    private static final Logger LOGGER = LoggerFactory.getLogger("motionsuite");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final Path FILE = CONFIG_DIR.resolve("motionsuite.json");
    private static final Path BAD_FILE = CONFIG_DIR.resolve("motionsuite.json.bad");
    private static final String BLUR_SECTION = "blur";
    private static final String CAMERA_SECTION = "camera";

    private static final AtomicBoolean dirty = new AtomicBoolean();
    private static final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "motionsuite-config-writer");
        thread.setDaemon(true);
        return thread;
    });

    private Settings() {
    }

    public static void load() {
        if (Files.isRegularFile(FILE)) {
            loadUnified();
            return;
        }
        save();
    }

    public static void reload() {
        flush();
        load();
    }

    public static void save() {
        dirty.set(true);
        writer.execute(Settings::flush);
    }

    public static void flush() {
        if (!dirty.compareAndSet(true, false)) {
            return;
        }
        writeSnapshot();
    }

    static double number(JsonObject json, String key, double fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        double value = element.getAsDouble();
        return Double.isNaN(value) || Double.isInfinite(value) ? fallback : value;
    }

    static boolean flag(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            return fallback;
        }
        return element.getAsBoolean();
    }

    private static void loadUnified() {
        try {
            JsonObject root = parseObject(FILE);
            BLUR.read(section(root, BLUR_SECTION));
            CAMERA.read(section(root, CAMERA_SECTION));
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Config unreadable, backed up as {} and defaults restored", BAD_FILE.getFileName(), e);
            quarantine();
            BLUR.reset();
            CAMERA.reset();
            save();
        }
    }

    private static JsonObject parseObject(Path path) throws IOException {
        JsonElement element = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
        if (!element.isJsonObject()) {
            throw new JsonParseException("Root is not an object");
        }
        return element.getAsJsonObject();
    }

    private static JsonObject section(JsonObject root, String key) {
        JsonElement element = root.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static void quarantine() {
        try {
            Files.move(FILE, BAD_FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up the unreadable config", e);
        }
    }

    private static synchronized void writeSnapshot() {
        JsonObject root = new JsonObject();
        root.add(BLUR_SECTION, BLUR.write());
        root.add(CAMERA_SECTION, CAMERA.write());
        Path temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
            moveIntoPlace(temp);
        } catch (IOException e) {
            LOGGER.warn("Could not write {}", FILE.getFileName(), e);
        }
    }

    private static void moveIntoPlace(Path temp) throws IOException {
        try {
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

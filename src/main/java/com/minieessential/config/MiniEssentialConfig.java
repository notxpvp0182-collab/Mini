package com.minieessential.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minieessential.MiniEssential;
import com.minieessential.feature.skin.SkinSlot;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persisted settings for MiniEssential.
 * Stored locally at .minecraft/config/miniessential.json using Gson.
 * Never touches the network or any remote service.
 */
public class MiniEssentialConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Path CONFIG_DIR = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
    public static final Path CONFIG_FILE = CONFIG_DIR.resolve("miniessential.json");

    /** FOV multiplier applied while zooming (1.0 = normal view). */
    public float zoomLevel = 0.25f;
    /** Interpolation speed per tick toward the target zoom (0.01 - 1.0). */
    public float zoomSmoothSpeed = 0.2f;
    /** Scroll-wheel adjustment step per tick while holding the zoom key. */
    public float zoomScrollStep = 0.05f;
    /** Clamp bounds for the zoom level. */
    public float minZoom = 0.1f;
    public float maxZoom = 1.0f;

    /** Stored local skin slots (max 5). */
    public List<SkinSlot> skins = new ArrayList<>();

    public static MiniEssentialConfig load() {
        try {
            if (!Files.isDirectory(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            if (Files.isRegularFile(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                MiniEssentialConfig loaded = GSON.fromJson(json, MiniEssentialConfig.class);
                if (loaded != null) {
                    loaded.clamp();
                    MiniEssential.LOGGER.info("Loaded MiniEssential config from {}", CONFIG_FILE);
                    return loaded;
                }
            }
        } catch (Exception e) {
            MiniEssential.LOGGER.warn("Failed to load MiniEssential config, using defaults", e);
        }

        MiniEssentialConfig config = new MiniEssentialConfig();
        // initialize empty skin slots if none exist
        if (config.skins.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                config.skins.add(new SkinSlot());
            }
        }
        config.save();
        return config;
    }

    public void save() {
        try {
            if (!Files.isDirectory(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_FILE, json);
        } catch (Exception e) {
            MiniEssential.LOGGER.warn("Failed to save MiniEssential config", e);
        }
    }

    /** Clamp all numeric settings into sane ranges after loading. */
    private void clamp() {
        zoomLevel = clamp(zoomLevel, minZoom, maxZoom);
        zoomSmoothSpeed = clamp(zoomSmoothSpeed, 0.01f, 1.0f);
        zoomScrollStep = clamp(zoomScrollStep, 0.001f, 0.5f);
        if (minZoom <= 0) minZoom = 0.05f;
        if (maxZoom > 1.0f) maxZoom = 1.0f;
        if (minZoom > maxZoom) minZoom = maxZoom;
    }

    private static float clamp(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }
}

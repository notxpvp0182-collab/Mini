package com.minieessential.feature.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minieessential.MiniEssential;
import com.minieessential.config.MiniEssentialConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature 3 - Offline Skin Switcher: backend.
 *
 * Stores up to 5 skins as PNG files in .minecraft/miniessential/skins/.
 * Loads them as local textures and applies the active one to the local player.
 * No authentication, no remote skin servers, fully offline/cracked-compatible.
 */
public class SkinSwitcher {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Skins folder: .minecraft/miniessential/skins/ */
    public static final Path SKINS_DIR =
            MinecraftClient.getInstance().runDirectory.toPath().resolve("miniessential").resolve("skins");

    /** Max stored skins. */
    public static final int MAX_SKINS = 5;

    /** Identifier prefix for our custom skin textures. */
    private static final String TEX_PREFIX = MiniEssential.MOD_ID + ":skins/";

    private final MinecraftClient mc = MinecraftClient.getInstance();

    /** Currently loaded custom skin textures, keyed by slot index. */
    private final List<Identifier> skinTextures = new ArrayList<>();

    /** Lazily initialized after config loads. */
    private boolean initialized = false;

    public SkinSwitcher() {
        // ensure folder exists
        try {
            if (!Files.isDirectory(SKINS_DIR)) {
                Files.createDirectories(SKINS_DIR);
            }
        } catch (IOException e) {
            MiniEssential.LOGGER.warn("Failed to create skins folder {}", SKINS_DIR, e);
        }
    }

    /** Call once after MiniEssential.config is loaded. */
    public void init(MiniEssentialConfig config) {
        if (initialized) return;
        initialized = true;
        // clamp list size to MAX_SKINS
        while (config.skins.size() > MAX_SKINS) {
            config.skins.remove(config.skins.size() - 1);
        }
        while (config.skins.size() < MAX_SKINS) {
            config.skins.add(new SkinSlot());
        }
        // ensure only one active
        boolean hasActive = config.skins.stream().anyMatch(s -> s.active);
        if (!hasActive && !config.skins.isEmpty()) {
            config.skins.get(0).active = true;
        }
        reloadTextures();
    }

    /** (Re)load all active skin textures from disk. */
    public void reloadTextures() {
        skinTextures.clear();
        for (int i = 0; i < MAX_SKINS; i++) {
            SkinSlot slot = i < MiniEssential.config.skins.size() ? MiniEssential.config.skins.get(i) : null;
            if (slot != null && !slot.fileName.isEmpty()) {
                Path file = SKINS_DIR.resolve(slot.fileName);
                try {
                    if (Files.isRegularFile(file)) {
                        NativeImage image = NativeImage.read(file.toFile());
                        DynamicTexture texture = new DynamicTexture(image);
                        Identifier id = new Identifier(MiniEssential.MOD_ID, "skins/slot" + i);
                        mc.getTextureManager().registerDynamicTexture(id, texture);
                        skinTextures.add(id);
                    } else {
                        skinTextures.add(null);
                    }
                } catch (IOException e) {
                    MiniEssential.LOGGER.warn("Failed to load skin for slot {}: {}", i, slot.fileName, e);
                    skinTextures.add(null);
                }
            } else {
                skinTextures.add(null);
            }
        }
    }

    /** Identifier of the active skin texture, or null if none. */
    public Identifier getActiveSkinTexture() {
        if (!initialized) return null;
        for (int i = 0; i < MiniEssential.config.skins.size(); i++) {
            SkinSlot slot = MiniEssential.config.skins.get(i);
            if (slot.active && i < skinTextures.size()) {
                return skinTextures.get(i);
            }
        }
        return null;
    }

    /** Whether the active skin uses slim (Alex) arms. */
    public boolean isActiveSlimArms() {
        if (!initialized) return false;
        for (SkinSlot slot : MiniEssential.config.skins) {
            if (slot.active) return slot.slimArms;
        }
        return false;
    }

    /** Set active slot (deactivates others). */
    public void setActive(int index) {
        if (!initialized) return;
        for (SkinSlot slot : MiniEssential.config.skins) {
            slot.active = false;
        }
        if (index >= 0 && index < MiniEssential.config.skins.size()) {
            MiniEssential.config.skins.get(index).active = true;
        }
        MiniEssential.config.save();
    }

    /** Scan the skins folder for .png files and return their names. */
    public List<String> scanAvailableSkins() {
        List<String> names = new ArrayList<>();
        try {
            if (!Files.isDirectory(SKINS_DIR)) {
                Files.createDirectories(SKINS_DIR);
                return names;
            }
            try (var stream = Files.list(SKINS_DIR)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> p.toString().toLowerCase().endsWith(".png"))
                      .map(p -> p.getFileName().toString())
                      .sorted()
                      .forEach(names::add);
            }
        } catch (IOException e) {
            MiniEssential.LOGGER.warn("Failed to scan skins folder", e);
        }
        return names;
    }

    /** Save config to disk. */
    public void save() {
        MiniEssential.config.save();
    }
}

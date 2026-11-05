package com.minieessential.feature.skin;

import com.minieessential.MiniEssential;
import com.minieessential.config.MiniEssentialConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SkinSwitcher {
    public static final Path SKINS_DIR =
            MinecraftClient.getInstance().runDirectory.toPath().resolve("miniessential").resolve("skins");

    public static final int MAX_SKINS = 5;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<Identifier> skinTextures = new ArrayList<>();
    private boolean initialized = false;

    public SkinSwitcher() {
        try {
            if (!Files.isDirectory(SKINS_DIR)) {
                Files.createDirectories(SKINS_DIR);
            }
        } catch (IOException e) {
            MiniEssential.LOGGER.warn("Failed to create skins folder {}", SKINS_DIR, e);
        }
    }

    public void init(MiniEssentialConfig config) {
        if (initialized) return;
        initialized = true;
        while (config.skins.size() > MAX_SKINS) {
            config.skins.remove(config.skins.size() - 1);
        }
        while (config.skins.size() < MAX_SKINS) {
            config.skins.add(new SkinSlot());
        }
        boolean hasActive = config.skins.stream().anyMatch(s -> s.active);
        if (!hasActive && !config.skins.isEmpty()) {
            config.skins.get(0).active = true;
        }
        reloadTextures();
    }

    public void reloadTextures() {
        skinTextures.clear();
        for (int i = 0; i < MAX_SKINS; i++) {
            SkinSlot slot = i < MiniEssential.config.skins.size() ? MiniEssential.config.skins.get(i) : null;
            if (slot != null && !slot.fileName.isEmpty()) {
                Path file = SKINS_DIR.resolve(slot.fileName);
                try {
                    if (Files.isRegularFile(file)) {
                        try (InputStream in = Files.newInputStream(file)) {
                            NativeImage image = NativeImage.read(in);
                            int slotIdx = i;
                            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "miniessential_skin_" + slotIdx, image);
                            Identifier id = Identifier.of(MiniEssential.MOD_ID, "skins/slot" + i);
                            mc.getTextureManager().registerTexture(id, texture);
                            skinTextures.add(id);
                        }
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

    public boolean isActiveSlimArms() {
        if (!initialized) return false;
        for (SkinSlot slot : MiniEssential.config.skins) {
            if (slot.active) return slot.slimArms;
        }
        return false;
    }

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

    public void save() {
        MiniEssential.config.save();
    }
}

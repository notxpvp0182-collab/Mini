package com.minieessential;

import com.minieessential.config.MiniEssentialConfig;
import com.minieessential.feature.skin.SkinSwitcher;
import com.minieessential.feature.zoom.ZoomManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MiniEssential - lightweight client-side utility mod for Minecraft Java 1.21.x (Fabric).
 *
 * Design notes:
 * - Client-side only. No Mojang/Microsoft authentication, no remote skin servers.
 * - All data lives in the local game directory (.minecraft).
 * - No Desktop.getDesktop() / AWT calls - safe for Android launchers (PojavLauncher, etc.).
 * - Mixins are used only where Fabric events cannot do the job (scroll input + FOV).
 */
public class MiniEssential implements ClientModInitializer {
    public static final String MOD_ID = "miniessential";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Local configuration persisted to .minecraft/config/miniessential.json */
    public static MiniEssentialConfig config;
    /** Zoom feature state and interpolation */
    public static ZoomManager zoomManager;
    /** Skin switcher state */
    public static SkinSwitcher skinSwitcher;
    /** Hold 'C' to zoom. Bound in the Controls menu and saved by vanilla. */
    public static KeyBinding zoomKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing MiniEssential client...");

        // Load settings from disk (creates defaults if missing)
        config = MiniEssentialConfig.load();

        // Feature 1: Smooth Zoom
        zoomManager = new ZoomManager(config);

        // Feature 3: Offline Skin Switcher
        skinSwitcher = new SkinSwitcher();
        skinSwitcher.init(config);

        // Register the zoom keybind (default 'C', configurable in Controls menu)
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.minieessential.zoom",
                GLFW.GLFW_KEY_C,
                "category.minieessential.features"
        ));

        // Run zoom interpolation every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> zoomManager.update());

        LOGGER.info("MiniEssential initialized. Hold 'C' to zoom; scroll wheel adjusts zoom level.");
    }
}

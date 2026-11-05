package com.minieessential.feature.zoom;

import com.minieessential.MiniEssential;
import com.minieessential.config.MiniEssentialConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Feature 1 - Smooth Zoom.
 *
 * Behavior:
 *   Hold 'C' to zoom in; release to return to normal view.
 *   Scroll wheel while holding 'C' adjusts the zoom strength live.
 *   Zoom interpolates smoothly (linear interpolation / lerp) each tick so it
 *   never snaps instantly.
 *
 * The actual FOV change is applied by GameRendererZoomMixin, which multiplies
 * the game's FOV modifier by the current zoom value.
 */
public class ZoomManager {
    private final MiniEssentialConfig config;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    /** Currently applied zoom factor (1.0 = normal). Smoothed each tick. */
    private float currentZoom = 1.0f;
    /** Zoom factor we are interpolating toward. */
    private float targetZoom = 1.0f;

    public ZoomManager(MiniEssentialConfig config) {
        this.config = config;
    }

    /** Call once per client tick (registered via END_CLIENT_TICK). */
    public void update() {
        boolean holdingZoom = MiniEssential.zoomKey.isPressed();

        // When holding C, aim for the configured zoom level; otherwise aim for normal view.
        float requested = holdingZoom ? config.zoomLevel : 1.0f;
        this.targetZoom = MathHelper.clamp(requested, config.minZoom, config.maxZoom);

        // Smoothly interpolate toward the target (lerp) each tick.
        float speed = MathHelper.clamp(config.zoomSmoothSpeed, 0.01f, 1.0f);
        this.currentZoom = MathHelper.lerp(speed, this.currentZoom, this.targetZoom);

        // Snap exactly to 1.0 when idle to avoid floating-point jitter.
        if (!holdingZoom && Math.abs(this.currentZoom - 1.0f) < 0.001f) {
            this.currentZoom = 1.0f;
        }
    }

    /**
     * Adjust the configured zoom level via the scroll wheel while holding 'C'.
     * Scroll up (vertical > 0) zooms in; scroll down zooms out.
     */
    public void onScroll(double vertical) {
        if (!MiniEssential.zoomKey.isPressed()) {
            return;
        }
        if (vertical == 0.0) {
            return;
        }

        // Scroll up -> stronger zoom (smaller FOV multiplier). Scroll down -> weaker zoom.
        if (vertical > 0.0) {
            config.zoomLevel -= config.zoomScrollStep;
        } else {
            config.zoomLevel += config.zoomScrollStep;
        }
        config.zoomLevel = MathHelper.clamp(config.zoomLevel, config.minZoom, config.maxZoom);
        this.targetZoom = config.zoomLevel;

        // Persist the new preferred zoom level to disk.
        MiniEssential.config.save();
    }

    /** Current smoothed zoom factor (used by the FOV mixin). */
    public float getCurrentZoom() {
        return currentZoom;
    }

    /** Whether the zoom key is currently held down. */
    public boolean isZooming() {
        return MiniEssential.zoomKey.isPressed();
    }
}

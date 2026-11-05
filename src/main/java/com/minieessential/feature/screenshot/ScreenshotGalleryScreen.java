package com.minieessential.feature.screenshot;

import com.minieessential.MiniEssential;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feature 2 - Offline Screenshot Manager: gallery screen.
 *
 * Shows a scrollable list of local screenshots with thumbnails.
 * - Left click an entry -> open ScreenshotViewerScreen
 * - Right click an entry -> open ScreenshotOptionsScreen (Copy Path / Open Folder / Delete)
 *
 * Fully local. No AWT/Desktop calls - safe on Android launchers (PojavLauncher).
 * Clipboard uses GLFW (LWJGL); folder opening uses Util.getOperatingSystem() + ProcessBuilder
 * guarded to desktop OSes, falling back to copy-to-clipboard on unknown (e.g. Android).
 */
public class ScreenshotGalleryScreen extends Screen {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ScreenshotManager manager = new ScreenshotManager();

    /** Loaded thumbnail textures, keyed by screenshot path. */
    private final Map<Path, Identifier> textures = new ConcurrentHashMap<>();

    private int scrollY = 0;
    private final int entryHeight = 46;
    private final int listTop = 44;
    private final int listLeft = 30;
    private ScreenshotManager.ScreenshotEntry hovered = null;

    public ScreenshotGalleryScreen() {
        super(Text.literal("Screenshot Gallery"));
    }

    @Override
    protected void init() {
        // Back button
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> mc.setScreen(null))
                .dimensions(width / 2 - 100, height - 28, 200, 20).build());
        // Open the screenshots folder in the OS file explorer (desktop OSes only)
        addDrawableChild(ButtonWidget.builder(Text.literal("Open Screenshots Folder"), btn -> openFolder())
                .dimensions(width / 2 - 100, height - 52, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        renderBackground(graphics);
        graphics.drawCenteredText(mc.textRenderer, title, width / 2, 16, 0xFFFFFF);

        List<ScreenshotManager.ScreenshotEntry> entries = manager.listScreenshots();
        int y = listTop - scrollY;
        hovered = null;

        for (ScreenshotManager.ScreenshotEntry entry : entries) {
            // skip rows outside the visible area
            if (y + entryHeight < listTop || y > height - 40) {
                y += entryHeight;
                continue;
            }

            boolean mouseOver = mouseX >= listLeft && mouseX <= listLeft + width - 60
                                && mouseY >= y && mouseY <= y + entryHeight - 6;
            if (mouseOver) {
                hovered = entry;
            }

            // row background
            int rowColor = mouseOver ? 0x50FFFFFF : 0x22FFFFFF;
            graphics.fill(listLeft, y, listLeft + width - 60, y + entryHeight - 6, rowColor);

            // thumbnail
            Identifier tex = getTexture(entry.path());
            if (tex != null) {
                graphics.blit(tex, listLeft + 6, y + 6, 0, 0, 34, 34, 34, 34);
            } else {
                graphics.fill(listLeft + 6, y + 6, listLeft + 40, y + 40, 0xFF555555);
            }

            // entry name
            graphics.drawString(mc.textRenderer, entry.name(), listLeft + 48, y + 8, 0xFFFFFF);
            // size + date
            String info = formatSize(entry.size()) + "  ·  " + new Date(entry.lastModified()).toString();
            graphics.drawString(mc.textRenderer, info, listLeft + 48, y + 24, 0xA0A0A0);

            y += entryHeight;
        }

        if (entries.isEmpty()) {
            graphics.drawCenteredText(mc.textRenderer, "No screenshots found.", width / 2, height / 2, 0x808080);
        }

        super.render(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<ScreenshotManager.ScreenshotEntry> entries = manager.listScreenshots();
        int y = listTop - scrollY;
        for (ScreenshotManager.ScreenshotEntry entry : entries) {
            if (mouseX >= listLeft && mouseX <= listLeft + width - 60
                && mouseY >= y && mouseY <= y + entryHeight - 6) {
                if (button == 0) {
                    // left click: open viewer
                    mc.setScreen(new ScreenshotViewerScreen(entry));
                    return true;
                } else if (button == 1) {
                    // right click: options menu
                    mc.setScreen(new ScreenshotOptionsScreen(this, entry));
                    return true;
                }
            }
            y += entryHeight;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double scrollAmount) {
        List<ScreenshotManager.ScreenshotEntry> entries = manager.listScreenshots();
        int maxScroll = Math.max(0, entries.size() * entryHeight - (height - listTop - 40));
        scrollY = (int) Math.clamp(scrollY - (int) (scrollAmount * 16), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            mc.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        textures.clear();
        super.close();
    }

    /** Load (and cache) a dynamic texture from a screenshot file. */
    private Identifier getTexture(Path path) {
        return textures.computeIfAbsent(path, p -> {
            try {
                NativeImage image = NativeImage.read(p.toFile());
                DynamicTexture texture = new DynamicTexture(image);
                String safe = p.getFileName().toString().toLowerCase();
                Identifier id = new Identifier(MiniEssential.MOD_ID, "screenshot/" + safe);
                mc.getTextureManager().registerDynamicTexture(id, texture);
                return id;
            } catch (IOException e) {
                MiniEssential.LOGGER.warn("Failed to load screenshot thumbnail: {}", p, e);
                return null;
            }
        });
    }

    private void openFolder() {
        Path folder = ScreenshotManager.SCREENSHOTS_DIR;
        Util.OperatingSystem os = Util.getOperatingSystem();
        try {
            if (os == Util.OperatingSystem.WINDOWS) {
                new ProcessBuilder("explorer", folder.toAbsolutePath().toString()).start();
            } else if (os == Util.OperatingSystem.MACOS) {
                new ProcessBuilder("open", folder.toAbsolutePath().toString()).start();
            } else if (os == Util.OperatingSystem.LINUX) {
                new ProcessBuilder("xdg-open", folder.toAbsolutePath().toString()).start();
            } else {
                // Android / unknown: copy path to clipboard instead
                copyToClipboard(folder.toAbsolutePath().toString());
            }
        } catch (IOException e) {
            MiniEssential.LOGGER.warn("Failed to open screenshot folder", e);
            copyToClipboard(folder.toAbsolutePath().toString());
        }
    }

    /** Copy text to the OS clipboard via GLFW (no AWT). */
    private void copyToClipboard(String text) {
        long window = mc.getWindow().getHandle();
        GLFW.glfwSetClipboardString(window, text);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024L * 1024)) + " MB";
    }
}

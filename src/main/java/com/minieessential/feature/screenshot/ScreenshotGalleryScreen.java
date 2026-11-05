package com.minieessential.feature.screenshot;

import com.minieessential.MiniEssential;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScreenshotGalleryScreen extends Screen {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ScreenshotManager manager = new ScreenshotManager();

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
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> mc.setScreen(null))
                .dimensions(width / 2 - 100, height - 28, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Open Screenshots Folder"), btn -> openFolder())
                .dimensions(width / 2 - 100, height - 52, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        super.render(context, mouseX, mouseY, tickDelta);
        context.drawCenteredTextWithShadow(mc.textRenderer, title, width / 2, 16, 0xFFFFFFFF);

        List<ScreenshotManager.ScreenshotEntry> entries = manager.listScreenshots();
        int y = listTop - scrollY;
        hovered = null;

        for (ScreenshotManager.ScreenshotEntry entry : entries) {
            if (y + entryHeight < listTop || y > height - 60) {
                y += entryHeight;
                continue;
            }

            boolean mouseOver = mouseX >= listLeft && mouseX <= listLeft + width - 60
                                && mouseY >= y && mouseY <= y + entryHeight - 6;
            if (mouseOver) {
                hovered = entry;
            }

            int rowColor = mouseOver ? 0x50FFFFFF : 0x22FFFFFF;
            context.fill(listLeft, y, listLeft + width - 60, y + entryHeight - 6, rowColor);

            Identifier tex = getTexture(entry.path());
            if (tex != null) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, tex, listLeft + 6, y + 6, 0.0f, 0.0f, 34, 34, 34, 34);
            } else {
                context.fill(listLeft + 6, y + 6, listLeft + 40, y + 40, 0xFF555555);
            }

            context.drawTextWithShadow(mc.textRenderer, entry.name(), listLeft + 48, y + 8, 0xFFFFFFFF);
            String info = formatSize(entry.size()) + "  ·  " + new Date(entry.lastModified()).toString();
            context.drawTextWithShadow(mc.textRenderer, info, listLeft + 48, y + 24, 0xFFA0A0A0);

            y += entryHeight;
        }

        if (entries.isEmpty()) {
            context.drawCenteredTextWithShadow(mc.textRenderer, "No screenshots found.", width / 2, height / 2, 0xFF808080);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        List<ScreenshotManager.ScreenshotEntry> entries = manager.listScreenshots();
        int y = listTop - scrollY;
        for (ScreenshotManager.ScreenshotEntry entry : entries) {
            if (mouseX >= listLeft && mouseX <= listLeft + width - 60
                && mouseY >= y && mouseY <= y + entryHeight - 6) {
                if (button == 0) {
                    mc.setScreen(new ScreenshotViewerScreen(entry));
                    return true;
                } else if (button == 1) {
                    mc.setScreen(new ScreenshotOptionsScreen(this, entry));
                    return true;
                }
            }
            y += entryHeight;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<ScreenshotManager.ScreenshotEntry> entries = manager.listScreenshots();
        int maxScroll = Math.max(0, entries.size() * entryHeight - (height - listTop - 60));
        scrollY = Math.clamp(scrollY - (int) (verticalAmount * 16), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) {
            mc.setScreen(null);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        textures.clear();
        super.close();
    }

    private Identifier getTexture(Path path) {
        return textures.computeIfAbsent(path, p -> {
            try (InputStream in = Files.newInputStream(p)) {
                NativeImage image = NativeImage.read(in);
                String safe = p.getFileName().toString().toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
                NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "screenshot_" + safe, image);
                Identifier id = Identifier.of(MiniEssential.MOD_ID, "screenshot/" + safe);
                mc.getTextureManager().registerTexture(id, texture);
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
            } else if (os == Util.OperatingSystem.OSX) {
                new ProcessBuilder("open", folder.toAbsolutePath().toString()).start();
            } else if (os == Util.OperatingSystem.LINUX) {
                new ProcessBuilder("xdg-open", folder.toAbsolutePath().toString()).start();
            } else {
                copyToClipboard(folder.toAbsolutePath().toString());
            }
        } catch (IOException e) {
            MiniEssential.LOGGER.warn("Failed to open screenshot folder", e);
            copyToClipboard(folder.toAbsolutePath().toString());
        }
    }

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

package com.minieessential.feature.screenshot;

import com.minieessential.MiniEssential;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;

public class ScreenshotOptionsScreen extends Screen {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Screen parent;
    private final ScreenshotManager.ScreenshotEntry entry;
    private final ScreenshotManager manager = new ScreenshotManager();

    public ScreenshotOptionsScreen(Screen parent, ScreenshotManager.ScreenshotEntry entry) {
        super(Text.literal("Screenshot Options"));
        this.parent = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2 - 40;
        int bw = 160, bh = 20;
        int gap = 26;

        addDrawableChild(ButtonWidget.builder(Text.literal("Open Folder"), btn -> openFolder())
                .dimensions(cx - bw / 2, cy, bw, bh).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Copy Path"), btn -> copyPath())
                .dimensions(cx - bw / 2, cy + gap, bw, bh).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), btn -> delete())
                .dimensions(cx - bw / 2, cy + gap * 2, bw, bh).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> mc.setScreen(parent))
                .dimensions(cx - bw / 2, cy + gap * 3, bw, bh).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        super.render(context, mouseX, mouseY, tickDelta);
        context.drawCenteredTextWithShadow(mc.textRenderer, entry.name(), width / 2, height / 2 - 60, 0xFFFFFFFF);
    }

    private void openFolder() {
        Path folder = entry.path().getParent();
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
            mc.setScreen(parent);
        } catch (IOException e) {
            MiniEssential.LOGGER.warn("Failed to open folder", e);
            copyToClipboard(folder.toAbsolutePath().toString());
        }
    }

    private void copyPath() {
        copyToClipboard(entry.path().toAbsolutePath().toString());
        mc.setScreen(parent);
    }

    private void delete() {
        try {
            manager.deleteScreenshot(entry.path());
        } catch (IOException e) {
            MiniEssential.LOGGER.warn("Failed to delete screenshot: {}", entry.path(), e);
        }
        mc.setScreen(parent);
    }

    private void copyToClipboard(String text) {
        long window = mc.getWindow().getHandle();
        GLFW.glfwSetClipboardString(window, text);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) {
            mc.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }
}

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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Feature 2 - Offline Screenshot Manager: single-image viewer.
 *
 * Loads one local screenshot as a dynamic texture and draws it centered.
 * No network, no remote services.
 */
public class ScreenshotViewerScreen extends Screen {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ScreenshotManager.ScreenshotEntry entry;

    private Identifier textureId;

    public ScreenshotViewerScreen(ScreenshotManager.ScreenshotEntry entry) {
        super(Text.literal("Screenshot"));
        this.entry = entry;
    }

    @Override
    public void tick() {
        super.tick();
        // load texture once the screen is open
        if (textureId == null) {
            loadTexture();
        }
    }

    private void loadTexture() {
        try {
            NativeImage image = NativeImage.read(entry.path().toFile());
            DynamicTexture texture = new DynamicTexture(image);
            textureId = new Identifier(MiniEssential.MOD_ID, "viewer/" + entry.name().toLowerCase());
            mc.getTextureManager().registerDynamicTexture(textureId, texture);
        } catch (IOException e) {
            MiniEssential.LOGGER.warn("Failed to load screenshot: {}", entry.path(), e);
            mc.setScreen(null);
        }
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> mc.setScreen(null))
                .dimensions(width / 2 - 100, height - 28, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        renderBackground(graphics);
        if (textureId != null) {
            // draw thumbnail-sized preview centered (256x256); aspect preserved loosely
            int size = 256;
            int x = (width - size) / 2;
            int y = (height - size) / 2 - 20;
            graphics.blit(textureId, x, y, 0, 0, size, size, size, size);
        }
        graphics.drawCenteredText(mc.textRenderer, entry.name(), width / 2, 16, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, tickDelta);
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
        super.close();
    }
}

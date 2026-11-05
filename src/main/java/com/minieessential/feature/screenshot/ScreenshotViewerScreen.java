package com.minieessential.feature.screenshot;

import com.minieessential.MiniEssential;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

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
        if (textureId == null) {
            loadTexture();
        }
    }

    private void loadTexture() {
        try (InputStream in = Files.newInputStream(entry.path())) {
            NativeImage image = NativeImage.read(in);
            String safe = entry.name().toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "viewer_" + safe, image);
            textureId = Identifier.of(MiniEssential.MOD_ID, "viewer/" + safe);
            mc.getTextureManager().registerTexture(textureId, texture);
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
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        super.render(context, mouseX, mouseY, tickDelta);
        if (textureId != null) {
            int size = 256;
            int x = (width - size) / 2;
            int y = (height - size) / 2 - 20;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, textureId, x, y, 0.0f, 0.0f, size, size, size, size);
        }
        context.drawCenteredTextWithShadow(mc.textRenderer, entry.name(), width / 2, 16, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) {
            mc.setScreen(null);
            return true;
        }
        return super.keyPressed(input);
    }
}

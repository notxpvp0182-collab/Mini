package com.minieessential.feature.menu.mixin;

import com.minieessential.feature.screenshot.ScreenshotGalleryScreen;
import com.minieessential.feature.skin.SkinSwitcherScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class PauseMenuMixin extends Screen {
    protected PauseMenuMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int bx = this.width - 118;
        int by = 62;
        int bw = 102;
        int bh = 20;
        int gap = 24;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("button.minieessential.screenshots"),
                btn -> MinecraftClient.getInstance().setScreen(new ScreenshotGalleryScreen()))
                .dimensions(bx, by, bw, bh).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("button.minieessential.skins"),
                btn -> MinecraftClient.getInstance().setScreen(new SkinSwitcherScreen()))
                .dimensions(bx, by + gap, bw, bh).build());
    }
}

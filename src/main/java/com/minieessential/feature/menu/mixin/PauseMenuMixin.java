package com.minieessential.feature.menu.mixin;

import com.minieessential.MiniEssential;
import com.minieessential.feature.screenshot.ScreenshotGalleryScreen;
import com.minieessential.feature.skin.SkinSwitcherScreen;
import net.minecraft.client.gui.screen.PauseMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feature 4 - Pause Menu Integration.
 *
 * Adds two small, non-intrusive buttons to the in-game pause (ESC) menu so the
 * player can open the Screenshot Manager and Skin Switcher directly, without
 * leaving the game or typing commands.
 *
 * Placed on the right side of the screen so they never collide with the
 * centered vanilla buttons (Back to Game, Options, Save and Quit, …).
 */
@Mixin(PauseMenuScreen.class)
public class PauseMenuMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        PauseMenuScreen screen = (PauseMenuScreen) (Object) this;

        int bx = screen.width - 118;   // right side, clear of centered vanilla buttons
        int by = 62;                   // start below the title area
        int bw = 102;
        int bh = 20;
        int gap = 24;

        // Screenshot Manager button
        screen.addDrawableChild(ButtonWidget.builder(
                Text.translatable("button.minieessential.screenshots"),
                btn -> screen.client.setScreen(new ScreenshotGalleryScreen()))
                .dimensions(bx, by, bw, bh).build());

        // Skin Switcher button
        screen.addDrawableChild(ButtonWidget.builder(
                Text.translatable("button.minieessential.skins"),
                btn -> screen.client.setScreen(new SkinSwitcherScreen()))
                .dimensions(bx, by + gap, bw, bh).build());
    }
}

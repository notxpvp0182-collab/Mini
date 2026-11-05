package com.minieessential.feature.zoom.mixin;

import com.minieessential.MiniEssential;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures mouse scroll input so the scroll wheel can adjust zoom strength
 * while holding 'C'. Fabric does not expose a public scroll event, so this is
 * the one necessary mixin for input. It cancels the scroll when zooming so the
 * hotbar does not scroll at the same time.
 */
@Mixin(Mouse.class)
public class MouseScrollMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (vertical != 0.0 && MiniEssential.zoomKey.isPressed()) {
            MiniEssential.zoomManager.onScroll(vertical);
            ci.cancel();
        }
    }
}

package com.minieessential.feature.zoom.mixin;

import com.minieessential.MiniEssential;
import com.minieessential.feature.zoom.ZoomManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the smoothed zoom value to the game's field of view.
 *
 * Hooked into GameRenderer.getFovModifier so the zoom multiplies the FOV the
 * same way vanilla/zoom mods do - this is the standard, compatible place for
 * client-side zoom in Fabric 1.21.x.
 */
@Mixin(GameRenderer.class)
public class GameRendererZoomMixin {
    @Inject(method = "getFovModifier", at = @At("RETURN"), cancellable = true)
    private void onGetFovModifier(Camera camera, boolean renderHand, float tickDelta, CallbackInfoReturnable<Float> cir) {
        float fov = (Float) cir.getReturnValue();
        float zoom = MiniEssential.zoomManager.getCurrentZoom();
        cir.setReturnValue(fov * zoom);
    }
}

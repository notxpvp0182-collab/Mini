package com.minieessential.feature.skin.mixin;

import com.minieessential.MiniEssential;
import com.minieessential.feature.skin.SkinSwitcher;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Toggles arm thickness for the local player model based on the active skin slot.
 *
 * When slimArms is true, arms render thinner (Alex style); when false, wider (Steve style).
 * This is a client-side-only visual change for cracked/local play.
 */
@Mixin(PlayerEntityModel.class)
public class PlayerModelSlimMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        // The slim field on PlayerEntityModel/HumanoidModel controls arm thickness.
        // We set it based on the active skin slot's slimArms flag.
        boolean slim = MiniEssential.skinSwitcher.isActiveSlimArms();
        try {
            java.lang.reflect.Field f = model.getClass().getDeclaredField("slim");
            f.setAccessible(true);
            f.setBoolean(model, slim);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            MiniEssential.LOGGER.warn("Could not set slim arms on player model", e);
        }
    }
}

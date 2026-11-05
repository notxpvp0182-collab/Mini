package com.minieessential.feature.skin.mixin;

import com.minieessential.MiniEssential;
import com.minieessential.feature.skin.SkinSwitcher;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects the active local skin texture into the local player's rendering.
 *
 * Fabric's AbstractClientPlayerEntity.getSkinTexture() normally returns the
 * skin texture loaded from Mojang's servers. For cracked/local play we override
 * it to return our locally-loaded texture so the custom skin renders instantly.
 */
@Mixin(AbstractClientPlayerEntity.class)
public class PlayerSkinMixin {
    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void onGetSkinTexture(CallbackInfoReturnable<Identifier> cir) {
        // Only override the local player; other players keep their normal skins.
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        if (player != player.getWorld().getClosestPlayer(player.getX(), player.getY(), player.getZ(), 1.0, false)) {
            return;
        }

        Identifier custom = MiniEssential.skinSwitcher.getActiveSkinTexture();
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}

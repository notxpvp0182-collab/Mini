package com.minieessential.feature.skin.mixin;

import com.minieessential.MiniEssential;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class PlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void onGetSkin(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        if (player != MinecraftClient.getInstance().player) {
            return;
        }

        Identifier custom = MiniEssential.skinSwitcher.getActiveSkinTexture();
        if (custom != null) {
            SkinTextures orig = cir.getReturnValue();
            AssetInfo.TextureAsset bodyAsset = new AssetInfo.TextureAssetInfo(custom, custom);
            PlayerSkinType model = MiniEssential.skinSwitcher.isActiveSlimArms()
                    ? PlayerSkinType.SLIM
                    : PlayerSkinType.WIDE;
            cir.setReturnValue(SkinTextures.create(bodyAsset, orig.cape(), orig.elytra(), model));
        }
    }
}

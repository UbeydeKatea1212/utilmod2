package com.example.utilmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.utilmod.freecam.FreecamController;

import net.minecraft.client.MinecraftClient;

/**
 * Interaction lock. Vanilla raycasts and interacts from the PLAYER's real position, not the camera's, so while the
 * camera is detached a click would hit something you aren't looking at. Block those actions entirely.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void utilmod$noAttack(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamController.isActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void utilmod$noUse(CallbackInfo ci) {
        if (FreecamController.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void utilmod$noBreak(boolean breaking, CallbackInfo ci) {
        if (FreecamController.isActive()) {
            ci.cancel();
        }
    }
}

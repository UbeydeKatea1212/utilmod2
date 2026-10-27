package com.example.utilmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.utilmod.freecam.FreecamController;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

/** Mouse movement arrives via Entity#changeLookDirection on the local player. Redirect it to the camera. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void utilmod$redirectLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (FreecamController.isActive() && (Object) this instanceof ClientPlayerEntity) {
            FreecamController.look(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}

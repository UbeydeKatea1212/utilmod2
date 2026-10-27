package com.example.utilmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.utilmod.freecam.FreecamController;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;

/**
 * After vanilla has read the movement keys into the player's Input, clear them so the player stands still while
 * the same keys drive the camera (FreecamController reads the KeyBindings directly).
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void utilmod$freezePlayerInput(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (!FreecamController.isActive()) {
            return;
        }
        this.pressingForward = false;
        this.pressingBack = false;
        this.pressingLeft = false;
        this.pressingRight = false;
        this.jumping = false;
        this.sneaking = false;
        this.movementForward = 0.0F;
        this.movementSideways = 0.0F;
    }
}

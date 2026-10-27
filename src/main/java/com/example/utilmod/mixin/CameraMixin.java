package com.example.utilmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.utilmod.freecam.FreecamController;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

/**
 * Camera#update computes the render camera from the focused entity (the player). We let vanilla run, then
 * overwrite position and rotation at the tail. Everything downstream (frustum, chunk culling, fog, sounds,
 * our own ESP renderer that reads context.camera()) sees the freecam position automatically.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    private boolean thirdPerson;

    @Inject(method = "update", at = @At("TAIL"))
    private void utilmod$applyFreecam(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                      boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!FreecamController.isActive()) {
            return;
        }
        Vec3d pos = FreecamController.getInterpolatedPos(tickDelta);
        this.setPos(pos.x, pos.y, pos.z);
        this.setRotation(FreecamController.getYaw(), FreecamController.getPitch());
        // "Third person" makes the renderer draw the player model (so you can see where you left it)
        // and hides the first-person hand.
        this.thirdPerson = true;
    }
}

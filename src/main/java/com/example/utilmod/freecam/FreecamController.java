package com.example.utilmod.freecam;

import com.example.utilmod.config.UtilModConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * State and movement for the detached camera.
 *
 * How it works, in short:
 *  - The real player entity is left completely alone (same position, same rotation, no packets sent).
 *  - CameraMixin overrides the render Camera's position/rotation every frame with the values held here.
 *  - KeyboardInputMixin zeroes the player's movement input so WASD moves the camera instead of the player.
 *  - EntityMixin redirects mouse-look into {@link #look} instead of turning the player.
 *  - MinecraftClientMixin blocks attack / use / break so clicks can't act from the player's real position.
 *
 * Movement is simulated per tick and interpolated per frame (prevPos -> pos by tickDelta) for smooth motion.
 */
public final class FreecamController {
    private FreecamController() {}

    private static boolean active;
    private static Vec3d prevPos = Vec3d.ZERO;
    private static Vec3d pos = Vec3d.ZERO;
    private static float yaw;
    private static float pitch;

    public static boolean isActive() {
        return active;
    }

    public static void toggle(MinecraftClient client) {
        if (active) {
            disable();
        } else {
            enable(client);
        }
    }

    public static void enable(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        // Start exactly where the normal camera currently is, so there's no visual jump.
        Camera camera = client.gameRenderer.getCamera();
        pos = camera.getPos();
        prevPos = pos;
        yaw = camera.getYaw();
        pitch = camera.getPitch();
        active = true;
    }

    public static void disable() {
        active = false;
    }

    /** Client tick: apply movement keys to the camera. */
    public static void tick(MinecraftClient client) {
        if (!active) {
            return;
        }
        prevPos = pos;
        if (client.currentScreen != null) {
            return; // inventory / chat open: don't drift
        }

        GameOptions options = client.options;
        double forward = axis(options.forwardKey.isPressed(), options.backKey.isPressed());
        double strafe = axis(options.leftKey.isPressed(), options.rightKey.isPressed());
        double vertical = axis(options.jumpKey.isPressed(), options.sneakKey.isPressed());

        // Minecraft yaw 0 faces +Z; forward = (-sin, cos), left = (cos, sin).
        double yawRad = Math.toRadians(yaw);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double lx = Math.cos(yawRad);
        double lz = Math.sin(yawRad);

        Vec3d move = new Vec3d(fx * forward + lx * strafe, vertical, fz * forward + lz * strafe);
        if (move.lengthSquared() > 1.0E-6) {
            double speed = UtilModConfig.freecamSpeed
                    * (options.sprintKey.isPressed() ? UtilModConfig.freecamSprintMultiplier : 1.0);
            pos = pos.add(move.normalize().multiply(speed));
        }
    }

    /** Mouse look, same sensitivity scaling as vanilla's Entity#changeLookDirection. */
    public static void look(double cursorDeltaX, double cursorDeltaY) {
        yaw += (float) cursorDeltaX * 0.15F;
        pitch = MathHelper.clamp(pitch + (float) cursorDeltaY * 0.15F, -90.0F, 90.0F);
    }

    public static Vec3d getInterpolatedPos(float tickDelta) {
        return prevPos.lerp(pos, tickDelta);
    }

    public static Vec3d getPos() {
        return pos;
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }

    private static double axis(boolean positive, boolean negative) {
        return (positive ? 1.0 : 0.0) - (negative ? 1.0 : 0.0);
    }
}

package com.example.utilmod.render;

import org.joml.Matrix4f;

import com.example.utilmod.config.UtilModConfig;
import com.example.utilmod.scan.BlockScanner;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/**
 * Draws a box around every scanner hit.
 *
 * Event choice: AFTER_TRANSLUCENT runs once terrain, entities and translucent geometry are drawn and while the
 * world's model-view/projection state is still set up, so a direct Tessellator draw lines up with the world.
 * WorldRenderEvents.LAST works the same way for this purpose (it fires later, after the world border etc.) - to
 * try it, change the one register call below.
 */
public final class EspRenderer {
    private EspRenderer() {}

    public static void register(BlockScanner scanner) {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> render(context, scanner));
    }

    private static void render(WorldRenderContext context, BlockScanner scanner) {
        if (!UtilModConfig.espEnabled || scanner.matchCount() == 0) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        double maxDistSq = UtilModConfig.espMaxDistance * UtilModConfig.espMaxDistance;

        LineBatch batch = new LineBatch(positionMatrix, cameraPos);
        scanner.forEachMatch(match -> {
            if (match.pos().getSquaredDistance(cameraPos) > maxDistSq) {
                return; // distance cull: don't even emit vertices
            }
            batch.blockOutline(match.pos(), match.target().rgb(), 0.9F);
        });
        batch.draw(UtilModConfig.espSeeThrough, UtilModConfig.espLineWidth);
    }
}

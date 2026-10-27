package com.example.utilmod.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Collects any number of world-space lines/boxes into ONE buffer and draws them with ONE draw call.
 *
 * Coordinates are converted to camera-relative space in double precision before being cast to float. That keeps
 * the outlines stable even at huge coordinates (float precision gets visibly jittery past ~100k blocks).
 *
 * Usage: create, add shapes, call {@link #draw}. Always call draw() once, even if nothing was added.
 */
public final class LineBatch {
    private static final float EDGE_INFLATE = 0.002F; // avoids z-fighting with the block faces

    private final BufferBuilder buffer;
    private final Matrix4f matrix;
    private final double camX;
    private final double camY;
    private final double camZ;
    private int vertexCount;

    public LineBatch(Matrix4f positionMatrix, Vec3d cameraPos) {
        this.matrix = positionMatrix;
        this.camX = cameraPos.x;
        this.camY = cameraPos.y;
        this.camZ = cameraPos.z;
        this.buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
    }

    /** One line segment in world coordinates. {@code rgb} is 0xRRGGBB, alpha is 0..1. */
    public void line(double x1, double y1, double z1, double x2, double y2, double z2, int rgb, float alpha) {
        float ax = (float) (x1 - camX);
        float ay = (float) (y1 - camY);
        float az = (float) (z1 - camZ);
        float bx = (float) (x2 - camX);
        float by = (float) (y2 - camY);
        float bz = (float) (z2 - camZ);

        // The lines shader expands each segment into a quad along its direction, so it needs the normal.
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float length = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6F) {
            return;
        }
        dx /= length;
        dy /= length;
        dz /= length;

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int a = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);

        buffer.vertex(matrix, ax, ay, az).color(r, g, b, a).normal(dx, dy, dz);
        buffer.vertex(matrix, bx, by, bz).color(r, g, b, a).normal(dx, dy, dz);
        vertexCount += 2;
    }

    /** The 12 edges of an axis-aligned box in world coordinates. */
    public void box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                    int rgb, float alpha) {
        // bottom ring
        line(minX, minY, minZ, maxX, minY, minZ, rgb, alpha);
        line(maxX, minY, minZ, maxX, minY, maxZ, rgb, alpha);
        line(maxX, minY, maxZ, minX, minY, maxZ, rgb, alpha);
        line(minX, minY, maxZ, minX, minY, minZ, rgb, alpha);
        // top ring
        line(minX, maxY, minZ, maxX, maxY, minZ, rgb, alpha);
        line(maxX, maxY, minZ, maxX, maxY, maxZ, rgb, alpha);
        line(maxX, maxY, maxZ, minX, maxY, maxZ, rgb, alpha);
        line(minX, maxY, maxZ, minX, maxY, minZ, rgb, alpha);
        // verticals
        line(minX, minY, minZ, minX, maxY, minZ, rgb, alpha);
        line(maxX, minY, minZ, maxX, maxY, minZ, rgb, alpha);
        line(maxX, minY, maxZ, maxX, maxY, maxZ, rgb, alpha);
        line(minX, minY, maxZ, minX, maxY, maxZ, rgb, alpha);
    }

    /** Outline of one full block, nudged outward a hair so it doesn't z-fight. */
    public void blockOutline(BlockPos pos, int rgb, float alpha) {
        box(pos.getX() - EDGE_INFLATE, pos.getY() - EDGE_INFLATE, pos.getZ() - EDGE_INFLATE,
                pos.getX() + 1 + EDGE_INFLATE, pos.getY() + 1 + EDGE_INFLATE, pos.getZ() + 1 + EDGE_INFLATE,
                rgb, alpha);
    }

    /** Sets render state, submits the buffer, restores render state. */
    public void draw(boolean seeThroughWalls, float lineWidth) {
        BuiltBuffer built = buffer.endNullable(); // null when nothing was added; also releases the buffer
        if (built == null) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (seeThroughWalls) {
            RenderSystem.disableDepthTest();
        }

        BufferRenderer.drawWithGlobalProgram(built);

        // Always restore state: other renderers run after us and assume vanilla defaults.
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0F);
    }
}

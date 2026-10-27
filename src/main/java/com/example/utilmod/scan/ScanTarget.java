package com.example.utilmod.scan;

import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Describes one thing the scanner looks for.
 *
 * @param id          stable identifier (for HUD / config)
 * @param blockFilter cheap, pure predicate on a BlockState. It is ALSO run against each chunk section's
 *                    palette, so it must depend on the state only (no world access).
 * @param verifier    optional second stage with world access, run only for candidates that passed
 *                    {@code blockFilter}. Use it for multi-block "structure" checks.
 * @param rgb         outline colour as 0xRRGGBB
 */
public record ScanTarget(String id, Predicate<BlockState> blockFilter, WorldCheck verifier, int rgb) {

    @FunctionalInterface
    public interface WorldCheck {
        boolean test(ClientWorld world, BlockPos pos);
    }

    public static final WorldCheck ALWAYS = (world, pos) -> true;

    public static ScanTarget of(String id, Block block, int rgb) {
        return new ScanTarget(id, state -> state.isOf(block), ALWAYS, rgb);
    }
}

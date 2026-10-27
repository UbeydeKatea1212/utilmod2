package com.example.utilmod.scan;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Ready-made targets. Add your own here. */
public final class ScanTargets {
    private ScanTargets() {}

    public static final ScanTarget SPAWNER = ScanTarget.of("spawner", Blocks.SPAWNER, 0xFF3030);
    public static final ScanTarget TRIAL_SPAWNER = ScanTarget.of("trial_spawner", Blocks.TRIAL_SPAWNER, 0x30D0FF);

    /**
     * Example of a two-stage "structure" target: a spawner that touches a cobweb (typical of mineshaft
     * cave-spider spawners). Stage 1 is the cheap palette-friendly block check, stage 2 looks at neighbours.
     */
    public static final ScanTarget MINESHAFT_SPAWNER = new ScanTarget(
            "mineshaft_spawner",
            state -> state.isOf(Blocks.SPAWNER),
            (world, pos) -> hasNeighbor(world, pos, Blocks.COBWEB),
            0xFFAA00);

    /** What the mod scans for by default. */
    public static final List<ScanTarget> DEFAULTS = List.of(SPAWNER, TRIAL_SPAWNER);

    private static boolean hasNeighbor(ClientWorld world, BlockPos pos, Block block) {
        for (Direction dir : Direction.values()) {
            if (world.getBlockState(pos.offset(dir)).isOf(block)) {
                return true;
            }
        }
        return false;
    }
}

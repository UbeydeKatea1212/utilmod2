package com.example.utilmod.scan;

import net.minecraft.util.math.BlockPos;

/** One scan hit. {@code pos} is immutable (a real BlockPos, not a Mutable). */
public record Match(BlockPos pos, ScanTarget target) {}

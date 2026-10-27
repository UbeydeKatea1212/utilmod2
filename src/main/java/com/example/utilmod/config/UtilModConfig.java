package com.example.utilmod.config;

/**
 * Central, mutable runtime settings. Everything lives on the client thread, so plain statics are fine.
 * Swap this for a JSON-backed config (Gson + FabricLoader.getConfigDir()) once the mod grows.
 */
public final class UtilModConfig {
    private UtilModConfig() {}

    // --- scanner ---
    /** Horizontal scan radius in blocks. */
    public static int scanRadiusBlocks = 64;
    /** Max time the scanner may use per client tick (1 ms ~= 2% of a 50 ms tick). */
    public static long scanBudgetNanos = 1_000_000L;
    /** Ticks to wait after a full pass before starting the next one. */
    public static int rescanIntervalTicks = 40;
    /** Safety cap so a very common block can't flood the result list. */
    public static int maxMatchesPerChunk = 256;

    // --- ESP ---
    public static boolean espEnabled = true;
    public static boolean espSeeThrough = true;
    public static double espMaxDistance = 96.0;
    public static float espLineWidth = 2.0F;

    // --- freecam ---
    /** Blocks per tick. */
    public static double freecamSpeed = 0.6;
    public static double freecamSprintMultiplier = 3.0;
}

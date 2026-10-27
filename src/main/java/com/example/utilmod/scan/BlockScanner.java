package com.example.utilmod.scan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.example.utilmod.config.UtilModConfig;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Time-sliced block scanner.
 *
 * Why not a worker thread? ClientWorld / chunk data is mutated by the client thread (packets, light, block
 * updates) and is not thread-safe, so reading it from another thread risks torn reads and crashes. The robust
 * pattern is a cooperative scheduler on the client thread: split the work into small units (one chunk) and only
 * run as many units per tick as a fixed time budget allows.
 *
 * Three things keep each unit cheap:
 * <ol>
 *   <li>Sections that are empty are skipped.</li>
 *   <li>{@link ChunkSection#hasAny(Predicate)} runs the predicate against the section's PALETTE (a handful of
 *       distinct states) instead of 4096 blocks, so most sections are rejected in nanoseconds.</li>
 *   <li>Only sections that pass are scanned block by block, nearest chunks first.</li>
 * </ol>
 */
public final class BlockScanner {
    private final List<ScanTarget> targets;
    private final Predicate<BlockState> anyTarget;

    private final Map<ChunkPos, List<Match>> results = new HashMap<>();
    private final ArrayDeque<ChunkPos> queue = new ArrayDeque<>();

    private int cooldownTicks;
    private int matchCount;

    public BlockScanner(List<ScanTarget> targets) {
        this.targets = List.copyOf(targets);
        Predicate<BlockState> combined = state -> false;
        for (ScanTarget target : this.targets) {
            combined = combined.or(target.blockFilter());
        }
        this.anyTarget = combined;
    }

    /** Call once per client tick. {@code center} is the point the radius is measured from. */
    public void tick(MinecraftClient client, Vec3d center) {
        ClientWorld world = client.world;
        if (world == null || center == null) {
            reset();
            return;
        }

        if (queue.isEmpty()) {
            if (cooldownTicks > 0) {
                cooldownTicks--;
                return;
            }
            beginPass(center);
        }

        long deadline = System.nanoTime() + UtilModConfig.scanBudgetNanos;
        do { // always make progress: at least one chunk per tick
            ChunkPos next = queue.poll();
            if (next == null) {
                break;
            }
            scanChunk(world, next);
        } while (System.nanoTime() < deadline);

        recount();
        if (queue.isEmpty()) {
            cooldownTicks = UtilModConfig.rescanIntervalTicks;
        }
    }

    public void reset() {
        results.clear();
        queue.clear();
        matchCount = 0;
        cooldownTicks = 0;
    }

    public void forEachMatch(Consumer<Match> consumer) {
        for (List<Match> list : results.values()) {
            for (int i = 0; i < list.size(); i++) {
                consumer.accept(list.get(i));
            }
        }
    }

    public int matchCount() {
        return matchCount;
    }

    public int pendingChunks() {
        return queue.size();
    }

    // ------------------------------------------------------------------------------------------------

    private void beginPass(Vec3d center) {
        int radius = UtilModConfig.scanRadiusBlocks;
        int chunkRadius = (radius >> 4) + 1;
        int centerChunkX = MathHelper.floor(center.x) >> 4;
        int centerChunkZ = MathHelper.floor(center.z) >> 4;
        double maxDist = radius + 12.0; // 12 ~= half a chunk diagonal
        double maxDistSq = maxDist * maxDist;

        List<ChunkPos> order = new ArrayList<>();
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkPos pos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);
                if (distanceSq(pos, center) <= maxDistSq) {
                    order.add(pos);
                }
            }
        }
        order.sort(Comparator.comparingDouble(pos -> distanceSq(pos, center))); // nearest first

        // Forget chunks that fell out of range so the result map can't grow forever.
        Set<ChunkPos> inRange = new HashSet<>(order);
        results.keySet().removeIf(pos -> !inRange.contains(pos));

        queue.clear();
        queue.addAll(order);
    }

    private static double distanceSq(ChunkPos pos, Vec3d center) {
        double dx = pos.getCenterX() - center.x;
        double dz = pos.getCenterZ() - center.z;
        return dx * dx + dz * dz;
    }

    private void scanChunk(ClientWorld world, ChunkPos chunkPos) {
        WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z);
        if (chunk == null) {
            results.remove(chunkPos); // not loaded (anymore)
            return;
        }

        List<Match> found = null;
        int cap = UtilModConfig.maxMatchesPerChunk;
        ChunkSection[] sections = chunk.getSectionArray();

        sectionLoop:
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section.isEmpty() || !section.hasAny(anyTarget)) {
                continue; // fast reject via palette
            }

            int baseX = chunkPos.getStartX();
            int baseZ = chunkPos.getStartZ();
            int baseY = ChunkSectionPos.getBlockCoord(chunk.getBottomSectionCoord() + i);

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!anyTarget.test(state)) {
                            continue;
                        }
                        for (ScanTarget target : targets) {
                            if (!target.blockFilter().test(state)) {
                                continue;
                            }
                            BlockPos pos = new BlockPos(baseX + x, baseY + y, baseZ + z);
                            if (target.verifier().test(world, pos)) {
                                if (found == null) {
                                    found = new ArrayList<>();
                                }
                                found.add(new Match(pos, target));
                                if (found.size() >= cap) {
                                    break sectionLoop;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (found == null) {
            results.remove(chunkPos);
        } else {
            results.put(chunkPos, found);
        }
    }

    private void recount() {
        int total = 0;
        for (List<Match> list : results.values()) {
            total += list.size();
        }
        matchCount = total;
    }
}

package com.optimalaz.client.optimization;

import net.minecraft.client.MinecraftClient;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class OptimizationManager {

    private int tickCounter = 0;
    private static final int GC_INTERVAL_TICKS = 6000;
    private static final long RAM_THRESHOLD_PERCENT = 85;

    public void applyStartupOptimizations() {
        System.setProperty("java.awt.headless", "true");

        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory();
        long targetDirect = Math.min(maxMem / 4, 512L * 1024 * 1024);
        System.setProperty("io.netty.maxDirectMemory", String.valueOf(targetDirect));

        System.setProperty("fml.readTimeout", "90");
        System.setProperty("fml.loginTimeout", "90");

        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                String.valueOf(Math.max(2, Runtime.getRuntime().availableProcessors() - 1)));

        scheduleGcIfNeeded(true);
    }

    public void tick(MinecraftClient client) {
        tickCounter++;

        if (tickCounter % GC_INTERVAL_TICKS == 0) {
            scheduleGcIfNeeded(false);
        }

        if (tickCounter % 200 == 0 && client.world != null) {
            applyRuntimeOptimizations(client);
        }
    }

    private void applyRuntimeOptimizations(MinecraftClient client) {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memBean.getHeapMemoryUsage();

        long usedPercent = (heapUsage.getUsed() * 100) / Math.max(heapUsage.getMax(), 1);

        if (usedPercent > RAM_THRESHOLD_PERCENT) {
            System.gc();
        }
    }

    private void scheduleGcIfNeeded(boolean force) {
        if (force) {
            System.gc();
            return;
        }
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        if (used * 100 / max > RAM_THRESHOLD_PERCENT) {
            System.gc();
        }
    }

    public static long getUsedRamMb() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    public static long getMaxRamMb() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }
}

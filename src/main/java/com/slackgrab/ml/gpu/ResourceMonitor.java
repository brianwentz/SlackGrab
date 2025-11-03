package com.slackgrab.ml.gpu;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slackgrab.core.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * Monitor CPU, GPU, and memory resource usage
 *
 * Provides resource monitoring to ensure ML operations stay within
 * configured limits and don't impact system performance.
 *
 * Limits:
 * - GPU: 80% max RAM usage
 * - CPU: 5% when GPU active, 20% when CPU-only
 * - Memory: 4GB total
 */
@Singleton
public class ResourceMonitor {
    private static final Logger logger = LoggerFactory.getLogger(ResourceMonitor.class);

    private static final double MAX_CPU_USAGE_WITH_GPU = 0.05; // 5%
    private static final double MAX_CPU_USAGE_WITHOUT_GPU = 0.20; // 20%
    private static final long MAX_MEMORY_MB = 4096; // 4GB

    private final GpuAccelerator gpuAccelerator;
    private final ErrorHandler errorHandler;

    private final OperatingSystemMXBean osBean;

    @Inject
    public ResourceMonitor(GpuAccelerator gpuAccelerator, ErrorHandler errorHandler) {
        this.gpuAccelerator = gpuAccelerator;
        this.errorHandler = errorHandler;
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
    }

    /**
     * Get current resource usage
     *
     * @return Resource usage snapshot
     */
    public ResourceUsage getCurrentUsage() {
        try {
            // CPU usage
            double cpuUsage = getCpuUsage();

            // Memory usage
            GpuAccelerator.MemoryUsage memoryUsage = gpuAccelerator.getMemoryUsage();

            // GPU status
            GpuAccelerator.GpuStatus gpuStatus = gpuAccelerator.getStatus();

            return new ResourceUsage(
                cpuUsage,
                memoryUsage.getUsedMemoryMB(),
                gpuStatus.isGpuActive(),
                memoryUsage.getUsagePercentage()
            );

        } catch (Exception e) {
            errorHandler.handleError("Failed to get resource usage", e);
            return ResourceUsage.unknown();
        }
    }

    /**
     * Get CPU usage percentage
     *
     * @return CPU usage (0.0-1.0)
     */
    private double getCpuUsage() {
        try {
            double systemLoad = osBean.getSystemLoadAverage();
            int availableProcessors = osBean.getAvailableProcessors();

            if (systemLoad >= 0) {
                return Math.min(1.0, systemLoad / availableProcessors);
            }

            // Fallback: use process CPU (if available via JMX)
            return 0.0;

        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Check if resources are within limits
     *
     * @return true if within limits
     */
    public boolean isWithinLimits() {
        ResourceUsage usage = getCurrentUsage();

        // Check memory limit
        if (usage.memoryUsageMB() > MAX_MEMORY_MB) {
            logger.warn("Memory usage exceeds limit: {} MB > {} MB",
                usage.memoryUsageMB(), MAX_MEMORY_MB);
            return false;
        }

        // Check CPU limit
        double maxCpu = usage.gpuActive()
            ? MAX_CPU_USAGE_WITH_GPU
            : MAX_CPU_USAGE_WITHOUT_GPU;

        if (usage.cpuUsage() > maxCpu) {
            logger.warn("CPU usage exceeds limit: {:.1f}% > {:.1f}%",
                usage.cpuUsage() * 100, maxCpu * 100);
            return false;
        }

        // Check GPU memory limit
        if (usage.gpuActive() && !gpuAccelerator.isMemoryWithinLimits()) {
            logger.warn("GPU memory usage exceeds limit");
            return false;
        }

        return true;
    }

    /**
     * Check if training should be paused due to resource constraints
     *
     * @return true if should pause
     */
    public boolean shouldPauseTraining() {
        ResourceUsage usage = getCurrentUsage();

        // Pause if CPU usage too high
        double maxCpu = usage.gpuActive()
            ? MAX_CPU_USAGE_WITH_GPU * 1.5 // Allow 1.5x for training
            : MAX_CPU_USAGE_WITHOUT_GPU * 1.5;

        if (usage.cpuUsage() > maxCpu) {
            logger.debug("Training paused due to high CPU usage: {:.1f}%",
                usage.cpuUsage() * 100);
            return true;
        }

        // Pause if memory usage too high
        if (usage.memoryUsageMB() > MAX_MEMORY_MB * 0.9) {
            logger.debug("Training paused due to high memory usage: {} MB",
                usage.memoryUsageMB());
            return true;
        }

        return false;
    }

    /**
     * Log current resource usage
     */
    public void logResourceUsage() {
        ResourceUsage usage = getCurrentUsage();
        logger.info("Resource usage - CPU: {:.1f}%, Memory: {} MB, GPU: {}",
            usage.cpuUsage() * 100,
            usage.memoryUsageMB(),
            usage.gpuActive() ? "Active" : "Inactive");
    }

    /**
     * Resource usage snapshot
     */
    public record ResourceUsage(
        double cpuUsage,        // 0.0-1.0
        long memoryUsageMB,     // MB
        boolean gpuActive,      // true if GPU in use
        double memoryPercent    // Memory usage percentage
    ) {
        public static ResourceUsage unknown() {
            return new ResourceUsage(0.0, 0, false, 0.0);
        }

        public boolean isHealthy() {
            return cpuUsage < 0.5 && memoryUsageMB < MAX_MEMORY_MB;
        }
    }
}

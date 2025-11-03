package com.slackgrab.ml.gpu;

import com.google.inject.Singleton;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GPU acceleration manager for neural network operations
 *
 * Currently provides CPU-based implementation with monitoring.
 * Future enhancement: Intel oneAPI or CUDA backends.
 *
 * Features:
 * - Automatic CPU/GPU detection
 * - Memory usage monitoring
 * - Resource limit enforcement
 * - Graceful fallback to CPU
 */
@Singleton
public class GpuAccelerator {
    private static final Logger logger = LoggerFactory.getLogger(GpuAccelerator.class);

    private ExecutionMode currentMode;
    private double maxMemoryUsage = 0.8; // 80% max
    private boolean initialized;

    public GpuAccelerator() {
        this.initialized = false;
    }

    /**
     * Initialize GPU acceleration
     *
     * Detects available hardware and configures backend.
     *
     * @return true if initialization successful
     */
    public boolean initialize() {
        logger.info("Initializing GPU accelerator...");

        try {
            // Detect backend
            String backend = Nd4j.getBackend().getClass().getSimpleName();
            logger.info("ND4J backend: {}", backend);

            // Check for GPU availability
            if (isGpuAvailable()) {
                currentMode = ExecutionMode.GPU;
                logger.info("GPU acceleration enabled");
            } else {
                currentMode = ExecutionMode.CPU;
                logger.info("GPU not available, using CPU");
            }

            initialized = true;
            return true;

        } catch (Exception e) {
            logger.error("Failed to initialize GPU accelerator", e);
            currentMode = ExecutionMode.CPU;
            initialized = false;
            return false;
        }
    }

    /**
     * Check if GPU is available
     *
     * @return true if GPU detected and usable
     */
    private boolean isGpuAvailable() {
        try {
            // Check ND4J backend
            String backend = Nd4j.getBackend().getClass().getSimpleName();

            // CUDA backend indicates GPU
            if (backend.contains("Cuda") || backend.contains("cuda")) {
                logger.info("CUDA GPU detected");
                return true;
            }

            // Check for other GPU backends
            // In production, would check Intel oneAPI, OpenCL, etc.

            return false;

        } catch (Exception e) {
            logger.warn("Error checking GPU availability", e);
            return false;
        }
    }

    /**
     * Get current execution mode
     *
     * @return Current mode (GPU or CPU)
     */
    public ExecutionMode getExecutionMode() {
        return currentMode;
    }

    /**
     * Get GPU status information
     *
     * @return GPU status
     */
    public GpuStatus getStatus() {
        return new GpuStatus(
            currentMode,
            isGpuAvailable(),
            getMemoryUsage(),
            maxMemoryUsage
        );
    }

    /**
     * Get current memory usage
     *
     * @return Memory usage information
     */
    public MemoryUsage getMemoryUsage() {
        try {
            // Get Java heap memory
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            // ND4J workspace memory (if using workspaces)
            // Note: Method not available in all ND4J versions
            long nd4jMemory = 0;

            return new MemoryUsage(
                usedMemory,
                totalMemory,
                maxMemory,
                nd4jMemory
            );

        } catch (Exception e) {
            logger.warn("Error getting memory usage", e);
            return MemoryUsage.unknown();
        }
    }

    /**
     * Set maximum memory usage percentage
     *
     * @param percentage Maximum usage (0.0 to 1.0)
     */
    public void setMaxMemoryUsage(double percentage) {
        if (percentage > 0.0 && percentage <= 1.0) {
            this.maxMemoryUsage = percentage;
            logger.info("Max memory usage set to: {}%", percentage * 100);
        }
    }

    /**
     * Check if memory usage is within limits
     *
     * @return true if within limits
     */
    public boolean isMemoryWithinLimits() {
        MemoryUsage usage = getMemoryUsage();
        double usageRatio = (double) usage.usedMemory() / usage.maxMemory();
        return usageRatio < maxMemoryUsage;
    }

    /**
     * Force fallback to CPU execution
     */
    public void fallbackToCPU() {
        if (currentMode == ExecutionMode.GPU) {
            logger.warn("Falling back to CPU execution");
            currentMode = ExecutionMode.CPU;
        }
    }

    /**
     * Attempt to switch back to GPU
     *
     * @return true if switched successfully
     */
    public boolean tryEnableGPU() {
        if (isGpuAvailable() && isMemoryWithinLimits()) {
            currentMode = ExecutionMode.GPU;
            logger.info("Switched to GPU execution");
            return true;
        }
        return false;
    }

    /**
     * Check if initialized
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Shutdown and release resources
     */
    public void shutdown() {
        logger.info("Shutting down GPU accelerator");
        // Cleanup ND4J workspaces
        try {
            Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
        } catch (Exception e) {
            logger.warn("Error cleaning up workspaces", e);
        }
        initialized = false;
    }

    /**
     * Execution mode enum
     */
    public enum ExecutionMode {
        GPU,
        CPU
    }

    /**
     * GPU status record
     */
    public record GpuStatus(
        ExecutionMode mode,
        boolean gpuAvailable,
        MemoryUsage memoryUsage,
        double maxMemoryUsage
    ) {
        public boolean isGpuActive() {
            return mode == ExecutionMode.GPU && gpuAvailable;
        }
    }

    /**
     * Memory usage record
     */
    public record MemoryUsage(
        long usedMemory,
        long totalMemory,
        long maxMemory,
        long nd4jMemory
    ) {
        public static MemoryUsage unknown() {
            return new MemoryUsage(0, 0, 0, 0);
        }

        public double getUsagePercentage() {
            if (maxMemory == 0) return 0.0;
            return (double) usedMemory / maxMemory * 100.0;
        }

        public long getUsedMemoryMB() {
            return usedMemory / (1024 * 1024);
        }

        public long getTotalMemoryMB() {
            return totalMemory / (1024 * 1024);
        }

        public long getMaxMemoryMB() {
            return maxMemory / (1024 * 1024);
        }
    }
}

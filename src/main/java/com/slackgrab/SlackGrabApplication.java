package com.slackgrab;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.slackgrab.core.ApplicationModule;
import com.slackgrab.core.ServiceCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SlackGrab Application Entry Point
 *
 * Windows 11+ desktop application that uses neural network technology to intelligently
 * prioritize Slack messages through continuous local learning.
 *
 * Core principles:
 * - Local-first processing: All computation happens on user's machine
 * - Silent resilience: Errors never interrupt user experience
 * - Zero configuration: System self-tunes through behavioral observation
 * - Continuous learning: Neural network adapts incrementally
 */
public class SlackGrabApplication {
    private static final Logger logger = LoggerFactory.getLogger(SlackGrabApplication.class);

    private final Injector injector;
    private final ServiceCoordinator serviceCoordinator;

    public SlackGrabApplication() {
        logger.info("Initializing SlackGrab application...");

        // Initialize dependency injection
        this.injector = Guice.createInjector(new ApplicationModule());
        this.serviceCoordinator = injector.getInstance(ServiceCoordinator.class);

        logger.info("Dependency injection configured");
    }

    /**
     * Start the application and all its services
     */
    public void start() {
        try {
            logger.info("Starting SlackGrab services...");

            // Start all core services
            serviceCoordinator.start();

            logger.info("SlackGrab started successfully");
        } catch (Exception e) {
            logger.error("Failed to start SlackGrab services", e);
            throw new RuntimeException("Application startup failed", e);
        }
    }

    /**
     * Shutdown the application gracefully
     */
    public void shutdown() {
        try {
            logger.info("Shutting down SlackGrab...");

            serviceCoordinator.shutdown();

            logger.info("SlackGrab shutdown complete");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    public static void main(String[] args) {
        logger.info("SlackGrab starting...");

        try {
            SlackGrabApplication app = new SlackGrabApplication();

            // Add shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));

            // Start the application
            app.start();

            // Keep application running
            Thread.currentThread().join();

        } catch (Exception e) {
            logger.error("Fatal error in SlackGrab", e);
            System.exit(1);
        }
    }
}

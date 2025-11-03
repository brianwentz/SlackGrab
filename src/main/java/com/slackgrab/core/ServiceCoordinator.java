package com.slackgrab.core;

import com.google.inject.Inject;
import com.slackgrab.data.DatabaseManager;
import com.slackgrab.webhook.WebhookServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates the lifecycle of all application services
 *
 * Manages startup and shutdown order to ensure proper dependency initialization
 */
public class ServiceCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(ServiceCoordinator.class);

    private final ConfigurationManager configurationManager;
    private final ErrorHandler errorHandler;
    private final DatabaseManager databaseManager;
    private final WebhookServer webhookServer;

    private final List<ManagedService> services;
    private boolean started = false;

    @Inject
    public ServiceCoordinator(
            ConfigurationManager configurationManager,
            ErrorHandler errorHandler,
            DatabaseManager databaseManager,
            WebhookServer webhookServer) {
        this.configurationManager = configurationManager;
        this.errorHandler = errorHandler;
        this.databaseManager = databaseManager;
        this.webhookServer = webhookServer;

        // Initialize service list in startup order
        this.services = new ArrayList<>();
        services.add(databaseManager);
        services.add(webhookServer);
    }

    /**
     * Start all services in order
     */
    public synchronized void start() throws Exception {
        if (started) {
            logger.warn("Services already started");
            return;
        }

        logger.info("Starting {} services...", services.size());

        for (ManagedService service : services) {
            try {
                String serviceName = service.getClass().getSimpleName();
                logger.info("Starting service: {}", serviceName);

                service.start();

                logger.info("Service started successfully: {}", serviceName);
            } catch (Exception e) {
                errorHandler.handleError("Failed to start service: " + service.getClass().getSimpleName(), e);
                throw e;
            }
        }

        started = true;
        logger.info("All services started successfully");
    }

    /**
     * Shutdown all services in reverse order
     */
    public synchronized void shutdown() {
        if (!started) {
            logger.warn("Services not started, nothing to shutdown");
            return;
        }

        logger.info("Shutting down {} services...", services.size());

        // Shutdown in reverse order
        for (int i = services.size() - 1; i >= 0; i--) {
            ManagedService service = services.get(i);
            try {
                String serviceName = service.getClass().getSimpleName();
                logger.info("Stopping service: {}", serviceName);

                service.stop();

                logger.info("Service stopped successfully: {}", serviceName);
            } catch (Exception e) {
                errorHandler.handleError("Error stopping service: " + service.getClass().getSimpleName(), e);
                // Continue shutting down other services
            }
        }

        started = false;
        logger.info("All services shut down");
    }

    public boolean isStarted() {
        return started;
    }
}

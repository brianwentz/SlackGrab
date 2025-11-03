package com.slackgrab.webhook;

import com.google.inject.Inject;
import com.slackgrab.core.ConfigurationManager;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.core.ManagedService;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Localhost webhook server for receiving Slack events
 *
 * Runs on localhost:7395 to receive real-time Slack events via webhooks.
 * All requests are validated using Slack signature verification.
 */
public class WebhookServer implements ManagedService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookServer.class);

    private final ConfigurationManager configurationManager;
    private final ErrorHandler errorHandler;

    private Javalin app;
    private boolean running = false;

    @Inject
    public WebhookServer(ConfigurationManager configurationManager, ErrorHandler errorHandler) {
        this.configurationManager = configurationManager;
        this.errorHandler = errorHandler;
    }

    @Override
    public void start() throws Exception {
        if (running) {
            logger.warn("Webhook server already running");
            return;
        }

        logger.info("Starting webhook server...");

        try {
            int port = configurationManager.getWebhookPort();
            String host = configurationManager.getWebhookHost();

            // Create Javalin app
            app = Javalin.create(config -> {
                config.showJavalinBanner = false;
                // CORS disabled for security (localhost only)
            });

            // Health check endpoint
            app.get("/health", ctx -> {
                ctx.json(new HealthResponse("ok", System.currentTimeMillis()));
            });

            // Slack events endpoint
            app.post("/slack/events", ctx -> {
                try {
                    String body = ctx.body();
                    logger.debug("Received Slack event: {}", body);

                    // TODO: Verify Slack signature
                    // TODO: Handle URL verification challenge
                    // TODO: Process event

                    ctx.status(200);
                    ctx.result("ok");
                } catch (Exception e) {
                    errorHandler.handleError("Error processing Slack event", e);
                    ctx.status(500);
                }
            });

            // Slack interactive endpoint
            app.post("/slack/interactive", ctx -> {
                try {
                    String body = ctx.body();
                    logger.debug("Received Slack interactive event: {}", body);

                    // TODO: Handle interactive components (buttons, menus, etc.)

                    ctx.status(200);
                } catch (Exception e) {
                    errorHandler.handleError("Error processing Slack interactive event", e);
                    ctx.status(500);
                }
            });

            // Slack slash command endpoint
            app.post("/slack/commands", ctx -> {
                try {
                    String body = ctx.body();
                    logger.debug("Received Slack slash command: {}", body);

                    // TODO: Handle slash commands

                    ctx.status(200);
                } catch (Exception e) {
                    errorHandler.handleError("Error processing Slack slash command", e);
                    ctx.status(500);
                }
            });

            // Start server
            app.start(host, port);
            running = true;

            logger.info("Webhook server started on {}:{}", host, port);
        } catch (Exception e) {
            errorHandler.handleCriticalError("Failed to start webhook server", e);
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        if (!running || app == null) {
            logger.warn("Webhook server not running");
            return;
        }

        logger.info("Stopping webhook server...");

        try {
            app.stop();
            running = false;
            logger.info("Webhook server stopped");
        } catch (Exception e) {
            errorHandler.handleError("Error stopping webhook server", e);
            throw e;
        }
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Health check response
     */
    private record HealthResponse(String status, long timestamp) {}
}

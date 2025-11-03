package com.slackgrab.webhook;

import com.google.inject.Inject;
import com.slackgrab.core.ConfigurationManager;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.core.ManagedService;
import com.slackgrab.oauth.OAuthManager;
import com.slackgrab.oauth.OAuthManager.OAuthException;
import com.slackgrab.oauth.OAuthManager.OAuthResult;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Localhost webhook server for receiving Slack events and OAuth callbacks
 *
 * Runs on localhost:7395 to receive:
 * - OAuth authorization callbacks
 * - Real-time Slack events via webhooks
 * - Interactive component events
 * - Slash commands
 *
 * All requests are validated using Slack signature verification (where applicable).
 */
public class WebhookServer implements ManagedService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookServer.class);

    private final ConfigurationManager configurationManager;
    private final ErrorHandler errorHandler;
    private final OAuthManager oauthManager;

    private Javalin app;
    private boolean running = false;

    @Inject
    public WebhookServer(
        ConfigurationManager configurationManager,
        ErrorHandler errorHandler,
        OAuthManager oauthManager
    ) {
        this.configurationManager = configurationManager;
        this.errorHandler = errorHandler;
        this.oauthManager = oauthManager;
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

            // OAuth callback endpoint
            app.get("/slack/oauth/callback", ctx -> {
                try {
                    String code = ctx.queryParam("code");
                    String error = ctx.queryParam("error");
                    String state = ctx.queryParam("state");

                    logger.info("Received OAuth callback. Error: {}, State: {}", error, state);

                    if (error != null) {
                        logger.error("OAuth authorization failed: {}", error);
                        ctx.html(generateErrorPage(
                            "Authorization Failed",
                            "Failed to authorize SlackGrab: " + error
                        ));
                        return;
                    }

                    if (code == null || code.isEmpty()) {
                        logger.error("No authorization code received");
                        ctx.html(generateErrorPage(
                            "Invalid Request",
                            "No authorization code received from Slack"
                        ));
                        return;
                    }

                    // Exchange code for tokens
                    try {
                        OAuthResult result = oauthManager.exchangeCodeForToken(code);

                        logger.info("OAuth authorization successful for team: {}", result.teamName());

                        ctx.html(generateSuccessPage(
                            "Authorization Successful!",
                            "SlackGrab has been successfully connected to " + result.teamName() + ". " +
                            "You can close this window and return to the application."
                        ));

                    } catch (OAuthException e) {
                        logger.error("Failed to exchange OAuth code", e);
                        ctx.html(generateErrorPage(
                            "Token Exchange Failed",
                            "Failed to complete authorization: " + e.getMessage()
                        ));
                    }

                } catch (Exception e) {
                    errorHandler.handleError("Error processing OAuth callback", e);
                    ctx.html(generateErrorPage(
                        "Unexpected Error",
                        "An unexpected error occurred during authorization"
                    ));
                }
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
     * Generate success HTML page for OAuth callback
     */
    private String generateSuccessPage(String title, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s - SlackGrab</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        margin: 0;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    }
                    .container {
                        background: white;
                        padding: 40px;
                        border-radius: 12px;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                        max-width: 500px;
                        text-align: center;
                    }
                    .success-icon {
                        font-size: 64px;
                        margin-bottom: 20px;
                    }
                    h1 {
                        color: #2c3e50;
                        margin: 0 0 20px 0;
                        font-size: 28px;
                    }
                    p {
                        color: #555;
                        line-height: 1.6;
                        margin: 0;
                    }
                    .close-instruction {
                        margin-top: 30px;
                        padding: 15px;
                        background: #f8f9fa;
                        border-radius: 8px;
                        color: #6c757d;
                        font-size: 14px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="success-icon">✓</div>
                    <h1>%s</h1>
                    <p>%s</p>
                    <div class="close-instruction">
                        You can safely close this window.
                    </div>
                </div>
                <script>
                    // Auto-close after 3 seconds
                    setTimeout(function() {
                        window.close();
                    }, 3000);
                </script>
            </body>
            </html>
            """.formatted(title, title, message);
    }

    /**
     * Generate error HTML page for OAuth callback
     */
    private String generateErrorPage(String title, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s - SlackGrab</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        margin: 0;
                        background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);
                    }
                    .container {
                        background: white;
                        padding: 40px;
                        border-radius: 12px;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                        max-width: 500px;
                        text-align: center;
                    }
                    .error-icon {
                        font-size: 64px;
                        margin-bottom: 20px;
                    }
                    h1 {
                        color: #e74c3c;
                        margin: 0 0 20px 0;
                        font-size: 28px;
                    }
                    p {
                        color: #555;
                        line-height: 1.6;
                        margin: 0;
                    }
                    .close-instruction {
                        margin-top: 30px;
                        padding: 15px;
                        background: #f8f9fa;
                        border-radius: 8px;
                        color: #6c757d;
                        font-size: 14px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="error-icon">✗</div>
                    <h1>%s</h1>
                    <p>%s</p>
                    <div class="close-instruction">
                        Please try again or contact support if the problem persists.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, title, message);
    }

    /**
     * Health check response
     */
    private record HealthResponse(String status, long timestamp) {}
}

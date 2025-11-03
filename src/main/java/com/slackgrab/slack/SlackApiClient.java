package com.slackgrab.slack;

import com.google.inject.Inject;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.auth.AuthTestResponse;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.security.CredentialManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Slack API client wrapper
 *
 * Provides high-level interface to Slack API with automatic token management,
 * rate limiting, and error handling.
 */
public class SlackApiClient {
    private static final Logger logger = LoggerFactory.getLogger(SlackApiClient.class);

    private final CredentialManager credentialManager;
    private final ErrorHandler errorHandler;

    private final Slack slack;
    private String accessToken;

    @Inject
    public SlackApiClient(CredentialManager credentialManager, ErrorHandler errorHandler) {
        this.credentialManager = credentialManager;
        this.errorHandler = errorHandler;
        this.slack = Slack.getInstance();

        // Load token if available
        loadAccessToken();
    }

    /**
     * Load access token from credential manager
     */
    private void loadAccessToken() {
        Optional<String> token = credentialManager.getAccessToken();
        if (token.isPresent()) {
            this.accessToken = token.get();
            logger.info("Loaded access token from credential manager");
        } else {
            logger.info("No access token found in credential manager");
        }
    }

    /**
     * Set access token and store in credential manager
     *
     * @param token Access token to set
     */
    public void setAccessToken(String token) {
        this.accessToken = token;
        credentialManager.storeAccessToken(token);
        logger.info("Access token updated and stored");
    }

    /**
     * Check if client has valid access token
     *
     * @return true if access token is available
     */
    public boolean hasAccessToken() {
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Test the current connection to Slack
     *
     * @return true if connection is valid
     */
    public boolean testConnection() {
        if (!hasAccessToken()) {
            logger.warn("Cannot test connection: No access token available");
            return false;
        }

        try {
            AuthTestResponse response = slack.methods(accessToken).authTest(req -> req);

            if (response.isOk()) {
                logger.info("Slack connection test successful. Team: {}, User: {}",
                    response.getTeam(), response.getUser());
                return true;
            } else {
                logger.error("Slack connection test failed: {}", response.getError());
                return false;
            }
        } catch (IOException | SlackApiException e) {
            errorHandler.handleError("Failed to test Slack connection", e);
            return false;
        }
    }

    /**
     * Get the Slack client instance
     *
     * @return Slack client
     */
    public Slack getSlack() {
        return slack;
    }

    /**
     * Get the current access token
     *
     * @return Access token if available
     */
    public Optional<String> getAccessToken() {
        return Optional.ofNullable(accessToken);
    }

    /**
     * Clear access token
     */
    public void clearAccessToken() {
        this.accessToken = null;
        credentialManager.deleteAllCredentials();
        logger.info("Access token cleared");
    }
}

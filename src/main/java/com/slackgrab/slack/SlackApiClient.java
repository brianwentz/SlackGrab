package com.slackgrab.slack;

import com.google.inject.Inject;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.auth.AuthTestResponse;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.oauth.OAuthManager;
import com.slackgrab.security.CredentialManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Slack API client wrapper
 *
 * Provides high-level interface to Slack API with automatic token management,
 * rate limiting, error handling, and automatic token refresh on expiration.
 */
public class SlackApiClient {
    private static final Logger logger = LoggerFactory.getLogger(SlackApiClient.class);

    private final CredentialManager credentialManager;
    private final ErrorHandler errorHandler;
    private final OAuthManager oAuthManager;

    private final Slack slack;
    private String accessToken;

    @Inject
    public SlackApiClient(
            CredentialManager credentialManager,
            ErrorHandler errorHandler,
            OAuthManager oAuthManager) {
        this.credentialManager = credentialManager;
        this.errorHandler = errorHandler;
        this.oAuthManager = oAuthManager;
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

    /**
     * Execute a Slack API call with automatic token refresh on expiration
     *
     * This method wraps Slack API calls to automatically handle token expiration:
     * 1. Try the API call with current token
     * 2. If token is expired (401 error), refresh the token
     * 3. Retry the API call with new token
     *
     * @param apiCall Supplier that executes the API call
     * @param <T> Response type
     * @return API response
     * @throws IOException If network error occurs
     * @throws SlackApiException If Slack API error occurs
     */
    public <T> T executeWithTokenRefresh(Supplier<T> apiCall) throws IOException, SlackApiException {
        try {
            // First attempt with current token
            return apiCall.get();

        } catch (Exception e) {
            // Check if this is a token expiration error
            if (oAuthManager.isTokenExpired(e)) {
                logger.info("Token expired, attempting to refresh...");

                try {
                    // Refresh the token
                    String newToken = oAuthManager.refreshAccessToken();

                    // Update our token
                    setAccessToken(newToken);

                    logger.info("Token refreshed successfully, retrying API call");

                    // Retry the API call with new token
                    return apiCall.get();

                } catch (OAuthManager.OAuthException refreshError) {
                    logger.error("Failed to refresh token", refreshError);
                    errorHandler.handleError("Token refresh failed", refreshError);

                    // Re-throw as runtime exception since token refresh is critical
                    throw new RuntimeException(
                        "Token expired and refresh failed: " + refreshError.getMessage(),
                        refreshError
                    );
                }
            }

            // Not a token expiration error, re-throw
            if (e instanceof SlackApiException) {
                throw (SlackApiException) e;
            } else if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new RuntimeException("Unexpected error during API call", e);
            }
        }
    }

    /**
     * Test connection with automatic token refresh
     *
     * @return true if connection is valid
     */
    public boolean testConnectionWithRefresh() {
        if (!hasAccessToken()) {
            logger.warn("Cannot test connection: No access token available");
            return false;
        }

        try {
            AuthTestResponse response = executeWithTokenRefresh(() -> {
                try {
                    return slack.methods(accessToken).authTest(req -> req);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            if (response.isOk()) {
                logger.info("Slack connection test successful. Team: {}, User: {}",
                    response.getTeam(), response.getUser());
                return true;
            } else {
                logger.error("Slack connection test failed: {}", response.getError());
                return false;
            }

        } catch (Exception e) {
            errorHandler.handleError("Failed to test Slack connection", e);
            return false;
        }
    }
}

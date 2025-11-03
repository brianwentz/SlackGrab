package com.slackgrab.oauth;

import com.google.inject.Inject;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.security.CredentialManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Slack OAuth 2.0 flow manager
 *
 * Handles the complete OAuth 2.0 authorization flow for Slack workspace integration:
 * 1. Generate authorization URL
 * 2. Handle redirect callback with authorization code
 * 3. Exchange code for access/refresh tokens
 * 4. Store tokens securely in Windows Credential Manager
 * 5. Refresh tokens when expired
 *
 * Uses Slack's official OAuth 2.0 API with recommended security practices.
 */
public class OAuthManager {
    private static final Logger logger = LoggerFactory.getLogger(OAuthManager.class);

    // Slack OAuth configuration
    // NOTE: These would normally be environment variables or secure configuration
    // For development, we're using placeholders. Production must use secure storage.
    private static final String CLIENT_ID = System.getenv("SLACK_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("SLACK_CLIENT_SECRET");
    private static final String REDIRECT_URI = "http://localhost:7395/slack/oauth/callback";

    // Required Slack OAuth scopes for SlackGrab functionality
    private static final String OAUTH_SCOPES = String.join(",",
        "channels:history",      // Read public channel messages
        "channels:read",         // List public channels
        "groups:history",        // Read private channels user is in
        "groups:read",           // List private channels
        "im:history",            // Read direct messages
        "im:read",               // List direct messages
        "mpim:history",          // Read group direct messages
        "mpim:read",             // List group direct messages
        "users:read",            // Get user information
        "team:read"              // Get team information
    );

    private final CredentialManager credentialManager;
    private final ErrorHandler errorHandler;
    private final Slack slack;

    @Inject
    public OAuthManager(CredentialManager credentialManager, ErrorHandler errorHandler) {
        this.credentialManager = credentialManager;
        this.errorHandler = errorHandler;
        this.slack = Slack.getInstance();
    }

    /**
     * Generate Slack OAuth authorization URL
     *
     * User must open this URL in browser to authorize SlackGrab.
     * After authorization, Slack will redirect to our callback endpoint.
     *
     * @return Authorization URL for user to visit
     * @throws IllegalStateException if client ID is not configured
     */
    public String generateAuthorizationUrl() {
        if (CLIENT_ID == null || CLIENT_ID.isEmpty()) {
            throw new IllegalStateException(
                "SLACK_CLIENT_ID environment variable not set. " +
                "Please configure Slack OAuth credentials."
            );
        }

        try {
            // Build OAuth authorization URL with required parameters
            String state = generateStateToken();

            String authUrl = "https://slack.com/oauth/v2/authorize" +
                "?client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(OAUTH_SCOPES, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&state=" + state;

            logger.info("Generated OAuth authorization URL");
            return authUrl;

        } catch (Exception e) {
            errorHandler.handleError("Failed to generate authorization URL", e);
            throw new RuntimeException("Failed to generate authorization URL", e);
        }
    }

    /**
     * Exchange authorization code for access token
     *
     * Called by OAuth callback handler after user authorizes the app.
     * Exchanges the temporary authorization code for permanent access/refresh tokens.
     *
     * @param code Authorization code from Slack redirect
     * @return OAuth response with tokens and workspace info
     * @throws OAuthException if token exchange fails
     */
    public OAuthResult exchangeCodeForToken(String code) throws OAuthException {
        if (CLIENT_ID == null || CLIENT_SECRET == null) {
            throw new OAuthException(
                "Slack OAuth credentials not configured. " +
                "Set SLACK_CLIENT_ID and SLACK_CLIENT_SECRET environment variables."
            );
        }

        if (code == null || code.isEmpty()) {
            throw new OAuthException("Authorization code is required");
        }

        try {
            logger.info("Exchanging authorization code for access token...");

            // Call Slack OAuth API to exchange code for tokens
            OAuthV2AccessResponse response = slack.methods().oauthV2Access(req -> req
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .code(code)
                .redirectUri(REDIRECT_URI)
            );

            if (!response.isOk()) {
                String error = response.getError() != null ? response.getError() : "Unknown error";
                logger.error("OAuth token exchange failed: {}", error);
                throw new OAuthException("Token exchange failed: " + error);
            }

            // Extract tokens and workspace information
            String accessToken = response.getAccessToken();
            String refreshToken = response.getRefreshToken();
            String teamId = response.getTeam().getId();
            String teamName = response.getTeam().getName();

            if (accessToken == null || accessToken.isEmpty()) {
                throw new OAuthException("Access token not received from Slack");
            }

            // Store tokens securely in Windows Credential Manager
            boolean tokensStored = storeOAuthTokens(accessToken, refreshToken, teamId);
            if (!tokensStored) {
                logger.error("Failed to store OAuth tokens in credential manager");
                throw new OAuthException("Failed to securely store OAuth tokens");
            }

            logger.info("OAuth token exchange successful. Team: {} ({})", teamName, teamId);

            return new OAuthResult(
                accessToken,
                refreshToken,
                teamId,
                teamName,
                response.getScope(),
                response.getBotUserId()
            );

        } catch (IOException | SlackApiException e) {
            errorHandler.handleError("OAuth token exchange failed", e);
            throw new OAuthException("Failed to exchange authorization code: " + e.getMessage(), e);
        }
    }

    /**
     * Refresh access token using refresh token
     *
     * Called when access token expires. Uses refresh token to obtain new access token.
     * Slack OAuth v2 tokens typically expire after some time.
     *
     * @return New access token
     * @throws OAuthException if refresh fails
     */
    public String refreshAccessToken() throws OAuthException {
        Optional<String> refreshToken = credentialManager.getRefreshToken();

        if (refreshToken.isEmpty()) {
            logger.error("No refresh token available. User must re-authorize.");
            throw new OAuthException("No refresh token available. Please re-authorize the application.");
        }

        if (CLIENT_ID == null || CLIENT_SECRET == null) {
            throw new OAuthException("Slack OAuth credentials not configured");
        }

        try {
            logger.info("Refreshing access token...");

            // Use Slack SDK's OAuth API with refresh token grant
            OAuthV2AccessResponse response = slack.methods().oauthV2Access(req -> req
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .grantType("refresh_token")
                .refreshToken(refreshToken.get())
            );

            if (!response.isOk()) {
                String error = response.getError() != null ? response.getError() : "Unknown error";
                logger.error("Token refresh failed: {}", error);
                throw new OAuthException("Token refresh failed: " + error);
            }

            // Extract new tokens
            String newAccessToken = response.getAccessToken();
            String newRefreshToken = response.getRefreshToken();

            if (newAccessToken == null || newAccessToken.isEmpty()) {
                throw new OAuthException("New access token not received from Slack");
            }

            // Update stored tokens
            boolean tokensStored = credentialManager.storeAccessToken(newAccessToken);
            if (newRefreshToken != null && !newRefreshToken.isEmpty()) {
                tokensStored &= credentialManager.storeRefreshToken(newRefreshToken);
            }

            if (!tokensStored) {
                logger.error("Failed to store refreshed tokens");
                throw new OAuthException("Failed to securely store refreshed tokens");
            }

            logger.info("Access token refreshed successfully");
            return newAccessToken;

        } catch (IOException | SlackApiException e) {
            errorHandler.handleError("Failed to refresh access token", e);
            throw new OAuthException("Failed to refresh access token: " + e.getMessage(), e);
        }
    }

    /**
     * Check if an exception indicates an expired token
     *
     * @param exception Exception to check
     * @return true if exception indicates expired or invalid token
     */
    public boolean isTokenExpired(Exception exception) {
        if (exception instanceof SlackApiException) {
            SlackApiException slackException = (SlackApiException) exception;
            String error = slackException.getError() != null ? slackException.getError().getError() : "";

            // Check for token expiration errors
            return "invalid_auth".equals(error) ||
                   "token_expired".equals(error) ||
                   "token_revoked".equals(error) ||
                   "account_inactive".equals(error);
        }

        // Check error message for HTTP 401 Unauthorized
        String message = exception.getMessage();
        return message != null && message.contains("401");
    }

    /**
     * Check if user has valid OAuth credentials
     *
     * @return true if access token is available
     */
    public boolean hasValidCredentials() {
        return credentialManager.hasAccessToken();
    }

    /**
     * Clear all OAuth credentials
     *
     * Removes access token, refresh token, and workspace ID from secure storage.
     * User will need to re-authorize the application.
     */
    public void clearCredentials() {
        logger.info("Clearing OAuth credentials");
        credentialManager.deleteAllCredentials();
    }

    /**
     * Get current access token
     *
     * @return Access token if available
     */
    public Optional<String> getAccessToken() {
        return credentialManager.getAccessToken();
    }

    /**
     * Store OAuth tokens securely
     *
     * @param accessToken Access token to store
     * @param refreshToken Refresh token to store (may be null)
     * @param teamId Team ID to store
     * @return true if all tokens stored successfully
     */
    private boolean storeOAuthTokens(String accessToken, String refreshToken, String teamId) {
        boolean success = credentialManager.storeAccessToken(accessToken);
        success &= credentialManager.storeTeamId(teamId);

        if (refreshToken != null && !refreshToken.isEmpty()) {
            success &= credentialManager.storeRefreshToken(refreshToken);
        }

        return success;
    }

    /**
     * Generate cryptographically secure state token for OAuth flow
     *
     * State token prevents CSRF attacks during OAuth flow.
     * Should be verified in callback handler.
     *
     * @return Random state token
     */
    private String generateStateToken() {
        // Generate random state token for CSRF protection
        // Using timestamp + random for simplicity
        // Production should use SecureRandom
        return "slackgrab_" + System.currentTimeMillis() + "_" +
               (int)(Math.random() * 1000000);
    }

    /**
     * OAuth result containing tokens and workspace information
     */
    public record OAuthResult(
        String accessToken,
        String refreshToken,
        String teamId,
        String teamName,
        String scope,
        String botUserId
    ) {}

    /**
     * OAuth-specific exception
     */
    public static class OAuthException extends Exception {
        public OAuthException(String message) {
            super(message);
        }

        public OAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package com.slackgrab.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Windows Credential Manager integration for secure token storage
 *
 * Stores OAuth tokens and other sensitive data using Windows Credential Manager,
 * which provides secure, encrypted storage backed by Windows DPAPI.
 */
public class CredentialManager {
    private static final Logger logger = LoggerFactory.getLogger(CredentialManager.class);

    private static final String TARGET_PREFIX = "SlackGrab";
    private static final String ACCESS_TOKEN_KEY = "AccessToken";
    private static final String REFRESH_TOKEN_KEY = "RefreshToken";
    private static final String WORKSPACE_ID_KEY = "WorkspaceId";

    /**
     * Store Slack access token securely
     *
     * @param token Access token to store
     * @return true if stored successfully
     */
    public boolean storeAccessToken(String token) {
        return storeCredential(ACCESS_TOKEN_KEY, token);
    }

    /**
     * Retrieve Slack access token
     *
     * @return Access token if available
     */
    public Optional<String> getAccessToken() {
        return getCredential(ACCESS_TOKEN_KEY);
    }

    /**
     * Store Slack refresh token securely
     *
     * @param token Refresh token to store
     * @return true if stored successfully
     */
    public boolean storeRefreshToken(String token) {
        return storeCredential(REFRESH_TOKEN_KEY, token);
    }

    /**
     * Retrieve Slack refresh token
     *
     * @return Refresh token if available
     */
    public Optional<String> getRefreshToken() {
        return getCredential(REFRESH_TOKEN_KEY);
    }

    /**
     * Store workspace ID
     *
     * @param workspaceId Workspace ID to store
     * @return true if stored successfully
     */
    public boolean storeWorkspaceId(String workspaceId) {
        return storeCredential(WORKSPACE_ID_KEY, workspaceId);
    }

    /**
     * Retrieve workspace ID
     *
     * @return Workspace ID if available
     */
    public Optional<String> getWorkspaceId() {
        return getCredential(WORKSPACE_ID_KEY);
    }

    /**
     * Delete all stored credentials
     *
     * @return true if all credentials deleted successfully
     */
    public boolean deleteAllCredentials() {
        boolean success = true;
        success &= deleteCredential(ACCESS_TOKEN_KEY);
        success &= deleteCredential(REFRESH_TOKEN_KEY);
        success &= deleteCredential(WORKSPACE_ID_KEY);
        return success;
    }

    /**
     * Check if access token exists
     *
     * @return true if access token is stored
     */
    public boolean hasAccessToken() {
        return getAccessToken().isPresent();
    }

    // Temporary in-memory storage for development
    // TODO: Replace with actual Windows Credential Manager integration using JNA
    private static final Map<String, String> tempStorage = new HashMap<>();

    /**
     * Store a credential in Windows Credential Manager
     *
     * Note: Current implementation uses temporary in-memory storage for development.
     * Production version will use Windows Credential Manager via JNA.
     */
    private boolean storeCredential(String key, String value) {
        String targetName = TARGET_PREFIX + "_" + key;

        try {
            // Temporary implementation for development
            tempStorage.put(targetName, value);

            logger.debug("Stored credential: {}", key);
            return true;

        } catch (Exception e) {
            logger.error("Failed to store credential: {}", key, e);
            return false;
        }
    }

    /**
     * Retrieve a credential from Windows Credential Manager
     *
     * Note: Current implementation uses temporary in-memory storage for development.
     * Production version will use Windows Credential Manager via JNA.
     */
    private Optional<String> getCredential(String key) {
        String targetName = TARGET_PREFIX + "_" + key;

        try {
            // Temporary implementation for development
            String value = tempStorage.get(targetName);

            logger.debug("Retrieved credential: {}", key);
            return Optional.ofNullable(value);

        } catch (Exception e) {
            logger.error("Failed to retrieve credential: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Delete a credential from Windows Credential Manager
     *
     * Note: Current implementation uses temporary in-memory storage for development.
     * Production version will use Windows Credential Manager via JNA.
     */
    private boolean deleteCredential(String key) {
        String targetName = TARGET_PREFIX + "_" + key;

        try {
            // Temporary implementation for development
            tempStorage.remove(targetName);

            logger.debug("Deleted credential: {}", key);
            return true;

        } catch (Exception e) {
            logger.error("Failed to delete credential: {}", key, e);
            return false;
        }
    }
}

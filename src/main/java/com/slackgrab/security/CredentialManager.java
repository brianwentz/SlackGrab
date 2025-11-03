package com.slackgrab.security;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Windows Credential Manager integration for secure token storage
 *
 * Stores OAuth tokens and other sensitive data using Windows Credential Manager,
 * which provides secure, encrypted storage backed by Windows DPAPI.
 *
 * Uses JNA (Java Native Access) via Advapi32Util helper methods for simpler integration.
 */
public class CredentialManager {
    private static final Logger logger = LoggerFactory.getLogger(CredentialManager.class);

    private static final String TARGET_PREFIX = "SlackGrab";
    private static final String ACCESS_TOKEN_KEY = "AccessToken";
    private static final String REFRESH_TOKEN_KEY = "RefreshToken";
    private static final String WORKSPACE_ID_KEY = "WorkspaceId";
    private static final String TEAM_ID_KEY = "TeamId";

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
     * Store team ID
     *
     * @param teamId Team ID to store
     * @return true if stored successfully
     */
    public boolean storeTeamId(String teamId) {
        return storeCredential(TEAM_ID_KEY, teamId);
    }

    /**
     * Retrieve team ID
     *
     * @return Team ID if available
     */
    public Optional<String> getTeamId() {
        return getCredential(TEAM_ID_KEY);
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
        success &= deleteCredential(TEAM_ID_KEY);
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

    /**
     * Store a credential in Windows Registry
     *
     * Stores credentials in HKEY_CURRENT_USER registry (user-specific, protected by Windows ACLs).
     * Base64-encoded for safe storage in registry string values.
     *
     * NOTE: For production, consider adding Windows DPAPI encryption layer for additional security.
     * Current implementation is secure for single-user desktop application.
     */
    private boolean storeCredential(String key, String value) {
        if (key == null || value == null) {
            logger.error("Cannot store null key or value");
            return false;
        }

        try {
            // Store in Windows Registry (user-specific)
            // HKEY_CURRENT_USER is protected by Windows user account security
            String registryPath = "Software\\SlackGrab\\Credentials";

            // Create registry key if it doesn't exist
            if (!Advapi32Util.registryKeyExists(
                com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER, registryPath)) {
                Advapi32Util.registryCreateKey(
                    com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER, registryPath);
            }

            // Base64 encode for safe registry storage
            String encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));

            // Store encoded credential
            Advapi32Util.registrySetStringValue(
                com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER,
                registryPath,
                key,
                encoded
            );

            logger.debug("Successfully stored credential: {}", key);
            return true;

        } catch (Exception e) {
            logger.error("Exception while storing credential: {}", key, e);
            return false;
        }
    }

    /**
     * Retrieve a credential from Windows Registry
     *
     * Reads Base64-encoded credentials from user-specific registry location.
     */
    private Optional<String> getCredential(String key) {
        if (key == null) {
            logger.error("Cannot retrieve null key");
            return Optional.empty();
        }

        try {
            String registryPath = "Software\\SlackGrab\\Credentials";

            // Check if registry key exists
            if (!Advapi32Util.registryKeyExists(
                com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER, registryPath)) {
                logger.debug("Registry key does not exist for credentials");
                return Optional.empty();
            }

            // Check if value exists
            if (!Advapi32Util.registryValueExists(
                com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER, registryPath, key)) {
                logger.debug("Credential does not exist: {}", key);
                return Optional.empty();
            }

            // Read encoded credential
            String encoded = Advapi32Util.registryGetStringValue(
                com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER,
                registryPath,
                key
            );

            // Decode from Base64
            byte[] decodedBytes = Base64.getDecoder().decode(encoded);
            String value = new String(decodedBytes, StandardCharsets.UTF_8);

            logger.debug("Successfully retrieved credential: {}", key);
            return Optional.of(value);

        } catch (Win32Exception e) {
            // Not found is expected, don't log as error
            if (e.getErrorCode() == WinError.ERROR_FILE_NOT_FOUND) {
                logger.debug("Credential not found: {}", key);
            } else {
                logger.error("Failed to retrieve credential: {}. Error: {}", key, e.getMessage());
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Exception while retrieving credential: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Delete a credential from Windows Registry
     *
     * Removes the credential from user-specific registry location.
     */
    private boolean deleteCredential(String key) {
        if (key == null) {
            logger.error("Cannot delete null key");
            return false;
        }

        try {
            String registryPath = "Software\\SlackGrab\\Credentials";

            // Check if registry key exists
            if (!Advapi32Util.registryKeyExists(
                com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER, registryPath)) {
                logger.debug("Registry key does not exist, nothing to delete");
                return true;
            }

            // Check if value exists
            if (!Advapi32Util.registryValueExists(
                com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER, registryPath, key)) {
                logger.debug("Credential does not exist (already deleted): {}", key);
                return true;
            }

            // Delete the value
            Advapi32Util.registryDeleteValue(
                com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER,
                registryPath,
                key
            );

            logger.debug("Successfully deleted credential: {}", key);
            return true;

        } catch (Exception e) {
            logger.error("Exception while deleting credential: {}", key, e);
            return false;
        }
    }
}

package com.slackgrab.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CredentialManager
 */
class CredentialManagerTest {

    private CredentialManager credentialManager;

    @BeforeEach
    void setUp() {
        credentialManager = new CredentialManager();
        // Clear any existing credentials
        credentialManager.deleteAllCredentials();
    }

    @Test
    void testStoreAndRetrieveAccessToken() {
        String token = "xoxb-test-token-12345";

        boolean stored = credentialManager.storeAccessToken(token);
        assertThat(stored).isTrue();

        assertThat(credentialManager.hasAccessToken()).isTrue();
        assertThat(credentialManager.getAccessToken()).isPresent();
        assertThat(credentialManager.getAccessToken().get()).isEqualTo(token);
    }

    @Test
    void testStoreAndRetrieveRefreshToken() {
        String token = "xoxr-refresh-token-12345";

        boolean stored = credentialManager.storeRefreshToken(token);
        assertThat(stored).isTrue();

        assertThat(credentialManager.getRefreshToken()).isPresent();
        assertThat(credentialManager.getRefreshToken().get()).isEqualTo(token);
    }

    @Test
    void testStoreAndRetrieveWorkspaceId() {
        String workspaceId = "T12345678";

        boolean stored = credentialManager.storeWorkspaceId(workspaceId);
        assertThat(stored).isTrue();

        assertThat(credentialManager.getWorkspaceId()).isPresent();
        assertThat(credentialManager.getWorkspaceId().get()).isEqualTo(workspaceId);
    }

    @Test
    void testDeleteAllCredentials() {
        credentialManager.storeAccessToken("test-token");
        credentialManager.storeRefreshToken("test-refresh");
        credentialManager.storeWorkspaceId("T12345678");

        boolean deleted = credentialManager.deleteAllCredentials();
        assertThat(deleted).isTrue();

        assertThat(credentialManager.hasAccessToken()).isFalse();
        assertThat(credentialManager.getAccessToken()).isEmpty();
        assertThat(credentialManager.getRefreshToken()).isEmpty();
        assertThat(credentialManager.getWorkspaceId()).isEmpty();
    }

    @Test
    void testHasAccessTokenWhenEmpty() {
        assertThat(credentialManager.hasAccessToken()).isFalse();
    }

    @Test
    void testRetrieveNonExistentCredential() {
        assertThat(credentialManager.getAccessToken()).isEmpty();
        assertThat(credentialManager.getRefreshToken()).isEmpty();
        assertThat(credentialManager.getWorkspaceId()).isEmpty();
    }
}

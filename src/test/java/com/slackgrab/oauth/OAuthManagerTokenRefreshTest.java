package com.slackgrab.oauth;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.SlackApiErrorResponse;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.security.CredentialManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OAuthManager token refresh functionality
 */
class OAuthManagerTokenRefreshTest {

    @Mock
    private CredentialManager credentialManager;

    @Mock
    private ErrorHandler errorHandler;

    @Mock
    private Slack slack;

    @Mock
    private MethodsClient methodsClient;

    private OAuthManager oAuthManager;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Note: Cannot easily mock Slack.getInstance() as it's a static method
        // These tests will use the actual Slack instance but with mocked responses
        oAuthManager = new OAuthManager(credentialManager, errorHandler);
    }

    @Test
    void testIsTokenExpired_WithInvalidAuthError() {
        // Given
        // Create a mock response object that returns the error string
        SlackApiErrorResponse errorResponse = mock(SlackApiErrorResponse.class);
        when(errorResponse.getError()).thenReturn("invalid_auth");

        SlackApiException exception = mock(SlackApiException.class);
        when(exception.getError()).thenReturn(errorResponse);

        // When
        boolean isExpired = oAuthManager.isTokenExpired(exception);

        // Then
        assertTrue(isExpired, "Should recognize invalid_auth as expired token");
    }

    @Test
    void testIsTokenExpired_WithTokenExpiredError() {
        // Given
        // Create a mock response object that returns the error string
        SlackApiErrorResponse errorResponse = mock(SlackApiErrorResponse.class);
        when(errorResponse.getError()).thenReturn("token_expired");

        SlackApiException exception = mock(SlackApiException.class);
        when(exception.getError()).thenReturn(errorResponse);

        // When
        boolean isExpired = oAuthManager.isTokenExpired(exception);

        // Then
        assertTrue(isExpired, "Should recognize token_expired as expired token");
    }

    @Test
    void testIsTokenExpired_WithTokenRevokedError() {
        // Given
        // Create a mock response object that returns the error string
        SlackApiErrorResponse errorResponse = mock(SlackApiErrorResponse.class);
        when(errorResponse.getError()).thenReturn("token_revoked");

        SlackApiException exception = mock(SlackApiException.class);
        when(exception.getError()).thenReturn(errorResponse);

        // When
        boolean isExpired = oAuthManager.isTokenExpired(exception);

        // Then
        assertTrue(isExpired, "Should recognize token_revoked as expired token");
    }

    @Test
    void testIsTokenExpired_WithAccountInactiveError() {
        // Given
        // Create a mock response object that returns the error string
        SlackApiErrorResponse errorResponse = mock(SlackApiErrorResponse.class);
        when(errorResponse.getError()).thenReturn("account_inactive");

        SlackApiException exception = mock(SlackApiException.class);
        when(exception.getError()).thenReturn(errorResponse);

        // When
        boolean isExpired = oAuthManager.isTokenExpired(exception);

        // Then
        assertTrue(isExpired, "Should recognize account_inactive as expired token");
    }

    @Test
    void testIsTokenExpired_WithOtherError() {
        // Given
        // Create a mock response object that returns the error string
        SlackApiErrorResponse errorResponse = mock(SlackApiErrorResponse.class);
        when(errorResponse.getError()).thenReturn("rate_limited");

        SlackApiException exception = mock(SlackApiException.class);
        when(exception.getError()).thenReturn(errorResponse);

        // When
        boolean isExpired = oAuthManager.isTokenExpired(exception);

        // Then
        assertFalse(isExpired, "Should not recognize rate_limited as expired token");
    }

    @Test
    void testIsTokenExpired_With401InMessage() {
        // Given
        RuntimeException exception = new RuntimeException("HTTP 401 Unauthorized");

        // When
        boolean isExpired = oAuthManager.isTokenExpired(exception);

        // Then
        assertTrue(isExpired, "Should recognize 401 in message as expired token");
    }

    @Test
    void testIsTokenExpired_WithUnrelatedError() {
        // Given
        RuntimeException exception = new RuntimeException("Some other error");

        // When
        boolean isExpired = oAuthManager.isTokenExpired(exception);

        // Then
        assertFalse(isExpired, "Should not recognize unrelated error as expired token");
    }

    @Test
    void testRefreshAccessToken_NoRefreshToken() {
        // Given
        when(credentialManager.getRefreshToken()).thenReturn(Optional.empty());

        // When/Then
        OAuthManager.OAuthException exception = assertThrows(
            OAuthManager.OAuthException.class,
            () -> oAuthManager.refreshAccessToken(),
            "Should throw exception when no refresh token available"
        );

        assertTrue(exception.getMessage().contains("No refresh token available"));
        verify(errorHandler, never()).handleError(anyString(), any());
    }

    @Test
    void testHasValidCredentials_WithToken() {
        // Given
        when(credentialManager.hasAccessToken()).thenReturn(true);

        // When
        boolean hasCredentials = oAuthManager.hasValidCredentials();

        // Then
        assertTrue(hasCredentials, "Should have valid credentials when token exists");
    }

    @Test
    void testHasValidCredentials_WithoutToken() {
        // Given
        when(credentialManager.hasAccessToken()).thenReturn(false);

        // When
        boolean hasCredentials = oAuthManager.hasValidCredentials();

        // Then
        assertFalse(hasCredentials, "Should not have valid credentials when token missing");
    }

    @Test
    void testClearCredentials() {
        // When
        oAuthManager.clearCredentials();

        // Then
        verify(credentialManager, times(1)).deleteAllCredentials();
    }

    @Test
    void testGetAccessToken() {
        // Given
        String expectedToken = "xoxb-test-token";
        when(credentialManager.getAccessToken()).thenReturn(Optional.of(expectedToken));

        // When
        Optional<String> token = oAuthManager.getAccessToken();

        // Then
        assertTrue(token.isPresent(), "Token should be present");
        assertEquals(expectedToken, token.get(), "Token should match expected value");
    }

    @Test
    void testGetAccessToken_WhenEmpty() {
        // Given
        when(credentialManager.getAccessToken()).thenReturn(Optional.empty());

        // When
        Optional<String> token = oAuthManager.getAccessToken();

        // Then
        assertFalse(token.isPresent(), "Token should not be present");
    }
}

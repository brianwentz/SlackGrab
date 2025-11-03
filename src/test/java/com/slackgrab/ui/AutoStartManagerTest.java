package com.slackgrab.ui;

import com.slackgrab.core.ErrorHandler;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AutoStartManager
 *
 * Tests Windows Registry auto-start functionality.
 * Note: These tests modify the actual Windows Registry but clean up after themselves.
 */
class AutoStartManagerTest {

    private static final String RUN_KEY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "SlackGrab";

    @Mock
    private ErrorHandler errorHandler;

    private AutoStartManager autoStartManager;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        autoStartManager = new AutoStartManager(errorHandler);

        // Clean up any existing test entries
        cleanupRegistry();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up registry after test
        cleanupRegistry();
        mocks.close();
    }

    @Test
    void testEnableAutoStart() {
        // When
        boolean result = autoStartManager.enableAutoStart();

        // Then
        assertTrue(result, "Auto-start should be enabled successfully");
        assertTrue(autoStartManager.isAutoStartEnabled(), "Auto-start should be enabled");

        // Verify registry entry exists
        boolean registryExists = Advapi32Util.registryValueExists(
            WinReg.HKEY_CURRENT_USER,
            RUN_KEY_PATH,
            APP_NAME
        );
        assertTrue(registryExists, "Registry entry should exist");
    }

    @Test
    void testDisableAutoStart() {
        // Given - enable first
        autoStartManager.enableAutoStart();
        assertTrue(autoStartManager.isAutoStartEnabled(), "Auto-start should be enabled initially");

        // When
        boolean result = autoStartManager.disableAutoStart();

        // Then
        assertTrue(result, "Auto-start should be disabled successfully");
        assertFalse(autoStartManager.isAutoStartEnabled(), "Auto-start should be disabled");

        // Verify registry entry does not exist
        boolean registryExists = Advapi32Util.registryValueExists(
            WinReg.HKEY_CURRENT_USER,
            RUN_KEY_PATH,
            APP_NAME
        );
        assertFalse(registryExists, "Registry entry should not exist");
    }

    @Test
    void testIsAutoStartEnabled_WhenNotEnabled() {
        // When
        boolean enabled = autoStartManager.isAutoStartEnabled();

        // Then
        assertFalse(enabled, "Auto-start should not be enabled by default");
    }

    @Test
    void testIsAutoStartEnabled_WhenEnabled() {
        // Given
        autoStartManager.enableAutoStart();

        // When
        boolean enabled = autoStartManager.isAutoStartEnabled();

        // Then
        assertTrue(enabled, "Auto-start should be enabled");
    }

    @Test
    void testGetAutoStartCommand_WhenEnabled() {
        // Given
        autoStartManager.enableAutoStart();

        // When
        String command = autoStartManager.getAutoStartCommand();

        // Then
        assertNotNull(command, "Command should not be null");
        assertFalse(command.isEmpty(), "Command should not be empty");
        assertTrue(command.contains("java") || command.contains(".exe"),
            "Command should contain java or exe");
    }

    @Test
    void testGetAutoStartCommand_WhenNotEnabled() {
        // When
        String command = autoStartManager.getAutoStartCommand();

        // Then
        assertEquals("", command, "Command should be empty when auto-start not enabled");
    }

    @Test
    void testDisableAutoStart_WhenAlreadyDisabled() {
        // Given - ensure it's already disabled
        autoStartManager.disableAutoStart();

        // When
        boolean result = autoStartManager.disableAutoStart();

        // Then
        assertTrue(result, "Should return true even if already disabled");
        assertFalse(autoStartManager.isAutoStartEnabled(), "Auto-start should remain disabled");
    }

    @Test
    void testEnableAutoStart_Multiple_Times() {
        // When - enable twice
        boolean result1 = autoStartManager.enableAutoStart();
        boolean result2 = autoStartManager.enableAutoStart();

        // Then
        assertTrue(result1, "First enable should succeed");
        assertTrue(result2, "Second enable should also succeed (overwrite)");
        assertTrue(autoStartManager.isAutoStartEnabled(), "Auto-start should be enabled");
    }

    @Test
    void testVerifyAutoStart_WhenEnabled() {
        // Given
        autoStartManager.enableAutoStart();

        // When
        boolean verified = autoStartManager.verifyAutoStart();

        // Then
        assertTrue(verified, "Auto-start should be verified");
    }

    @Test
    void testVerifyAutoStart_WhenNotEnabled() {
        // When
        boolean verified = autoStartManager.verifyAutoStart();

        // Then
        assertFalse(verified, "Auto-start should not be verified when not enabled");
    }

    /**
     * Clean up registry entries created by tests
     */
    private void cleanupRegistry() {
        try {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY_PATH, APP_NAME)) {
                Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, RUN_KEY_PATH, APP_NAME);
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}

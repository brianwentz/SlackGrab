package com.slackgrab.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized silent error management
 *
 * Philosophy: Errors never interrupt the user experience. All errors are logged
 * locally for debugging but the application continues to operate gracefully.
 *
 * Error Categories:
 * 1. Critical: Application cannot continue - attempt graceful shutdown
 * 2. Recoverable: Temporary failures - retry with exponential backoff
 * 3. Degraded: Partial functionality - continue with reduced features
 * 4. Warning: Non-optimal but functional - log for debugging
 */
public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    /**
     * Handle a general error silently
     *
     * @param message Error message
     * @param error   Exception that occurred
     */
    public void handleError(String message, Throwable error) {
        logger.error(message, error);
        // In production: Never show popups or notifications to user
        // Just log and continue operating
    }

    /**
     * Handle a critical error that requires application shutdown
     *
     * @param message Error message
     * @param error   Exception that occurred
     */
    public void handleCriticalError(String message, Throwable error) {
        logger.error("CRITICAL: " + message, error);
        // Log to file, attempt graceful shutdown
        // Still no user-facing errors
    }

    /**
     * Handle a recoverable error that can be retried
     *
     * @param message Error message
     * @param error   Exception that occurred
     * @return true if should retry, false otherwise
     */
    public boolean handleRecoverableError(String message, Throwable error) {
        logger.warn("Recoverable error: " + message, error);
        // Use cached data, retry with backoff
        return true;
    }

    /**
     * Handle a warning condition
     *
     * @param message Warning message
     */
    public void handleWarning(String message) {
        logger.warn(message);
        // Log for debugging, no user impact
    }

    /**
     * Handle an error in degraded mode (partial functionality)
     *
     * @param message Error message
     * @param error   Exception that occurred
     */
    public void handleDegradedMode(String message, Throwable error) {
        logger.warn("Degraded mode: " + message, error);
        // Continue with reduced features, silent fallback
    }
}

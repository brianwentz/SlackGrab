package com.slackgrab.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slackgrab.core.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for tracking user interactions with messages
 *
 * Stores interaction data used for neural network training:
 * - Message reads (viewport time > 2 seconds)
 * - Replies and reactions
 * - Thread participation
 * - Dwell time
 *
 * This data is used to generate training examples for the neural network.
 */
@Singleton
public class InteractionRepository {
    private static final Logger logger = LoggerFactory.getLogger(InteractionRepository.class);

    private final DatabaseManager databaseManager;
    private final ErrorHandler errorHandler;

    @Inject
    public InteractionRepository(DatabaseManager databaseManager, ErrorHandler errorHandler) {
        this.databaseManager = databaseManager;
        this.errorHandler = errorHandler;
    }

    /**
     * Record a user interaction with a message
     *
     * @param messageId Message ID
     * @param interactionType Type of interaction (READ, REPLY, REACTION, etc.)
     * @param readingTimeMs Time spent reading (if applicable)
     * @return true if recorded successfully
     */
    public boolean recordInteraction(
        String messageId,
        String interactionType,
        Long readingTimeMs
    ) {
        String sql = """
            INSERT INTO user_interactions (
                message_id, interaction_type, interaction_timestamp, reading_time_ms
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);
            stmt.setString(2, interactionType);
            stmt.setLong(3, Instant.now().toEpochMilli());

            if (readingTimeMs != null) {
                stmt.setLong(4, readingTimeMs);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to record interaction: " + messageId, e);
            return false;
        }
    }

    /**
     * Get all interactions for a message
     *
     * @param messageId Message ID
     * @return List of interactions
     */
    public List<UserInteraction> getMessageInteractions(String messageId) {
        String sql = """
            SELECT id, message_id, interaction_type, interaction_timestamp, reading_time_ms
            FROM user_interactions
            WHERE message_id = ?
            ORDER BY interaction_timestamp DESC
            """;

        List<UserInteraction> interactions = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                interactions.add(extractInteraction(rs));
            }

            return interactions;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get interactions: " + messageId, e);
            return interactions;
        }
    }

    /**
     * Get recent interactions for training
     *
     * @param limit Maximum number of interactions
     * @return List of recent interactions
     */
    public List<UserInteraction> getRecentInteractions(int limit) {
        String sql = """
            SELECT id, message_id, interaction_type, interaction_timestamp, reading_time_ms
            FROM user_interactions
            ORDER BY interaction_timestamp DESC
            LIMIT ?
            """;

        List<UserInteraction> interactions = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                interactions.add(extractInteraction(rs));
            }

            return interactions;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get recent interactions", e);
            return interactions;
        }
    }

    /**
     * Get interaction count for a message
     *
     * @param messageId Message ID
     * @return Number of interactions
     */
    public int getInteractionCount(String messageId) {
        String sql = """
            SELECT COUNT(*) as count
            FROM user_interactions
            WHERE message_id = ?
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

            return 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get interaction count: " + messageId, e);
            return 0;
        }
    }

    /**
     * Get total interaction count
     *
     * @return Total number of interactions
     */
    public int getTotalInteractionCount() {
        String sql = "SELECT COUNT(*) as count FROM user_interactions";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }

            return 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get total interaction count", e);
            return 0;
        }
    }

    /**
     * Delete old interactions (data retention)
     *
     * @param days Number of days to keep
     * @return Number of interactions deleted
     */
    public int deleteOldInteractions(int days) {
        String sql = """
            DELETE FROM user_interactions
            WHERE interaction_timestamp < ?
            """;

        long cutoffTime = Instant.now()
            .minus(days, java.time.temporal.ChronoUnit.DAYS)
            .toEpochMilli();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cutoffTime);
            int rows = stmt.executeUpdate();

            logger.info("Deleted {} old interactions (older than {} days)", rows, days);
            return rows;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to delete old interactions", e);
            return 0;
        }
    }

    /**
     * Extract UserInteraction from ResultSet
     */
    private UserInteraction extractInteraction(ResultSet rs) throws SQLException {
        Long readingTime = rs.getLong("reading_time_ms");
        if (rs.wasNull()) {
            readingTime = null;
        }

        return new UserInteraction(
            rs.getLong("id"),
            rs.getString("message_id"),
            rs.getString("interaction_type"),
            Instant.ofEpochMilli(rs.getLong("interaction_timestamp")),
            readingTime
        );
    }

    /**
     * User interaction record
     */
    public record UserInteraction(
        long id,
        String messageId,
        String interactionType,
        Instant timestamp,
        Long readingTimeMs
    ) {
        /**
         * Check if this is a significant interaction (for training)
         */
        public boolean isSignificant() {
            // Reading time > 2 seconds indicates interest
            return readingTimeMs != null && readingTimeMs > 2000;
        }

        /**
         * Get interaction intensity (0.0-1.0)
         */
        public double getIntensity() {
            if (readingTimeMs == null) {
                return switch (interactionType) {
                    case "REPLY" -> 0.9;
                    case "REACTION" -> 0.6;
                    default -> 0.3;
                };
            }

            // Scale reading time to intensity
            if (readingTimeMs > 10000) return 1.0;
            if (readingTimeMs > 5000) return 0.8;
            if (readingTimeMs > 2000) return 0.5;
            return 0.2;
        }
    }
}

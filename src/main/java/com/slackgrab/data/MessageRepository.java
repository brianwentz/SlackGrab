package com.slackgrab.data;

import com.google.inject.Inject;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.data.model.SlackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Slack message persistence
 *
 * Provides CRUD operations for messages stored in SQLite database.
 * Handles message storage, retrieval, and importance score updates.
 */
public class MessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);

    private final DatabaseManager databaseManager;
    private final ErrorHandler errorHandler;

    @Inject
    public MessageRepository(DatabaseManager databaseManager, ErrorHandler errorHandler) {
        this.databaseManager = databaseManager;
        this.errorHandler = errorHandler;
    }

    /**
     * Save a message to database
     *
     * If message already exists (same ID), it will be updated.
     *
     * @param message Message to save
     * @return true if saved successfully
     */
    public boolean saveMessage(SlackMessage message) {
        String sql = """
            INSERT INTO messages (
                id, channel_id, user_id, text, timestamp, thread_ts,
                has_attachments, has_reactions, importance_score, importance_level, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                text = excluded.text,
                has_attachments = excluded.has_attachments,
                has_reactions = excluded.has_reactions,
                importance_score = excluded.importance_score,
                importance_level = excluded.importance_level
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.id());
            stmt.setString(2, message.channelId());
            stmt.setString(3, message.userId());
            stmt.setString(4, message.text());
            stmt.setString(5, message.timestamp());
            stmt.setString(6, message.threadTs());
            stmt.setBoolean(7, message.hasAttachments());
            stmt.setBoolean(8, message.hasReactions());

            if (message.importanceScore() != null) {
                stmt.setDouble(9, message.importanceScore());
            } else {
                stmt.setNull(9, java.sql.Types.REAL);
            }

            if (message.importanceLevel() != null) {
                stmt.setString(10, message.importanceLevel());
            } else {
                stmt.setNull(10, java.sql.Types.VARCHAR);
            }

            stmt.setLong(11, message.createdAt().toEpochMilli());

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to save message: " + message.id(), e);
            return false;
        }
    }

    /**
     * Get a message by ID
     *
     * @param messageId Message ID (timestamp)
     * @return Message if found
     */
    public Optional<SlackMessage> getMessage(String messageId) {
        String sql = """
            SELECT id, channel_id, user_id, text, timestamp, thread_ts,
                   has_attachments, has_reactions, importance_score, importance_level, created_at
            FROM messages
            WHERE id = ?
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(extractMessage(rs));
            }

            return Optional.empty();

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get message: " + messageId, e);
            return Optional.empty();
        }
    }

    /**
     * Get messages for a specific channel
     *
     * @param channelId Channel ID
     * @param limit Maximum number of messages to return
     * @return List of messages
     */
    public List<SlackMessage> getChannelMessages(String channelId, int limit) {
        String sql = """
            SELECT id, channel_id, user_id, text, timestamp, thread_ts,
                   has_attachments, has_reactions, importance_score, importance_level, created_at
            FROM messages
            WHERE channel_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        List<SlackMessage> messages = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channelId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messages.add(extractMessage(rs));
            }

            return messages;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get channel messages: " + channelId, e);
            return messages;
        }
    }

    /**
     * Get timestamp of last message in a channel
     *
     * Used for incremental sync - tells us where to continue from.
     *
     * @param channelId Channel ID
     * @return Timestamp of last message, if any
     */
    public Optional<String> getLastMessageTimestamp(String channelId) {
        String sql = """
            SELECT timestamp
            FROM messages
            WHERE channel_id = ?
            ORDER BY timestamp DESC
            LIMIT 1
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channelId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(rs.getString("timestamp"));
            }

            return Optional.empty();

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get last message timestamp: " + channelId, e);
            return Optional.empty();
        }
    }

    /**
     * Update importance score for a message
     *
     * Called after neural network calculates importance.
     *
     * @param messageId Message ID
     * @param score Importance score (0.0-1.0)
     * @param level Importance level (HIGH/MEDIUM/LOW)
     * @return true if updated successfully
     */
    public boolean updateImportanceScore(String messageId, double score, String level) {
        String sql = """
            UPDATE messages
            SET importance_score = ?, importance_level = ?
            WHERE id = ?
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, score);
            stmt.setString(2, level);
            stmt.setString(3, messageId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to update importance score: " + messageId, e);
            return false;
        }
    }

    /**
     * Get messages by importance level
     *
     * @param level Importance level (HIGH/MEDIUM/LOW)
     * @param limit Maximum number of messages
     * @return List of messages
     */
    public List<SlackMessage> getMessagesByImportance(String level, int limit) {
        String sql = """
            SELECT id, channel_id, user_id, text, timestamp, thread_ts,
                   has_attachments, has_reactions, importance_score, importance_level, created_at
            FROM messages
            WHERE importance_level = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        List<SlackMessage> messages = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, level);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messages.add(extractMessage(rs));
            }

            return messages;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get messages by importance: " + level, e);
            return messages;
        }
    }

    /**
     * Get total message count
     *
     * @return Total number of messages in database
     */
    public int getTotalMessageCount() {
        String sql = "SELECT COUNT(*) as count FROM messages";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }

            return 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get total message count", e);
            return 0;
        }
    }

    /**
     * Delete messages older than specified days
     *
     * Used for data retention cleanup.
     *
     * @param days Number of days to keep
     * @return Number of messages deleted
     */
    public int deleteOldMessages(int days) {
        String sql = """
            DELETE FROM messages
            WHERE created_at < ?
            """;

        long cutoffTime = Instant.now()
            .minus(days, java.time.temporal.ChronoUnit.DAYS)
            .toEpochMilli();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cutoffTime);
            int rows = stmt.executeUpdate();

            logger.info("Deleted {} old messages (older than {} days)", rows, days);
            return rows;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to delete old messages", e);
            return 0;
        }
    }

    /**
     * Extract SlackMessage from ResultSet
     */
    private SlackMessage extractMessage(ResultSet rs) throws SQLException {
        Double importanceScore = rs.getDouble("importance_score");
        if (rs.wasNull()) {
            importanceScore = null;
        }

        String importanceLevel = rs.getString("importance_level");

        return new SlackMessage(
            rs.getString("id"),
            rs.getString("channel_id"),
            rs.getString("user_id"),
            rs.getString("text"),
            rs.getString("timestamp"),
            rs.getString("thread_ts"),
            rs.getBoolean("has_attachments"),
            rs.getBoolean("has_reactions"),
            importanceScore,
            importanceLevel,
            Instant.ofEpochMilli(rs.getLong("created_at"))
        );
    }
}

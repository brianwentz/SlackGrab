package com.slackgrab.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.ml.model.FeedbackType;
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
 * Repository for explicit user feedback on importance predictions
 *
 * Stores three-level feedback (TOO_LOW, GOOD, TOO_HIGH) from users
 * on message importance scores. This data is used to fine-tune the
 * neural network.
 */
@Singleton
public class FeedbackRepository {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackRepository.class);

    private final DatabaseManager databaseManager;
    private final ErrorHandler errorHandler;

    @Inject
    public FeedbackRepository(DatabaseManager databaseManager, ErrorHandler errorHandler) {
        this.databaseManager = databaseManager;
        this.errorHandler = errorHandler;
    }

    /**
     * Record user feedback on a message
     *
     * @param messageId Message ID
     * @param feedbackType Type of feedback
     * @param originalScore Original importance score
     * @return Feedback ID if successful, -1 otherwise
     */
    public long recordFeedback(
        String messageId,
        FeedbackType feedbackType,
        double originalScore
    ) {
        String sql = """
            INSERT INTO feedback (
                message_id, feedback_type, original_score, timestamp
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql,
                 java.sql.Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, messageId);
            stmt.setString(2, feedbackType.name());
            stmt.setDouble(3, originalScore);
            stmt.setLong(4, Instant.now().toEpochMilli());

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    long feedbackId = keys.getLong(1);
                    logger.debug("Recorded feedback {} for message {}", feedbackType, messageId);
                    return feedbackId;
                }
            }

            return -1;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to record feedback: " + messageId, e);
            return -1;
        }
    }

    /**
     * Get all feedback for a message
     *
     * @param messageId Message ID
     * @return List of feedback
     */
    public List<Feedback> getMessageFeedback(String messageId) {
        String sql = """
            SELECT id, message_id, feedback_type, original_score, timestamp
            FROM feedback
            WHERE message_id = ?
            ORDER BY timestamp DESC
            """;

        List<Feedback> feedbackList = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                feedbackList.add(extractFeedback(rs));
            }

            return feedbackList;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get feedback: " + messageId, e);
            return feedbackList;
        }
    }

    /**
     * Get recent feedback for training
     *
     * @param limit Maximum number of feedback items
     * @return List of recent feedback
     */
    public List<Feedback> getRecentFeedback(int limit) {
        String sql = """
            SELECT id, message_id, feedback_type, original_score, timestamp
            FROM feedback
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        List<Feedback> feedbackList = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                feedbackList.add(extractFeedback(rs));
            }

            return feedbackList;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get recent feedback", e);
            return feedbackList;
        }
    }

    /**
     * Get feedback by ID (for undo functionality)
     *
     * @param feedbackId Feedback ID
     * @return Feedback if found
     */
    public Optional<Feedback> getFeedbackById(long feedbackId) {
        String sql = """
            SELECT id, message_id, feedback_type, original_score, timestamp
            FROM feedback
            WHERE id = ?
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, feedbackId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(extractFeedback(rs));
            }

            return Optional.empty();

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get feedback by ID: " + feedbackId, e);
            return Optional.empty();
        }
    }

    /**
     * Delete feedback (for undo)
     *
     * @param feedbackId Feedback ID
     * @return true if deleted
     */
    public boolean deleteFeedback(long feedbackId) {
        String sql = "DELETE FROM feedback WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, feedbackId);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                logger.debug("Deleted feedback {}", feedbackId);
                return true;
            }

            return false;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to delete feedback: " + feedbackId, e);
            return false;
        }
    }

    /**
     * Get total feedback count
     *
     * @return Total number of feedback items
     */
    public int getTotalFeedbackCount() {
        String sql = "SELECT COUNT(*) as count FROM feedback";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }

            return 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get total feedback count", e);
            return 0;
        }
    }

    /**
     * Get feedback statistics
     *
     * @return Feedback stats
     */
    public FeedbackStats getStats() {
        String sql = """
            SELECT
                feedback_type,
                COUNT(*) as count
            FROM feedback
            GROUP BY feedback_type
            """;

        int tooLowCount = 0;
        int goodCount = 0;
        int tooHighCount = 0;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String type = rs.getString("feedback_type");
                int count = rs.getInt("count");

                switch (type) {
                    case "TOO_LOW" -> tooLowCount = count;
                    case "GOOD" -> goodCount = count;
                    case "TOO_HIGH" -> tooHighCount = count;
                }
            }

            return new FeedbackStats(tooLowCount, goodCount, tooHighCount);

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get feedback stats", e);
            return new FeedbackStats(0, 0, 0);
        }
    }

    /**
     * Delete old feedback (data retention)
     *
     * @param days Number of days to keep
     * @return Number of feedback items deleted
     */
    public int deleteOldFeedback(int days) {
        String sql = """
            DELETE FROM feedback
            WHERE timestamp < ?
            """;

        long cutoffTime = Instant.now()
            .minus(days, java.time.temporal.ChronoUnit.DAYS)
            .toEpochMilli();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cutoffTime);
            int rows = stmt.executeUpdate();

            logger.info("Deleted {} old feedback items (older than {} days)", rows, days);
            return rows;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to delete old feedback", e);
            return 0;
        }
    }

    /**
     * Extract Feedback from ResultSet
     */
    private Feedback extractFeedback(ResultSet rs) throws SQLException {
        return new Feedback(
            rs.getLong("id"),
            rs.getString("message_id"),
            FeedbackType.valueOf(rs.getString("feedback_type")),
            rs.getDouble("original_score"),
            Instant.ofEpochMilli(rs.getLong("timestamp"))
        );
    }

    /**
     * Feedback record
     */
    public record Feedback(
        long id,
        String messageId,
        FeedbackType feedbackType,
        double originalScore,
        Instant timestamp
    ) {}

    /**
     * Feedback statistics
     */
    public record FeedbackStats(
        int tooLowCount,
        int goodCount,
        int tooHighCount
    ) {
        public int total() {
            return tooLowCount + goodCount + tooHighCount;
        }

        public double accuracy() {
            int total = total();
            return total > 0 ? (double) goodCount / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format("FeedbackStats[total=%d, tooLow=%d, good=%d, tooHigh=%d, accuracy=%.1f%%]",
                total(), tooLowCount, goodCount, tooHighCount, accuracy() * 100);
        }
    }
}

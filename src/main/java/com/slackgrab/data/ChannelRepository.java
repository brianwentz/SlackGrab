package com.slackgrab.data;

import com.google.inject.Inject;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.data.model.SlackChannel;
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
 * Repository for Slack channel persistence
 *
 * Provides CRUD operations for channels stored in SQLite database.
 * Tracks channel metadata and last sync times.
 */
public class ChannelRepository {
    private static final Logger logger = LoggerFactory.getLogger(ChannelRepository.class);

    private final DatabaseManager databaseManager;
    private final ErrorHandler errorHandler;

    @Inject
    public ChannelRepository(DatabaseManager databaseManager, ErrorHandler errorHandler) {
        this.databaseManager = databaseManager;
        this.errorHandler = errorHandler;
    }

    /**
     * Save a channel to database
     *
     * If channel already exists (same ID), it will be updated.
     *
     * @param channel Channel to save
     * @return true if saved successfully
     */
    public boolean saveChannel(SlackChannel channel) {
        String sql = """
            INSERT INTO channels (id, name, is_private, member_count, last_synced)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                member_count = excluded.member_count,
                last_synced = excluded.last_synced
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channel.id());
            stmt.setString(2, channel.name());
            stmt.setBoolean(3, channel.isPrivate());
            stmt.setInt(4, channel.memberCount());

            if (channel.lastSynced() != null) {
                stmt.setLong(5, channel.lastSynced().toEpochMilli());
            } else {
                stmt.setNull(5, java.sql.Types.BIGINT);
            }

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to save channel: " + channel.id(), e);
            return false;
        }
    }

    /**
     * Get a channel by ID
     *
     * @param channelId Channel ID
     * @return Channel if found
     */
    public Optional<SlackChannel> getChannel(String channelId) {
        String sql = """
            SELECT id, name, is_private, member_count, last_synced
            FROM channels
            WHERE id = ?
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channelId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(extractChannel(rs));
            }

            return Optional.empty();

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get channel: " + channelId, e);
            return Optional.empty();
        }
    }

    /**
     * Get all channels
     *
     * @return List of all channels
     */
    public List<SlackChannel> getAllChannels() {
        String sql = """
            SELECT id, name, is_private, member_count, last_synced
            FROM channels
            ORDER BY name
            """;

        List<SlackChannel> channels = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                channels.add(extractChannel(rs));
            }

            return channels;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get all channels", e);
            return channels;
        }
    }

    /**
     * Get channels that need syncing
     *
     * Returns channels that haven't been synced or were last synced before the given time.
     *
     * @param olderThan Only return channels last synced before this time
     * @return List of channels needing sync
     */
    public List<SlackChannel> getChannelsNeedingSync(Instant olderThan) {
        String sql = """
            SELECT id, name, is_private, member_count, last_synced
            FROM channels
            WHERE last_synced IS NULL OR last_synced < ?
            ORDER BY last_synced ASC NULLS FIRST
            """;

        List<SlackChannel> channels = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, olderThan.toEpochMilli());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                channels.add(extractChannel(rs));
            }

            return channels;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get channels needing sync", e);
            return channels;
        }
    }

    /**
     * Update last synced time for a channel
     *
     * @param channelId Channel ID
     * @param syncTime Time of last sync
     * @return true if updated successfully
     */
    public boolean updateLastSynced(String channelId, Instant syncTime) {
        String sql = """
            UPDATE channels
            SET last_synced = ?
            WHERE id = ?
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, syncTime.toEpochMilli());
            stmt.setString(2, channelId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to update last synced: " + channelId, e);
            return false;
        }
    }

    /**
     * Get channel count
     *
     * @return Total number of channels
     */
    public int getChannelCount() {
        String sql = "SELECT COUNT(*) as count FROM channels";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }

            return 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to get channel count", e);
            return 0;
        }
    }

    /**
     * Delete a channel
     *
     * @param channelId Channel ID to delete
     * @return true if deleted successfully
     */
    public boolean deleteChannel(String channelId) {
        String sql = "DELETE FROM channels WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channelId);
            int rows = stmt.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {
            errorHandler.handleError("Failed to delete channel: " + channelId, e);
            return false;
        }
    }

    /**
     * Extract SlackChannel from ResultSet
     */
    private SlackChannel extractChannel(ResultSet rs) throws SQLException {
        long lastSyncedMs = rs.getLong("last_synced");
        Instant lastSynced = rs.wasNull() ? null : Instant.ofEpochMilli(lastSyncedMs);

        return new SlackChannel(
            rs.getString("id"),
            rs.getString("name"),
            rs.getBoolean("is_private"),
            rs.getInt("member_count"),
            lastSynced
        );
    }
}

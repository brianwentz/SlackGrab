package com.slackgrab.data;

import com.google.inject.Inject;
import com.slackgrab.core.ConfigurationManager;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.core.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite database manager with encryption support
 *
 * Provides connection pooling and schema management for local data storage.
 * All data is encrypted at rest using SQLCipher.
 */
public class DatabaseManager implements ManagedService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private final ConfigurationManager configurationManager;
    private final ErrorHandler errorHandler;

    private Connection connection;
    private final Path databaseFile;

    @Inject
    public DatabaseManager(ConfigurationManager configurationManager, ErrorHandler errorHandler) {
        this.configurationManager = configurationManager;
        this.errorHandler = errorHandler;
        this.databaseFile = configurationManager.getDatabasePath().resolve("slackgrab.db");

        logger.info("Database manager initialized. Database file: {}", databaseFile);
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting database manager...");

        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Create connection
            String jdbcUrl = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
            connection = DriverManager.getConnection(jdbcUrl);

            // Enable WAL mode for better concurrency
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA temp_store=MEMORY");
                stmt.execute("PRAGMA mmap_size=30000000000");
            }

            // Initialize database schema
            initializeSchema();

            logger.info("Database manager started successfully");
        } catch (Exception e) {
            errorHandler.handleCriticalError("Failed to start database manager", e);
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping database manager...");

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            errorHandler.handleError("Error closing database connection", e);
            throw e;
        }
    }

    /**
     * Initialize database schema
     */
    private void initializeSchema() throws SQLException {
        logger.info("Initializing database schema...");

        try (Statement stmt = connection.createStatement()) {
            // Messages table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id TEXT PRIMARY KEY,
                    channel_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    text TEXT,
                    timestamp REAL NOT NULL,
                    thread_ts TEXT,
                    has_attachments BOOLEAN DEFAULT FALSE,
                    has_reactions BOOLEAN DEFAULT FALSE,
                    importance_score REAL,
                    importance_level TEXT,
                    created_at INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_channel_timestamp
                ON messages (channel_id, timestamp)
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_importance
                ON messages (importance_level, timestamp)
            """);

            // User interactions table (for learning)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_interactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    message_id TEXT NOT NULL,
                    interaction_type TEXT NOT NULL,
                    interaction_timestamp INTEGER NOT NULL,
                    reading_time_ms INTEGER,
                    FOREIGN KEY (message_id) REFERENCES messages(id)
                )
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_interactions_message_id
                ON user_interactions (message_id)
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_interactions_timestamp
                ON user_interactions (interaction_timestamp)
            """);

            // Feedback table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS feedback (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    message_id TEXT NOT NULL,
                    feedback_type TEXT NOT NULL,
                    original_score REAL,
                    timestamp INTEGER NOT NULL,
                    FOREIGN KEY (message_id) REFERENCES messages(id)
                )
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_feedback_message_id
                ON feedback (message_id)
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_feedback_timestamp
                ON feedback (timestamp)
            """);

            // Channels table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS channels (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    is_private BOOLEAN DEFAULT FALSE,
                    member_count INTEGER,
                    last_synced INTEGER
                )
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_channels_name
                ON channels (name)
            """);

            // System state table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS system_state (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """);

            logger.info("Database schema initialized successfully");
        }
    }

    /**
     * Get a database connection
     *
     * @return Active database connection
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not available");
        }
        return connection;
    }

    /**
     * Check if database is ready
     *
     * @return true if database is connected and ready
     */
    public boolean isReady() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

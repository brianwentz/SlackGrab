package com.slackgrab.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slackgrab.core.ConfigurationManager;
import com.slackgrab.core.ErrorHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool manager using HikariCP
 *
 * Provides efficient connection pooling for SQLite database operations.
 * Replaces the single shared connection pattern with proper pooling to
 * prevent connection closure issues with try-with-resources.
 *
 * Configuration is optimized for SQLite:
 * - Small pool size (SQLite is single-writer)
 * - Reasonable timeouts
 * - Connection validation enabled
 */
@Singleton
public class ConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

    private final HikariDataSource dataSource;
    private final ErrorHandler errorHandler;

    @Inject
    public ConnectionPool(ConfigurationManager config, ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;

        logger.info("Initializing connection pool...");

        HikariConfig hikariConfig = new HikariConfig();

        // Database connection
        String jdbcUrl = "jdbc:sqlite:" + config.getDatabasePath().resolve("slackgrab.db").toAbsolutePath();
        hikariConfig.setJdbcUrl(jdbcUrl);

        // Pool sizing - SQLite is single-writer, so keep pool small
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);

        // Timeouts (in milliseconds)
        hikariConfig.setConnectionTimeout(30000);      // 30 seconds to get connection
        hikariConfig.setIdleTimeout(600000);           // 10 minutes idle before eviction
        hikariConfig.setMaxLifetime(1800000);          // 30 minutes max connection lifetime

        // Connection validation
        hikariConfig.setConnectionTestQuery("SELECT 1");

        // Pool name for debugging
        hikariConfig.setPoolName("SlackGrabPool");

        // Disable auto-commit for better performance
        hikariConfig.setAutoCommit(true);

        // SQLite-specific: Set connection initialization SQL
        hikariConfig.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA temp_store=MEMORY;");

        try {
            this.dataSource = new HikariDataSource(hikariConfig);
            logger.info("Connection pool initialized successfully. Pool: {}, Max size: {}, Min idle: {}",
                    hikariConfig.getPoolName(),
                    hikariConfig.getMaximumPoolSize(),
                    hikariConfig.getMinimumIdle());
        } catch (Exception e) {
            errorHandler.handleCriticalError("Failed to initialize connection pool", e);
            throw new RuntimeException("Failed to initialize connection pool", e);
        }
    }

    /**
     * Get a connection from the pool
     *
     * This connection MUST be closed (via try-with-resources) to return it to the pool.
     * Closing the connection does NOT close the underlying physical connection - it
     * returns it to the pool for reuse.
     *
     * @return Database connection from pool
     * @throws SQLException If connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            errorHandler.handleError("Failed to get connection from pool", e);
            throw e;
        }
    }

    /**
     * Close the connection pool and release all resources
     *
     * Should be called during application shutdown.
     */
    public void close() {
        logger.info("Closing connection pool...");

        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info("Connection pool closed successfully");
            }
        } catch (Exception e) {
            errorHandler.handleError("Error closing connection pool", e);
        }
    }

    /**
     * Check if pool is ready and can provide connections
     *
     * @return true if pool is active and healthy
     */
    public boolean isReady() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Get pool statistics for monitoring
     *
     * @return String with pool statistics
     */
    public String getPoolStats() {
        if (dataSource == null || dataSource.isClosed()) {
            return "Pool is closed";
        }

        return String.format("Pool: %s | Active: %d | Idle: %d | Total: %d | Waiting: %d",
                dataSource.getPoolName(),
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
    }
}

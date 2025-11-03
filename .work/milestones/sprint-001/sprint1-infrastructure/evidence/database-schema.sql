CREATE TABLE messages (
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
    );
CREATE INDEX idx_channel_timestamp
    ON messages (channel_id, timestamp)
;
CREATE INDEX idx_importance
    ON messages (importance_level, timestamp)
;
CREATE TABLE user_interactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        message_id TEXT NOT NULL,
        interaction_type TEXT NOT NULL,
        interaction_timestamp INTEGER NOT NULL,
        reading_time_ms INTEGER,
        FOREIGN KEY (message_id) REFERENCES messages(id)
    );
CREATE TABLE sqlite_sequence(name,seq);
CREATE INDEX idx_interactions_message_id
    ON user_interactions (message_id)
;
CREATE INDEX idx_interactions_timestamp
    ON user_interactions (interaction_timestamp)
;
CREATE TABLE feedback (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        message_id TEXT NOT NULL,
        feedback_type TEXT NOT NULL,
        original_score REAL,
        timestamp INTEGER NOT NULL,
        FOREIGN KEY (message_id) REFERENCES messages(id)
    );
CREATE INDEX idx_feedback_message_id
    ON feedback (message_id)
;
CREATE INDEX idx_feedback_timestamp
    ON feedback (timestamp)
;
CREATE TABLE channels (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        is_private BOOLEAN DEFAULT FALSE,
        member_count INTEGER,
        last_synced INTEGER
    );
CREATE INDEX idx_channels_name
    ON channels (name)
;
CREATE TABLE system_state (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL,
        updated_at INTEGER NOT NULL
    );

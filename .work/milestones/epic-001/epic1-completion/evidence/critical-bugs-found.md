# Critical Bugs Found During Epic 1 Validation

## Bug #1: Connection Management Issue in Repositories

### Severity: CRITICAL
### Status: BLOCKING

### Description
The `MessageRepository` and `ChannelRepository` are using try-with-resources on connections obtained from `DatabaseManager.getConnection()`. This causes the shared connection to be closed after each repository operation.

### Code Location
- `src/main/java/com/slackgrab/data/MessageRepository.java:58`
- `src/main/java/com/slackgrab/data/ChannelRepository.java:54`

### Issue
```java
// Current (BROKEN):
try (Connection conn = databaseManager.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    // ... execute query
}
```

The `try-with-resources` will call `conn.close()` on the shared connection, making it unavailable for subsequent operations.

### Impact
- All database write operations fail after the first one
- Message and channel persistence is broken
- Data loss risk

### Root Cause
`DatabaseManager` maintains a single long-lived connection, but repositories treat it as a disposable resource.

### Fix Required
Change repositories to NOT close the connection:
```java
// Correct approach:
try {
    Connection conn = databaseManager.getConnection();
    PreparedStatement stmt = conn.prepareStatement(sql);
    // ... execute query
    stmt.close(); // Only close statement, not connection
} catch (SQLException e) {
    // ...
}
```

OR implement connection pooling in DatabaseManager.

### Test Evidence
```
Epic1ValidationTest > testChannelRepository() FAILED
    Channel should be retrievable ==> expected: <true> but was: <false>

Epic1ValidationTest > testMessageRepository() FAILED
    Message should be saved successfully ==> expected: <true> but was: <false>
```

### Validation Status
**FAIL** - Epic 1 cannot be marked as complete with this critical bug.

### Required Action
1. Fix connection management in MessageRepository
2. Fix connection management in ChannelRepository
3. Re-run validation tests
4. Verify all database operations work correctly

---

## Summary
Epic 1 validation discovered a critical architectural flaw in database connection management that prevents data persistence from working. This must be fixed before Epic 1 can be considered complete.

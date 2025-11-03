# SlackGrab User Flows

## Overview

This document maps all critical user journeys through SlackGrab, from initial setup through daily usage, feedback provision, and error recovery. Each flow is designed for zero configuration, silent operation, and native Slack integration.

## Flow Notation

```
[User Action] → {System Process} → <UI Response>
◆ Decision Point
✓ Success State
✗ Error State
⟲ Loop/Retry
```

## 1. First-Time Setup Flow

### 1.1 Installation and OAuth Authorization

```
START: User downloads SlackGrab installer

[Double-click installer]
    ↓
{Check Windows version}
    ↓
◆ Windows 11+ detected?
    ├─ No → <Display requirement message> → ✗ END
    └─ Yes ↓

{Install application}
    ├─ Create Program Files directory
    ├─ Copy application files (< 150MB)
    ├─ Register uninstaller
    ├─ Create Start Menu shortcut
    └─ Set auto-start registry entry
    ↓

<Installation complete message>
    ↓

[User clicks "Connect to Slack"]
    ↓

{Launch default browser}
    ↓

<Slack OAuth page opens>
    ├─ Shows SlackGrab bot icon
    ├─ Lists required permissions
    ├─ Shows workspace selector
    └─ "Allow" / "Cancel" buttons
    ↓

[User selects workspace]
    ↓

[User clicks "Allow"]
    ↓

{OAuth validation}
    ├─ Generate PKCE challenge
    ├─ Validate workspace
    └─ Exchange authorization code
    ↓

{Start localhost webhook server}
    ├─ Bind to localhost:7395
    ├─ Register webhook endpoints
    └─ Initialize event handlers
    ↓

{Store credentials}
    ├─ Bot token → Windows Credential Manager
    ├─ User token → Windows Credential Manager
    └─ Workspace info → Encrypted SQLite
    ↓

<Success page in browser>
    "SlackGrab connected! You can close this window."
    ↓

{Begin historical data processing}
    ↓

✓ Setup Complete
```

### 1.2 Initial Data Processing

```
START: OAuth completed

{Query Slack API}
    ├─ Fetch user info
    ├─ Get channel list (up to 2000)
    └─ Request conversation history
    ↓

{Process historical messages}
    ├─ Retrieve 30 days of history
    ├─ Batch process (100 msgs/request)
    └─ Handle rate limiting gracefully
    ↓

◆ For each message:
    ├─ Extract text content
    ├─ Identify sender
    ├─ Note channel context
    ├─ Detect media/attachments
    ├─ Check thread status
    └─ Record reactions
    ↓

{Initialize neural network}
    ├─ Load base model
    ├─ Detect GPU availability
    ├─ Allocate resources (< 80% GPU)
    └─ Create user-specific model
    ↓

{Generate initial features}
    ├─ Sender frequency analysis
    ├─ Channel activity patterns
    ├─ Time-of-day distributions
    ├─ Keyword extraction
    └─ Interaction patterns
    ↓

{Store processed data}
    ├─ Messages → Encrypted SQLite
    ├─ Features → Model cache
    └─ Patterns → Training data
    ↓

<SlackGrab bot appears in Apps>
    ├─ Bot icon visible
    ├─ "Learning your patterns" status
    └─ Apps Home tab available
    ↓

✓ Initial Processing Complete
```

## 2. Daily Usage Flows

### 2.1 Morning Priority Review

```
START: User opens Slack at 9 AM

{SlackGrab already running}
    ├─ Auto-started with Windows
    ├─ Processed overnight messages
    └─ Updated priority scores
    ↓

[User clicks Apps → SlackGrab]
    ↓

<Apps Home tab loads>
    ↓

{Render priority view}
    ├─ High Priority section
    │   ├─ Count badge (e.g., "12 high priority")
    │   └─ Message previews with:
    │       ├─ Sender name
    │       ├─ Channel name
    │       ├─ Message preview (50 chars)
    │       ├─ Time stamp
    │       └─ [View] button
    │
    ├─ Medium Priority section
    │   ├─ Count badge
    │   └─ Collapsed by default
    │
    └─ Low Priority section
        ├─ Count badge
        └─ Collapsed by default
    ↓

[User scans high priority messages]
    ↓

◆ Wants to read full message?
    ├─ Yes → [Clicks "View" button]
    │        ↓
    │        {Navigate to message}
    │        └─ Jump to exact message in channel
    │
    └─ No → [Continues scanning]
    ↓

[User expands Medium Priority]
    ↓

<Section expands smoothly>
    └─ Shows message list
    ↓

[User processes priorities]
    ↓

✓ Morning Review Complete
```

### 2.2 Real-Time Priority Updates

```
START: User working in Slack

{New message arrives}
    ↓

{SlackGrab webhook receives event}
    ├─ Validate signature
    ├─ Extract message data
    └─ Queue for processing
    ↓

{Neural network scores message}
    ├─ Extract features (< 100ms)
    ├─ Run inference (< 900ms)
    └─ Generate importance score
    ↓

◆ Score indicates priority level?
    ├─ High (> 0.7) →
    │   {Update Apps Home immediately}
    │   └─ Insert at top of High Priority
    │
    ├─ Medium (0.3 - 0.7) →
    │   {Batch update Apps Home}
    │   └─ Add to Medium Priority queue
    │
    └─ Low (< 0.3) →
        {Minimal update}
        └─ Increment Low Priority count
    ↓

<Apps Home reflects change>
    ├─ High: Instant update
    ├─ Medium: 30-second batch
    └─ Low: 5-minute batch
    ↓

◆ User has Apps Home open?
    ├─ Yes → <Live update visible>
    └─ No → <Ready when opened>
    ↓

✓ Real-Time Update Complete
```

### 2.3 Bot Channel Summary Flow

```
START: Hourly timer triggers

{Aggregate high priority messages}
    ├─ Last hour's messages
    ├─ Score > 0.7 threshold
    └─ Group by channel
    ↓

◆ Any high priority messages?
    ├─ No → ✓ Skip summary
    └─ Yes ↓

{Format summary message}
    ├─ Header: "📊 Hourly Priority Summary"
    ├─ Time range: "9:00 AM - 10:00 AM"
    ├─ Count: "5 high-priority messages"
    └─ For each message:
        ├─ Sender @mention
        ├─ Channel #link
        ├─ Preview text (50 chars)
        └─ [View] link to message
    ↓

{Post to bot channel}
    ├─ Use bot token
    ├─ Target: #slackgrab channel
    └─ Format: Rich message blocks
    ↓

<Summary appears in channel>
    ↓

[User reviews summary]
    ↓

◆ Wants to view message?
    ├─ Yes → [Clicks message link]
    │        └─ Opens original message
    └─ No → ✓ Continue
    ↓

✓ Summary Delivered
```

## 3. Feedback Flows

### 3.1 Single Message Feedback

```
START: User notices incorrect priority

[User opens Apps Home]
    ↓

[Locates message in list]
    ↓

[Types /slackgrab feedback]
    ↓

<Command palette appears>
    └─ Shows: "feedback [too-low|good|too-high] [message-id]"
    ↓

[User types: /slackgrab feedback too-low]
    ↓

{Parse command}
    ├─ Extract feedback type
    ├─ Identify last viewed message
    └─ Validate parameters
    ↓

{Record feedback}
    ├─ Store in feedback table
    ├─ Link to message ID
    └─ Timestamp record
    ↓

{Update neural network}
    ├─ Generate training sample
    ├─ Add to replay buffer
    └─ Queue for next training
    ↓

<Confirmation message>
    └─ "Feedback recorded. The model will learn from this."
    ↓

{Background training}
    ├─ Wait for batch size
    ├─ Check resource availability
    └─ Run incremental training
    ↓

✓ Feedback Processed
```

### 3.2 Batch Feedback Flow

```
START: User wants to correct multiple messages

[User types /slackgrab batch-feedback]
    ↓

<Interactive message appears>
    ├─ "Select messages to provide feedback on"
    └─ Shows recent 10 messages with checkboxes
    ↓

[User selects 5 messages]
    ↓

[User clicks "Next"]
    ↓

<Feedback options appear>
    ├─ "How should these be prioritized?"
    ├─ [Too Low] [Good] [Too High] buttons
    └─ Selected messages listed
    ↓

[User clicks "Too High"]
    ↓

{Process batch feedback}
    ├─ Apply to all selected messages
    ├─ Generate training samples
    └─ Update replay buffer
    ↓

<Confirmation message>
    ├─ "Feedback applied to 5 messages"
    └─ [Undo] button available
    ↓

✓ Batch Feedback Complete
```

### 3.3 Undo Feedback Flow

```
START: User provided incorrect feedback

<Previous confirmation visible>
    └─ Contains [Undo] button
    ↓

[User clicks "Undo"]
    ↓

{Reverse feedback}
    ├─ Remove from feedback table
    ├─ Delete training samples
    └─ Restore original state
    ↓

<Undo confirmation>
    └─ "Feedback undone"
    ↓

◆ Want to provide correct feedback?
    ├─ Yes → [Return to feedback flow]
    └─ No → ✓ Complete
    ↓

✓ Undo Complete
```

## 4. Error Recovery Flows

### 4.1 Slack API Disconnection

```
START: API connection lost

{Connection error detected}
    ├─ HTTP 401/403/500
    ├─ Network timeout
    └─ Rate limit exceeded
    ↓

{Log error silently}
    └─ Write to local log file
    ↓

◆ Error type?
    ├─ Auth failure →
    │   {Clear invalid token}
    │   └─ Queue re-authentication
    │
    ├─ Rate limit →
    │   {Calculate backoff time}
    │   └─ ⟲ Retry after delay
    │
    └─ Network issue →
        {Activate circuit breaker}
        └─ ⟲ Retry with exponential backoff
    ↓

{Continue with cached data}
    ├─ Use last known priorities
    ├─ Show cached messages
    └─ Queue new operations
    ↓

<Apps Home shows stale indicator>
    └─ Small "offline" badge (subtle)
    ↓

⟲ {Attempt reconnection}
    └─ Every 30 seconds
    ↓

◆ Connection restored?
    ├─ No → ⟲ Continue retrying
    └─ Yes ↓

{Process queued operations}
    ├─ Sync missed messages
    ├─ Update priorities
    └─ Apply pending feedback
    ↓

<Remove offline indicator>
    ↓

✓ Connection Restored
```

### 4.2 Resource Exhaustion

```
START: High CPU/Memory usage

{Resource monitor detects issue}
    ├─ CPU > 80% for 30 seconds
    ├─ Memory > 3.5GB
    └─ GPU memory > 80%
    ↓

{Pause neural network training}
    ├─ Complete current batch
    ├─ Save checkpoint
    └─ Release GPU memory
    ↓

{Reduce operation intensity}
    ├─ Increase scoring cache TTL
    ├─ Batch messages more aggressively
    └─ Reduce update frequency
    ↓

{Continue inference only}
    ├─ Scoring continues (CPU mode)
    ├─ Feedback queued for later
    └─ Updates remain functional
    ↓

⟲ {Monitor resources}
    └─ Check every 60 seconds
    ↓

◆ Resources available?
    ├─ No → ⟲ Continue monitoring
    └─ Yes ↓

{Resume normal operation}
    ├─ Re-enable training
    ├─ Process queued feedback
    └─ Restore GPU acceleration
    ↓

✓ Normal Operation Restored
```

## 5. Learning Period Flows

### 5.1 First Week Learning

```
DAY 1: Initial Learning
{Show learning indicator}
    └─ "Learning your patterns..."
    ↓
{Basic scoring active}
    └─ Using general patterns
    ↓

DAY 2-3: Pattern Recognition
{Track interactions}
    ├─ Message read time
    ├─ Reply patterns
    └─ Reaction usage
    ↓
{Identify preferences}
    ├─ Important senders
    ├─ Critical channels
    └─ Time patterns
    ↓

DAY 4-5: Refinement
{Incorporate feedback}
    └─ Adjust weights
    ↓
{Improve accuracy}
    └─ Better predictions
    ↓

DAY 6-7: Stabilization
{Reach baseline accuracy}
    └─ 85%+ precision
    ↓
{Remove learning indicator}
    ↓

✓ Learning Period Complete
```

## 6. Advanced Flows

### 6.1 Slash Command Help

```
START: User types /slackgrab help

<Help message appears>
Commands:
• /slackgrab feedback [too-low|good|too-high] - Rate last message
• /slackgrab batch-feedback - Rate multiple messages
• /slackgrab status - View learning status
• /slackgrab undo - Undo last feedback

✓ Help Displayed
```

### 6.2 Performance Degradation

```
START: 5000 messages/day load

{Monitor performance metrics}
    ↓
◆ Latency increasing?
    ├─ Yes → {Enable aggressive caching}
    │        {Increase batch sizes}
    │        {Reduce UI update frequency}
    └─ No → ✓ Continue normal operation

✓ Performance Maintained
```

## User Flow Best Practices

### Principles Applied

1. **Minimal Steps**
   - Every flow optimized for fewest interactions
   - Maximum 3 clicks to any function
   - Direct navigation paths

2. **Clear Feedback**
   - Every action acknowledged
   - State changes visible
   - Undo always available

3. **Graceful Degradation**
   - Errors never block workflow
   - Cached data maintains function
   - Silent recovery attempts

4. **Progressive Disclosure**
   - Start with high priority only
   - Expand for more detail
   - Advanced features hidden

5. **Consistent Patterns**
   - Same interaction models throughout
   - Predictable behavior
   - Familiar Slack patterns

## Flow Validation Checklist

### Critical Path Testing
- [ ] Installation completes < 2 minutes
- [ ] OAuth flow succeeds first try
- [ ] Initial processing handles 30 days
- [ ] Apps Home loads < 1 second
- [ ] Priority updates in real-time
- [ ] Feedback applies immediately
- [ ] Errors recover silently
- [ ] Learning improves daily

### Edge Case Handling
- [ ] 2000 channels processed
- [ ] 5000 daily messages handled
- [ ] Offline mode functional
- [ ] Resource limits respected
- [ ] Rate limiting managed
- [ ] Undo always works
- [ ] Batch operations smooth

### Accessibility Validation
- [ ] Keyboard navigation complete
- [ ] Screen reader compatible
- [ ] Focus management correct
- [ ] Time limits sufficient
- [ ] Error messages clear
- [ ] Alternative paths available

## Next Steps

1. Prototype Apps Home layout based on priority flow
2. Design feedback interaction components
3. Create error state mockups
4. Test flow efficiency with users
5. Validate accessibility paths
6. Optimize for 5000 msg/day load

These user flows provide the blueprint for implementing SlackGrab's zero-configuration, intelligent prioritization system within Slack's native interface.
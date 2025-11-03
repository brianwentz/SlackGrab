# SlackGrab Interaction Patterns

## Overview

This document defines all interaction patterns for SlackGrab, focusing on the three-level feedback system, priority indicators, message navigation, and real-time updates. Every pattern is designed for maximum efficiency with zero configuration.

## Core Interaction Principles

### Design Philosophy
1. **Two-Second Rule**: Any feedback action must complete in under 2 seconds
2. **Single-Click Priority**: One click to provide feedback
3. **Visible State Changes**: Every action has immediate visual feedback
4. **Reversible Actions**: Undo always available within session
5. **Silent Success**: Confirmations are subtle, never modal

## Three-Level Feedback System

### Feedback Levels

#### Too Low
- **Meaning**: This message is more important than indicated
- **User Intent**: "I need to see messages like this sooner"
- **System Response**: Increase weight for similar patterns
- **Visual Feedback**: Brief upward animation, green confirmation
- **Keyboard Shortcut**: `Alt+L` when message focused

#### Good
- **Meaning**: Priority assessment is correct
- **User Intent**: "This is exactly right"
- **System Response**: Reinforce current patterns
- **Visual Feedback**: Pulse animation, blue confirmation
- **Keyboard Shortcut**: `Alt+G` when message focused

#### Too High
- **Meaning**: This message is less important than indicated
- **User Intent**: "This could wait"
- **System Response**: Decrease weight for similar patterns
- **Visual Feedback**: Brief downward animation, amber confirmation
- **Keyboard Shortcut**: `Alt+H` when message focused

### Feedback Interaction Methods

#### Method 1: Inline Buttons (Primary)
```
┌─────────────────────────────────────────────┐
│ Message Content                              │
│ [View] [Too Low] [Good] [Too High]          │
└─────────────────────────────────────────────┘

Interaction:
1. User clicks feedback button
2. Button shows pressed state (100ms)
3. Confirmation appears (fade in 200ms)
4. Button disabled briefly (1 second)
5. Undo option appears
```

#### Method 2: Slash Command (Power User)
```
Input: /slackgrab feedback too-low
Response: ✅ Feedback recorded for last viewed message [Undo]

Input: /slackgrab feedback good msg_123456
Response: ✅ Priority confirmed as correct [Undo]
```

#### Method 3: Keyboard Shortcuts (Accessibility)
```
Focus on message + Alt+L = Too Low
Focus on message + Alt+G = Good
Focus on message + Alt+H = Too High

Visual: Keyboard icon appears on focus
Audio: Screen reader announces "Press Alt+L for too low"
```

#### Method 4: Batch Feedback (Efficiency)
```
┌─────────────────────────────────────────────┐
│ Batch Feedback Mode                          │
├─────────────────────────────────────────────┤
│ ☑ Message 1: "Urgent customer issue..."     │
│ ☑ Message 2: "Server down in production..." │
│ ☐ Message 3: "Team lunch tomorrow..."       │
│ ☑ Message 4: "Critical security patch..."   │
│                                               │
│ Selected: 3 messages                         │
│ [Cancel] [Mark as Too Low] [Good] [Too High] │
└─────────────────────────────────────────────┘

Interaction:
1. Enter batch mode: /slackgrab batch-feedback
2. Checkbox appears next to each message
3. Select multiple messages (click or space)
4. Choose feedback level for all selected
5. Confirmation: "3 messages updated [Undo All]"
```

### Undo Functionality

#### Single Undo
```
After feedback:
"✅ Marked as too high [Undo]"

Click Undo:
"↩️ Feedback removed"

Timeout: Undo available for 30 seconds
```

#### Batch Undo
```
After batch feedback:
"✅ Updated 5 messages [Undo All]"

Click Undo All:
"↩️ All 5 changes reverted"
```

#### Undo Stack
```
Maximum depth: Last 10 actions
Access: /slackgrab undo (reverses last action)
        /slackgrab undo all (clears session)
```

## Priority Level Indicators

### Visual Hierarchy

#### High Priority Indicator
```
┌─────────────────────────────────────┐
│ 🔴 HIGH PRIORITY                    │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                       │
│ Strong visual weight                  │
│ Bold text, larger size                │
│ Red accent (accessible)               │
│ Always above the fold                 │
└─────────────────────────────────────┘

Properties:
- Font weight: 600 (semi-bold)
- Color: #CC3333 (WCAG AA compliant)
- Icon: 🔴 or 📍 (culturally neutral)
- Position: Top of list, never hidden
- Animation: Subtle pulse on new arrival
```

#### Medium Priority Indicator
```
┌─────────────────────────────────────┐
│ 🟡 Medium Priority                  │
│ ─────────────────────────────        │
│                                       │
│ Standard visual weight                │
│ Regular text size                     │
│ Neutral color                         │
└─────────────────────────────────────┘

Properties:
- Font weight: 400 (regular)
- Color: #1D1C1D (standard text)
- Icon: 🟡 or 📊
- Position: Middle section
- Animation: None
```

#### Low Priority Indicator
```
┌─────────────────────────────────────┐
│ 🟢 Low Priority                     │
│ · · · · · · · · · · · · ·           │
│                                       │
│ Reduced visual weight                 │
│ Smaller, muted text                   │
│ De-emphasized                         │
└─────────────────────────────────────┘

Properties:
- Font weight: 400 (regular)
- Color: #616061 (muted)
- Icon: 🟢 or 📉
- Position: Bottom, often collapsed
- Animation: None
```

### Priority Transitions

#### Priority Change Animation
```
When priority updates:
1. Fade out (200ms)
2. Move to new position (300ms ease-out)
3. Fade in (200ms)
4. Brief highlight (400ms pulse)

Total duration: 900ms
```

#### Real-time Update Pattern
```
New High Priority:
- Slide in from top
- Red pulse (2x, 400ms each)
- Auto-expand High section

Medium Update:
- Fade in place
- Count badge increments
- No section expansion

Low Update:
- Count only update
- No visual disruption
```

## Message Navigation Patterns

### Click-to-Jump Navigation

#### Direct Message Navigation
```
[View Message] button clicked
    ↓
Open Slack desktop/web
    ↓
Navigate to exact channel
    ↓
Scroll to specific message
    ↓
Highlight message briefly (2s yellow background)
    ↓
Focus moves to message
```

#### Deep Link Format
```
slack://channel?team={TEAM_ID}&id={CHANNEL_ID}&message={TIMESTAMP}

Fallback for web:
https://app.slack.com/client/{TEAM_ID}/{CHANNEL_ID}/thread/{TIMESTAMP}
```

### Keyboard Navigation

#### Navigation Keys
```
Tab         - Next interactive element
Shift+Tab   - Previous element
Enter       - Activate button/link
Space       - Toggle checkbox/expand section
Arrow keys  - Navigate within list
Escape      - Close modal/cancel action
```

#### Focus Management
```
Focus Order:
1. High Priority header
2. First high priority message
   a. Message content
   b. View button
   c. Feedback buttons
3. Next message...
4. Load more button
5. Medium Priority header
6. Low Priority header
```

### Touch Interactions (Future Tablet Support)

#### Touch Targets
```
Minimum size: 44x44 pixels
Spacing: 8px between targets
Touch feedback: Ripple effect
Long press: Show context menu
Swipe right: Mark as good
Swipe left: Show feedback options
```

## Real-Time Update Patterns

### Update Frequencies

#### High Priority Updates
```
Trigger: New message scored > 0.7
Latency: < 1 second
Method: WebSocket push or immediate poll
UI Update: Instant with animation
```

#### Medium Priority Updates
```
Trigger: New message scored 0.3-0.7
Latency: 30 seconds (batched)
Method: Batch update via API
UI Update: Smooth fade-in
```

#### Low Priority Updates
```
Trigger: New message scored < 0.3
Latency: 5 minutes (highly batched)
Method: Background sync
UI Update: Count only
```

### Update Indicators

#### Live Update Badge
```
┌──────────────┐
│ 🔵 Live      │  <- Pulsing blue dot
└──────────────┘

States:
- Blue pulse: Connected, receiving updates
- Gray: Cached data, reconnecting
- Hidden: Fully offline mode
```

#### New Message Indicator
```
┌─────────────────────────────────────┐
│ 📍 High Priority (12 messages)      │
│     ↑ 3 new since you looked        │
└─────────────────────────────────────┘

Behavior:
- Shows count of new messages
- Clears when section viewed
- Persists across section collapse
```

## Loading States

### Initial Load
```
┌─────────────────────────────────────┐
│ ⏳ Loading your priorities...        │
│                                       │
│ [Skeleton loader animation]          │
│ ████████████░░░░░░░░░░░░            │
└─────────────────────────────────────┘

Duration: 1-2 seconds typical
Fallback: Show cached if > 3 seconds
```

### Pagination Loading
```
[Load More Messages]
    ↓ (clicked)
[⟲ Loading...] <- Spinner in button
    ↓
[New messages appear]
[Load More Messages] <- Button returns
```

### Background Update
```
No visible loading state
Updates appear seamlessly
Only show if error occurs
```

## Error States

### Connection Error
```
┌─────────────────────────────────────┐
│ ⚠️ Connection temporarily lost       │
│ Showing priorities from 2 min ago    │
│ Will reconnect automatically...      │
└─────────────────────────────────────┘

Behavior:
- Non-blocking warning
- Auto-dismiss when connected
- Cache remains interactive
```

### Feedback Error
```
After feedback attempt:
"⚠️ Couldn't save feedback. [Retry] [Dismiss]"

Behavior:
- Inline error message
- Retry maintains context
- Dismiss removes message
- Auto-retry after 30 seconds
```

## Batch Operations

### Batch Selection Pattern
```
Enable: Shift+Click or /slackgrab batch
Visual: Checkboxes appear

Selection Methods:
- Click: Toggle individual
- Shift+Click: Range select
- Ctrl+A: Select all visible
- Escape: Cancel batch mode

Actions appear after selection:
[Clear] [Mark Too Low] [Good] [Too High]
```

### Bulk Action Confirmation
```
Before: "Mark 12 messages as Too High?"
        [Cancel] [Confirm]

After:  "✅ Updated 12 messages [Undo All]"
```

## Responsive Behavior

### Breakpoint Behaviors

#### Mobile (320-767px)
```
- Single column layout
- Feedback buttons stack vertically
- Truncate message to 50 chars
- Swipe gestures enabled
- Larger touch targets (48x48)
```

#### Tablet (768-1023px)
```
- Single column, wider
- Feedback buttons horizontal
- Truncate message to 100 chars
- Touch and mouse support
- Standard touch targets (44x44)
```

#### Desktop (1024px+)
```
- Optimal single column
- All buttons horizontal
- Show 150 chars of message
- Mouse/keyboard primary
- Compact touch targets (36x36)
```

## Accessibility Patterns

### Screen Reader Announcements

#### Priority Changes
```
"Message from Sarah Chen moved from Medium to High priority"
"5 new high priority messages available"
"Feedback recorded: message marked as too low"
```

#### Navigation
```
"High priority section, 12 messages, expanded"
"Message 1 of 12, from Marcus Rodriguez"
"View message button, press Enter to navigate"
```

### Focus Management

#### Focus Trap Prevention
```
Modal/overlay: Never used
Focus flow: Natural tab order
Escape key: Always provides exit
Focus visible: 2px blue outline
```

#### Focus Restoration
```
After action:
- Feedback: Return to message
- Navigation: Return to list
- Undo: Return to affected item
- Load more: Focus first new item
```

## Gesture Support (Planned)

### Mouse Gestures
```
Hover: Show feedback buttons
Right-click: Context menu
Drag: Multi-select messages
Scroll: Natural pagination
```

### Future Touch Gestures
```
Swipe right: Mark as good
Swipe left: Feedback options
Pinch: Zoom message text
Long press: Select mode
Pull down: Refresh
```

## Performance Patterns

### Optimistic UI Updates
```
1. User provides feedback
2. UI updates immediately
3. Request sent to backend
4. If success: Keep UI state
5. If failure: Revert with message
```

### Debouncing
```
Search: 300ms delay
Feedback: Immediate (no debounce)
Navigation: Immediate
Updates: Batched per schedule
```

### Throttling
```
Scroll events: 100ms throttle
Resize: 200ms throttle
API calls: 50/minute max
UI updates: 60fps target
```

## Interaction Validation

### Before Implementation
- [ ] All interactions < 3 clicks
- [ ] Feedback < 2 seconds to complete
- [ ] Undo always available
- [ ] Keyboard accessible
- [ ] Touch targets 44x44px minimum

### After Implementation
- [ ] Test with 5000 messages/day
- [ ] Verify real-time updates
- [ ] Validate keyboard navigation
- [ ] Test with screen readers
- [ ] Measure interaction times

These interaction patterns ensure SlackGrab provides a fluid, efficient, and accessible experience that enhances rather than interrupts the natural Slack workflow.
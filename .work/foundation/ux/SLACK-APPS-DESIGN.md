# SlackGrab Slack Apps Design

## Overview

This document provides comprehensive design specifications for SlackGrab's integration with Slack's Apps platform. All designs use native Slack UI components, follow Slack's design guidelines, and require zero configuration from users.

## Apps Home Tab Design

### Layout Structure

```
┌─────────────────────────────────────────────────┐
│  SlackGrab  [Learning indicator: optional]       │
├─────────────────────────────────────────────────┤
│                                                   │
│  📍 High Priority (12 messages)                  │
│  ─────────────────────────────────────           │
│                                                   │
│  ┌─────────────────────────────────────────┐    │
│  │ @sarah.chen                             │    │
│  │ #product-team • 2 min ago               │    │
│  │ "Need the Q4 roadmap updates ASAP..."   │    │
│  │ [View Message] [Too Low] [Good] [Too Hi]│    │
│  └─────────────────────────────────────────┘    │
│                                                   │
│  ┌─────────────────────────────────────────┐    │
│  │ @marcus.rodriguez                       │    │
│  │ #engineering • 5 min ago                │    │
│  │ "Production deployment blocked on..."    │    │
│  │ [View Message] [Too Low] [Good] [Too Hi]│    │
│  └─────────────────────────────────────────┘    │
│                                                   │
│  [Load More High Priority Messages]              │
│                                                   │
│  ─────────────────────────────────────           │
│                                                   │
│  📊 Medium Priority (47 messages) [Expand ▼]     │
│                                                   │
│  ─────────────────────────────────────           │
│                                                   │
│  📉 Low Priority (231 messages) [Expand ▼]       │
│                                                   │
└─────────────────────────────────────────────────┘
```

### Component Specifications

#### Header Section
```json
{
  "type": "header",
  "text": {
    "type": "plain_text",
    "text": "SlackGrab",
    "emoji": true
  }
}
```

#### Learning Indicator (First 7 Days)
```json
{
  "type": "context",
  "elements": [
    {
      "type": "mrkdwn",
      "text": "🧠 _Learning your patterns..._"
    }
  ]
}
```

#### Priority Section Headers
```json
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "*📍 High Priority* (12 messages)"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "Expand",
      "emoji": true
    },
    "action_id": "toggle_high_priority"
  }
}
```

#### Message Card Component
```json
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "*@sarah.chen*\n#product-team • 2 min ago\n\"Need the Q4 roadmap updates ASAP for the board meeting tomorrow morning...\""
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Message",
      "emoji": true
    },
    "style": "primary",
    "action_id": "view_message",
    "value": "C123456789.1234567890"
  }
}
```

#### Feedback Buttons Row
```json
{
  "type": "actions",
  "elements": [
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "text": "Too Low",
        "emoji": true
      },
      "action_id": "feedback_too_low",
      "value": "msg_123456"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "text": "Good",
        "emoji": true
      },
      "style": "primary",
      "action_id": "feedback_good",
      "value": "msg_123456"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "text": "Too High",
        "emoji": true
      },
      "action_id": "feedback_too_high",
      "value": "msg_123456"
    }
  ]
}
```

### Priority Level Designs

#### High Priority Messages
- **Visual Weight**: Bold sender names, prominent display
- **Color**: Use Slack's danger/urgent color sparingly
- **Icon**: 📍 (pushpin) or 🔴 (red circle)
- **Display**: Always expanded on load
- **Limit**: Show 10 initially, paginate rest
- **Update**: Real-time updates
- **Feedback**: Inline buttons always visible

#### Medium Priority Messages
- **Visual Weight**: Standard text weight
- **Color**: Slack's default text color
- **Icon**: 📊 (bar chart) or 🟡 (yellow circle)
- **Display**: Collapsed by default
- **Limit**: Show 20 when expanded
- **Update**: Batch updates every 30 seconds
- **Feedback**: Buttons appear on hover/focus

#### Low Priority Messages
- **Visual Weight**: Subdued, smaller text
- **Color**: Slack's muted text color
- **Icon**: 📉 (chart decreasing) or 🟢 (green circle)
- **Display**: Collapsed by default
- **Limit**: Show count only, list on demand
- **Update**: Batch updates every 5 minutes
- **Feedback**: Available but de-emphasized

### Responsive Behavior

#### Message Truncation
```
Short (Mobile): 50 characters
Medium (Tablet): 100 characters
Long (Desktop): 150 characters

Example:
"Need the Q4 roadmap updates ASAP for the board..."
```

#### Pagination Controls
```json
{
  "type": "button",
  "text": {
    "type": "plain_text",
    "text": "Load More Messages"
  },
  "action_id": "load_more",
  "value": "high_priority_page_2"
}
```

### Empty States

#### No High Priority Messages
```
┌─────────────────────────────────────────┐
│  📍 High Priority                       │
│  ─────────────────────────               │
│                                           │
│     ✨ All caught up!                    │
│     No high priority messages right now  │
│                                           │
└─────────────────────────────────────────┘
```

#### Learning Period (First Day)
```
┌─────────────────────────────────────────┐
│  🧠 Learning Your Patterns               │
│  ─────────────────────────               │
│                                           │
│  SlackGrab is analyzing your message     │
│  patterns. Initial priorities may not    │
│  be perfect, but they'll improve quickly │
│  as the system learns your preferences.  │
│                                           │
│  You can help by providing feedback on   │
│  any message using the buttons below.    │
│                                           │
└─────────────────────────────────────────┘
```

## Bot Messages Design

### Hourly Summary Format

```
┌─────────────────────────────────────────────┐
│ 📊 Hourly Priority Summary                   │
│ 10:00 AM - 11:00 AM                         │
├─────────────────────────────────────────────┤
│                                               │
│ You have 5 high-priority messages:           │
│                                               │
│ 1. @ceo in #leadership                       │
│    "Urgent: Board meeting prep needed..."    │
│    View: <link to message>                   │
│                                               │
│ 2. @customer in #support-critical            │
│    "System is down for our entire..."        │
│    View: <link to message>                   │
│                                               │
│ 3. @sarah.chen in #product-team              │
│    "Blocker: Need approval on pricing..."    │
│    View: <link to message>                   │
│                                               │
│ [Provide Feedback] [Adjust Frequency]        │
└─────────────────────────────────────────────┘
```

### Bot Message Blocks

#### Summary Header
```json
{
  "type": "header",
  "text": {
    "type": "plain_text",
    "text": "📊 Hourly Priority Summary",
    "emoji": true
  }
}
```

#### Time Range Context
```json
{
  "type": "context",
  "elements": [
    {
      "type": "plain_text",
      "text": "10:00 AM - 11:00 AM"
    }
  ]
}
```

#### Message Entry
```json
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "*1.* <@U123456> in <#C123456|leadership>\n\"Urgent: Board meeting prep needed...\"\n<https://slack.com/archives/C123456/p1234567890|View message>"
  }
}
```

### Welcome Message

```
┌─────────────────────────────────────────────┐
│ 👋 Welcome to SlackGrab!                     │
├─────────────────────────────────────────────┤
│                                               │
│ I'm here to help you focus on what matters.  │
│                                               │
│ ✅ I'm now monitoring your messages          │
│ ✅ Learning your communication patterns      │
│ ✅ No configuration needed                   │
│                                               │
│ 📍 View priorities: Click my name in Apps    │
│ 💬 Provide feedback: /slackgrab feedback     │
│ ❓ Get help: /slackgrab help                │
│                                               │
│ I'll get better at understanding what's      │
│ important to you over the next few days.     │
└─────────────────────────────────────────────┘
```

## Slash Commands Design

### Command Structure

#### /slackgrab (root command)
```
/slackgrab
Response: "Use /slackgrab [feedback|status|help|undo]"
```

#### /slackgrab feedback
```
/slackgrab feedback [too-low|good|too-high] [message-id]

Examples:
/slackgrab feedback too-low
/slackgrab feedback good msg_123456
/slackgrab feedback too-high
```

#### /slackgrab batch-feedback
```
/slackgrab batch-feedback

Opens interactive message selector:
┌─────────────────────────────────────────┐
│ Select messages to rate:                 │
│                                           │
│ ☐ @sarah - "Q4 roadmap updates..."      │
│ ☐ @marcus - "Deployment blocked..."      │
│ ☐ @jennifer - "Customer escalation..."   │
│ ☐ @david - "Team standup notes..."       │
│ ☐ @alex - "Server monitoring alert..."   │
│                                           │
│ [Cancel] [Next: Choose Rating]           │
└─────────────────────────────────────────┘
```

#### /slackgrab status
```
/slackgrab status

Response:
"SlackGrab Status:
• Learning period: Day 3 of 7
• Messages processed today: 1,247
• High priority: 23 (4%)
• Medium priority: 178 (14%)
• Low priority: 1,046 (82%)
• Feedback provided: 12 corrections"
```

#### /slackgrab help
```
/slackgrab help

Response:
"SlackGrab Commands:
• feedback [rating] - Rate message priority
• batch-feedback - Rate multiple messages
• status - View current statistics
• undo - Undo last feedback
• help - Show this message

View priorities: Click SlackGrab in Apps sidebar"
```

### Interactive Components

#### Feedback Modal
```json
{
  "type": "modal",
  "title": {
    "type": "plain_text",
    "text": "Rate Message Priority"
  },
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "How should this message be prioritized?"
      }
    },
    {
      "type": "actions",
      "elements": [
        {
          "type": "button",
          "text": {
            "type": "plain_text",
            "text": "📉 Too Low"
          },
          "style": "danger",
          "action_id": "rate_too_low"
        },
        {
          "type": "button",
          "text": {
            "type": "plain_text",
            "text": "✅ Good"
          },
          "style": "primary",
          "action_id": "rate_good"
        },
        {
          "type": "button",
          "text": {
            "type": "plain_text",
            "text": "📈 Too High"
          },
          "action_id": "rate_too_high"
        }
      ]
    }
  ]
}
```

#### Confirmation Messages
```json
{
  "type": "ephemeral",
  "text": "✅ Feedback recorded. The model will learn from this.",
  "attachments": [
    {
      "text": "Changed priority from High to Medium",
      "color": "good",
      "actions": [
        {
          "type": "button",
          "text": "Undo",
          "action_id": "undo_feedback"
        }
      ]
    }
  ]
}
```

## Visual Design Guidelines

### Color Palette
```
High Priority:   #CC3333 (Slack danger) - use sparingly
Medium Priority: #1D1C1D (Slack primary text)
Low Priority:    #616061 (Slack muted text)
Success:         #2F7D32 (Slack success)
Learning:        #1164A3 (Slack info)
Background:      #FFFFFF (Slack surface)
Borders:         #DDDDDD (Slack border)
```

### Typography
```
Headers:     Slack default bold, 16px
Body:        Slack default, 14px
Metadata:    Slack muted, 12px
Buttons:     Slack button text, 14px
```

### Spacing
```
Section padding:    16px
Message spacing:    12px between cards
Button spacing:     8px between buttons
Line height:        1.5x
```

### Icons
```
High Priority:    📍 or 🔴
Medium Priority:  📊 or 🟡
Low Priority:     📉 or 🟢
Learning:         🧠
Success:          ✅
Warning:          ⚠️
Info:             ℹ️
```

## Accessibility Specifications

### Keyboard Navigation
```
Tab Order:
1. High Priority section header
2. First high priority message
3. View Message button
4. Feedback buttons (Too Low, Good, Too High)
5. Next message...
6. Load More button
7. Medium Priority section header
8. Low Priority section header
```

### Screen Reader Annotations
```html
<section role="region" aria-label="High Priority Messages">
  <h2>High Priority (12 messages)</h2>
  <article aria-label="Message from Sarah Chen">
    <p>Sarah Chen in product-team channel, 2 minutes ago</p>
    <p>Need the Q4 roadmap updates ASAP...</p>
    <button aria-label="View full message in channel">View Message</button>
    <button aria-label="Mark priority as too low">Too Low</button>
    <button aria-label="Mark priority as correct">Good</button>
    <button aria-label="Mark priority as too high">Too High</button>
  </article>
</section>
```

### Focus Indicators
```css
:focus {
  outline: 2px solid #1164A3;
  outline-offset: 2px;
  border-radius: 4px;
}
```

### Color Contrast
```
Text on background:     7:1 (exceeds WCAG AAA)
Muted text:            4.5:1 (meets WCAG AA)
Button text:           8:1 (exceeds WCAG AAA)
Icons:                 3:1 (meets WCAG AA for graphics)
```

## Interaction States

### Loading States
```
┌─────────────────────────────────────────┐
│  ⏳ Loading priorities...                │
│                                           │
│  [Animated Slack skeleton loader]        │
│                                           │
└─────────────────────────────────────────┘
```

### Error States
```
┌─────────────────────────────────────────┐
│  ℹ️ Temporarily offline                  │
│                                           │
│  Showing cached priorities from 5 min ago│
│  Will reconnect automatically            │
│                                           │
└─────────────────────────────────────────┘
```

### Success States
```
┌─────────────────────────────────────────┐
│  ✅ Feedback recorded                    │
│  [Undo]                                   │
└─────────────────────────────────────────┘
```

## Performance Optimizations

### Progressive Loading
1. Load High Priority first (< 500ms)
2. Lazy load Medium/Low sections
3. Paginate after 10 messages per section
4. Virtual scrolling for large lists

### Caching Strategy
```
High Priority:   Real-time updates
Medium Priority: 30-second cache
Low Priority:    5-minute cache
User feedback:   Immediate UI update
```

### Update Frequency
```
New high priority:     Immediate push
Medium updates:        30-second batch
Low updates:          5-minute batch
Feedback response:     < 100ms UI update
```

## Implementation Notes

### Slack Block Kit Limits
- Maximum 100 blocks per message
- Maximum 3000 characters per text field
- Maximum 25 options in select menus
- Maximum 5 actions per block

### API Considerations
- Use views.publish for Apps Home updates
- Batch updates to avoid rate limits
- Cache view state to minimize API calls
- Use lazy loading for large message lists

### Platform Constraints
- No custom CSS/JavaScript
- Limited to Slack's component library
- Must work within Block Kit limitations
- Respect Slack's rate limits (50 req/min)

## Design Validation Checklist

### Visual Design
- [ ] Follows Slack's visual guidelines
- [ ] Uses native Slack components only
- [ ] Maintains visual hierarchy
- [ ] Provides clear priority differentiation
- [ ] Supports light and dark modes

### Interaction Design
- [ ] Maximum 3 clicks to any action
- [ ] Feedback mechanism intuitive
- [ ] Undo always available
- [ ] Loading states clear
- [ ] Error states non-disruptive

### Accessibility
- [ ] Keyboard navigable
- [ ] Screen reader compatible
- [ ] Color contrast sufficient
- [ ] Focus indicators visible
- [ ] Touch targets 44x44px minimum

### Performance
- [ ] Apps Home loads < 1 second
- [ ] Updates appear real-time
- [ ] Handles 5000 messages/day
- [ ] Pagination works smoothly
- [ ] Cache strategy effective

This comprehensive design specification ensures SlackGrab integrates seamlessly with Slack's Apps platform while providing an intuitive, accessible, and performant user experience.
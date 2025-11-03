# SlackGrab Visual Design System

## Overview

This document defines SlackGrab's visual design system, ensuring consistency with Slack's design language while providing clear priority differentiation. All designs prioritize accessibility, performance, and zero visual configuration.

## Design Principles

### Visual Philosophy
1. **Invisible Until Needed**: Blend seamlessly with Slack's interface
2. **Clear Hierarchy**: Priority levels instantly distinguishable
3. **Accessible First**: Color is enhancement, not requirement
4. **Performance Conscious**: Minimal visual overhead
5. **Consistent Experience**: Same visual language throughout

## Color System

### Primary Palette

#### Priority Colors
```
High Priority
- Primary: #D32F2F (Red-orange, urgent)
- Background: #FFEBEE (Light red tint)
- Border: #CC3333 (Darker red)
- Text: #B71C1C (Dark red for contrast)
- WCAG AA: ✓ (4.5:1 on white)

Medium Priority
- Primary: #1D1C1D (Slack default black)
- Background: #FFFFFF (White)
- Border: #DDDDDD (Slack border gray)
- Text: #1D1C1D (Slack primary text)
- WCAG AA: ✓ (15:1 on white)

Low Priority
- Primary: #616061 (Slack muted gray)
- Background: #F8F8F8 (Light gray)
- Border: #E8E8E8 (Lighter border)
- Text: #616061 (Muted text)
- WCAG AA: ✓ (4.7:1 on white)
```

#### System Colors
```
Success
- Primary: #2F7D32 (Green)
- Light: #4CAF50
- Dark: #1B5E20
- WCAG AA: ✓ (4.9:1)

Warning
- Primary: #F57C00 (Orange)
- Light: #FFB74D
- Dark: #E65100
- WCAG AA: ✓ (4.5:1)

Info
- Primary: #1164A3 (Slack blue)
- Light: #2196F3
- Dark: #0D47A1
- WCAG AA: ✓ (4.8:1)

Error
- Primary: #D32F2F (Red)
- Light: #EF5350
- Dark: #C62828
- WCAG AA: ✓ (4.5:1)
```

#### Neutral Colors
```
Background
- Surface: #FFFFFF
- Canvas: #F8F8F8
- Overlay: rgba(0,0,0,0.5)

Text
- Primary: #1D1C1D (Slack primary)
- Secondary: #616061 (Slack muted)
- Disabled: #AAAAAA
- Inverse: #FFFFFF

Borders
- Default: #DDDDDD
- Focus: #1164A3
- Subtle: #E8E8E8
- Strong: #616061
```

### Color Usage Guidelines

#### Priority Indication
```css
/* High Priority */
.high-priority {
  color: #D32F2F;
  border-left: 4px solid #D32F2F;
  background: linear-gradient(90deg, #FFEBEE 0%, #FFFFFF 100%);
}

/* Medium Priority */
.medium-priority {
  color: #1D1C1D;
  border-left: 4px solid #1D1C1D;
  background: #FFFFFF;
}

/* Low Priority */
.low-priority {
  color: #616061;
  border-left: 4px solid #E8E8E8;
  background: #F8F8F8;
}
```

#### Accessibility Considerations
- Never use color alone to convey information
- Always pair with icons, text, or patterns
- Maintain 4.5:1 contrast ratio for normal text
- Maintain 3:1 contrast ratio for large text and UI components

## Typography

### Font Stack
```css
font-family: Slack-Lato, Slack-Fractions, appleLogo, sans-serif;
/* Fallback */
font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
```

### Type Scale
```
Display (Rare)
- Size: 28px
- Line Height: 32px
- Weight: 700 (Bold)
- Usage: Major section headers only

Headline
- Size: 20px
- Line Height: 28px
- Weight: 700 (Bold)
- Usage: App name, main headers

Title
- Size: 16px
- Line Height: 24px
- Weight: 600 (Semi-bold)
- Usage: Section headers, priority labels

Body
- Size: 14px
- Line Height: 22px
- Weight: 400 (Regular)
- Usage: Message content, standard text

Caption
- Size: 12px
- Line Height: 18px
- Weight: 400 (Regular)
- Usage: Metadata, timestamps, counts

Small
- Size: 11px
- Line Height: 16px
- Weight: 400 (Regular)
- Usage: Legal text, version numbers
```

### Typography Hierarchy

#### Message Card Typography
```
Sender Name    - 14px, 600 weight, #1D1C1D
Channel Name   - 12px, 400 weight, #616061
Timestamp      - 12px, 400 weight, #616061
Message Text   - 14px, 400 weight, #1D1C1D
Button Text    - 14px, 500 weight, varies by type
```

#### Priority Section Headers
```
High Priority   - 16px, 700 weight, #D32F2F
Medium Priority - 16px, 600 weight, #1D1C1D
Low Priority    - 16px, 400 weight, #616061
Count Badge     - 14px, 500 weight, matching priority
```

## Iconography

### Priority Icons
```
High Priority:   🔴 or 📍 (pin)
Medium Priority: 🟡 or 📊 (chart)
Low Priority:    🟢 or 📉 (declining chart)

Alternatives for better accessibility:
High:   ▲ (up triangle)
Medium: ■ (square)
Low:    ▼ (down triangle)
```

### System Icons
```
Success:     ✅ or ✓
Error:       ❌ or ✗
Warning:     ⚠️ or !
Info:        ℹ️ or i
Loading:     ⟲ or ◌
Expand:      ▼ or ⌄
Collapse:    ▲ or ⌃
Settings:    ⚙️ or ☰
Help:        ❓ or ?
Undo:        ↩️ or ⟲
```

### Icon Usage Rules
- Always pair icons with text labels
- Ensure icons have sufficient contrast (3:1 minimum)
- Provide aria-labels for icon-only buttons
- Use consistent icon set throughout
- Avoid culture-specific symbols

## Spacing System

### Base Unit
```
Base unit: 4px
Used for all spacing calculations
```

### Spacing Scale
```
Space-0:  0px
Space-1:  4px  (Base)
Space-2:  8px  (Base × 2)
Space-3:  12px (Base × 3)
Space-4:  16px (Base × 4)
Space-5:  20px (Base × 5)
Space-6:  24px (Base × 6)
Space-8:  32px (Base × 8)
Space-10: 40px (Base × 10)
Space-12: 48px (Base × 12)
```

### Component Spacing

#### Message Card
```
Padding:         16px
Margin between:  12px
Internal gap:    8px
Button spacing:  8px
```

#### Sections
```
Header padding:  16px 16px 12px 16px
Content padding: 0 16px 16px 16px
Section gap:     24px
Divider margin:  16px 0
```

#### Buttons
```
Padding:        8px 12px
Margin between: 8px
Icon gap:       4px
Min height:     32px
Min width:      64px
```

## Layout Grid

### Apps Home Grid
```
Container
- Max width: 100% (Slack controlled)
- Padding: 20px
- Background: #FFFFFF

Content Area
- Single column layout
- Max width: 680px (optimal reading)
- Centered in container
- Responsive padding: 16-20px
```

### Component Layout
```
Message Card
┌──────────────────────────────────┐
│ [8px]                            │
│ Avatar  [12px]  Content          │
│ (32px)          ┌──────────────┐ │
│                 │ Sender       │ │
│                 │ Channel·Time │ │
│                 │ Message...   │ │
│                 └──────────────┘ │
│                                  │
│ [Buttons Row - 8px spacing]      │
│ [8px]                            │
└──────────────────────────────────┘
```

## Visual Hierarchy

### Priority Levels Visual Weight
```
High Priority:   100% - Maximum visual emphasis
- Bold typography
- Strong color
- Larger spacing
- Always visible

Medium Priority: 70% - Standard emphasis
- Regular typography
- Neutral color
- Standard spacing
- Collapsible

Low Priority:    40% - Minimal emphasis
- Regular typography
- Muted color
- Compact spacing
- Hidden by default
```

### Z-Index Layers
```
Base:        0    - Page content
Cards:       1    - Message cards
Dropdowns:   100  - Select menus
Modals:      1000 - Modal overlays (avoided)
Tooltips:    1100 - Hover tooltips
```

## Component Styling

### Buttons

#### Primary Button (High Action)
```css
.btn-primary {
  background: #1164A3;
  color: #FFFFFF;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  font-weight: 500;
}

.btn-primary:hover {
  background: #0D47A1;
}

.btn-primary:focus {
  outline: 2px solid #1164A3;
  outline-offset: 2px;
}
```

#### Secondary Button (Standard Action)
```css
.btn-secondary {
  background: #FFFFFF;
  color: #1D1C1D;
  border: 1px solid #DDDDDD;
  padding: 8px 16px;
  border-radius: 4px;
}

.btn-secondary:hover {
  background: #F8F8F8;
}
```

#### Feedback Buttons
```css
.btn-too-low {
  background: #FFF3E0;
  color: #F57C00;
  border: 1px solid #FFB74D;
}

.btn-good {
  background: #E8F5E9;
  color: #2F7D32;
  border: 1px solid #4CAF50;
}

.btn-too-high {
  background: #FFEBEE;
  color: #D32F2F;
  border: 1px solid #EF5350;
}
```

### Cards

#### Message Card
```css
.message-card {
  background: #FFFFFF;
  border: 1px solid #DDDDDD;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  transition: box-shadow 0.2s ease;
}

.message-card:hover {
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.message-card.high-priority {
  border-left: 4px solid #D32F2F;
}
```

### Focus States
```css
:focus {
  outline: 2px solid #1164A3;
  outline-offset: 2px;
  border-radius: 4px;
}

:focus:not(:focus-visible) {
  outline: none; /* Remove for mouse users */
}

:focus-visible {
  outline: 2px solid #1164A3;
  outline-offset: 2px;
}
```

## Animation & Motion

### Timing Functions
```css
--ease-out: cubic-bezier(0.0, 0.0, 0.2, 1);
--ease-in: cubic-bezier(0.4, 0.0, 1, 1);
--ease-in-out: cubic-bezier(0.4, 0.0, 0.2, 1);
--spring: cubic-bezier(0.175, 0.885, 0.32, 1.275);
```

### Animation Durations
```css
--instant: 0ms;
--fast: 200ms;
--normal: 300ms;
--slow: 500ms;
```

### Common Animations

#### Fade In
```css
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.fade-in {
  animation: fadeIn var(--fast) var(--ease-out);
}
```

#### Slide Down
```css
@keyframes slideDown {
  from {
    transform: translateY(-8px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
```

#### Priority Pulse
```css
@keyframes priorityPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.new-high-priority {
  animation: priorityPulse 0.4s ease-in-out 2;
}
```

### Motion Guidelines
- Keep animations under 300ms for responsiveness
- Use ease-out for enter animations
- Use ease-in for exit animations
- Respect prefers-reduced-motion setting
- Avoid animations during high load

## Responsive Design

### Breakpoints
```css
Mobile:  320px - 767px
Tablet:  768px - 1023px
Desktop: 1024px - 1439px
Wide:    1440px+
```

### Responsive Typography
```css
/* Mobile */
@media (max-width: 767px) {
  .message-text {
    font-size: 13px;
    line-height: 20px;
  }
}

/* Desktop */
@media (min-width: 1024px) {
  .message-text {
    font-size: 14px;
    line-height: 22px;
  }
}
```

### Responsive Spacing
```css
/* Mobile */
@media (max-width: 767px) {
  .container { padding: 12px; }
  .message-card { padding: 12px; }
}

/* Desktop */
@media (min-width: 1024px) {
  .container { padding: 20px; }
  .message-card { padding: 16px; }
}
```

## Dark Mode Support

### Dark Mode Colors
```css
@media (prefers-color-scheme: dark) {
  :root {
    --bg-primary: #1A1D21;
    --bg-secondary: #232529;
    --text-primary: #D1D2D3;
    --text-secondary: #ABADB0;
    --border: #363940;
  }
}
```

### Dark Mode Adjustments
- Reduce color saturation by 20%
- Increase contrast for text
- Use darker backgrounds for cards
- Adjust shadows to be more subtle
- Ensure focus indicators remain visible

## Visual Accessibility

### Color Contrast Requirements
```
Normal Text (14px):     4.5:1 minimum
Large Text (18px+):     3:1 minimum
UI Components:          3:1 minimum
Focus Indicators:       3:1 minimum
Disabled States:        No requirement (but visible)
```

### Visual Indicators
- Never rely on color alone
- Always provide text labels
- Include patterns or icons
- Ensure focus is always visible
- Support high contrast mode

## Windows 11 Integration

### System Theme Respect
```css
/* Follow Windows accent color */
@media (prefers-color-scheme: light) {
  :root {
    --accent: env(--windows-accent-color, #1164A3);
  }
}
```

### Native Controls
- Use Windows 11 button styles
- Respect system font size
- Follow Windows spacing guidelines
- Support Windows high contrast mode

## Performance Considerations

### Visual Performance
- Minimize repaints and reflows
- Use CSS transforms for animations
- Implement virtual scrolling for long lists
- Lazy load images and icons
- Use CSS containment for complex layouts

### Resource Optimization
```css
/* Use will-change sparingly */
.animating {
  will-change: transform;
}

/* Remove after animation */
.animation-done {
  will-change: auto;
}
```

## Implementation Checklist

### Pre-Implementation
- [ ] Colors meet WCAG AA standards
- [ ] Typography scale defined
- [ ] Spacing system consistent
- [ ] Icons accessible
- [ ] Dark mode considered

### Post-Implementation
- [ ] Test with colorblind simulator
- [ ] Verify contrast ratios
- [ ] Check focus indicators
- [ ] Test animations performance
- [ ] Validate responsive behavior

This visual design system ensures SlackGrab maintains a consistent, accessible, and performant appearance while seamlessly integrating with Slack's native interface.
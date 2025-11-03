# Claude Code Session Guide for SlackGrab

## Project Overview

**SlackGrab** is a Windows 11+ application that uses neural network technology to intelligently prioritize Slack messages through continuous local learning. The system watches user behavior (reading time, replies, reactions) to learn what messages are important and surfaces them via Slack Apps integration.

**Tech Stack**: Java 25, SQLite, Slack Apps API, Neural Network (custom implementation)
**Target Platform**: Windows 11+ only (no Windows 10 support)
**Development Approach**: Epic-based with 8 epics covering 19 user stories

---

## Project Organization Structure

### 1. Work Planning Documents (`.work/foundation/`)

All planning and requirements live in `.work/foundation/`:

#### Product Documentation (`.work/foundation/product/`)
- **`product-roadmap.md`** - Complete 12-week roadmap with all 8 epics
- **`user-stories.md`** - All 19 user stories (US-001 to US-019) with acceptance criteria
- **`mvp-checklist.md`** - Detailed feature checklist aligned with user stories
- **`golden-paths.md`** - User journey scenarios for testing
- **`requirements-summary.md`** - Quick reference for all requirements

#### Architecture Documentation (`.work/foundation/reviews/`)
- **`architecture-review.md`** - Technical architecture decisions and epic breakdown

**Key Principle**: Read these files first in any new session to understand scope and requirements.

---

### 2. Epic-Based Development (`.work/milestones/`)

Work is organized into **8 epics**, each in `.work/milestones/epic-XXX/`:

#### Epic Structure
```
.work/milestones/epic-XXX/
├── IMPLEMENTATION-SUMMARY.md   # What was built
├── [component-name]/
│   ├── INTERFACE.md           # Public APIs and contracts
│   ├── EVIDENCE.md            # Implementation proof
│   └── README.md              # Component overview
└── validation/
    ├── VALIDATION-REPORT.md   # E2E test results
    ├── README.md              # Validation summary
    └── evidence/              # Test outputs, logs, analysis
```

#### Current Epic Status

| Epic | User Stories | Status | Branch |
|------|-------------|--------|--------|
| Epic 1 | US-001 to US-003 | ✅ COMPLETE | merged to main |
| Epic 2 | US-004 to US-006 | ✅ COMPLETE | brian/epic-2 |
| Epic 3 | US-007 to US-009 | 🔜 NEXT | - |
| Epic 4 | US-010 to US-011 | ⏳ PENDING | - |
| Epic 5 | US-012 to US-013 | ⏳ PENDING | - |
| Epic 6 | US-014 to US-015 | ⏳ PENDING | - |
| Epic 7 | US-016 to US-017 | ⏳ PENDING | - |
| Epic 8 | US-018 to US-019 | ⏳ PENDING | - |

**Next Epic**: Epic 3 - Slack Apps Integration

---

### 3. Source Code Structure

```
src/
├── main/java/com/slackgrab/
│   ├── core/              # Application startup, DI (ApplicationModule)
│   ├── oauth/             # OAuth flow (OAuthManager)
│   ├── slack/             # Slack API client, MessageCollector
│   ├── data/              # Database (ConnectionPool, repositories)
│   ├── security/          # CredentialManager (Windows Registry)
│   ├── webhook/           # WebhookServer for OAuth callbacks
│   ├── ml/                # Neural network, feature extraction
│   ├── ui/                # System tray, status window (Epic 2)
│   └── util/              # Utilities, resource management
└── test/java/com/slackgrab/
    └── validation/        # Epic validation tests
```

---

## Development Workflow

### Starting a New Epic

1. **Read Planning Documents**
   - Review `.work/foundation/product/user-stories.md` for the epic's user stories
   - Check `.work/foundation/product/product-roadmap.md` for technical requirements
   - Review `.work/foundation/product/mvp-checklist.md` for specific feature checklist

2. **Create Epic Branch**
   ```bash
   git checkout -b brian/epic-X
   ```

3. **Create Epic Directory Structure**
   ```
   .work/milestones/epic-XXX/
   ├── [component-name]/
   │   ├── INTERFACE.md
   │   ├── EVIDENCE.md
   │   └── README.md
   ```

4. **Implement Features**
   - Follow existing architecture patterns from Epic 1 & 2
   - Use dependency injection (Guice) via `ApplicationModule`
   - Write unit tests as you go

5. **Document Implementation**
   - Update component INTERFACE.md with APIs
   - Update EVIDENCE.md with implementation proof
   - Create IMPLEMENTATION-SUMMARY.md for the epic

6. **Validate Epic**
   - Run build: `./gradlew clean build`
   - Run tests: `./gradlew test`
   - Create validation directory with test evidence
   - Write VALIDATION-REPORT.md

7. **Create Epic Completion Documentation**
   - Validation summary in `.work/milestones/epic-XXX/validation/README.md`
   - Test results in `.work/milestones/epic-XXX/validation/evidence/`

---

## Testing Strategy

### Unit Tests
- Location: `src/test/java/com/slackgrab/`
- Framework: JUnit 5 (Jupiter), Mockito
- Pattern: One test class per production class
- Naming: `[ClassName]Test.java`

### Validation Tests
- Location: `src/test/java/com/slackgrab/validation/`
- Purpose: End-to-end epic validation
- Pattern: `Epic[N]ValidationTest.java`
- Example: `Epic1ValidationTest.java` (19/19 tests passing)

### Running Tests
```bash
# All tests
./gradlew test

# Specific test
./gradlew test --tests Epic1ValidationTest

# With output
./gradlew test --info
```

---

## Key Project Files

### Configuration
- **`build.gradle`** - Dependencies, build configuration
- **`settings.gradle`** - Project settings
- **`instructions/project.md`** - Original project requirements

### Documentation
- **`SLACK-APP-SETUP.md`** - Slack OAuth setup guide
- **`EPIC1-FINAL-VALIDATION-COMPLETE.md`** - Epic 1 completion proof
- **`CLAUDE.md`** - This file

### Git
- **Main branch**: `main` (production-ready code)
- **Current branch**: `brian/epic-2` (Epic 2 complete, ready to merge)
- **Branch pattern**: `brian/epic-X` for each epic

---

## Design Principles

### 1. Zero Configuration
- No settings exposed to users
- Neural network auto-tunes based on behavior
- Silent operation (no error popups)

### 2. Local-First
- All processing happens locally
- No cloud dependencies
- SQLite database with encryption
- Windows Registry for credentials

### 3. Dependency Injection
- Guice for DI container
- `ApplicationModule` binds all components
- Constructor injection preferred
- Example:
  ```java
  @Inject
  public MessageCollector(SlackApiClient client,
                          MessageRepository repository) {
      this.client = client;
      this.repository = repository;
  }
  ```

### 4. Connection Pooling
- HikariCP for database connections
- `ConnectionPool` singleton pattern
- Repositories use `getConnection()`, never close connections
- Connection lifecycle managed by pool

### 5. Error Handling
- Silent failures (log only, no UI errors)
- Graceful degradation
- Automatic recovery attempts
- Local log files only

---

## Performance Targets

From acceptance criteria (must meet these):

| Metric | Target | Epic |
|--------|--------|------|
| Application startup | < 3 seconds | Epic 1 |
| Memory usage | < 4GB | Epic 5 |
| CPU usage (average) | < 5% | Epic 5 |
| Message scoring | < 1 second | Epic 2 |
| Slack API response | < 100ms | Epic 3 |
| Messages/day | 5000 | Epic 5 |
| Channels supported | 2000 | Epic 5 |

---

## Common Commands

### Build & Test
```bash
# Clean build
./gradlew clean build

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests Epic1ValidationTest

# Build without tests
./gradlew build -x test
```

### Git Workflow
```bash
# Check status
git status

# View recent commits
git log --oneline -10

# Create feature branch
git checkout -b brian/epic-X

# Commit with co-author
git commit -m "feat: epic X work complete

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Database
```bash
# SQLite database location
C:\Users\brian\AppData\Local\SlackGrab\slackgrab.db

# View schema
sqlite3 slackgrab.db ".schema"
```

---

## Picking Up Work in a New Session

### Quick Start Checklist

1. **Check Current Status**
   - Review git branch: `git status`
   - Check recent commits: `git log --oneline -5`
   - Look for `EPIC[N]-FINAL-VALIDATION-COMPLETE.md` files

2. **Identify Next Epic**
   - Read `.work/foundation/product/product-roadmap.md`
   - Find the next incomplete epic (currently Epic 3)
   - Review user stories in `.work/foundation/product/user-stories.md`

3. **Understand Context**
   - Read completed epic summaries in `.work/milestones/epic-XXX/`
   - Check validation reports for any known issues
   - Review `instructions/project.md` for core requirements

4. **Review Code Structure**
   - Check `src/main/java/com/slackgrab/` for existing components
   - Review `ApplicationModule.java` for DI configuration
   - Look at existing tests in `src/test/java/com/slackgrab/`

5. **Ask User**
   - Confirm which epic to work on
   - Clarify any ambiguities in requirements
   - Verify understanding before starting

---

## Epic Quick Reference

### Epic 1: Initial Setup (✅ Complete)
- OAuth flow, message collection, database setup
- Components: OAuthManager, MessageCollector, ConnectionPool
- Validation: 19/19 tests passing

### Epic 2: Neural Network (✅ Complete)
- ML infrastructure, feature extraction, GPU acceleration
- Components: NeuralNetwork, TextFeatureExtractor, ImportanceScorer
- Infrastructure: SystemTrayManager, AutoStartManager, TokenRefresh
- Validation: 52/52 tests passing

### Epic 3: Slack Apps Integration (🔜 Next)
- SlackGrab bot in Apps section
- Priority display (High/Medium/Low)
- Bot channel for summaries
- Slash commands

### Epic 4: Feedback System
- Three-level feedback (Too Low/Good/Too High)
- Neural network adaptation
- Batch feedback, undo functionality

### Epic 5: System Performance
- 5000 msg/day optimization
- Resource limits (<4GB, <5% CPU)
- GPU RAM limiting (80% max)

### Epic 6: Error Handling
- Silent error recovery
- Automatic reconnection
- Rate limit handling

### Epic 7: User Experience
- Zero configuration validation
- Learning period indicators
- No exposed settings

### Epic 8: Testing Infrastructure
- Mock Slack API tests
- Performance benchmarks
- Windows 11+ compatibility

---

## Critical Success Factors

1. **Neural Network Quality** - Must continuously improve, 85%+ accuracy within 1 week
2. **Zero Configuration** - Actually zero, not "minimal"
3. **Silent Operation** - Never interrupt users with errors
4. **Local Processing** - No cloud dependencies ever
5. **Performance** - Meet all targets (<4GB, <5% CPU, <1s scoring)

---

## Out of Scope (Never Implement)

- Windows 10 support
- Multi-workspace functionality
- Configuration UI
- Custom UI overlays
- Browser extensions
- Mobile applications
- Enterprise features
- Automatic error reporting

---

## Useful Tips for Claude Sessions

### When Starting
1. Always check git status and branch
2. Read this file first
3. Review user stories for current epic
4. Ask user which epic to work on

### During Development
1. Follow existing patterns from Epic 1 & 2
2. Use TodoWrite tool for multi-step tasks
3. Write tests alongside implementation
4. Document as you go (INTERFACE.md, EVIDENCE.md)

### Before Completing
1. Run full test suite
2. Create validation documentation
3. Write IMPLEMENTATION-SUMMARY.md
4. Update this file if structure changed

### When Stuck
1. Check `.work/foundation/product/user-stories.md` for requirements
2. Review similar components from completed epics
3. Ask user for clarification
4. Check `instructions/project.md` for original intent

---

## Contact and Resources

- **Project Location**: `C:\Users\brian\source\repos\SlackGrab`
- **Main Branch**: `main`
- **Current Branch**: `brian/epic-2`
- **Slack App Setup**: See `SLACK-APP-SETUP.md`
- **Git Workflow**: Standard feature branch workflow

---

## Version History

- **2025-11-04**: Initial version created after Epic 2 completion
- Epic 1 complete: Slack OAuth, message collection, database
- Epic 2 complete: Neural network, system tray, auto-start, token refresh
- Next: Epic 3 - Slack Apps Integration

---

**Last Updated**: 2025-11-04
**Status**: Epic 2 complete, ready for Epic 3
**Branch**: brian/epic-2

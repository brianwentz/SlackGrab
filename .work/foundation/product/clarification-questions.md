# SlackGrab - Clarification Questions and Ambiguities

## Critical Questions for Stakeholder Review

### 1. Slack Integration Architecture

**Current Ambiguity:** How exactly will SlackGrab interact with the locally installed Slack application?

**Questions:**
- Should we use Slack's official API (requires internet) or attempt to hook into the local Slack Electron app?
- What happens if Slack is using their web version instead of desktop app?
- How do we handle multiple Slack workspace connections?
- What's our approach if Slack updates their app and breaks our integration?
- Do we need to support both Slack's old and new desktop clients?

**Recommendation:** Use official Slack API with local caching for resilience.

**Answers:**
Use slacks official API.  only support the desktop app.  when the user adds slackgrab they should choose which workspace to use.
Don't worry about slack updates breaking the application.  Only support the latest current desktop client.

### 2. Machine Learning Model Specifics

**Current Ambiguity:** The complexity and type of ML model isn't specified.

**Questions:**
- Should we start with simple statistical models or jump to neural networks?
- What's the expected model size limit (affects memory usage)?
- How often should the model retrain (daily, weekly, continuous)?
- Should different users share any learned patterns or completely isolated?
- What's our approach for handling multiple languages in messages?
- Do we need to handle images, GIFs, and other media in importance scoring?

**Recommendation:** Start with lightweight gradient boosting, evolve to neural networks in v2.

**Answers:**
You should use neural networks, the AI component of this project is key to its success and needs to be cutting edge.
The model should be able to run locally on a modern PC with a Core i7 processor and 64gb of ram.
The model should retrain continuously based on usage.  It should be specific to 1 user and never shared.  You can assume all
messages will be in 1 language, english.  You do not need to handle other media in scoring, although its presence and type
may be important for scoring.

### 3. Privacy and Compliance

**Current Ambiguity:** Privacy requirements beyond "local processing" aren't defined.

**Questions:**
- Do we need GDPR compliance for EU users?
- Should we implement audit logging for enterprise users?
- How long should we retain user behavior data?
- Do we need to handle corporate data retention policies?
- Should deleted Slack messages be purged from our database?
- What about handling sensitive data (SSN, credit cards) in messages?

**Recommendation:** Implement configurable retention with secure deletion and audit logs.

**Answers:** 
You do not need to worry about privacy in this project for now as all data should be kept locally and encrypted, that is enough.

### 4. Performance Boundaries

**Current Ambiguity:** Scale limitations aren't clearly defined.

**Questions:**
- What's the maximum number of messages per day we should handle?
- How many Slack workspaces should we support simultaneously?
- What's the minimum Windows version we support (Windows 10 20H2, 21H1, 22H2)?
- Should we support Windows on ARM?
- What's the maximum acceptable latency for importance scoring?
- How do we handle users with 1000+ channels?

**Recommendation:** Target 1000 messages/day, 5 workspaces, Windows 10 21H1+.

**Answers:**
You should expect to handle 5000 messages a day.  You only need to support 1 slack workspace.  The minimum windows version
is windows 11.  You do not need to support windows on arm.  For latency on scoring, that should be a bg task and can take up to 1s.
Some users will have up to 2000 channels but you should expect a normal user will have 1000 or fewer.

### 5. UI Enhancement Method

**Current Ambiguity:** How we modify Slack's UI without breaking it.

**Questions:**
- Should we inject CSS/JavaScript into Slack's Electron app?
- Do we create an overlay that sits on top of Slack?
- How do we handle Slack's frequent UI updates?
- What if user has Slack's custom themes?
- Should we support Slack's accessible/high-contrast modes?
- How do we ensure our changes don't violate Slack's ToS?

**Recommendation:** Use transparent overlay approach for stability.

**Answers:** If you can integrate into slack using its api to extend the ui you should do that.  For example bots create 
separate channels and can show ui like buttons, that may be enough for this project.  You should try to not have to build
custom ui apart from what slack api allows as much as possible and instead use slack's integration mechanisms to do the work and ui.

### 6. Notification System Architecture

**Current Ambiguity:** How we intercept and modify Slack notifications.

**Questions:**
- Do we suppress Slack's native notifications completely?
- How do we handle notification sync across multiple devices?
- Should we integrate with Windows Focus Assist?
- What about Windows notification history?
- How do we handle Do Not Disturb synchronization with Slack?
- Should we support Windows 11's notification priorities?

**Recommendation:** Proxy notifications through our app, maintain Slack's DND state.

**Answers:** You should not try to integrate with the Windows notification system nor should you suppress or change any
slack notifications.  Instead you can do something more native to slack, for example using the Apps section in slack to
display all data for the app.  Please look at slack integration best practices to help make the right choice here.  Its possible
that this may require using a service so that slack will send notifications to a web address.  If that is the case we should
be able to handle that locally by running a web service at localhost for example.

### 7. Model Training Resources

**Current Ambiguity:** GPU/NPU utilization strategy not specified.

**Questions:**
- What if user has no GPU/NPU - CPU fallback performance?
- Should we support NVIDIA CUDA, AMD ROCm, or both?
- How do we handle Intel integrated graphics?
- What about Apple Silicon Macs if we expand platforms?
- Should training pause during gaming/high GPU usage?
- Memory allocation strategy when GPU VRAM is limited?

**Recommendation:** CPU default, optional GPU acceleration with auto-detection.

**Answers:** Yes, fall back to CPU performance.  For now lets assume Intel processors, and integrated graphics will
be common, so that needs to work.  No support for Apple macs right now.  You can pause during high cpu usage, and you can
use up to 80% of gpu ram when needed.

### 8. Feedback Mechanism Scope

**Current Ambiguity:** How sophisticated should the feedback system be?

**Questions:**
- Should feedback be binary (good/bad) or scaled (1-5 stars)?
- Do we need to track why user marked something as important?
- Should we allow batch feedback on multiple messages?
- How quickly should feedback affect predictions?
- Do we need an "undo feedback" feature?
- Should users see how their feedback changed the model?

**Recommendation:** Three-level feedback (Too Low/Good/Too High) with undo capability.

**Answers:**
Feedback as 3 levels is good, with undo.  Batch feedback on multiple messages is desired.  No need to track why a user 
marked something important, the model must learn to infer that.  No need to show users how their feedback changed the model.

### 9. Enterprise vs Consumer Features

**Current Ambiguity:** Target market not clearly defined.

**Questions:**
- Are we targeting individual users or enterprise deployments?
- Do we need centralized administration features?
- Should we support enterprise proxy servers?
- What about Single Sign-On (SSO) integration?
- Do we need deployment via Group Policy?
- Should we have different pricing/licensing models?

**Recommendation:** Start consumer-focused, add enterprise features in v2.0.

**Answers:** we are targetting individual users, no need for centralized support, proxy, etc.  The app will be free.

### 10. Testing and Quality Strategy

**Current Ambiguity:** Specific testing requirements for ML components.

**Questions:**
- How do we generate realistic test data without real user data?
- What's our strategy for testing different user personas?
- Should we implement shadow mode testing in production?
- How do we validate model fairness and bias?
- What's our approach for testing Slack API changes?
- Do we need continuous model quality monitoring?

**Recommendation:** Synthetic data generation with persona-based testing.

**Answers:**
For test data you should indeed gather real world data, watching what the user does.  Its ok to have a start-up or learning
period before the app begins to function.  You do not need to worry about personas nor shadow mode.  For fairness and bias, please
use industry best practices but keep it simple.  To validate usage of the slack api there should be mocks, but you should not
try to track slack api changes.  To monitor the app it should produce a local log file indicating actions taken, errors, etc useful for
debugging problems with the app.

## Technical Ambiguities

### Data Architecture
- Message storage format (raw vs processed)
- Encryption approach for sensitive data
- Backup and restore capabilities
- Data migration between versions
- Handling of Slack's message edits/deletions

### Integration Points
- Browser extension for Slack web version?
- Mobile notification mirroring?
- Calendar integration for meeting context?
- Email client integration roadmap?
- Webhook support for automation?

**Answers:** No need for these.

### Scalability Concerns
- Multi-threading strategy for real-time processing
- Database optimization for large message volumes
- Memory management with long-running sessions
- Network resilience for API failures
- Batch processing for historical data

## User Experience Ambiguities

### Onboarding Flow
- How much historical data to process initially?
- Should we have a tutorial or guided tour?
- Default settings (aggressive vs conservative)?
- How to handle partial permissions?
- What if Slack workspace requires admin approval?

**Answers:** You can use up to 30 days of historical data initially.  No need for a tutorial.  Default settings should be moderate.
For permissions and approval, the user can approve those via the slack ui.

### Customization Depth
- How many configuration options to expose?
- Should we have basic/advanced modes?
- Preset profiles for different roles?
- Theme customization beyond light/dark?
- Custom importance algorithms?

**Answers:** for now lets keep it simple and not expose any options.

### Error Handling
- User-friendly error messages vs technical details?
- Automatic error reporting (with consent)?
- Self-healing capabilities?
- Fallback modes when ML fails?
- Update rollback triggers?

**Answers:**  Errors should go to a local log file.  When there is an error it should be silent and the fallback is to do
nothing, eg slack behaves as if the app was not present.

## Business Model Questions

### Licensing
- Free tier limitations?
- Personal vs commercial use?
- Subscription vs one-time purchase?
- Family/team licenses?
- Educational discounts?
  
**Answers:** this will be free, no limitations

### Support Model
- Community support only or paid support?
- Documentation depth requirement?
- Video tutorials needed?
- In-app help system?
- Support ticket system?

**Answers:** Self-support, no need for a support model.

## Recommendations for Next Steps

1. **Immediate Priorities:**
   - Define Slack integration approach (API vs local hooks)
   - Clarify ML model complexity for MVP
   - Determine enterprise vs consumer focus
   - Establish privacy/compliance requirements

2. **Technical Decisions Needed:**
   - UI enhancement method selection
   - Notification architecture design
   - GPU/NPU strategy confirmation
   - Testing approach for ML components

3. **User Research Required:**
   - Validate importance scoring accuracy needs
   - Understand feedback mechanism preferences
   - Determine customization requirements
   - Assess performance tolerance levels

4. **Risk Mitigation Planning:**
   - Slack API change contingency
   - Model drift detection strategy
   - Privacy compliance framework
   - Performance degradation handling

## Assumptions Made in Product Planning

1. Users have stable internet for Slack API access
2. Windows 10 21H1 or later is acceptable minimum
3. 200MB memory footprint is acceptable
4. Local-only processing is a hard requirement
5. English language support is sufficient for MVP
6. Single Slack workspace is primary use case
7. Users will provide feedback to improve model
8. 85% accuracy is achievable and acceptable
9. Sub-100ms scoring latency is achievable
10. Slack ToS allows UI enhancement

These assumptions should be validated with stakeholders before development begins.
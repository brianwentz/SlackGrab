package com.slackgrab.slack;

import com.google.inject.Inject;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.Message;
import com.slack.api.model.User;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.data.MessageRepository;
import com.slackgrab.data.ChannelRepository;
import com.slackgrab.data.model.SlackMessage;
import com.slackgrab.data.model.SlackChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Slack message collection service
 *
 * Collects messages from Slack using the official API:
 * - Fetches channel list (conversations.list)
 * - Fetches historical messages (conversations.history)
 * - Handles pagination and rate limiting
 * - Stores messages in local database
 *
 * Supports:
 * - Initial 30-day historical sync
 * - Incremental updates (fetch only new messages)
 * - Up to 2000 channels
 * - Up to 5000 messages/day throughput
 */
public class MessageCollector {
    private static final Logger logger = LoggerFactory.getLogger(MessageCollector.class);

    // Configuration constants
    private static final int DAYS_OF_HISTORY = 30;
    private static final int MAX_CHANNELS = 2000;
    private static final int MESSAGES_PER_PAGE = 100;
    private static final int MAX_MESSAGES_PER_DAY = 5000;
    private static final long RATE_LIMIT_DELAY_MS = 1000; // 1 second between API calls

    private final SlackApiClient slackApiClient;
    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ErrorHandler errorHandler;

    private volatile boolean isCollecting = false;
    private volatile int messagesCollectedToday = 0;
    private volatile Instant lastResetTime = Instant.now();

    @Inject
    public MessageCollector(
        SlackApiClient slackApiClient,
        MessageRepository messageRepository,
        ChannelRepository channelRepository,
        ErrorHandler errorHandler
    ) {
        this.slackApiClient = slackApiClient;
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
        this.errorHandler = errorHandler;
    }

    /**
     * Perform initial historical message collection (30 days)
     *
     * This should be called once after OAuth authorization completes.
     * Fetches all messages from the past 30 days across all channels.
     *
     * @return CollectionResult with statistics
     * @throws MessageCollectionException if collection fails
     */
    public CollectionResult performInitialCollection() throws MessageCollectionException {
        if (isCollecting) {
            throw new MessageCollectionException("Collection already in progress");
        }

        if (!slackApiClient.hasAccessToken()) {
            throw new MessageCollectionException("No Slack access token available. Please authorize first.");
        }

        isCollecting = true;
        CollectionResult result = new CollectionResult();

        try {
            logger.info("Starting initial message collection (30 days of history)...");

            // 1. Fetch channel list
            List<SlackChannel> channels = fetchAndStoreChannels();
            result.channelsDiscovered = channels.size();
            logger.info("Discovered {} channels", channels.size());

            // 2. Calculate cutoff time (30 days ago)
            Instant cutoffTime = Instant.now().minus(DAYS_OF_HISTORY, ChronoUnit.DAYS);
            String oldestTimestamp = String.valueOf(cutoffTime.getEpochSecond());

            // 3. Fetch messages from each channel
            for (SlackChannel channel : channels) {
                if (result.messagesCollected >= MAX_MESSAGES_PER_DAY) {
                    logger.warn("Reached daily message limit ({}). Stopping collection.", MAX_MESSAGES_PER_DAY);
                    break;
                }

                try {
                    int channelMessages = fetchChannelHistory(channel.id(), oldestTimestamp, null);
                    result.messagesCollected += channelMessages;
                    result.channelsProcessed++;

                    logger.info("Collected {} messages from channel: {} ({})",
                        channelMessages, channel.name(), channel.id());

                    // Rate limiting
                    Thread.sleep(RATE_LIMIT_DELAY_MS);

                } catch (Exception e) {
                    result.errors++;
                    errorHandler.handleError("Failed to collect messages from channel: " + channel.name(), e);
                    // Continue with next channel
                }
            }

            logger.info("Initial collection complete. Channels: {}, Messages: {}, Errors: {}",
                result.channelsProcessed, result.messagesCollected, result.errors);

            return result;

        } catch (Exception e) {
            errorHandler.handleError("Initial message collection failed", e);
            throw new MessageCollectionException("Failed to perform initial collection", e);
        } finally {
            isCollecting = false;
        }
    }

    /**
     * Perform incremental message collection
     *
     * Fetches only new messages since last sync.
     * Should be called periodically (e.g., every 5 minutes).
     *
     * @return CollectionResult with statistics
     */
    public CollectionResult performIncrementalCollection() throws MessageCollectionException {
        if (isCollecting) {
            logger.debug("Collection already in progress, skipping incremental sync");
            return new CollectionResult();
        }

        if (!slackApiClient.hasAccessToken()) {
            throw new MessageCollectionException("No Slack access token available");
        }

        // Reset daily counter if needed
        resetDailyCounterIfNeeded();

        if (messagesCollectedToday >= MAX_MESSAGES_PER_DAY) {
            logger.warn("Daily message limit reached. Skipping incremental collection.");
            return new CollectionResult();
        }

        isCollecting = true;
        CollectionResult result = new CollectionResult();

        try {
            logger.debug("Starting incremental message collection...");

            // Get list of channels
            List<SlackChannel> channels = channelRepository.getAllChannels();

            for (SlackChannel channel : channels) {
                if (messagesCollectedToday >= MAX_MESSAGES_PER_DAY) {
                    break;
                }

                try {
                    // Get timestamp of last message in this channel
                    Optional<String> lastTimestamp = messageRepository.getLastMessageTimestamp(channel.id());

                    // Fetch new messages since last timestamp
                    int newMessages = fetchChannelHistory(channel.id(), lastTimestamp.orElse(null), null);
                    result.messagesCollected += newMessages;
                    result.channelsProcessed++;

                    if (newMessages > 0) {
                        logger.debug("Collected {} new messages from channel: {}",
                            newMessages, channel.name());
                    }

                    // Update channel sync time
                    channelRepository.updateLastSynced(channel.id(), Instant.now());

                    // Rate limiting
                    Thread.sleep(RATE_LIMIT_DELAY_MS);

                } catch (Exception e) {
                    result.errors++;
                    errorHandler.handleError("Failed incremental collection for channel: " + channel.name(), e);
                }
            }

            logger.debug("Incremental collection complete. New messages: {}", result.messagesCollected);
            return result;

        } catch (Exception e) {
            errorHandler.handleError("Incremental message collection failed", e);
            throw new MessageCollectionException("Failed to perform incremental collection", e);
        } finally {
            isCollecting = false;
        }
    }

    /**
     * Fetch and store channel list from Slack
     *
     * @return List of channels
     */
    private List<SlackChannel> fetchAndStoreChannels() throws IOException, SlackApiException {
        List<SlackChannel> allChannels = new ArrayList<>();
        String[] cursorHolder = {null}; // Use array to allow mutation in lambda
        int totalFetched = 0;

        do {
            final String currentCursor = cursorHolder[0];
            ConversationsListResponse response = slackApiClient.getSlack()
                .methods(slackApiClient.getAccessToken().orElseThrow())
                .conversationsList(req -> req
                    .excludeArchived(true)
                    .cursor(currentCursor)
                    .limit(200)
                );

            if (!response.isOk()) {
                throw new IOException("Failed to fetch channels: " + response.getError());
            }

            List<Conversation> conversations = response.getChannels();
            for (Conversation conv : conversations) {
                // Get member count - use 0 if not available
                Integer numMembers = conv.getNumOfMembers();
                int memberCount = (numMembers != null) ? numMembers : 0;

                SlackChannel channel = new SlackChannel(
                    conv.getId(),
                    conv.getName(),
                    conv.isPrivate(),
                    memberCount,
                    Instant.now()
                );

                allChannels.add(channel);
                channelRepository.saveChannel(channel);
                totalFetched++;

                if (totalFetched >= MAX_CHANNELS) {
                    logger.warn("Reached maximum channel limit ({})", MAX_CHANNELS);
                    return allChannels;
                }
            }

            cursorHolder[0] = response.getResponseMetadata() != null ?
                response.getResponseMetadata().getNextCursor() : null;

        } while (cursorHolder[0] != null && !cursorHolder[0].isEmpty());

        return allChannels;
    }

    /**
     * Fetch message history for a specific channel
     *
     * @param channelId Channel ID to fetch from
     * @param oldest Oldest timestamp to fetch (null for no limit)
     * @param latest Latest timestamp to fetch (null for no limit)
     * @return Number of messages fetched
     */
    private int fetchChannelHistory(String channelId, String oldest, String latest)
        throws IOException, SlackApiException, InterruptedException {

        int messageCount = 0;
        String[] cursorHolder = {null}; // Use array to allow mutation in lambda

        do {
            // Check daily limit
            if (messagesCollectedToday >= MAX_MESSAGES_PER_DAY) {
                logger.warn("Daily message limit reached during channel history fetch");
                break;
            }

            final String currentCursor = cursorHolder[0];
            ConversationsHistoryResponse response = slackApiClient.getSlack()
                .methods(slackApiClient.getAccessToken().orElseThrow())
                .conversationsHistory(req -> req
                    .channel(channelId)
                    .oldest(oldest)
                    .latest(latest)
                    .cursor(currentCursor)
                    .limit(MESSAGES_PER_PAGE)
                );

            if (!response.isOk()) {
                String error = response.getError();

                // Handle specific errors gracefully
                if ("channel_not_found".equals(error) || "not_in_channel".equals(error)) {
                    logger.warn("Channel not accessible: {}. Error: {}", channelId, error);
                    return messageCount;
                }

                throw new IOException("Failed to fetch channel history: " + error);
            }

            List<Message> messages = response.getMessages();
            for (Message msg : messages) {
                // Convert Slack message to our model
                SlackMessage slackMessage = convertToSlackMessage(msg, channelId);

                // Store in database
                messageRepository.saveMessage(slackMessage);
                messageCount++;
                messagesCollectedToday++;

                if (messagesCollectedToday >= MAX_MESSAGES_PER_DAY) {
                    break;
                }
            }

            // Check for more pages
            cursorHolder[0] = response.getResponseMetadata() != null ?
                response.getResponseMetadata().getNextCursor() : null;

            // Rate limiting between pages
            if (cursorHolder[0] != null && !cursorHolder[0].isEmpty()) {
                Thread.sleep(RATE_LIMIT_DELAY_MS);
            }

        } while (cursorHolder[0] != null && !cursorHolder[0].isEmpty());

        return messageCount;
    }

    /**
     * Convert Slack API Message to our SlackMessage model
     */
    private SlackMessage convertToSlackMessage(Message msg, String channelId) {
        boolean hasAttachments = (msg.getAttachments() != null && !msg.getAttachments().isEmpty()) ||
                                 (msg.getFiles() != null && !msg.getFiles().isEmpty());
        boolean hasReactions = msg.getReactions() != null && !msg.getReactions().isEmpty();

        return new SlackMessage(
            msg.getTs(),  // Message timestamp is the ID
            channelId,
            msg.getUser() != null ? msg.getUser() : "UNKNOWN",
            msg.getText() != null ? msg.getText() : "",
            msg.getTs(),
            msg.getThreadTs(),
            hasAttachments,
            hasReactions,
            null,  // importance_score - will be calculated by neural network
            null,  // importance_level - will be calculated by neural network
            Instant.now()
        );
    }

    /**
     * Reset daily message counter if it's a new day
     */
    private void resetDailyCounterIfNeeded() {
        Instant now = Instant.now();
        long hoursSinceReset = ChronoUnit.HOURS.between(lastResetTime, now);

        if (hoursSinceReset >= 24) {
            logger.info("Resetting daily message counter. Collected yesterday: {}", messagesCollectedToday);
            messagesCollectedToday = 0;
            lastResetTime = now;
        }
    }

    /**
     * Check if collection is currently in progress
     */
    public boolean isCollecting() {
        return isCollecting;
    }

    /**
     * Get number of messages collected today
     */
    public int getMessagesCollectedToday() {
        return messagesCollectedToday;
    }

    /**
     * Collection result statistics
     */
    public static class CollectionResult {
        public int channelsDiscovered = 0;
        public int channelsProcessed = 0;
        public int messagesCollected = 0;
        public int errors = 0;

        @Override
        public String toString() {
            return String.format(
                "CollectionResult{channels=%d/%d, messages=%d, errors=%d}",
                channelsProcessed, channelsDiscovered, messagesCollected, errors
            );
        }
    }

    /**
     * Message collection exception
     */
    public static class MessageCollectionException extends Exception {
        public MessageCollectionException(String message) {
            super(message);
        }

        public MessageCollectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

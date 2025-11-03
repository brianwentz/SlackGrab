package com.slackgrab.ml.features;

import com.google.inject.Singleton;
import com.slackgrab.ml.model.ScoringContext;

import java.util.regex.Pattern;

/**
 * Extract text-based features from message content
 *
 * Features (10 total):
 * 1. Text length (normalized)
 * 2. Word count (normalized)
 * 3. Has question marks
 * 4. Has URLs
 * 5. Has mentions (@user)
 * 6. Has emojis
 * 7. Uppercase ratio
 * 8. Exclamation count
 * 9. Average word length
 * 10. Urgent keyword match
 */
@Singleton
public class TextFeatureExtractor {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+");
    private static final Pattern MENTION_PATTERN = Pattern.compile("<@[A-Z0-9]+>");
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":[a-z_]+:");

    private static final int MAX_LENGTH = 4000;  // Normalize text length
    private static final int MAX_WORDS = 500;    // Normalize word count

    /**
     * Extract text features from message content
     *
     * @param text Message text
     * @param context Scoring context (for urgent keywords)
     * @return Array of 10 text features
     */
    public float[] extractFeatures(String text, ScoringContext context) {
        if (text == null || text.isEmpty()) {
            return new float[10];
        }

        float[] features = new float[10];

        // 0: Text length (normalized 0-1)
        features[0] = Math.min(1.0f, (float) text.length() / MAX_LENGTH);

        // 1: Word count (normalized 0-1)
        String[] words = text.split("\\s+");
        features[1] = Math.min(1.0f, (float) words.length / MAX_WORDS);

        // 2: Has question marks
        features[2] = text.contains("?") ? 1.0f : 0.0f;

        // 3: Has URLs
        features[3] = URL_PATTERN.matcher(text).find() ? 1.0f : 0.0f;

        // 4: Has mentions (@user)
        features[4] = MENTION_PATTERN.matcher(text).find() ? 1.0f : 0.0f;

        // 5: Has emojis
        features[5] = EMOJI_PATTERN.matcher(text).find() ? 1.0f : 0.0f;

        // 6: Uppercase ratio
        long uppercaseCount = text.chars().filter(Character::isUpperCase).count();
        long letterCount = text.chars().filter(Character::isLetter).count();
        features[6] = letterCount > 0 ? (float) uppercaseCount / letterCount : 0.0f;

        // 7: Exclamation count (normalized)
        long exclamationCount = text.chars().filter(c -> c == '!').count();
        features[7] = Math.min(1.0f, (float) exclamationCount / 5.0f);

        // 8: Average word length (normalized)
        float avgWordLength = words.length > 0
            ? (float) text.length() / words.length
            : 0.0f;
        features[8] = Math.min(1.0f, avgWordLength / 20.0f);

        // 9: Urgent keyword match
        features[9] = containsUrgentKeyword(text.toLowerCase(), context) ? 1.0f : 0.0f;

        return features;
    }

    /**
     * Check if text contains any urgent keywords
     */
    private boolean containsUrgentKeyword(String text, ScoringContext context) {
        // Default urgent keywords
        String[] defaultUrgent = {
            "urgent", "asap", "important", "critical", "emergency",
            "deadline", "priority", "immediately", "alert", "issue"
        };

        for (String keyword : defaultUrgent) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        // Context-specific keywords
        for (String keyword : context.getUrgentKeywords()) {
            if (text.toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}

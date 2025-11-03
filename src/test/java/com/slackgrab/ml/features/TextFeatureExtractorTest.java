package com.slackgrab.ml.features;

import com.slackgrab.ml.model.ScoringContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TextFeatureExtractor
 */
class TextFeatureExtractorTest {

    private TextFeatureExtractor extractor;
    private ScoringContext context;

    @BeforeEach
    void setUp() {
        extractor = new TextFeatureExtractor();
        context = ScoringContext.createDefault();
    }

    @Test
    void extractFeatures_emptyText_returnsZeroFeatures() {
        float[] features = extractor.extractFeatures("", context);

        assertThat(features).hasSize(10);
        assertThat(features).containsOnly(0.0f);
    }

    @Test
    void extractFeatures_simpleText_extractsBasicFeatures() {
        String text = "Hello world!";
        float[] features = extractor.extractFeatures(text, context);

        assertThat(features).hasSize(10);
        // Text length should be normalized
        assertThat(features[0]).isGreaterThan(0.0f).isLessThan(1.0f);
        // Word count should be normalized
        assertThat(features[1]).isGreaterThan(0.0f).isLessThan(1.0f);
    }

    @Test
    void extractFeatures_textWithQuestion_detectsQuestion() {
        String text = "How are you doing?";
        float[] features = extractor.extractFeatures(text, context);

        // Feature 2: Has question marks
        assertThat(features[2]).isEqualTo(1.0f);
    }

    @Test
    void extractFeatures_textWithURL_detectsURL() {
        String text = "Check out https://example.com for more info";
        float[] features = extractor.extractFeatures(text, context);

        // Feature 3: Has URLs
        assertThat(features[3]).isEqualTo(1.0f);
    }

    @Test
    void extractFeatures_textWithMention_detectsMention() {
        String text = "Hey <@U12345> can you help?";
        float[] features = extractor.extractFeatures(text, context);

        // Feature 4: Has mentions
        assertThat(features[4]).isEqualTo(1.0f);
    }

    @Test
    void extractFeatures_textWithEmoji_detectsEmoji() {
        String text = "Great work :thumbsup:";
        float[] features = extractor.extractFeatures(text, context);

        // Feature 5: Has emojis
        assertThat(features[5]).isEqualTo(1.0f);
    }

    @Test
    void extractFeatures_uppercaseText_detectsUppercase() {
        String text = "URGENT MESSAGE";
        float[] features = extractor.extractFeatures(text, context);

        // Feature 6: Uppercase ratio should be high
        assertThat(features[6]).isGreaterThan(0.8f);
    }

    @Test
    void extractFeatures_textWithExclamations_countsExclamations() {
        String text = "Important! Please respond!!";
        float[] features = extractor.extractFeatures(text, context);

        // Feature 7: Exclamation count
        assertThat(features[7]).isGreaterThan(0.0f);
    }

    @Test
    void extractFeatures_urgentKeyword_detectsUrgency() {
        String text = "This is urgent, please respond ASAP";
        float[] features = extractor.extractFeatures(text, context);

        // Feature 9: Urgent keyword match
        assertThat(features[9]).isEqualTo(1.0f);
    }

    @Test
    void extractFeatures_normalText_noUrgency() {
        String text = "Just a regular message";
        float[] features = extractor.extractFeatures(text, context);

        // Feature 9: No urgent keywords
        assertThat(features[9]).isEqualTo(0.0f);
    }
}

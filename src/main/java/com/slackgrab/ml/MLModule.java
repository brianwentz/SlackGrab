package com.slackgrab.ml;

import com.google.inject.AbstractModule;
import com.slackgrab.ml.features.*;
import com.slackgrab.ml.gpu.GpuAccelerator;
import com.slackgrab.ml.gpu.ResourceMonitor;
import com.slackgrab.ml.model.NeuralNetworkModel;
import com.slackgrab.ml.training.BatchTrainer;
import com.slackgrab.ml.training.OnlineTrainer;
import com.slackgrab.ml.training.TrainingScheduler;

/**
 * Guice module for machine learning components
 *
 * Binds all ML-related dependencies for dependency injection.
 */
public class MLModule extends AbstractModule {

    @Override
    protected void configure() {
        // Core ML components
        bind(ImportanceScorer.class);
        bind(NeuralNetworkModel.class);

        // Feature extraction
        bind(FeatureExtractor.class);
        bind(TextFeatureExtractor.class);
        bind(UserFeatureExtractor.class);
        bind(MediaFeatureExtractor.class);
        bind(TemporalFeatureExtractor.class);

        // Training
        bind(OnlineTrainer.class);
        bind(BatchTrainer.class);
        bind(TrainingScheduler.class);

        // GPU/Resource management
        bind(GpuAccelerator.class);
        bind(ResourceMonitor.class);
    }
}

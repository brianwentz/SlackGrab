package com.slackgrab.ml.model;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Map;

/**
 * Feature vector for message importance scoring
 *
 * Contains extracted features from a message and provides conversion
 * to ND4J arrays for neural network input.
 */
public class FeatureVector {
    private final float[] values;
    private final Map<String, Integer> featureIndices;
    private final int dimension;

    public FeatureVector(float[] values, Map<String, Integer> featureIndices) {
        this.values = values;
        this.featureIndices = featureIndices;
        this.dimension = values.length;
    }

    /**
     * Get feature value by name
     *
     * @param featureName Name of the feature
     * @return Feature value, or 0.0 if not found
     */
    public float getValue(String featureName) {
        Integer index = featureIndices.get(featureName);
        return index != null ? values[index] : 0.0f;
    }

    /**
     * Get feature value by index
     *
     * @param index Feature index
     * @return Feature value
     */
    public float getValue(int index) {
        if (index < 0 || index >= values.length) {
            return 0.0f;
        }
        return values[index];
    }

    /**
     * Get all feature values as array
     *
     * @return Feature values
     */
    public float[] getValues() {
        return values.clone();
    }

    /**
     * Get feature dimension
     *
     * @return Number of features
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Get feature indices map
     *
     * @return Map of feature name to index
     */
    public Map<String, Integer> getFeatureIndices() {
        return Map.copyOf(featureIndices);
    }

    /**
     * Convert to ND4J array for neural network input
     *
     * @return INDArray representation
     */
    public INDArray toINDArray() {
        return Nd4j.create(values);
    }

    /**
     * Convert to batch INDArray (adds batch dimension)
     *
     * @return INDArray with shape [1, dimension]
     */
    public INDArray toBatchINDArray() {
        return Nd4j.create(values).reshape(1, dimension);
    }

    @Override
    public String toString() {
        return String.format("FeatureVector[dim=%d, features=%d]",
            dimension, featureIndices.size());
    }
}

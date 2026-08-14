package edu.boun.edgecloudsim.edge_orchestrator.ppo;

import java.util.Arrays;

/**
 * Immutable normalized feature vector consumed by the PPO actor.
 */
public final class PpoFeatureVector {

    public static final int FEATURE_COUNT =
            10;

    private final double[] values;

    public PpoFeatureVector(
            double[] values) {

        if(values == null
                || values.length
                != FEATURE_COUNT) {

            throw new IllegalArgumentException(
                    "PPO feature vector must contain "
                    + FEATURE_COUNT
                    + " values.");
        }

        this.values =
                Arrays.copyOf(
                        values,
                        values.length);

        for(double value
                : this.values) {

            if(Double.isNaN(value)
                    || Double.isInfinite(value)) {

                throw new IllegalArgumentException(
                        "PPO features must be finite.");
            }
        }
    }

    public double[] toArray() {

        return Arrays.copyOf(
                values,
                values.length);
    }

    public double get(
            int index) {

        return values[index];
    }

    @Override
    public String toString() {

        return Arrays.toString(
                values);
    }
}
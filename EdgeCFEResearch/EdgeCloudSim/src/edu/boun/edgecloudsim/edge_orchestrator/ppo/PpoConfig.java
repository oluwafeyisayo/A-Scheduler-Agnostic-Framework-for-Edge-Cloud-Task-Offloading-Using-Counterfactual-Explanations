package edu.boun.edgecloudsim.edge_orchestrator.ppo;

import java.util.Arrays;

/**
 * Immutable PPO inference configuration.
 *
 * The Java simulator performs deterministic actor inference only.
 * Actor weights are supplied by an offline PPO trainer.
 */
public final class PpoConfig {

    public static final String DEFAULT_MODEL_ID =
            "PPO_BOOTSTRAP_V1";

    public static final String FEATURE_SCHEMA_ID =
            "PPO_FEATURE_SCHEMA_V1";

    private final String modelId;
    private final String featureSchemaId;
    private final int inputSize;
    private final int hiddenSize;

    private final double[][] hiddenWeights;
    private final double[] hiddenBias;
    private final double[] outputWeights;
    private final double outputBias;

    private PpoConfig(
            String modelId,
            String featureSchemaId,
            int inputSize,
            int hiddenSize,
            double[][] hiddenWeights,
            double[] hiddenBias,
            double[] outputWeights,
            double outputBias) {

        this.modelId =
                requireText(modelId, "modelId");

        this.featureSchemaId =
                requireText(
                        featureSchemaId,
                        "featureSchemaId");

        if(inputSize < 1) {
            throw new IllegalArgumentException(
                    "inputSize must be positive.");
        }

        if(hiddenSize < 1) {
            throw new IllegalArgumentException(
                    "hiddenSize must be positive.");
        }

        validateMatrix(
                hiddenWeights,
                hiddenSize,
                inputSize,
                "hiddenWeights");

        validateVector(
                hiddenBias,
                hiddenSize,
                "hiddenBias");

        validateVector(
                outputWeights,
                hiddenSize,
                "outputWeights");

        validateFinite(
                outputBias,
                "outputBias");

        this.inputSize =
                inputSize;

        this.hiddenSize =
                hiddenSize;

        this.hiddenWeights =
                deepCopy(hiddenWeights);

        this.hiddenBias =
                Arrays.copyOf(
                        hiddenBias,
                        hiddenBias.length);

        this.outputWeights =
                Arrays.copyOf(
                        outputWeights,
                        outputWeights.length);

        this.outputBias =
                outputBias;
    }

    /**
     * Deterministic bootstrap actor used to verify integration before
     * offline PPO training is completed.
     *
     * It is intentionally identified as a bootstrap model and must not
     * be reported as a trained PPO policy in experiments.
     */
    public static PpoConfig createBootstrap() {

        int inputSize =
                PpoFeatureVector.FEATURE_COUNT;

        int hiddenSize =
                8;

        double[][] hiddenWeights =
                new double[hiddenSize][inputSize];

        /*
         * Small deterministic initialization. The values produce a
         * stable ranking while preserving the same actor architecture
         * that will consume trained weights later.
         */
        for(int row = 0;
                row < hiddenSize;
                row++) {

            for(int column = 0;
                    column < inputSize;
                    column++) {

                hiddenWeights[row][column] =
                        (
                                (
                                        row + 1
                                )
                                * (
                                        column + 3
                                )
                                % 11
                                - 5
                        )
                        * 0.01;
            }
        }

        double[] hiddenBias =
                new double[hiddenSize];

        double[] outputWeights =
                new double[] {
                        0.08,
                        -0.04,
                        0.06,
                        0.05,
                        -0.03,
                        0.07,
                        0.02,
                        0.04
                };

        return new PpoConfig(
                DEFAULT_MODEL_ID,
                FEATURE_SCHEMA_ID,
                inputSize,
                hiddenSize,
                hiddenWeights,
                hiddenBias,
                outputWeights,
                0.0);
    }

    public static PpoConfig of(
            String modelId,
            int inputSize,
            int hiddenSize,
            double[][] hiddenWeights,
            double[] hiddenBias,
            double[] outputWeights,
            double outputBias) {

        return new PpoConfig(
                modelId,
                FEATURE_SCHEMA_ID,
                inputSize,
                hiddenSize,
                hiddenWeights,
                hiddenBias,
                outputWeights,
                outputBias);
    }

    public String getModelId() {
        return modelId;
    }

    public String getFeatureSchemaId() {
        return featureSchemaId;
    }

    public int getInputSize() {
        return inputSize;
    }

    public int getHiddenSize() {
        return hiddenSize;
    }

    public double[][] getHiddenWeights() {
        return deepCopy(hiddenWeights);
    }

    public double[] getHiddenBias() {
        return Arrays.copyOf(
                hiddenBias,
                hiddenBias.length);
    }

    public double[] getOutputWeights() {
        return Arrays.copyOf(
                outputWeights,
                outputWeights.length);
    }

    public double getOutputBias() {
        return outputBias;
    }

    private static String requireText(
            String value,
            String fieldName) {

        if(value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                    + " cannot be empty.");
        }

        return value.trim();
    }

    private static void validateMatrix(
            double[][] values,
            int expectedRows,
            int expectedColumns,
            String fieldName) {

        if(values == null
                || values.length
                != expectedRows) {

            throw new IllegalArgumentException(
                    fieldName
                    + " has an invalid row count.");
        }

        for(int row = 0;
                row < values.length;
                row++) {

            validateVector(
                    values[row],
                    expectedColumns,
                    fieldName
                    + "["
                    + row
                    + "]");
        }
    }

    private static void validateVector(
            double[] values,
            int expectedLength,
            String fieldName) {

        if(values == null
                || values.length
                != expectedLength) {

            throw new IllegalArgumentException(
                    fieldName
                    + " has an invalid length.");
        }

        for(double value : values) {

            validateFinite(
                    value,
                    fieldName);
        }
    }

    private static void validateFinite(
            double value,
            String fieldName) {

        if(Double.isNaN(value)
                || Double.isInfinite(value)) {

            throw new IllegalArgumentException(
                    fieldName
                    + " must contain finite values.");
        }
    }

    private static double[][] deepCopy(
            double[][] source) {

        double[][] copy =
                new double[source.length][];

        for(int row = 0;
                row < source.length;
                row++) {

            copy[row] =
                    Arrays.copyOf(
                            source[row],
                            source[row].length);
        }

        return copy;
    }
}
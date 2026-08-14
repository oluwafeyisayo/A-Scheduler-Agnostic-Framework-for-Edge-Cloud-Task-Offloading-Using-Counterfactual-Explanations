package edu.boun.edgecloudsim.research.counterfactual.replay.state;

import edu.boun.edgecloudsim.edge_orchestrator.ppo.PpoConfig;

/**
 * Immutable PPO policy provenance required for deterministic replay.
 *
 * PPO inference is stateless at decision time. Replay therefore needs
 * only the actor model identifier and feature-schema identifier.
 */
public final class PpoPolicyState
        implements SchedulerPolicyState {

    private final String modelId;
    private final String featureSchemaId;
    private final boolean deterministicInference;

    public PpoPolicyState(
            String modelId,
            String featureSchemaId,
            boolean deterministicInference) {

        this.modelId =
                requireText(
                        modelId,
                        "modelId");

        this.featureSchemaId =
                requireText(
                        featureSchemaId,
                        "featureSchemaId");

        this.deterministicInference =
                deterministicInference;
    }

    /**
     * Creates the policy state used by the current bootstrap actor.
     */
    public static PpoPolicyState createBootstrap() {

        PpoConfig config =
                PpoConfig.createBootstrap();

        return new PpoPolicyState(
                config.getModelId(),
                config.getFeatureSchemaId(),
                true);
    }

    @Override
    public String getPolicyName() {

        return "PPO";
    }

    public String getModelId() {

        return modelId;
    }

    public String getFeatureSchemaId() {

        return featureSchemaId;
    }

    public boolean isDeterministicInference() {

        return deterministicInference;
    }

    /**
     * Phase 2C uses the same bootstrap configuration as the live
     * scheduler. This method becomes the model-loader boundary when
     * trained PPO weights are exported.
     */
    public PpoConfig toConfig() {

        PpoConfig config =
                PpoConfig.createBootstrap();

        if(!config.getModelId().equals(
                modelId)) {

            throw new IllegalStateException(
                    "PPO replay model mismatch. Logged model="
                    + modelId
                    + ", available model="
                    + config.getModelId()
                    + ".");
        }

        if(!config.getFeatureSchemaId().equals(
                featureSchemaId)) {

            throw new IllegalStateException(
                    "PPO feature-schema mismatch. Logged schema="
                    + featureSchemaId
                    + ", available schema="
                    + config.getFeatureSchemaId()
                    + ".");
        }

        if(!deterministicInference) {

            throw new IllegalStateException(
                    "Counterfactual replay requires deterministic "
                    + "PPO inference.");
        }

        return config;
    }

    private static String requireText(
            String value,
            String fieldName) {

        if(value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                    + " cannot be null or empty.");
        }

        return value.trim();
    }

    @Override
    public String toString() {

        return "PpoPolicyState{"
                + "modelId='"
                + modelId
                + '\''
                + ", featureSchemaId='"
                + featureSchemaId
                + '\''
                + ", deterministicInference="
                + deterministicInference
                + '}';
    }
}
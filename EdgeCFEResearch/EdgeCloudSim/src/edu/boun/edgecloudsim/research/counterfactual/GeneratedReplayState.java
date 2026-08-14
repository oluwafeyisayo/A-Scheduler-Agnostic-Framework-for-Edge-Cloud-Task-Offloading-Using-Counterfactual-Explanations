package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Represents one automatically generated perturbation and its ReplayState.
 *
 * This object does not claim that the scheduler decision has changed.
 * Scheduler replay must determine that in a later stage.
 */
public final class GeneratedReplayState {

    private final ReplayState replayState;
    private final FeatureChange generatedChange;
    private final String generationMethod;
    private final String generationReason;

    public GeneratedReplayState(
            ReplayState replayState,
            FeatureChange generatedChange,
            String generationMethod,
            String generationReason) {

        if(replayState == null){

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        if(generatedChange == null){

            throw new IllegalArgumentException(
                    "Generated FeatureChange cannot be null.");
        }

        this.replayState =
                replayState;

        this.generatedChange =
                generatedChange;

        this.generationMethod =
                safeText(
                        generationMethod,
                        "UNKNOWN_GENERATOR");

        this.generationReason =
                safeText(
                        generationReason,
                        "NO_GENERATION_REASON");
    }

    private static String safeText(
            String value,
            String defaultValue) {

        if(value == null
                || value.trim().isEmpty()){

            return defaultValue;
        }

        return value.trim();
    }

    public ReplayState getReplayState() {
        return replayState;
    }

    public FeatureChange getGeneratedChange() {
        return generatedChange;
    }

    public String getGenerationMethod() {
        return generationMethod;
    }

    public String getGenerationReason() {
        return generationReason;
    }

    @Override
    public String toString() {

        return "GeneratedReplayState{"
                + "taskId="
                + replayState.getTaskId()
                + ", generationMethod='"
                + generationMethod
                + '\''
                + ", generationReason='"
                + generationReason
                + '\''
                + ", generatedChange="
                + generatedChange
                + ", cpuFeasible="
                + replayState.isCpuFeasible()
                + ", cpuMargin="
                + replayState.getCpuFeasibilityMargin()
                + '}';
    }
}
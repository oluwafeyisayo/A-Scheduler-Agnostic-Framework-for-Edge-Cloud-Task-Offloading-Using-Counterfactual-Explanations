package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Evaluates whether the task-level network path associated with the
 * originally selected execution resource remains valid.
 *
 * This rule is diagnostic. The scheduler may move the task to another
 * candidate when the original execution target becomes infeasible.
 */
public final class SelectedResourceNetworkFeasibilityRule
        implements FeasibilityRule {

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .SELECTED_RESOURCE_NETWORK_PATH;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        boolean validBandwidth =
                replayState
                .hasValidAvailableBandwidth();

        boolean validLatency =
                replayState
                .hasValidNetworkLatency();

        boolean validUploadSize =
                replayState
                .hasValidUploadSize();

        double transmissionDelayMs =
                replayState
                .estimateUploadTransmissionTimeMs();

        double totalNetworkDelayMs =
                replayState
                .estimateTotalNetworkDelayMs();

        boolean finiteTransmissionDelay =
                isFiniteNonNegative(
                        transmissionDelayMs);

        boolean finiteTotalDelay =
                isFiniteNonNegative(
                        totalNetworkDelayMs);

        boolean passed =
                validBandwidth
                && validLatency
                && validUploadSize
                && finiteTransmissionDelay
                && finiteTotalDelay;

        double observedValue =
                totalNetworkDelayMs;

        double requiredValue =
                0.0;

        /*
         * There is not yet an application deadline. A positive margin
         * therefore represents a valid finite path, not deadline slack.
         */
        double margin =
                passed
                ? 1.0
                : -1.0;

        String explanation;

        if(passed) {

            explanation =
                    "The selected-resource network path is valid. "
                    + "Bandwidth="
                    + replayState.getAvailableBandwidthMbps()
                    + " Mbps, latency="
                    + replayState.getNetworkLatencyMs()
                    + " ms, upload size="
                    + replayState.getUploadSizeKb()
                    + " KB, transmission delay="
                    + transmissionDelayMs
                    + " ms, total network delay="
                    + totalNetworkDelayMs
                    + " ms.";
        }
        else {

            explanation =
                    "The selected-resource network path is invalid. "
                    + "validBandwidth="
                    + validBandwidth
                    + ", validLatency="
                    + validLatency
                    + ", validUploadSize="
                    + validUploadSize
                    + ", finiteTransmissionDelay="
                    + finiteTransmissionDelay
                    + ", finiteTotalDelay="
                    + finiteTotalDelay
                    + ".";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                passed,
                observedValue,
                requiredValue,
                margin,
                explanation);
    }

    private boolean isFiniteNonNegative(
            double value) {

        return !Double.isNaN(value)
                && !Double.isInfinite(value)
                && value >= 0.0;
    }
}
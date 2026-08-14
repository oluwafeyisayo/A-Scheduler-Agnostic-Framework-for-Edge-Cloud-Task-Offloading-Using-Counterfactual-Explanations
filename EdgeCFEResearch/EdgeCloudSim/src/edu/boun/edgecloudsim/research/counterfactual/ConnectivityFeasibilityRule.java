package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Verifies that the reconstructed scheduler state contains a usable
 * task-level communication path.
 *
 * Connectivity is defined using the network state available in
 * ReplayState: valid bandwidth, valid latency, valid upload size, and
 * a finite reconstructed network delay.
 */
public final class ConnectivityFeasibilityRule
        implements FeasibilityRule {

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint.CONNECTIVITY;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        boolean bandwidthValid =
                replayState.hasValidAvailableBandwidth();

        boolean latencyValid =
                replayState.hasValidNetworkLatency();

        boolean uploadSizeValid =
                replayState.hasValidUploadSize();

        double transmissionDelayMs =
                replayState
                .estimateUploadTransmissionTimeMs();

        double totalNetworkDelayMs =
                replayState
                .estimateTotalNetworkDelayMs();

        boolean transmissionDelayValid =
                isFiniteNonNegative(
                        transmissionDelayMs);

        boolean totalDelayValid =
                isFiniteNonNegative(
                        totalNetworkDelayMs);

        boolean connected =
                bandwidthValid
                && latencyValid
                && uploadSizeValid
                && transmissionDelayValid
                && totalDelayValid;

        double observedValue =
                connected
                ? 1.0
                : 0.0;

        double requiredValue =
                1.0;

        double margin =
                observedValue
                - requiredValue;

        String explanation;

        if(connected) {

            explanation =
                    "The reconstructed task-level communication "
                    + "path is connected. Bandwidth="
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
                    "The reconstructed task-level communication "
                    + "path is disconnected or invalid. "
                    + "Bandwidth valid="
                    + bandwidthValid
                    + ", latency valid="
                    + latencyValid
                    + ", upload size valid="
                    + uploadSizeValid
                    + ", transmission delay valid="
                    + transmissionDelayValid
                    + ", total network delay valid="
                    + totalDelayValid
                    + ".";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                connected,
                observedValue,
                requiredValue,
                margin,
                explanation);
    }

    private static boolean isFiniteNonNegative(
            double value) {

        return !Double.isNaN(value)
                && !Double.isInfinite(value)
                && value >= 0.0;
    }
}
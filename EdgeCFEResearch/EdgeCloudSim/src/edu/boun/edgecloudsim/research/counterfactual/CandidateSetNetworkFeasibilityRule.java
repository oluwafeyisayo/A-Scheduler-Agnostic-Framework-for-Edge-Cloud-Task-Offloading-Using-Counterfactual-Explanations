package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

/**
 * Determines whether the reconstructed task-level network path is
 * feasible for at least one operational candidate VM.
 *
 * Network state is currently stored once per task in ReplayState, so
 * all candidate VMs associated with the task share the same network
 * feasibility result.
 */
public final class CandidateSetNetworkFeasibilityRule
        implements CandidateSetFeasibilityRule {

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .CANDIDATE_SET_NETWORK_PATH;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidateSnapshots) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        if(candidateSnapshots == null) {

            throw new IllegalArgumentException(
                    "VM candidate list cannot be null.");
        }

        double transmissionDelayMs =
                replayState
                .estimateUploadTransmissionTimeMs();

        double totalNetworkDelayMs =
                replayState
                .estimateTotalNetworkDelayMs();

        boolean networkPathFeasible =
                replayState.hasValidAvailableBandwidth()
                && replayState.hasValidNetworkLatency()
                && replayState.hasValidUploadSize()
                && isFiniteNonNegative(
                        transmissionDelayMs)
                && isFiniteNonNegative(
                        totalNetworkDelayMs);

        int operationalCandidateCount =
                0;

        for(VmCandidateSnapshot candidate
                : candidateSnapshots) {

            if(candidate != null
                    && candidate
                    .isOperationallyAvailable()) {

                operationalCandidateCount++;
            }
        }

        /*
         * Because network state is task-level, the candidate set is
         * network feasible only when:
         *
         * 1. the reconstructed network path is valid; and
         * 2. at least one candidate VM is operationally available.
         */
        int networkFeasibleCandidateCount =
                networkPathFeasible
                ? operationalCandidateCount
                : 0;

        boolean passed =
                networkFeasibleCandidateCount > 0;

        double observedValue =
                networkFeasibleCandidateCount;

        double requiredValue =
                1.0;

        double margin =
                observedValue
                - requiredValue;

        String explanation;

        if(candidateSnapshots.isEmpty()) {

            explanation =
                    "No VM candidate snapshot is available for "
                    + "candidate-set network evaluation.";
        }
        else if(operationalCandidateCount == 0) {

            explanation =
                    "No candidate VM is operationally available, "
                    + "so the candidate set cannot satisfy the "
                    + "network-path requirement.";
        }
        else if(!networkPathFeasible) {

            explanation =
                    "The reconstructed task-level network path is "
                    + "invalid. Bandwidth="
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
                    "The reconstructed task-level network path is "
                    + "valid for the candidate set. Operational "
                    + "candidate count="
                    + operationalCandidateCount
                    + ", network-feasible candidate count="
                    + networkFeasibleCandidateCount
                    + ", bandwidth="
                    + replayState.getAvailableBandwidthMbps()
                    + " Mbps, latency="
                    + replayState.getNetworkLatencyMs()
                    + " ms, upload size="
                    + replayState.getUploadSizeKb()
                    + " KB, total network delay="
                    + totalNetworkDelayMs
                    + " ms.";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                passed,
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
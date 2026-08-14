package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

/**
 * Determines whether at least one operationally available VM
 * simultaneously satisfies:
 *
 * 1. CPU-capacity requirements
 * 2. Core-count requirements
 * 3. Processing-capacity requirements
 * 4. The reconstructed task-level network-path requirement
 *
 * The same VM must satisfy every compute requirement.
 *
 * Network state is currently reconstructed at task level and therefore
 * applies equally to all VM candidates associated with the task.
 */
public final class CandidateSetComputeNetworkFeasibilityRule
        implements CandidateSetFeasibilityRule {

    private static final double EPSILON =
            0.000001;

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .CANDIDATE_SET_COMPUTE_NETWORK_FEASIBILITY;
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

        boolean networkPathValid =
                replayState.hasValidAvailableBandwidth()
                && replayState.hasValidNetworkLatency()
                && replayState.hasValidUploadSize()
                && isFiniteNonNegative(
                        transmissionDelayMs)
                && isFiniteNonNegative(
                        totalNetworkDelayMs);

        double requiredCpuPercent =
                replayState.getRequiredCpuPercent();

        int requiredCoreCount =
                replayState.getRequiredCores();

        int operationalCandidateCount =
                0;

        int computeFeasibleCandidateCount =
                0;

        int combinedFeasibleCandidateCount =
                0;

        VmCandidateSnapshot firstCombinedCandidate =
                null;

        VmCandidateSnapshot bestCpuCandidate =
                null;

        double bestCpuMargin =
                Double.NEGATIVE_INFINITY;

        for(VmCandidateSnapshot candidate
                : candidateSnapshots) {

            if(candidate == null
                    || !candidate
                    .isOperationallyAvailable()) {

                continue;
            }

            operationalCandidateCount++;

            double availableCpuPercent =
                    candidate.getAvailableCpuPercent();

            double cpuMargin =
                    availableCpuPercent
                    - requiredCpuPercent;

            if(cpuMargin > bestCpuMargin) {

                bestCpuMargin =
                        cpuMargin;

                bestCpuCandidate =
                        candidate;
            }

            boolean cpuFeasible =
                    isFiniteNonNegative(
                            availableCpuPercent)
                    && availableCpuPercent
                            + EPSILON
                            >= requiredCpuPercent;

            boolean coreFeasible =
                    candidate.getVmCoreCount()
                    >= requiredCoreCount;

            double vmMips =
                    candidate.getVmMips();

            boolean processingCapacityValid =
                    !Double.isNaN(vmMips)
                    && !Double.isInfinite(vmMips)
                    && vmMips > 0.0;

            /*
             * Compute feasibility is recalculated from the current
             * synchronized replay state.
             *
             * This avoids relying on stale feasibility flags stored
             * in the original immutable VM snapshot.
             */
            boolean computeFeasible =
                    cpuFeasible
                    && coreFeasible
                    && processingCapacityValid;

            if(computeFeasible) {

                computeFeasibleCandidateCount++;
            }

            boolean combinedFeasible =
                    computeFeasible
                    && networkPathValid;

            if(combinedFeasible) {

                combinedFeasibleCandidateCount++;

                if(firstCombinedCandidate == null) {

                    firstCombinedCandidate =
                            candidate;
                }
            }
        }

        boolean passed =
                combinedFeasibleCandidateCount > 0;

        double observedValue =
                combinedFeasibleCandidateCount;

        double requiredValue =
                1.0;

        double margin =
                observedValue
                - requiredValue;

        String explanation;

        if(candidateSnapshots.isEmpty()) {

            explanation =
                    "No VM candidate snapshot is available for "
                    + "combined compute-network evaluation.";
        }
        else if(operationalCandidateCount == 0) {

            explanation =
                    "No candidate VM is operationally available.";
        }
        else if(!networkPathValid) {

            explanation =
                    "No candidate can satisfy combined feasibility "
                    + "because the reconstructed task-level network "
                    + "path is invalid. Bandwidth="
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
        else if(computeFeasibleCandidateCount == 0) {

            explanation =
                    "The reconstructed task-level network path is "
                    + "valid, but no operational candidate satisfies "
                    + "CPU capacity, core capacity, and valid "
                    + "processing-capacity requirements. "
                    + "Operational candidates="
                    + operationalCandidateCount
                    + ", required CPU="
                    + requiredCpuPercent
                    + "%, required cores="
                    + requiredCoreCount
                    + ", best CPU candidate="
                    + describeCandidate(
                            bestCpuCandidate)
                    + ".";
        }
        else {

            explanation =
                    "At least one candidate VM simultaneously "
                    + "satisfies compute feasibility and the "
                    + "reconstructed task-level network-path "
                    + "requirement. Operational candidates="
                    + operationalCandidateCount
                    + ", compute-feasible candidates="
                    + computeFeasibleCandidateCount
                    + ", combined-feasible candidates="
                    + combinedFeasibleCandidateCount
                    + ", first combined-feasible candidate="
                    + describeCandidate(
                            firstCombinedCandidate)
                    + ", total network delay="
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

    private static String describeCandidate(
            VmCandidateSnapshot candidate) {

        if(candidate == null) {

            return "NONE";
        }

        return "candidateIndex="
                + candidate.getCandidateIndex()
                + ", vmId="
                + candidate.getVmId()
                + ", hostId="
                + candidate.getHostId()
                + ", datacenterId="
                + candidate.getDatacenterId()
                + ", destination="
                + candidate.getDestination()
                + ", availableCpu="
                + candidate.getAvailableCpuPercent()
                + "%, vmCores="
                + candidate.getVmCoreCount()
                + ", vmMips="
                + candidate.getVmMips();
    }
}
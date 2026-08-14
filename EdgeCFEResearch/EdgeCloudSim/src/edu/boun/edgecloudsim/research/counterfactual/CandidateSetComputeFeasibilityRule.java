package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

/**
 * Determines whether at least one operationally available VM
 * simultaneously satisfies CPU, core-count, and processing-capacity
 * requirements.
 *
 * This prevents candidate-set feasibility from being incorrectly
 * established using different VMs for different constraints.
 */
public final class CandidateSetComputeFeasibilityRule
        implements CandidateSetFeasibilityRule {

    private static final double EPSILON =
            0.000001;

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .CANDIDATE_SET_COMPUTE_FEASIBILITY;
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

        double requiredCpuPercent =
                replayState.getRequiredCpuPercent();

        int requiredCoreCount =
                replayState.getRequiredCores();

        int operationalCandidateCount =
                0;

        int computeFeasibleCandidateCount =
                0;

        double bestCpuMargin =
                Double.NEGATIVE_INFINITY;

        VmCandidateSnapshot bestComputeCandidate =
                null;

        for(VmCandidateSnapshot candidate
                : candidateSnapshots) {

            if(candidate == null
                    || !candidate.isOperationallyAvailable()) {

                continue;
            }

            operationalCandidateCount++;

            double cpuMargin =
                    candidate.getAvailableCpuPercent()
                    - requiredCpuPercent;

            if(cpuMargin > bestCpuMargin) {

                bestCpuMargin =
                        cpuMargin;

                bestComputeCandidate =
                        candidate;
            }

            boolean cpuFeasible =
                    candidate.getAvailableCpuPercent()
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

            if(cpuFeasible
                    && coreFeasible
                    && processingCapacityValid) {

                computeFeasibleCandidateCount++;
            }
        }

        boolean passed =
                computeFeasibleCandidateCount > 0;

        double observedValue =
                computeFeasibleCandidateCount;

        double requiredValue =
                1.0;

        double margin =
                computeFeasibleCandidateCount
                - requiredValue;

        String explanation;

        if(candidateSnapshots.isEmpty()) {

            explanation =
                    "No VM candidate snapshot is available for "
                    + "combined compute-feasibility evaluation.";
        }
        else if(operationalCandidateCount == 0) {

            explanation =
                    "No candidate VM is operationally available.";
        }
        else if(!passed) {

            explanation =
                    "No single operationally available VM "
                    + "simultaneously satisfies CPU capacity, core "
                    + "capacity, and valid processing capacity. "
                    + "Operational candidate count="
                    + operationalCandidateCount
                    + ", required CPU="
                    + requiredCpuPercent
                    + "%, required cores="
                    + requiredCoreCount
                    + ", best CPU candidate="
                    + describeCandidate(
                            bestComputeCandidate)
                    + ".";
        }
        else {

            explanation =
                    "At least one operationally available VM "
                    + "simultaneously satisfies CPU capacity, core "
                    + "capacity, and valid processing capacity. "
                    + "Compute-feasible candidate count="
                    + computeFeasibleCandidateCount
                    + ", operational candidate count="
                    + operationalCandidateCount
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

    private String describeCandidate(
            VmCandidateSnapshot candidate) {

        if(candidate == null) {

            return "NONE";
        }

        return "candidateIndex="
                + candidate.getCandidateIndex()
                + ", vmId="
                + candidate.getVmId()
                + ", availableCpu="
                + candidate.getAvailableCpuPercent()
                + "%, vmCores="
                + candidate.getVmCoreCount()
                + ", vmMips="
                + candidate.getVmMips();
    }
}
package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

/**
 * Determines whether at least one operationally available candidate
 * VM satisfies the task's CPU requirement.
 *
 * Candidate feasibility is evaluated directly from the synchronized
 * VM snapshots rather than from aggregate ReplayState statistics.
 */
public final class CandidateSetCpuFeasibilityRule
        implements CandidateSetFeasibilityRule {

    private static final double EPSILON =
            0.000001;

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .CANDIDATE_SET_CPU_CAPACITY;
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

        double bestAvailableCpuPercent =
                Double.NEGATIVE_INFINITY;

        int operationalCandidateCount =
                0;

        int cpuFeasibleCandidateCount =
                0;

        for(VmCandidateSnapshot candidate
                : candidateSnapshots) {

            if(candidate == null) {

                continue;
            }

            if(!candidate.isOperationallyAvailable()) {

                continue;
            }

            operationalCandidateCount++;

            double availableCpuPercent =
                    candidate.getAvailableCpuPercent();

            if(availableCpuPercent
                    > bestAvailableCpuPercent) {

                bestAvailableCpuPercent =
                        availableCpuPercent;
            }

            if(availableCpuPercent + EPSILON
                    >= requiredCpuPercent) {

                cpuFeasibleCandidateCount++;
            }
        }

        boolean hasOperationalCandidates =
                operationalCandidateCount > 0;

        boolean passed =
                cpuFeasibleCandidateCount > 0;

        double observedValue =
                hasOperationalCandidates
                ? bestAvailableCpuPercent
                : Double.NaN;

        double margin =
                hasOperationalCandidates
                ? bestAvailableCpuPercent
                        - requiredCpuPercent
                : Double.NaN;

        String explanation;

        if(candidateSnapshots.isEmpty()) {

            explanation =
                    "No candidate VM snapshot is available for "
                    + "scheduler replay.";
        }
        else if(!hasOperationalCandidates) {

            explanation =
                    "Candidate VM snapshots are available, but none "
                    + "is operationally available.";
        }
        else if(!passed) {

            explanation =
                    "No operationally available candidate VM "
                    + "satisfies the required CPU capacity. "
                    + "Required CPU="
                    + requiredCpuPercent
                    + "%, best available CPU="
                    + bestAvailableCpuPercent
                    + "%, operational candidate count="
                    + operationalCandidateCount
                    + ".";
        }
        else {

            explanation =
                    "At least one operationally available candidate "
                    + "VM satisfies the required CPU capacity. "
                    + "Required CPU="
                    + requiredCpuPercent
                    + "%, best available CPU="
                    + bestAvailableCpuPercent
                    + "%, CPU-feasible candidate count="
                    + cpuFeasibleCandidateCount
                    + ".";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                passed,
                observedValue,
                requiredCpuPercent,
                margin,
                explanation);
    }
}
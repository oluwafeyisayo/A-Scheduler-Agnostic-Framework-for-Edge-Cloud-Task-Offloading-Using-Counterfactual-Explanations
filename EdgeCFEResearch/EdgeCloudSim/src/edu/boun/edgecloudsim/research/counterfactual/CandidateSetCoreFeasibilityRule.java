package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

/**
 * Determines whether at least one operationally available candidate
 * VM satisfies the task's required core count.
 */
public final class CandidateSetCoreFeasibilityRule
        implements CandidateSetFeasibilityRule {

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .CANDIDATE_SET_CORE_CAPACITY;
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

        int requiredCoreCount =
                replayState.getRequiredCores();

        int bestAvailableCoreCount =
                -1;

        int operationalCandidateCount =
                0;

        int coreFeasibleCandidateCount =
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

            int availableCoreCount =
                    candidate.getVmCoreCount();

            if(availableCoreCount
                    > bestAvailableCoreCount) {

                bestAvailableCoreCount =
                        availableCoreCount;
            }

            if(availableCoreCount
                    >= requiredCoreCount) {

                coreFeasibleCandidateCount++;
            }
        }

        boolean hasOperationalCandidates =
                operationalCandidateCount > 0;

        boolean passed =
                coreFeasibleCandidateCount > 0;

        double observedValue =
                hasOperationalCandidates
                ? bestAvailableCoreCount
                : Double.NaN;

        double margin =
                hasOperationalCandidates
                ? bestAvailableCoreCount
                        - requiredCoreCount
                : Double.NaN;

        String explanation;

        if(candidateSnapshots.isEmpty()) {

            explanation =
                    "No candidate VM snapshot is available for "
                    + "core-capacity evaluation.";
        }
        else if(!hasOperationalCandidates) {

            explanation =
                    "Candidate VM snapshots are available, but none "
                    + "is operationally available.";
        }
        else if(!passed) {

            explanation =
                    "No operationally available candidate VM "
                    + "satisfies the required core count. "
                    + "Required cores="
                    + requiredCoreCount
                    + ", highest candidate core count="
                    + bestAvailableCoreCount
                    + ", operational candidate count="
                    + operationalCandidateCount
                    + ".";
        }
        else {

            explanation =
                    "At least one operationally available candidate "
                    + "VM satisfies the required core count. "
                    + "Required cores="
                    + requiredCoreCount
                    + ", highest candidate core count="
                    + bestAvailableCoreCount
                    + ", core-feasible candidate count="
                    + coreFeasibleCandidateCount
                    + ".";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                passed,
                observedValue,
                requiredCoreCount,
                margin,
                explanation);
    }
}
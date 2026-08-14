package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

/**
 * Determines whether the candidate set contains at least one
 * operational candidate that can be reached through the reconstructed
 * task-level network path.
 *
 * The current provenance model stores network state once per task in
 * ReplayState. Therefore, all candidate VMs share the same reconstructed
 * connectivity status.
 */
public final class CandidateSetConnectivityRule
        implements CandidateSetFeasibilityRule {

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .CANDIDATE_SET_CONNECTIVITY;
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
                    "Candidate list cannot be null.");
        }

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

        boolean networkPathAvailable =
                replayState.isNetworkPathFeasible();

        /*
         * Because connectivity is currently task-level, every
         * operational candidate is reachable when the reconstructed
         * task network path is valid.
         */
        int reachableCandidateCount =
                networkPathAvailable
                ? operationalCandidateCount
                : 0;

        boolean passed =
                reachableCandidateCount > 0;

        double observedValue =
                reachableCandidateCount;

        double requiredValue =
                1.0;

        double margin =
                observedValue
                - requiredValue;

        String explanation;

        if(candidateSnapshots.isEmpty()) {

            explanation =
                    "No candidate VM snapshots are available for "
                    + "connectivity evaluation.";
        }
        else if(operationalCandidateCount == 0) {

            explanation =
                    "The candidate set contains no operationally "
                    + "available VM.";
        }
        else if(!networkPathAvailable) {

            explanation =
                    "The candidate set contains "
                    + operationalCandidateCount
                    + " operational candidate(s), but the "
                    + "reconstructed task-level network path is "
                    + "not feasible.";
        }
        else {

            explanation =
                    "The reconstructed task-level network path is "
                    + "available and "
                    + reachableCandidateCount
                    + " operational candidate(s) are reachable.";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                passed,
                observedValue,
                requiredValue,
                margin,
                explanation);
    }
}
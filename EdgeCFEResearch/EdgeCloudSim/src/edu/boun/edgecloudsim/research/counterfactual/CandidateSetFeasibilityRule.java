package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

/**
 * Common interface for feasibility rules that must inspect the
 * complete synchronized VM candidate set.
 *
 * These rules complement FeasibilityRule, whose evaluations depend
 * only on ReplayState.
 */
public interface CandidateSetFeasibilityRule {

    FeasibilityConstraint getConstraint();

    ConstraintEvaluation evaluate(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidateSnapshots);
}
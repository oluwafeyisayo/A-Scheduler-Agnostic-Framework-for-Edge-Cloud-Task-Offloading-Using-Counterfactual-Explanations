package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Assigns a semantic research classification to an evaluated
 * counterfactual candidate.
 */
public final class CounterfactualClassifier {

    /**
     * Classifies one counterfactual using validity, replay,
     * decision-change, and operational-feasibility information.
     *
     * Candidate validity is independent of scheduler outcome.
     *
     * A structurally valid candidate may replay without selecting a VM.
     * Such a candidate is ineffective, but it is not automatically invalid.
     *
     * @param taskId task identifier
     * @param candidateId counterfactual candidate identifier
     * @param candidateValid whether the candidate passed initial
     *        feature and structural validation
     * @param replaySuccessful whether scheduler replay produced a
     *        successful scheduling selection
     * @param targetAchieved whether the requested decision change
     *        was produced
     * @param operationallyFeasible whether mandatory constraints passed
     *
     * @return counterfactual classification result
     */
    public CounterfactualClassificationResult classify(
            long taskId,
            long candidateId,
            boolean candidateValid,
            boolean replaySuccessful,
            boolean targetAchieved,
            boolean operationallyFeasible) {

        /*
         * INVALID is reserved only for malformed or structurally
         * unusable counterfactual candidates.
         */
        if(!candidateValid) {

            return new CounterfactualClassificationResult(
                    taskId,
                    candidateId,
                    CounterfactualClassification.INVALID,
                    false,
                    replaySuccessful,
                    false,
                    false,
                    "The candidate failed feature or structural validation.");
        }

        /*
         * A valid candidate that does not result in a successful VM
         * selection is ineffective rather than invalid.
         *
         * For feasibility-restoration searches, this includes candidates
         * whose replay still returns NO_FEASIBLE_VM.
         */
        if(!replaySuccessful) {

            if(operationallyFeasible) {

                return new CounterfactualClassificationResult(
                        taskId,
                        candidateId,
                        CounterfactualClassification.FEASIBLE_INEFFECTIVE,
                        true,
                        false,
                        false,
                        true,
                        "The counterfactual candidate was structurally "
                        + "valid and operationally feasible, but scheduler "
                        + "replay did not produce a successful selection "
                        + "or the requested decision change.");
            }

            return new CounterfactualClassificationResult(
                    taskId,
                    candidateId,
                    CounterfactualClassification.INFEASIBLE_INEFFECTIVE,
                    true,
                    false,
                    false,
                    false,
                    "The counterfactual candidate was structurally valid, "
                    + "but remained operationally infeasible and scheduler "
                    + "replay did not produce the requested decision change.");
        }

        if(targetAchieved
                && operationallyFeasible) {

            return new CounterfactualClassificationResult(
                    taskId,
                    candidateId,
                    CounterfactualClassification.FEASIBLE_EFFECTIVE,
                    true,
                    true,
                    true,
                    true,
                    "The counterfactual was operationally feasible "
                    + "and produced the requested scheduler decision change.");
        }

        if(!targetAchieved
                && operationallyFeasible) {

            return new CounterfactualClassificationResult(
                    taskId,
                    candidateId,
                    CounterfactualClassification.FEASIBLE_INEFFECTIVE,
                    true,
                    true,
                    false,
                    true,
                    "The counterfactual was operationally feasible, "
                    + "but the requested scheduler decision change "
                    + "was not produced.");
        }

        if(targetAchieved
                && !operationallyFeasible) {

            return new CounterfactualClassificationResult(
                    taskId,
                    candidateId,
                    CounterfactualClassification.INFEASIBLE_EFFECTIVE,
                    true,
                    true,
                    true,
                    false,
                    "The counterfactual produced the requested scheduler "
                    + "decision change, but violated one or more mandatory "
                    + "operational constraints.");
        }

        return new CounterfactualClassificationResult(
                taskId,
                candidateId,
                CounterfactualClassification.INFEASIBLE_INEFFECTIVE,
                true,
                true,
                false,
                false,
                "The counterfactual was operationally infeasible and "
                + "did not produce the requested scheduler decision change.");
    }
}
package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Determines whether an adaptive counterfactual search should receive
 * an additional replay-evaluation allowance.
 *
 * The initial adaptive budget is calculated separately by
 * AdaptiveSearchBudgetCalculator.
 *
 * This policy controls later extension stages using stage-local replay
 * evidence rather than cumulative evidence.
 *
 * Policy:
 *
 * 1. Never extend a non-adaptive search.
 * 2. Never extend after an accepted counterfactual has been found.
 * 3. Never exceed the generated search space.
 * 4. Never exceed the configured adaptive maximum.
 * 5. Never exceed the configured extension-count ceiling.
 * 6. Grant one recovery extension after the initial adaptive stage.
 * 7. Grant later extensions only when the most recently completed
 *    stage produced new and diverse boundary evidence.
 */
public final class AdaptiveSearchExtensionPolicy {

    /**
     * Determines whether another adaptive evaluation stage should be
     * granted.
     *
     * The cumulative counts describe everything observed so far.
     * The stage-start counts describe the state at the beginning of
     * the most recently completed evaluation stage.
     *
     * Their differences are the stage-local classification counts.
     */
    public boolean shouldExtend(
            FeasibleCounterfactualSearchConfig config,
            int currentEvaluationBudget,
            int generatedCandidateCount,
            int evaluatedCandidateCount,
            int acceptedCandidateCount,
            int feasibleIneffectiveCount,
            int infeasibleEffectiveCount,
            int infeasibleIneffectiveCount,
            int invalidCount,
            int completedExtensionCount,
            int stageStartEvaluatedCount,
            int stageStartAcceptedCount,
            int stageStartFeasibleIneffectiveCount,
            int stageStartInfeasibleEffectiveCount,
            int stageStartInfeasibleIneffectiveCount,
            int stageStartInvalidCount) {

        if(config == null) {

            throw new IllegalArgumentException(
                    "Search configuration cannot be null.");
        }

        /*
         * Extension applies only to adaptive best-first search.
         */
        if(!config.isAdaptiveBestFirstSearch()) {

            return false;
        }

        /*
         * No further replay allocation is needed after discovering an
         * accepted counterfactual.
         */
        if(acceptedCandidateCount > 0) {

            return false;
        }

        /*
         * The current stage must be completely evaluated before its
         * replay evidence can be assessed.
         */
        if(evaluatedCandidateCount
                < currentEvaluationBudget) {

            return false;
        }

        /*
         * There is no remaining search frontier.
         */
        if(evaluatedCandidateCount
                >= generatedCandidateCount) {

            return false;
        }

        /*
         * Respect the configured adaptive evaluation ceiling.
         */
        if(currentEvaluationBudget
                >= config
                .getMaximumAdaptiveEvaluationBudget()) {

            return false;
        }

        /*
         * Respect the configured extension-count safety ceiling.
         */
        if(completedExtensionCount
                >= config
                .getMaximumAdaptiveBudgetExtensions()) {

            return false;
        }

        /*
         * Derive the evidence contributed only by the stage that has
         * just ended.
         */
        int stageEvaluatedCount =
                evaluatedCandidateCount
                - stageStartEvaluatedCount;

        int stageAcceptedCount =
                acceptedCandidateCount
                - stageStartAcceptedCount;

        int stageFeasibleIneffectiveCount =
                feasibleIneffectiveCount
                - stageStartFeasibleIneffectiveCount;

        int stageInfeasibleEffectiveCount =
                infeasibleEffectiveCount
                - stageStartInfeasibleEffectiveCount;

        int stageInfeasibleIneffectiveCount =
                infeasibleIneffectiveCount
                - stageStartInfeasibleIneffectiveCount;

        int stageInvalidCount =
                invalidCount
                - stageStartInvalidCount;

        /*
         * Defensive consistency checks.
         */
        if(stageEvaluatedCount <= 0
                || stageAcceptedCount < 0
                || stageFeasibleIneffectiveCount < 0
                || stageInfeasibleEffectiveCount < 0
                || stageInfeasibleIneffectiveCount < 0
                || stageInvalidCount < 0) {

            return false;
        }

        /*
         * This condition is normally covered by the cumulative
         * accepted-count check. It is retained to make the stage logic
         * explicit.
         */
        if(stageAcceptedCount > 0) {

            return false;
        }

        /*
         * Grant one recovery extension after the initial adaptive
         * stage.
         *
         * This protects against an initial budget that ends shortly
         * before an accepted counterfactual, as occurred for task 11.
         */
        if(completedExtensionCount == 0) {

            return true;
        }

        /*
         * New decision-boundary evidence:
         *
         * The most recent stage found candidates that changed the
         * requested scheduler decision but remained operationally
         * infeasible.
         */
        boolean newEffectiveBoundaryEvidence =
                stageInfeasibleEffectiveCount > 0;

        /*
         * Boundary diversity:
         *
         * Effective-but-infeasible candidates are more informative
         * when the same stage also contains candidates on another side
         * of the search boundary.
         *
         * INFEASIBLE_INEFFECTIVE indicates a transition in scheduler
         * effectiveness while feasibility remains violated.
         *
         * FEASIBLE_INEFFECTIVE indicates a transition in operational
         * feasibility while the scheduler target has not yet changed.
         */
        boolean newBoundaryDiversity =
                stageInfeasibleIneffectiveCount > 0
                || stageFeasibleIneffectiveCount > 0;

        /*
         * A stage containing only INFEASIBLE_EFFECTIVE candidates does
         * not justify repeated global-frontier extension.
         *
         * Such a pattern is better handled later by replay-guided
         * neighborhood refinement.
         */
        return newEffectiveBoundaryEvidence
                && newBoundaryDiversity;
    }

    /**
     * Calculates the budget for the next evaluation stage.
     */
    public int calculateExtendedBudget(
            FeasibleCounterfactualSearchConfig config,
            int currentEvaluationBudget,
            int generatedCandidateCount) {

        if(config == null) {

            throw new IllegalArgumentException(
                    "Search configuration cannot be null.");
        }

        if(generatedCandidateCount <= 0) {

            return 0;
        }

        long proposedBudget =
                (long) currentEvaluationBudget
                + config.getAdaptiveBudgetExtensionSize();

        int maximumPermittedBudget =
                Math.min(
                        config
                        .getMaximumAdaptiveEvaluationBudget(),
                        generatedCandidateCount);

        return (int) Math.min(
                proposedBudget,
                maximumPermittedBudget);
    }
}
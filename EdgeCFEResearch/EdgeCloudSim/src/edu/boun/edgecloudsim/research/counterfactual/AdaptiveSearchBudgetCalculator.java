package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Calculates a task-specific evaluation budget for adaptive
 * counterfactual search.
 *
 * The budget is based on:
 *
 * 1. A guaranteed minimum evaluation allowance
 * 2. The number of searchable features
 * 3. The number of VM candidates in the reconstructed decision
 * 4. The size of the generated counterfactual search space
 * 5. A small uncertainty allowance when multiple execution
 *    candidates exist
 *
 * The calculated budget is always bounded by:
 *
 * - the configured minimum adaptive budget
 * - the configured maximum adaptive budget
 * - the number of candidates actually generated
 */
public final class AdaptiveSearchBudgetCalculator {

    /*
     * Additional evaluations allocated per searchable feature.
     */
    private static final int EVALUATIONS_PER_FEATURE =
            5;

    /*
     * Additional evaluations allocated per original VM candidate.
     */
    private static final int EVALUATIONS_PER_VM_CANDIDATE =
            2;

    /*
     * Maximum number of VM candidates that contribute directly
     * to the budget. This prevents very large candidate sets from
     * immediately forcing the maximum budget.
     */
    private static final int MAXIMUM_COUNTED_VM_CANDIDATES =
            50;

    /*
     * Fraction of the generated search space reserved for broader
     * exploration.
     */
    private static final double SEARCH_SPACE_ALLOWANCE_RATIO =
            0.10;

    /*
     * Additional allowance when the original scheduler had more
     * than one possible execution candidate.
     */
    private static final int MULTI_CANDIDATE_UNCERTAINTY_ALLOWANCE =
            20;

    /**
     * Calculates the evaluation budget for one counterfactual
     * search.
     *
     * @param config
     *        counterfactual search configuration
     *
     * @param generatedCandidateCount
     *        number of counterfactual candidates generated for
     *        the task
     *
     * @param originalVmCandidateCount
     *        number of VM candidates available to the original
     *        scheduler decision
     *
     * @return bounded adaptive evaluation budget
     */
    public int calculateEvaluationBudget(
            FeasibleCounterfactualSearchConfig config,
            int generatedCandidateCount,
            int originalVmCandidateCount) {

        if(config == null) {

            throw new IllegalArgumentException(
                    "Search configuration cannot be null.");
        }

        if(generatedCandidateCount <= 0) {

            return 0;
        }

        int safeVmCandidateCount =
                Math.max(
                        0,
                        originalVmCandidateCount);

        int minimumBudget =
                config
                .getMinimumAdaptiveEvaluationBudget();

        int maximumBudget =
                config
                .getMaximumAdaptiveEvaluationBudget();

        int featureAllowance =
                config
                .getSearchableFeatureCount()
                * EVALUATIONS_PER_FEATURE;

        int countedVmCandidates =
                Math.min(
                        safeVmCandidateCount,
                        MAXIMUM_COUNTED_VM_CANDIDATES);

        int candidateSetAllowance =
                countedVmCandidates
                * EVALUATIONS_PER_VM_CANDIDATE;

        int searchSpaceAllowance =
                (int) Math.ceil(
                        generatedCandidateCount
                        * SEARCH_SPACE_ALLOWANCE_RATIO);

        int uncertaintyAllowance =
                safeVmCandidateCount > 1
                ? MULTI_CANDIDATE_UNCERTAINTY_ALLOWANCE
                : 0;

        long rawBudget =
                (long) minimumBudget
                + featureAllowance
                + candidateSetAllowance
                + searchSpaceAllowance
                + uncertaintyAllowance;

        int boundedBudget =
                (int) Math.min(
                        rawBudget,
                        maximumBudget);

        boundedBudget =
                Math.max(
                        minimumBudget,
                        boundedBudget);

        /*
         * The evaluation budget cannot exceed the number of
         * candidates that were actually generated.
         */
        boundedBudget =
                Math.min(
                        boundedBudget,
                        generatedCandidateCount);

        return boundedBudget;
    }
}
package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Defines how counterfactual candidates are generated and evaluated.
 *
 * FIXED_EXHAUSTIVE:
 * Uses the existing bounded generation and evaluation process.
 *
 * ADAPTIVE_BEST_FIRST:
 * Uses a data-adaptive budget and evaluates candidates according
 * to their estimated counterfactual quality.
 */
public enum CounterfactualSearchMode {

    /**
     * Existing RG-FCS behavior.
     *
     * Candidates are generated using configured limits and evaluated
     * in their generated order.
     */
    FIXED_EXHAUSTIVE,

    /**
     * Adaptive RG-FCS behavior.
     *
     * Search budget, candidate ordering, refinement, and stopping
     * may be adjusted using the observed decision state and replay
     * feedback.
     */
    ADAPTIVE_BEST_FIRST
}
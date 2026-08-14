package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Research classification assigned to an evaluated counterfactual.
 *
 * Classification is based on two dimensions:
 *
 * 1. Whether the requested scheduler decision change was achieved.
 * 2. Whether the resulting replay state was operationally feasible.
 *
 * INVALID is reserved for candidates that cannot be meaningfully
 * evaluated because they are malformed, structurally inconsistent,
 * outside permitted ranges, or cannot be replayed.
 */
public enum CounterfactualClassification {

    /**
     * The requested scheduler decision change was produced and all
     * mandatory operational constraints were satisfied.
     *
     * This is an actionable counterfactual explanation.
     */
    FEASIBLE_EFFECTIVE,

    /**
     * The candidate remained operationally feasible but did not produce
     * the requested scheduler decision change.
     */
    FEASIBLE_INEFFECTIVE,

    /**
     * The candidate produced the requested scheduler decision change,
     * but one or more mandatory operational constraints were violated.
     */
    INFEASIBLE_EFFECTIVE,

    /**
     * The candidate was operationally infeasible and also failed to
     * produce the requested scheduler decision change.
     */
    INFEASIBLE_INEFFECTIVE,

    /**
     * The candidate could not be meaningfully evaluated.
     */
    INVALID
}
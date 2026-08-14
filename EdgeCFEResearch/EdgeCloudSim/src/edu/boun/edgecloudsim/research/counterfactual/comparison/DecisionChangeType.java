package edu.boun.edgecloudsim.research.counterfactual.comparison;

/**
 * Requested decision outcome that the counterfactual search
 * attempts to produce.
 */
public enum DecisionChangeType {

    /**
     * Different execution resource while remaining in the same
     * destination tier.
     */
    EXECUTION_TARGET_CHANGE,

    /**
     * Different execution destination
     * (e.g., EDGE → CLOUD or CLOUD → EDGE).
     */
    DESTINATION_CHANGE,

    /**
     * Restore schedulability for a task that originally had no
     * feasible execution target.
     */
    FEASIBILITY_RESTORATION
}
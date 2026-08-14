package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Common interface for all feasibility rules.
 *
 * Future implementations may include:
 *
 * CpuFeasibilityRule
 * BandwidthFeasibilityRule
 * LatencyFeasibilityRule
 * MobilityFeasibilityRule
 * ServiceFeasibilityRule
 */
public interface FeasibilityRule {

    FeasibilityConstraint getConstraint();

    ConstraintEvaluation evaluate(
            ReplayState replayState);
}
package edu.boun.edgecloudsim.research.counterfactual.replay;

/**
 * Describes whether a scheduler decision was successfully reproduced.
 */
public enum SchedulerReplayStatus {

    /**
     * The strategy had sufficient information to reproduce a decision.
     */
    REPLAYED,

    /**
     * The policy is supported, but required state information was missing.
     */
    INDETERMINATE,

    /**
     * No replay strategy supports the requested policy.
     */
    UNSUPPORTED_POLICY,

    /**
     * The replay strategy encountered invalid or inconsistent data.
     */
    ERROR
}
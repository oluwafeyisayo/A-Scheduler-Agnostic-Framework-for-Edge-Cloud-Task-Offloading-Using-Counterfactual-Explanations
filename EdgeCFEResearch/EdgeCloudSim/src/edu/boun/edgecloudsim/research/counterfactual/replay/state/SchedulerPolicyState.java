package edu.boun.edgecloudsim.research.counterfactual.replay.state;

/**
 * Marker interface for policy-specific scheduler state.
 *
 * The counterfactual framework handles this through the interface
 * without depending on the concrete scheduler implementation.
 */
public interface SchedulerPolicyState {

    String getPolicyName();
}
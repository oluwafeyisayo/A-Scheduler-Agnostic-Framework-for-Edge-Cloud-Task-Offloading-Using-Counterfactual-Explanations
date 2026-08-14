package edu.boun.edgecloudsim.research.counterfactual.replay.state;

/**
 * LEAST_LOADED currently requires no additional internal cursor
 * or random-generator state.
 *
 * Its decision can be reconstructed from the candidate snapshots.
 */
public final class LeastLoadedPolicyState
        implements SchedulerPolicyState {

    @Override
    public String getPolicyName() {

        return "LEAST_LOADED";
    }

    @Override
    public String toString() {

        return "LeastLoadedPolicyState{"
                + "policyName='"
                + getPolicyName()
                + '\''
                + '}';
    }
}
package edu.boun.edgecloudsim.research.counterfactual.replay.state;

/**
 * Captures the flattened EDGE round-robin cursor immediately before
 * the live decision.
 *
 * The cursor is retained even when the live destination is CLOUD. In
 * that case, it represents the latent EDGE cursor that would be used
 * if a counterfactual state redirected the task to EDGE.
 */
public final class RoundRobinPolicyState
        implements SchedulerPolicyState {

    private final int startingCandidatePosition;

    public RoundRobinPolicyState(
            int startingCandidatePosition) {

        if(startingCandidatePosition < 0) {

            throw new IllegalArgumentException(
                    "Starting candidate position cannot be negative.");
        }

        this.startingCandidatePosition =
                startingCandidatePosition;
    }

    @Override
    public String getPolicyName() {

        return "ROUND_ROBIN";
    }

    /**
     * Returns the position in the destination-filtered replay list.
     */
    public int getStartingCandidateIndex() {

        return startingCandidatePosition;
    }

    public int getStartingCandidatePosition() {

        return startingCandidatePosition;
    }

    @Override
    public String toString() {

        return "RoundRobinPolicyState{"
                + "startingCandidatePosition="
                + startingCandidatePosition
                + '}';
    }
}
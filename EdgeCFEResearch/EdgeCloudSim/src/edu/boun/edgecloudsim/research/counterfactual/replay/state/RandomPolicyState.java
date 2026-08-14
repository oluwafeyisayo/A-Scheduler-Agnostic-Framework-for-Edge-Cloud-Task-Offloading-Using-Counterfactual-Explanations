package edu.boun.edgecloudsim.research.counterfactual.replay.state;

/**
 * Captures the random draw used by the original scheduler.
 *
 * Reusing the original sampled index prevents a counterfactual
 * decision from appearing to change merely because a new random
 * number was generated.
 */
public final class RandomPolicyState
        implements SchedulerPolicyState {

    private final int sampledCandidateIndex;

    public RandomPolicyState(
            int sampledCandidateIndex) {

        if(sampledCandidateIndex < 0) {

            throw new IllegalArgumentException(
                    "Sampled candidate index cannot be negative.");
        }

        this.sampledCandidateIndex =
                sampledCandidateIndex;
    }

    @Override
    public String getPolicyName() {

        return "RANDOM";
    }

    public int getSampledCandidateIndex() {

        return sampledCandidateIndex;
    }

    @Override
    public String toString() {

        return "RandomPolicyState{"
                + "sampledCandidateIndex="
                + sampledCandidateIndex
                + '}';
    }
}
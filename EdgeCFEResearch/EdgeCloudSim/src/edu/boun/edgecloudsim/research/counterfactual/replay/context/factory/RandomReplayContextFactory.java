package edu.boun.edgecloudsim.research.counterfactual.replay.context.factory;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.SchedulerReplayContextFactory;
import edu.boun.edgecloudsim.research.counterfactual.replay.VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.SchedulerReplayContext;
import edu.boun.edgecloudsim.research.counterfactual.replay.state.RandomPolicyState;

/**
 * Reconstructs RANDOM using the exact candidate index captured during
 * the live scheduling decision.
 */
public final class RandomReplayContextFactory
        implements SchedulerReplayContextFactory {

    @Override
    public SchedulerReplayContext create(
            ReplayState replayState,
            List<VmCandidateSnapshot> vmCandidates) {

        if(replayState == null) {
            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        if(vmCandidates == null
                || vmCandidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Random replay requires at least one VM candidate.");
        }

        int sampledCandidateIndex =
                replayState.getRandomSampledCandidateIndex();

        if(sampledCandidateIndex < 0) {
            throw new IllegalArgumentException(
                    "RANDOM replay requires a logged "
                    + "randomSampledCandidateIndex.");
        }

        if(findCandidateByIndex(
                vmCandidates,
                sampledCandidateIndex) == null) {
            throw new IllegalArgumentException(
                    "Logged randomSampledCandidateIndex "
                    + sampledCandidateIndex
                    + " does not exist in the candidate set for task "
                    + replayState.getTaskId()
                    + ".");
        }

        return new SchedulerReplayContext(
                replayState,
                vmCandidates,
                new RandomPolicyState(
                        sampledCandidateIndex));
    }

    private VmCandidateSnapshot findCandidateByIndex(
            List<VmCandidateSnapshot> candidates,
            int candidateIndex) {

        for(VmCandidateSnapshot candidate : candidates) {
            if(candidate != null
                    && candidate.getCandidateIndex()
                    == candidateIndex) {
                return candidate;
            }
        }

        return null;
    }

    @Override
    public String getSchedulerPolicy() {
        return "RANDOM";
    }
}
package edu.boun.edgecloudsim.research.counterfactual.replay.context.factory;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual
        .SchedulerReplayContextFactory;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.replay.context
        .SchedulerReplayContext;
import edu.boun.edgecloudsim.research.counterfactual.replay.state
        .MultiObjectivePolicyState;

/**
 * Creates deterministic replay contexts for MULTI_OBJECTIVE.
 */
public final class MultiObjectiveReplayContextFactory
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
                    "MULTI_OBJECTIVE replay requires at least "
                    + "one VM candidate.");
        }

        return new SchedulerReplayContext(
                replayState,
                vmCandidates,
                MultiObjectivePolicyState.createDefault());
    }

    @Override
    public String getSchedulerPolicy() {

        return "MULTI_OBJECTIVE";
    }
}
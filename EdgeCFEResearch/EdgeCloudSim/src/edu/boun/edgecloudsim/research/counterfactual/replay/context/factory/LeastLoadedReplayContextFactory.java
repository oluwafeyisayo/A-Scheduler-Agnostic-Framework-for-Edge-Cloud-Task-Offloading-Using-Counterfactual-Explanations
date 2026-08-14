package edu.boun.edgecloudsim.research.counterfactual.replay.context.factory;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual
        .ReplayState;

import edu.boun.edgecloudsim.research.counterfactual
        .SchedulerReplayContextFactory;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

import edu.boun.edgecloudsim.research.counterfactual.replay.context
        .SchedulerReplayContext;

import edu.boun.edgecloudsim.research.counterfactual.replay.state
        .LeastLoadedPolicyState;

public final class LeastLoadedReplayContextFactory
        implements SchedulerReplayContextFactory {

    @Override
    public SchedulerReplayContext create(
            ReplayState replayState,
            List<VmCandidateSnapshot> vmCandidates) {

        return new SchedulerReplayContext(
                replayState,
                vmCandidates,
                new LeastLoadedPolicyState());
    }

    @Override
    public String getSchedulerPolicy() {

        return "LEAST_LOADED";
    }
}
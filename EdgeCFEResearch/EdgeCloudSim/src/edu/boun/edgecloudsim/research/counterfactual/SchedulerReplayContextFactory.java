package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

import edu.boun.edgecloudsim.research.counterfactual.replay.context
        .SchedulerReplayContext;

/**
 * Creates scheduler-specific replay contexts.
 *
 * RG-FCS depends on this abstraction rather than directly depending
 * on LEAST_LOADED, ROUND_ROBIN, or RANDOM policy state classes.
 */
public interface SchedulerReplayContextFactory {

    SchedulerReplayContext create(
            ReplayState replayState,
            List<VmCandidateSnapshot> vmCandidates);

    String getSchedulerPolicy();
}
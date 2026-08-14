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
        .PpoPolicyState;

/**
 * Creates deterministic PPO replay contexts.
 *
 * PPO has no cursor or mutable per-decision scheduler state. The
 * context therefore contains the reconstructed environment, candidate
 * snapshots, and immutable actor-model provenance.
 */
public final class PpoReplayContextFactory
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
                    "PPO replay requires at least one "
                    + "VM candidate.");
        }

        return new SchedulerReplayContext(
                replayState,
                vmCandidates,
                PpoPolicyState.createBootstrap());
    }

    @Override
    public String getSchedulerPolicy() {

        return "PPO";
    }
}
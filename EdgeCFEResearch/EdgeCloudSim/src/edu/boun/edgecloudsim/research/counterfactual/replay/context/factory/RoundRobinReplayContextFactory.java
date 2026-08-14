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
        .RoundRobinPolicyState;

/**
 * Reconstructs the ROUND_ROBIN cursor used during deterministic replay.
 *
 * For selected decisions, the exact logged candidate index is converted
 * to its position in the replay candidate list.
 *
 * For an original NO_FEASIBLE_VM decision, the cursor does not affect
 * the original outcome because the scheduler completed a full cyclic
 * scan without finding a feasible VM. If that cursor is unavailable,
 * position zero is used as a deterministic fallback.
 */
public final class RoundRobinReplayContextFactory
        implements SchedulerReplayContextFactory {

    private static final int
            DETERMINISTIC_FALLBACK_POSITION = 0;

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
                    "Round-robin replay requires at least "
                    + "one VM candidate.");
        }

        int startingPosition =
                resolveStartingPosition(
                        replayState,
                        vmCandidates);

        return new SchedulerReplayContext(
                replayState,
                vmCandidates,
                new RoundRobinPolicyState(
                        startingPosition));
    }

    private int resolveStartingPosition(
            ReplayState replayState,
            List<VmCandidateSnapshot> vmCandidates) {

        int loggedCandidateIndex =
                replayState
                .getRoundRobinStartingCandidateIndex();

        if(loggedCandidateIndex >= 0) {

            int loggedPosition =
                    findCandidatePositionByIndex(
                            vmCandidates,
                            loggedCandidateIndex);

            if(loggedPosition >= 0) {

                return loggedPosition;
            }

            /*
             * For an originally selected decision, a logged cursor
             * missing from the candidate set is a provenance error.
             */
            if(!isOriginalNoFeasibleVm(
                    replayState)) {

                throw new IllegalArgumentException(
                        "Logged "
                        + "roundRobinStartingCandidateIndex "
                        + loggedCandidateIndex
                        + " does not exist in the candidate "
                        + "set for task "
                        + replayState.getTaskId()
                        + ".");
            }
        }

        /*
         * A NO_FEASIBLE_VM decision scanned the complete cycle.
         * Its unchanged baseline outcome is therefore independent
         * of the starting cursor.
         *
         * Position zero provides a reproducible cursor for evaluating
         * feasibility-restoration counterfactuals.
         */
        if(isOriginalNoFeasibleVm(
                replayState)) {

            return DETERMINISTIC_FALLBACK_POSITION;
        }

        throw new IllegalArgumentException(
                "ROUND_ROBIN replay requires a logged "
                + "roundRobinStartingCandidateIndex for task "
                + replayState.getTaskId()
                + ".");
    }

    private boolean isOriginalNoFeasibleVm(
            ReplayState replayState) {

        return "NO_FEASIBLE_VM".equalsIgnoreCase(
                replayState.getOriginalOutcome());
    }

    private int findCandidatePositionByIndex(
            List<VmCandidateSnapshot> candidates,
            int candidateIndex) {

        for(int position = 0;
                position < candidates.size();
                position++) {

            VmCandidateSnapshot candidate =
                    candidates.get(position);

            if(candidate != null
                    && candidate.getCandidateIndex()
                    == candidateIndex) {

                return position;
            }
        }

        return -1;
    }

    @Override
    public String getSchedulerPolicy() {

        return "ROUND_ROBIN";
    }
}
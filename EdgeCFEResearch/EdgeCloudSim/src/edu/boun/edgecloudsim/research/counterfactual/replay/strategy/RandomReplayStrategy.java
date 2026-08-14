package edu.boun.edgecloudsim.research.counterfactual.replay.strategy;

import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay.SchedulerDecision;
import edu.boun.edgecloudsim.research.counterfactual.replay.SchedulerReplayStatus;
import edu.boun.edgecloudsim.research.counterfactual.replay.VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.SchedulerReplayContext;
import edu.boun.edgecloudsim.research.counterfactual.replay.state.RandomPolicyState;
import edu.boun.edgecloudsim.research.counterfactual.replay.state.SchedulerPolicyState;

/**
 * Deterministically replays RANDOM using the original sampled
 * candidate index.
 *
 * It does not generate another random number during replay.
 */
public final class RandomReplayStrategy
        implements SchedulerReplayStrategy {

    @Override
    public String getSupportedPolicy() {

        return "RANDOM";
    }

    @Override
    public SchedulerDecision replay(
            SchedulerReplayContext context) {

        if(context == null) {

            throw new IllegalArgumentException(
                    "SchedulerReplayContext cannot be null.");
        }

        ReplayState replayState =
                context.getReplayState();

        if(!supports(
                replayState.getOriginalPolicy())) {

            return unsupportedDecision(
                    replayState);
        }

        if(!context.hasCandidateSnapshots()) {

            return indeterminateDecision(
                    replayState,
                    "RANDOM replay requires candidate "
                    + "VM snapshots.");
        }

        SchedulerPolicyState genericState =
                context.getPolicyState();

        if(!(genericState
                instanceof RandomPolicyState)) {

            return indeterminateDecision(
                    replayState,
                    "RANDOM replay requires "
                    + "RandomPolicyState.");
        }

        RandomPolicyState randomState =
                (RandomPolicyState)
                genericState;

        VmCandidateSnapshot sampledCandidate =
                context.findCandidateByIndex(
                        randomState
                        .getSampledCandidateIndex());

        if(sampledCandidate == null) {

            return new SchedulerDecision.Builder(
                    replayState.getTaskId(),
                    getSupportedPolicy())

                    .withReplayStatus(
                            SchedulerReplayStatus.ERROR)

                    .withReason(
                            "The captured random candidate index "
                            + randomState
                            .getSampledCandidateIndex()
                            + " does not exist in the replay "
                            + "candidate set.")

                    .build();
        }

        if(!sampledCandidate.isFeasible()) {

            return new SchedulerDecision.Builder(
                    replayState.getTaskId(),
                    getSupportedPolicy())

                    .withReplayStatus(
                            SchedulerReplayStatus.REPLAYED)

                    .withDecisionOutcome(
                            "NO_FEASIBLE_VM")

                    .withSelectedDestination(
                            "NONE")

                    .withSelectedCandidateIndex(
                            sampledCandidate
                            .getCandidateIndex())

                    .withReason(
                            "The originally sampled random VM "
                            + "is infeasible in the modified state.")

                    .build();
        }

        return new SchedulerDecision.Builder(
                replayState.getTaskId(),
                getSupportedPolicy())

                .withReplayStatus(
                        SchedulerReplayStatus.REPLAYED)

                .withDecisionOutcome(
                        "SELECTED")

                .withSelectedDestination(
                        sampledCandidate
                        .getDestination())

                .withSelectedVmId(
                        sampledCandidate.getVmId())

                .withSelectedHostId(
                        sampledCandidate.getHostId())

                .withSelectedDatacenterId(
                        sampledCandidate
                        .getDatacenterId())

                .withSelectedCandidateIndex(
                        sampledCandidate
                        .getCandidateIndex())
//
//                .withReason(
//                        "Reused the candidate index selected "
//                        + "by the original random draw.")
                
                .withReason(
                        "Reused the deterministic random candidate index "
                        + "assigned to this task.")

                .build();
    }

    private SchedulerDecision unsupportedDecision(
            ReplayState replayState) {

        return new SchedulerDecision.Builder(
                replayState.getTaskId(),
                replayState.getOriginalPolicy())

                .withReplayStatus(
                        SchedulerReplayStatus
                        .UNSUPPORTED_POLICY)

                .withReason(
                        "This strategy supports only "
                        + getSupportedPolicy()
                        + ".")

                .build();
    }

    private SchedulerDecision indeterminateDecision(
            ReplayState replayState,
            String reason) {

        return new SchedulerDecision.Builder(
                replayState.getTaskId(),
                getSupportedPolicy())

                .withReplayStatus(
                        SchedulerReplayStatus
                        .INDETERMINATE)

                .withReason(reason)

                .build();
    }
}
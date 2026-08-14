package edu.boun.edgecloudsim.research.counterfactual.replay.strategy;

import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay.SchedulerDecision;
import edu.boun.edgecloudsim.research.counterfactual.replay.SchedulerReplayStatus;
import edu.boun.edgecloudsim.research.counterfactual.replay.VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.SchedulerReplayContext;

/**
 * Replays LEAST_LOADED by selecting the feasible VM with the
 * greatest available CPU percentage.
 */
public final class LeastLoadedReplayStrategy
        implements SchedulerReplayStrategy {

    private static final double EPSILON =
            0.000001;

    @Override
    public String getSupportedPolicy() {

        return "LEAST_LOADED";
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

            return new SchedulerDecision.Builder(
                    replayState.getTaskId(),
                    getSupportedPolicy())

                    .withReplayStatus(
                            SchedulerReplayStatus
                            .INDETERMINATE)

                    .withReason(
                            "LEAST_LOADED replay requires "
                            + "candidate VM snapshots.")

                    .build();
        }

        VmCandidateSnapshot bestCandidate =
                null;

        for(VmCandidateSnapshot candidate
                : context.getCandidateSnapshots()) {

            if(candidate == null
                    || !candidate.isFeasible()) {

                continue;
            }

            if(bestCandidate == null) {

                bestCandidate = candidate;
                continue;
            }

            double candidateAvailableCpu =
                    candidate
                    .getAvailableCpuPercent();

            double bestAvailableCpu =
                    bestCandidate
                    .getAvailableCpuPercent();

            if(candidateAvailableCpu
                    > bestAvailableCpu + EPSILON) {

                bestCandidate = candidate;
            }
            else if(Math.abs(
                    candidateAvailableCpu
                    - bestAvailableCpu)
                    <= EPSILON
                    && candidate.getEvaluationOrder()
                    < bestCandidate
                    .getEvaluationOrder()) {

                bestCandidate = candidate;
            }
        }

        if(bestCandidate == null) {

            return noFeasibleVmDecision(
                    replayState,
                    "No feasible candidate VM was available.");
        }

        return selectedDecision(
                replayState,
                bestCandidate,
                "Selected the feasible VM with the greatest "
                + "available CPU capacity.");
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

    private SchedulerDecision noFeasibleVmDecision(
            ReplayState replayState,
            String reason) {

        return new SchedulerDecision.Builder(
                replayState.getTaskId(),
                getSupportedPolicy())

                .withReplayStatus(
                        SchedulerReplayStatus.REPLAYED)

                .withDecisionOutcome(
                        "NO_FEASIBLE_VM")

                .withSelectedDestination(
                        "NONE")

                .withSelectedVmId(
                        -1)

                .withSelectedHostId(
                        -1)

                .withSelectedDatacenterId(
                        -1)

                .withSelectedCandidateIndex(
                        -1)

                .withReason(reason)

                .build();
    }

    private SchedulerDecision selectedDecision(
            ReplayState replayState,
            VmCandidateSnapshot candidate,
            String reason) {

        return new SchedulerDecision.Builder(
                replayState.getTaskId(),
                getSupportedPolicy())

                .withReplayStatus(
                        SchedulerReplayStatus.REPLAYED)

                .withDecisionOutcome(
                        "SELECTED")

                .withSelectedDestination(
                        candidate.getDestination())

                .withSelectedVmId(
                        candidate.getVmId())

                .withSelectedHostId(
                        candidate.getHostId())

                .withSelectedDatacenterId(
                        candidate.getDatacenterId())

                .withSelectedCandidateIndex(
                        candidate.getCandidateIndex())

                .withReason(reason)

                .build();
    }
}
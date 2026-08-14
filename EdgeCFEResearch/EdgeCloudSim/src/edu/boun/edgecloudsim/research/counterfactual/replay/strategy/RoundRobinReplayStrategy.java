package edu.boun.edgecloudsim.research.counterfactual.replay.strategy;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay.SchedulerDecision;
import edu.boun.edgecloudsim.research.counterfactual.replay.SchedulerReplayStatus;
import edu.boun.edgecloudsim.research.counterfactual.replay.VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.SchedulerReplayContext;
import edu.boun.edgecloudsim.research.counterfactual.replay.state.RoundRobinPolicyState;
import edu.boun.edgecloudsim.research.counterfactual.replay.state.SchedulerPolicyState;

/**
 * Replays the live ROUND_ROBIN policy faithfully:
 *
 * - EDGE: cyclic scan from the captured EDGE cursor.
 * - CLOUD: least-loaded selection, matching the live cloud branch.
 *
 * Candidate-branch selection is based on the logged attempted
 * destination, not on whether the complete candidate list happens to
 * contain only CLOUD records.
 */
public final class RoundRobinReplayStrategy
        implements SchedulerReplayStrategy {

    private static final double EPSILON =
            0.000001;

    @Override
    public String getSupportedPolicy() {

        return "ROUND_ROBIN";
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
                    "ROUND_ROBIN replay requires candidate "
                    + "VM snapshots.");
        }

        /*
         * Preserve a genuine live scheduler failure for the unchanged
         * baseline state. This prevents a mixed EDGE/CLOUD candidate
         * list from causing an originally unsuccessful decision to
         * replay as SELECTED.
         *
         * Once RG-FCS applies a feature change, replay proceeds through
         * the normal policy branch and can restore feasibility.
         */
        if(isUnchangedOriginalFailure(
                replayState)) {

            return noFeasibleVmDecision(
                    replayState,
                    "Preserved the logged live NO_FEASIBLE_VM "
                    + "outcome for the unchanged baseline state.");
        }

        String attemptedDestination =
                resolveAttemptedDestination(
                        replayState);

        if(!isConcreteDestination(
                attemptedDestination)) {

            return indeterminateDecision(
                    replayState,
                    "ROUND_ROBIN replay requires a concrete "
                    + "attempted destination.");
        }

        List<VmCandidateSnapshot> destinationCandidates =
                context.getCandidatesForDestination(
                        attemptedDestination);

        if(destinationCandidates == null
                || destinationCandidates.isEmpty()) {

            return noFeasibleVmDecision(
                    replayState,
                    "No candidate VM snapshots were available "
                    + "for attempted destination "
                    + attemptedDestination
                    + ".");
        }

        if("CLOUD".equalsIgnoreCase(
                attemptedDestination)) {

            return replayCloudLeastLoaded(
                    replayState,
                    destinationCandidates);
        }

        if(!"EDGE".equalsIgnoreCase(
                attemptedDestination)) {

            return indeterminateDecision(
                    replayState,
                    "ROUND_ROBIN currently supports only EDGE "
                    + "and CLOUD destination branches.");
        }

        return replayEdgeRoundRobin(
                replayState,
                context,
                destinationCandidates);
    }

    private SchedulerDecision replayEdgeRoundRobin(
            ReplayState replayState,
            SchedulerReplayContext context,
            List<VmCandidateSnapshot> candidates) {

        SchedulerPolicyState genericState =
                context.getPolicyState();

        if(!(genericState
                instanceof RoundRobinPolicyState)) {

            return indeterminateDecision(
                    replayState,
                    "EDGE ROUND_ROBIN replay requires a captured "
                    + "RoundRobinPolicyState.");
        }

        RoundRobinPolicyState roundRobinState =
                (RoundRobinPolicyState)
                genericState;

        int candidateCount =
                candidates.size();

        int startingPosition =
                Math.floorMod(
                        roundRobinState
                        .getStartingCandidatePosition(),
                        candidateCount);

        for(int offset = 0;
                offset < candidateCount;
                offset++) {

            int position =
                    (startingPosition + offset)
                    % candidateCount;

            VmCandidateSnapshot candidate =
                    candidates.get(
                            position);

            if(candidate != null
                    && candidate.isFeasible()) {

                return selectedDecision(
                        replayState,
                        candidate,
                        "Selected the first feasible EDGE VM "
                        + "encountered from the captured "
                        + "round-robin cursor.");
            }
        }

        return noFeasibleVmDecision(
                replayState,
                "The complete EDGE round-robin cycle contained "
                + "no feasible VM.");
    }

    private SchedulerDecision replayCloudLeastLoaded(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidates) {

        VmCandidateSnapshot bestCandidate =
                null;

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate == null
                    || !candidate.isFeasible()) {

                continue;
            }

            if(bestCandidate == null) {

                bestCandidate =
                        candidate;

                continue;
            }

            double candidateCpu =
                    candidate.getAvailableCpuPercent();

            double bestCpu =
                    bestCandidate.getAvailableCpuPercent();

            if(candidateCpu > bestCpu + EPSILON
                    || (
                            Math.abs(candidateCpu - bestCpu)
                            <= EPSILON
                            && candidate.getEvaluationOrder()
                            < bestCandidate.getEvaluationOrder()
                    )) {

                bestCandidate =
                        candidate;
            }
        }

        if(bestCandidate == null) {

            return noFeasibleVmDecision(
                    replayState,
                    "The CLOUD least-loaded branch contained "
                    + "no feasible VM.");
        }

        return selectedDecision(
                replayState,
                bestCandidate,
                "Replayed the live ROUND_ROBIN CLOUD branch "
                + "using least-loaded VM selection.");
    }

    private boolean isUnchangedOriginalFailure(
            ReplayState replayState) {

        return "NO_FEASIBLE_VM".equalsIgnoreCase(
                    replayState.getOriginalOutcome())
                && (
                        replayState.getAppliedChanges() == null
                        || replayState.getAppliedChanges().isEmpty()
                );
    }

    private String resolveAttemptedDestination(
            ReplayState replayState) {

        String attemptedDestination =
                replayState.getAttemptedDestination();

        if(isConcreteDestination(
                attemptedDestination)) {

            return attemptedDestination;
        }

        String originalDestination =
                replayState.getOriginalDestination();

        if(isConcreteDestination(
                originalDestination)) {

            return originalDestination;
        }

        return "NONE";
    }

    private boolean isConcreteDestination(
            String destination) {

        return "EDGE".equalsIgnoreCase(
                    destination)
                || "CLOUD".equalsIgnoreCase(
                        destination);
    }

    private SchedulerDecision unsupportedDecision(
            ReplayState replayState) {

        return new SchedulerDecision.Builder(
                replayState.getTaskId(),
                replayState.getOriginalPolicy())

                .withReplayStatus(
                        SchedulerReplayStatus.UNSUPPORTED_POLICY)

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
                        SchedulerReplayStatus.INDETERMINATE)

                .withReason(reason)

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

                .withSelectedVmId(-1)
                .withSelectedHostId(-1)
                .withSelectedDatacenterId(-1)
                .withSelectedCandidateIndex(-1)

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
package edu.boun.edgecloudsim.research.counterfactual.replay.strategy;

import java.util.List;

import edu.boun.edgecloudsim.edge_orchestrator.ppo.PpoConfig;
import edu.boun.edgecloudsim.edge_orchestrator.ppo.PpoScheduler;
import edu.boun.edgecloudsim.edge_orchestrator.ppo.PpoSelectionResult;
import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerDecision;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerReplayStatus;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.replay.context
        .SchedulerReplayContext;
import edu.boun.edgecloudsim.research.counterfactual.replay.state
        .PpoPolicyState;
import edu.boun.edgecloudsim.research.counterfactual.replay.state
        .SchedulerPolicyState;

/**
 * Deterministically replays PPO by invoking the same feature encoder,
 * actor network, feasibility mask, and argmax action rule used by the
 * live scheduler.
 *
 * Destination choice remains state-aware because the live
 * BasicEdgeOrchestrator chooses EDGE or CLOUD before PPO selects a VM
 * within that tier.
 */
public final class PpoReplayStrategy
        implements SchedulerReplayStrategy {

    @Override
    public String getSupportedPolicy() {

        return "PPO";
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
                    "PPO replay requires candidate VM snapshots.");
        }

        SchedulerPolicyState genericState =
                context.getPolicyState();

        if(!(genericState
                instanceof PpoPolicyState)) {

            return indeterminateDecision(
                    replayState,
                    "PPO replay requires PpoPolicyState.");
        }

        PpoPolicyState policyState =
                (PpoPolicyState)
                genericState;

        PpoConfig config;

        try {

            config =
                    policyState.toConfig();
        }
        catch(RuntimeException exception) {

            return indeterminateDecision(
                    replayState,
                    exception.getMessage());
        }

        /*
         * Preserve a genuine live failure for the unchanged baseline.
         * Modified states still pass through PPO inference so RG-FCS can
         * discover feasibility-restoration counterfactuals.
         */
        if(isUnchangedOriginalFailure(
                replayState)) {

            return noFeasibleVmDecision(
                    replayState,
                    "Preserved the logged live NO_FEASIBLE_VM "
                    + "outcome for the unchanged PPO baseline.");
        }

        String destination =
                resolveReplayDestination(
                        replayState,
                        context.getCandidateSnapshots());

        if(!isConcreteDestination(
                destination)) {

            return noFeasibleVmDecision(
                    replayState,
                    "No operational EDGE or CLOUD destination "
                    + "was available for PPO replay.");
        }

        List<VmCandidateSnapshot> destinationCandidates =
                context.getCandidatesForDestination(
                        destination);

        if(destinationCandidates == null
                || destinationCandidates.isEmpty()) {

            return noFeasibleVmDecision(
                    replayState,
                    "No PPO candidates were available for "
                    + destination
                    + ".");
        }

        PpoScheduler ppoScheduler =
                new PpoScheduler(
                        config);

        PpoSelectionResult result =
                ppoScheduler.selectReplay(
                        replayState,
                        destinationCandidates);

        if(result == null
                || !result.hasSelection()) {

            return noFeasibleVmDecision(
                    replayState,
                    result == null
                    ? "PPO inference returned no result."
                    : result.getReason());
        }

        VmCandidateSnapshot selectedCandidate =
                findCandidateByIndex(
                        destinationCandidates,
                        result.getSelectedCandidateIndex());

        if(selectedCandidate == null) {

            return indeterminateDecision(
                    replayState,
                    "PPO selected candidate index "
                    + result.getSelectedCandidateIndex()
                    + ", but it was absent from the replay "
                    + "candidate set.");
        }

        return selectedDecision(
                replayState,
                selectedCandidate,
                result,
                policyState);
    }

    private boolean isUnchangedOriginalFailure(
            ReplayState replayState) {

        return "NO_FEASIBLE_VM".equalsIgnoreCase(
                    replayState.getOriginalOutcome())
                && !replayState.hasAppliedChanges();
    }

    private String resolveReplayDestination(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidates) {

        /*
         * Unchanged baseline replay preserves the tier chosen by the
         * live state-aware destination selector.
         */
        if(!replayState.hasAppliedChanges()) {

            String originalDestination =
                    replayState.getOriginalDestination();

            if(isConcreteDestination(
                    originalDestination)) {

                return originalDestination;
            }

            String attemptedDestination =
                    replayState.getAttemptedDestination();

            if(isConcreteDestination(
                    attemptedDestination)) {

                return attemptedDestination;
            }
        }

        /*
         * Counterfactual replay recomputes the state-aware destination
         * using the modified network and candidate states.
         */
        double edgeCost =
                estimateDestinationServiceCost(
                        replayState,
                        candidates,
                        "EDGE");

        double cloudCost =
                estimateDestinationServiceCost(
                        replayState,
                        candidates,
                        "CLOUD");

        if(Double.isFinite(edgeCost)
                && Double.isFinite(cloudCost)) {

            return edgeCost <= cloudCost
                    ? "EDGE"
                    : "CLOUD";
        }

        if(Double.isFinite(edgeCost)) {

            return "EDGE";
        }

        if(Double.isFinite(cloudCost)) {

            return "CLOUD";
        }

        return "NONE";
    }

    private double estimateDestinationServiceCost(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidates,
            String destination) {

        if(!replayState
                .isNetworkFeasibleForDestination(
                        destination)) {

            return Double.POSITIVE_INFINITY;
        }

        double uploadDelayMs =
                replayState
                .getUploadDelayMsForDestination(
                        destination);

        if(!Double.isFinite(uploadDelayMs)
                || uploadDelayMs < 0.0) {

            return Double.POSITIVE_INFINITY;
        }

        double bestProcessingTimeMs =
                Double.POSITIVE_INFINITY;

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate == null
                    || !destination.equalsIgnoreCase(
                            candidate.getDestination())
                    || !candidate.isFeasible()) {

                continue;
            }

            bestProcessingTimeMs =
                    Math.min(
                            bestProcessingTimeMs,
                            candidate
                            .estimateProcessingTimeMs(
                                    replayState
                                    .getTaskLengthMi()));
        }

        if(!Double.isFinite(
                bestProcessingTimeMs)) {

            return Double.POSITIVE_INFINITY;
        }

        return uploadDelayMs
                + bestProcessingTimeMs;
    }

    private VmCandidateSnapshot findCandidateByIndex(
            List<VmCandidateSnapshot> candidates,
            int candidateIndex) {

        if(candidates == null) {

            return null;
        }

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate != null
                    && candidate.getCandidateIndex()
                    == candidateIndex) {

                return candidate;
            }
        }

        return null;
    }

    private boolean isConcreteDestination(
            String destination) {

        return "EDGE".equalsIgnoreCase(
                    destination)
                || "CLOUD".equalsIgnoreCase(
                        destination);
    }

    private SchedulerDecision selectedDecision(
            ReplayState replayState,
            VmCandidateSnapshot candidate,
            PpoSelectionResult result,
            PpoPolicyState policyState) {

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

                .withReason(
                        "PPO_DETERMINISTIC_ARGMAX"
                        + "|MODEL="
                        + policyState.getModelId()
                        + "|SCHEMA="
                        + policyState.getFeatureSchemaId()
                        + "|CANDIDATE_INDEX="
                        + candidate.getCandidateIndex()
                        + "|LOGIT="
                        + result.getSelectedLogit()
                        + "|PROBABILITY="
                        + result.getSelectedProbability())

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
}
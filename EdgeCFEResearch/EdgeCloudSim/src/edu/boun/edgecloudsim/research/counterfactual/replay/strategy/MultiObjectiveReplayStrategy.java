package edu.boun.edgecloudsim.research.counterfactual.replay.strategy;

import java.util.List;

import edu.boun.edgecloudsim.edge_orchestrator.multiobjective
        .MultiObjectiveConfig;
import edu.boun.edgecloudsim.edge_orchestrator.multiobjective
        .MultiObjectiveScore;
import edu.boun.edgecloudsim.edge_orchestrator.multiobjective
        .MultiObjectiveScorer;
import edu.boun.edgecloudsim.edge_orchestrator.multiobjective
        .MultiObjectiveSelectionResult;
import edu.boun.edgecloudsim.research.counterfactual
        .ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerDecision;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerReplayStatus;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.replay.context
        .SchedulerReplayContext;
import edu.boun.edgecloudsim.research.counterfactual.replay.state
        .MultiObjectivePolicyState;
import edu.boun.edgecloudsim.research.counterfactual.replay.state
        .SchedulerPolicyState;

/**
 * Deterministically replays MULTI_OBJECTIVE using the exact shared
 * scorer used by the live scheduler.
 */
public final class MultiObjectiveReplayStrategy
        implements SchedulerReplayStrategy {

    @Override
    public String getSupportedPolicy() {

        return "MULTI_OBJECTIVE";
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
                    "MULTI_OBJECTIVE replay requires candidate "
                    + "VM snapshots.");
        }

        SchedulerPolicyState genericState =
                context.getPolicyState();

        if(!(genericState
                instanceof MultiObjectivePolicyState)) {

            return indeterminateDecision(
                    replayState,
                    "MULTI_OBJECTIVE replay requires "
                    + "MultiObjectivePolicyState.");
        }

        /*
         * The original logged outcome is authoritative for an unchanged
         * baseline state. This guard preserves a genuine live
         * NO_FEASIBLE_VM result when the candidate log lacks a transient
         * live-only rejection detail. Counterfactual states still pass
         * through the shared scorer because they contain applied changes.
         */
        if(isUnchangedOriginalFailure(
                replayState)) {

            return noFeasibleVmDecision(
                    replayState,
                    "Preserved the logged live NO_FEASIBLE_VM "
                    + "outcome for the unchanged baseline state.");
        }

        MultiObjectivePolicyState policyState =
                (MultiObjectivePolicyState)
                genericState;

        MultiObjectiveConfig config =
                policyState.toConfig();

        MultiObjectiveScorer scorer =
                new MultiObjectiveScorer(
                        config);

        String destination =
                resolveReplayDestination(
                        replayState,
                        context.getCandidateSnapshots());

        List<VmCandidateSnapshot> destinationCandidates =
                context.getCandidatesForDestination(
                        destination);

        boolean networkFeasible =
                replayState
                .isNetworkFeasibleForDestination(
                        destination);

        double uploadDelayMs =
                replayState
                .getUploadDelayMsForDestination(
                        destination);

        MultiObjectiveSelectionResult selection =
                scorer.selectSnapshots(
                        destinationCandidates,
                        replayState.getTaskLengthMi(),
                        uploadDelayMs,
                        networkFeasible);

        if(!selection.hasSelection()) {

            return noFeasibleVmDecision(
                    replayState,
                    selection.getReason());
        }

        VmCandidateSnapshot selectedCandidate =
                findCandidateByIndex(
                        destinationCandidates,
                        selection.getSelectedCandidateIndex());

        if(selectedCandidate == null) {

            return indeterminateDecision(
                    replayState,
                    "Shared scorer selected candidate index "
                    + selection.getSelectedCandidateIndex()
                    + ", but that candidate was not present "
                    + "in the replay destination set.");
        }

        return selectedDecision(
                replayState,
                selectedCandidate,
                selection.getSelectedScore(),
                policyState);
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

    private String resolveReplayDestination(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidates) {

        /*
         * Baseline replay:
         * when no counterfactual modifications have been applied,
         * preserve the original selected destination so the replay
         * remains faithful to the logged live decision.
         */
        if(replayState.getAppliedChanges() == null
                || replayState.getAppliedChanges().isEmpty()) {

            String originalDestination =
                    replayState.getOriginalDestination();

            if(isConcreteDestination(
                    originalDestination)) {

                return originalDestination;
            }
        }

        /*
         * Counterfactual replay:
         * recompute the preferred destination from the modified
         * network and resource state. This permits genuine
         * EDGE-to-CLOUD and CLOUD-to-EDGE counterfactual changes.
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

            double processingTimeMs =
                    candidate.estimateProcessingTimeMs(
                            replayState.getTaskLengthMi());

            bestProcessingTimeMs =
                    Math.min(
                            bestProcessingTimeMs,
                            processingTimeMs);
        }

        if(!Double.isFinite(
                bestProcessingTimeMs)) {

            return Double.POSITIVE_INFINITY;
        }

        return uploadDelayMs
                + bestProcessingTimeMs;
    }

    private boolean isConcreteDestination(
            String destination) {

        return "EDGE".equalsIgnoreCase(
                    destination)
                || "CLOUD".equalsIgnoreCase(
                        destination)
                || "MOBILE".equalsIgnoreCase(
                        destination);
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

    private SchedulerDecision selectedDecision(
            ReplayState replayState,
            VmCandidateSnapshot candidate,
            MultiObjectiveScore score,
            MultiObjectivePolicyState policyState) {

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
                        "MULTI_OBJECTIVE_MIN_SCORE"
                        + "|CONFIG="
                        + policyState.getConfigurationId()
                        + "|SCORE="
                        + score.getTotalScore()
                        + "|LATENCY_MS="
                        + score.getTotalLatencyMs()
                        + "|ENERGY_J="
                        + score.getTotalEnergyJoules()
                        + "|LOAD="
                        + score.getLoadPenalty())

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
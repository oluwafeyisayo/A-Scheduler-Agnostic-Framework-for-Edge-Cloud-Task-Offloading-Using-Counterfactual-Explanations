package edu.boun.edgecloudsim.research.counterfactual.comparison;

import edu.boun.edgecloudsim.research.counterfactual.DecisionState;
import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerDecision;

/**
 * Scheduler-independent comparison between an original scheduler
 * decision and a replayed counterfactual decision.
 *
 * Supported counterfactual outcomes:
 *
 * 1. EXECUTION_TARGET_CHANGE
 * 2. DESTINATION_CHANGE
 * 3. FEASIBILITY_RESTORATION
 */
public final class DecisionComparator {

    /**
     * Compares a logged DecisionState with a replayed scheduler decision.
     */
    public DecisionComparisonResult compare(
            DecisionState originalDecision,
            SchedulerDecision replayedDecision) {

        if(originalDecision == null) {

            throw new IllegalArgumentException(
                    "Original DecisionState cannot be null.");
        }

        if(replayedDecision == null) {

            throw new IllegalArgumentException(
                    "Replayed SchedulerDecision cannot be null.");
        }

        if(originalDecision.getTaskId()
                != replayedDecision.getTaskId()) {

            throw new IllegalArgumentException(
                    "Original and replayed decisions must "
                    + "refer to the same task.");
        }

        if(!replayedDecision.wasSuccessfullyReplayed()) {

            return createNotComparableResult(
                    originalDecision.getTaskId(),
                    "The scheduler decision could not be "
                    + "replayed deterministically. Status: "
                    + replayedDecision.getReplayStatus());
        }

        return compareValues(
                originalDecision.getTaskId(),

                originalDecision.getDecisionOutcome(),
                originalDecision.getSelectedDestination(),
                originalDecision.getSelectedVmId(),

                replayedDecision.getDecisionOutcome(),
                replayedDecision.getSelectedDestination(),
                replayedDecision.getSelectedVmId());
    }

    /**
     * Compares the original-decision values retained by ReplayState
     * with a replayed scheduler decision.
     *
     * ReplayState continues to contain the original observable
     * scheduler outcome even after its mutable task and environment
     * features are changed.
     */
    public DecisionComparisonResult compare(
            ReplayState replayState,
            SchedulerDecision replayedDecision) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        if(replayedDecision == null) {

            throw new IllegalArgumentException(
                    "Replayed SchedulerDecision cannot be null.");
        }

        if(replayState.getTaskId()
                != replayedDecision.getTaskId()) {

            throw new IllegalArgumentException(
                    "ReplayState and replayed decision must "
                    + "refer to the same task.");
        }

        if(!replayedDecision.wasSuccessfullyReplayed()) {

            return createNotComparableResult(
                    replayState.getTaskId(),
                    "The scheduler decision could not be "
                    + "replayed deterministically. Status: "
                    + replayedDecision.getReplayStatus());
        }

        return compareValues(
                replayState.getTaskId(),

                replayState.getOriginalOutcome(),
                replayState.getOriginalDestination(),
                replayState.getOriginalSelectedVmId(),

                replayedDecision.getDecisionOutcome(),
                replayedDecision.getSelectedDestination(),
                replayedDecision.getSelectedVmId());
    }

    /**
     * Performs the shared observable-decision comparison.
     */
    private DecisionComparisonResult compareValues(
            long taskId,
            String originalOutcome,
            String originalDestination,
            int originalVmId,
            String replayedOutcome,
            String replayedDestination,
            int replayedVmId) {

        boolean outcomeChanged =
                !sameText(
                        originalOutcome,
                        replayedOutcome);

        boolean originalSelectionSuccessful =
                isSuccessfulSelection(
                        originalOutcome,
                        originalDestination,
                        originalVmId);

        boolean replayedSelectionSuccessful =
                isSuccessfulSelection(
                        replayedOutcome,
                        replayedDestination,
                        replayedVmId);

        /*
         * A supported destination change requires two successful
         * scheduler selections with different concrete destinations.
         *
         * Therefore:
         *
         * EDGE  -> CLOUD  is a destination change.
         * CLOUD -> EDGE   is a destination change.
         * NONE  -> EDGE   is feasibility restoration.
         * EDGE  -> NONE   is loss of schedulability, not a destination
         *                 counterfactual.
         */
        boolean destinationChanged =
                originalSelectionSuccessful
                && replayedSelectionSuccessful
                && !sameDestination(
                        originalDestination,
                        replayedDestination);

        /*
         * VM change is meaningful as an execution-target change only
         * when both decisions successfully selected resources.
         */
        boolean vmChanged =
                originalSelectionSuccessful
                && replayedSelectionSuccessful
                && originalVmId
                != replayedVmId;

        /*
         * Feasibility restoration occurs only when the original
         * decision was unsuccessful and the replayed decision
         * successfully selects an execution resource.
         */
        boolean feasibilityRestored =
                !originalSelectionSuccessful
                && replayedSelectionSuccessful;

        /*
         * A resource changes when either:
         *
         * - the selected VM changes; or
         * - the destination changes.
         *
         * Including destination prevents overlapping VM IDs in
         * different tiers from hiding a genuine target change.
         */
        boolean executionTargetChanged =
                originalSelectionSuccessful
                && replayedSelectionSuccessful
                && (
                        vmChanged
                        || destinationChanged
                );

        /*
         * A broader offloading-decision flip includes an outcome or
         * destination transition.
         */
        boolean schedulabilityLost =
                originalSelectionSuccessful
                && !replayedSelectionSuccessful;

        boolean offloadingDecisionFlipped =
                outcomeChanged
                || destinationChanged
                || feasibilityRestored
                || schedulabilityLost;

        DecisionChangeType primaryChangeType =
                determinePrimaryChangeType(
                        feasibilityRestored,
                        destinationChanged,
                        executionTargetChanged);

        String reason =
                buildReason(
                        feasibilityRestored,
                        destinationChanged,
                        executionTargetChanged,
                        schedulabilityLost,
                        outcomeChanged);

        return new DecisionComparisonResult(
                taskId,
                true,
                outcomeChanged,
                destinationChanged,
                vmChanged,
                executionTargetChanged,
                offloadingDecisionFlipped,
                feasibilityRestored,
                primaryChangeType,
                reason);
    }

    /**
     * Creates an indeterminate result without requiring a static
     * factory method in DecisionComparisonResult.
     */
    private DecisionComparisonResult createNotComparableResult(
            long taskId,
            String reason) {

        return new DecisionComparisonResult(
                taskId,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                reason);
    }

    /**
     * Chooses the most specific supported counterfactual outcome.
     */
    private DecisionChangeType determinePrimaryChangeType(
            boolean feasibilityRestored,
            boolean destinationChanged,
            boolean executionTargetChanged) {

        if(feasibilityRestored) {

            return DecisionChangeType
                    .FEASIBILITY_RESTORATION;
        }

        if(destinationChanged) {

            return DecisionChangeType
                    .DESTINATION_CHANGE;
        }

        if(executionTargetChanged) {

            return DecisionChangeType
                    .EXECUTION_TARGET_CHANGE;
        }

        return null;
    }

    private String buildReason(
            boolean feasibilityRestored,
            boolean destinationChanged,
            boolean executionTargetChanged,
            boolean schedulabilityLost,
            boolean outcomeChanged) {

        if(feasibilityRestored) {

            return "The originally unschedulable task became "
                    + "schedulable after counterfactual replay.";
        }

        if(destinationChanged) {

            return "The counterfactual caused the scheduler to "
                    + "select a different offloading destination.";
        }

        if(executionTargetChanged) {

            return "The scheduler retained the destination but "
                    + "selected a different execution target.";
        }

        if(schedulabilityLost) {

            return "The counterfactual caused a previously "
                    + "schedulable task to lose schedulability. "
                    + "This is not a supported destination-change "
                    + "or execution-target counterfactual.";
        }

        if(outcomeChanged) {

            return "The observable scheduler outcome changed.";
        }

        return "The replayed scheduler decision did not produce "
                + "a supported counterfactual change.";
    }

    /**
     * Determines whether an observable decision represents a
     * successful execution-resource selection.
     */
    private boolean isSuccessfulSelection(
            String outcome,
            String destination,
            int selectedVmId) {

        if(!"SELECTED".equals(
                normalizeText(
                        outcome))) {

            return false;
        }

        if(isNoDestination(
                destination)) {

            return false;
        }

        return selectedVmId >= 0;
    }

    /**
     * Treats the different representations of an absent destination
     * as semantically equivalent.
     */
    private boolean sameDestination(
            String first,
            String second) {

        if(isNoDestination(first)
                && isNoDestination(second)) {

            return true;
        }

        return sameText(
                first,
                second);
    }

    private boolean isNoDestination(
            String destination) {

        String normalized =
                normalizeText(
                        destination);

        return normalized.length() == 0
                || "NONE".equals(normalized)
                || "NO_DESTINATION".equals(normalized)
                || "UNSELECTED".equals(normalized)
                || "UNDETERMINED".equals(normalized)
                || "UNKNOWN".equals(normalized);
    }

    private boolean sameText(
            String first,
            String second) {

        return normalizeText(first)
                .equals(
                        normalizeText(second));
    }

    private String normalizeText(
            String value) {

        if(value == null) {

            return "";
        }

        return value
                .trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');
    }
}
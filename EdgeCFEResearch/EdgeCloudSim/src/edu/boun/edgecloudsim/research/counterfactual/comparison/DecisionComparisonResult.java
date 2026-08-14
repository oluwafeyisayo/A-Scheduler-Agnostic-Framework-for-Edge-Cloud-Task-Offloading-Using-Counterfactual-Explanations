package edu.boun.edgecloudsim.research.counterfactual.comparison;

/**
 * Scheduler-independent comparison between an original scheduler
 * decision and a replayed counterfactual decision.
 *
 * Supported counterfactual outcomes:
 *
 * 1. Execution-target change
 * 2. Destination change
 * 3. Feasibility restoration
 */
public final class DecisionComparisonResult {

    private final long taskId;

    private final boolean comparable;

    private final boolean outcomeChanged;
    private final boolean destinationChanged;

    private final boolean vmChanged;
    private final boolean hostChanged;
    private final boolean datacenterChanged;

    private final boolean executionTargetChanged;
    private final boolean offloadingDecisionFlipped;
    private final boolean feasibilityRestored;

    /*
     * May be null when no supported change occurred or when the
     * original and replayed decisions were not comparable.
     */
    private final DecisionChangeType primaryChangeType;

    private final String reason;

    /**
     * Complete constructor supporting VM, host, and datacenter
     * comparison.
     */
    public DecisionComparisonResult(
            long taskId,
            boolean comparable,
            boolean outcomeChanged,
            boolean destinationChanged,
            boolean vmChanged,
            boolean hostChanged,
            boolean datacenterChanged,
            boolean executionTargetChanged,
            boolean offloadingDecisionFlipped,
            boolean feasibilityRestored,
            DecisionChangeType primaryChangeType,
            String reason) {

        this.taskId =
                taskId;

        this.comparable =
                comparable;

        this.outcomeChanged =
                outcomeChanged;

        this.destinationChanged =
                destinationChanged;

        this.vmChanged =
                vmChanged;

        this.hostChanged =
                hostChanged;

        this.datacenterChanged =
                datacenterChanged;

        this.executionTargetChanged =
                executionTargetChanged;

        this.offloadingDecisionFlipped =
                offloadingDecisionFlipped;

        this.feasibilityRestored =
                feasibilityRestored;

        this.primaryChangeType =
                primaryChangeType;

        this.reason =
                reason == null
                || reason.trim().isEmpty()
                ? "NO_REASON"
                : reason.trim();
    }

    /**
     * Compatibility constructor for comparison code that currently
     * compares only VM identifiers.
     *
     * Host and datacenter changes default to false.
     */
    public DecisionComparisonResult(
            long taskId,
            boolean comparable,
            boolean outcomeChanged,
            boolean destinationChanged,
            boolean vmChanged,
            boolean executionTargetChanged,
            boolean offloadingDecisionFlipped,
            boolean feasibilityRestored,
            DecisionChangeType primaryChangeType,
            String reason) {

        this(
                taskId,
                comparable,
                outcomeChanged,
                destinationChanged,
                vmChanged,
                false,
                false,
                executionTargetChanged,
                offloadingDecisionFlipped,
                feasibilityRestored,
                primaryChangeType,
                reason);
    }

    /**
     * Creates a non-comparable result when deterministic replay was
     * not completed.
     */
    public static DecisionComparisonResult notComparable(
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
                false,
                false,
                null,
                reason);
    }

    public long getTaskId() {

        return taskId;
    }

    public boolean isComparable() {

        return comparable;
    }

    public boolean isOutcomeChanged() {

        return outcomeChanged;
    }

    public boolean isDestinationChanged() {

        return destinationChanged;
    }

    public boolean isVmChanged() {

        return vmChanged;
    }

    public boolean isHostChanged() {

        return hostChanged;
    }

    public boolean isDatacenterChanged() {

        return datacenterChanged;
    }

    public boolean isExecutionTargetChanged() {

        return executionTargetChanged;
    }

    public boolean isOffloadingDecisionFlipped() {

        return offloadingDecisionFlipped;
    }

    /**
     * Backward-compatible alias.
     */
    public boolean isDecisionFlipped() {

        return offloadingDecisionFlipped;
    }

    public boolean isFeasibilityRestored() {

        return feasibilityRestored;
    }

    public DecisionChangeType getPrimaryChangeType() {

        return primaryChangeType;
    }

    /**
     * Backward-compatible alias.
     */
    public DecisionChangeType getChangeType() {

        return primaryChangeType;
    }

    public String getReason() {

        return reason;
    }

    /**
     * Returns true when at least one supported counterfactual
     * decision change occurred.
     */
    public boolean hasAnyDecisionChange() {

        if(!comparable) {

            return false;
        }

        return executionTargetChanged
                || destinationChanged
                || feasibilityRestored
                || outcomeChanged;
    }

    /**
     * Determines whether the observed replay result satisfies the
     * requested counterfactual objective.
     */
    public boolean satisfies(
            DecisionChangeType requestedChangeType) {

        if(!comparable
                || requestedChangeType == null) {

            return false;
        }

        switch(requestedChangeType) {

            case EXECUTION_TARGET_CHANGE:

                return executionTargetChanged;

            case DESTINATION_CHANGE:

                return destinationChanged;

            case FEASIBILITY_RESTORATION:

                return feasibilityRestored;

            default:

                return false;
        }
    }

    @Override
    public String toString() {

        return "DecisionComparisonResult{"
                + "taskId="
                + taskId
                + ", comparable="
                + comparable
                + ", outcomeChanged="
                + outcomeChanged
                + ", destinationChanged="
                + destinationChanged
                + ", vmChanged="
                + vmChanged
                + ", hostChanged="
                + hostChanged
                + ", datacenterChanged="
                + datacenterChanged
                + ", executionTargetChanged="
                + executionTargetChanged
                + ", offloadingDecisionFlipped="
                + offloadingDecisionFlipped
                + ", feasibilityRestored="
                + feasibilityRestored
                + ", primaryChangeType="
                + primaryChangeType
                + ", reason='"
                + reason
                + '\''
                + '}';
    }
}
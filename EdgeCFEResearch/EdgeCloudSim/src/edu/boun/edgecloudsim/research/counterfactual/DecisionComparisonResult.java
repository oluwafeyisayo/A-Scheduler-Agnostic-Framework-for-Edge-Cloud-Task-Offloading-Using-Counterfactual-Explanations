package edu.boun.edgecloudsim.research.counterfactual;

public final class DecisionComparisonResult {

    private final long taskId;

    private final boolean comparable;
    private final boolean outcomeChanged;
    private final boolean destinationChanged;
    private final boolean vmChanged;
    private final boolean decisionFlipped;

    private final String reason;

    public DecisionComparisonResult(
            long taskId,
            boolean comparable,
            boolean outcomeChanged,
            boolean destinationChanged,
            boolean vmChanged,
            boolean decisionFlipped,
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

        this.decisionFlipped =
                decisionFlipped;

        this.reason =
                reason == null
                ? "NO_REASON"
                : reason;
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

    public boolean isDecisionFlipped() {
        return decisionFlipped;
    }

    public String getReason() {
        return reason;
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
                + ", decisionFlipped="
                + decisionFlipped
                + ", reason='"
                + reason
                + '\''
                + '}';
    }
}
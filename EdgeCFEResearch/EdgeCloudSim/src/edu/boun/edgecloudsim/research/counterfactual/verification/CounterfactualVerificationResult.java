package edu.boun.edgecloudsim.research.counterfactual.verification;

import edu.boun.edgecloudsim.research.counterfactual.FeasibilityResult;
import edu.boun.edgecloudsim.research.counterfactual.comparison.DecisionChangeType;
import edu.boun.edgecloudsim.research.counterfactual.comparison.DecisionComparisonResult;
import edu.boun.edgecloudsim.research.counterfactual.replay.SchedulerDecision;

/**
 * Aggregates feasibility validation, scheduler replay, and decision
 * comparison into one scheduler-independent verification result.
 */
public final class CounterfactualVerificationResult {

    private final long taskId;

    private final CounterfactualVerificationStatus status;

    private final DecisionChangeType requestedChangeType;
    private final DecisionChangeType observedChangeType;

    private final boolean feasible;
    private final boolean replaySuccessful;
    private final boolean targetAchieved;
    private final boolean accepted;

    private final FeasibilityResult feasibilityResult;
    private final SchedulerDecision schedulerDecision;
    private final DecisionComparisonResult comparisonResult;

    private final String reason;

    public CounterfactualVerificationResult(
            long taskId,
            CounterfactualVerificationStatus status,
            DecisionChangeType requestedChangeType,
            DecisionChangeType observedChangeType,
            boolean feasible,
            boolean replaySuccessful,
            boolean targetAchieved,
            boolean accepted,
            FeasibilityResult feasibilityResult,
            SchedulerDecision schedulerDecision,
            DecisionComparisonResult comparisonResult,
            String reason) {

        this.taskId =
                taskId;

        this.status =
                status == null
                ? CounterfactualVerificationStatus.ERROR
                : status;

        if(requestedChangeType == null) {

            throw new IllegalArgumentException(
                    "Requested DecisionChangeType cannot be null.");
        }

        this.requestedChangeType =
                requestedChangeType;

        this.observedChangeType =
                observedChangeType;

        this.feasible =
                feasible;

        this.replaySuccessful =
                replaySuccessful;

        this.targetAchieved =
                targetAchieved;

        this.accepted =
                accepted;

        this.feasibilityResult =
                feasibilityResult;

        this.schedulerDecision =
                schedulerDecision;

        this.comparisonResult =
                comparisonResult;

        this.reason =
                reason == null
                ? "NO_REASON"
                : reason;
    }

    public long getTaskId() {
        return taskId;
    }

    public CounterfactualVerificationStatus getStatus() {
        return status;
    }

    public DecisionChangeType getRequestedChangeType() {
        return requestedChangeType;
    }

    public DecisionChangeType getObservedChangeType() {
        return observedChangeType;
    }

    public boolean isFeasible() {
        return feasible;
    }

    public boolean isReplaySuccessful() {
        return replaySuccessful;
    }

    public boolean isTargetAchieved() {
        return targetAchieved;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public FeasibilityResult getFeasibilityResult() {
        return feasibilityResult;
    }

    public SchedulerDecision getSchedulerDecision() {
        return schedulerDecision;
    }

    public DecisionComparisonResult getComparisonResult() {
        return comparisonResult;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {

        return "CounterfactualVerificationResult{"
                + "taskId="
                + taskId
                + ", status="
                + status
                + ", requestedChangeType="
                + requestedChangeType
                + ", observedChangeType="
                + observedChangeType
                + ", feasible="
                + feasible
                + ", replaySuccessful="
                + replaySuccessful
                + ", targetAchieved="
                + targetAchieved
                + ", accepted="
                + accepted
                + ", reason='"
                + reason
                + '\''
                + '}';
    }
}
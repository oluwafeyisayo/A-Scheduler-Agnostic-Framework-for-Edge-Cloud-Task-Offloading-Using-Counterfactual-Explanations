package edu.boun.edgecloudsim.research.counterfactual.verification;

import edu.boun.edgecloudsim.research.counterfactual.FeasibilityResult;
import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.comparison.DecisionChangeType;
import edu.boun.edgecloudsim.research.counterfactual.comparison.DecisionComparator;
import edu.boun.edgecloudsim.research.counterfactual.comparison.DecisionComparisonResult;
import edu.boun.edgecloudsim.research.counterfactual.replay.SchedulerDecision;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.SchedulerReplayContext;
import edu.boun.edgecloudsim.research.counterfactual.replay.engine.SchedulerReplayEngine;

/**
 * Scheduler-agnostic verification service.
 *
 * The verifier delegates scheduler behavior to the replay engine and
 * evaluates only the resulting observable decision.
 */
public final class CounterfactualVerifier {

    private final SchedulerReplayEngine replayEngine;
    private final DecisionComparator decisionComparator;

    public CounterfactualVerifier(
            SchedulerReplayEngine replayEngine) {

        if(replayEngine == null) {

            throw new IllegalArgumentException(
                    "SchedulerReplayEngine cannot be null.");
        }

        this.replayEngine =
                replayEngine;

        this.decisionComparator =
                new DecisionComparator();
    }

    public CounterfactualVerificationResult verify(
            SchedulerReplayContext replayContext,
            FeasibilityResult feasibilityResult,
            DecisionChangeType requestedChangeType) {

        if(replayContext == null) {

            throw new IllegalArgumentException(
                    "SchedulerReplayContext cannot be null.");
        }

        if(feasibilityResult == null) {

            throw new IllegalArgumentException(
                    "FeasibilityResult cannot be null.");
        }

        if(requestedChangeType == null) {

            throw new IllegalArgumentException(
                    "Requested DecisionChangeType cannot be null.");
        }

        ReplayState replayState =
                replayContext.getReplayState();

        if(!feasibilityResult.isOperationallyFeasible()) {

            return new CounterfactualVerificationResult(
                    replayState.getTaskId(),
                    CounterfactualVerificationStatus.INFEASIBLE,
                    requestedChangeType,
                    null,
                    false,
                    false,
                    false,
                    false,
                    feasibilityResult,
                    null,
                    null,
                    "The counterfactual is not operationally "
                    + "feasible. Primary failed diagnostic "
                    + "constraint: "
                    + feasibilityResult
                    .getPrimaryFailureReason());
        }

        SchedulerDecision schedulerDecision =
                replayEngine.replay(
                        replayContext);

        if(!schedulerDecision.wasSuccessfullyReplayed()) {

            return new CounterfactualVerificationResult(
                    replayState.getTaskId(),
                    CounterfactualVerificationStatus
                    .REPLAY_INDETERMINATE,
                    requestedChangeType,
                    null,
                    true,
                    false,
                    false,
                    false,
                    feasibilityResult,
                    schedulerDecision,
                    null,
                    "The scheduler could not be replayed "
                    + "deterministically. Status: "
                    + schedulerDecision.getReplayStatus());
        }

        DecisionComparisonResult comparisonResult =
                decisionComparator.compare(
                        replayState,
                        schedulerDecision);

        DecisionChangeType observedChangeType =
                comparisonResult.getPrimaryChangeType();

        if(!comparisonResult.hasAnyDecisionChange()) {

            return new CounterfactualVerificationResult(
                    replayState.getTaskId(),
                    CounterfactualVerificationStatus
                    .NO_DECISION_CHANGE,
                    requestedChangeType,
                    null,
                    true,
                    true,
                    false,
                    false,
                    feasibilityResult,
                    schedulerDecision,
                    comparisonResult,
                    "The modified state is feasible, but the "
                    + "scheduler decision did not produce a "
                    + "supported counterfactual change.");
        }

        boolean targetAchieved =
                comparisonResult.satisfies(
                        requestedChangeType);

        if(!targetAchieved) {

            return new CounterfactualVerificationResult(
                    replayState.getTaskId(),
                    CounterfactualVerificationStatus
                    .TARGET_NOT_ACHIEVED,
                    requestedChangeType,
                    observedChangeType,
                    true,
                    true,
                    false,
                    false,
                    feasibilityResult,
                    schedulerDecision,
                    comparisonResult,
                    "The scheduler decision changed, but the "
                    + "requested change type was not achieved.");
        }

        return new CounterfactualVerificationResult(
                replayState.getTaskId(),
                CounterfactualVerificationStatus.VERIFIED,
                requestedChangeType,
                observedChangeType,
                true,
                true,
                true,
                true,
                feasibilityResult,
                schedulerDecision,
                comparisonResult,
                buildVerifiedReason(
                        requestedChangeType));
    }

    private static String buildVerifiedReason(
            DecisionChangeType requestedChangeType) {

        switch(requestedChangeType) {

            case EXECUTION_TARGET_CHANGE:

                return "The counterfactual is operationally "
                        + "feasible and caused the scheduler to "
                        + "select a different execution target.";

            case DESTINATION_CHANGE:

                return "The counterfactual is operationally "
                        + "feasible and caused the scheduler to "
                        + "select a different offloading "
                        + "destination.";

            case FEASIBILITY_RESTORATION:

                return "The counterfactual is operationally "
                        + "feasible and restored schedulability "
                        + "for an originally unschedulable task.";

            default:

                return "The counterfactual is operationally "
                        + "feasible and produced the requested "
                        + "scheduler decision change.";
        }
    }
}
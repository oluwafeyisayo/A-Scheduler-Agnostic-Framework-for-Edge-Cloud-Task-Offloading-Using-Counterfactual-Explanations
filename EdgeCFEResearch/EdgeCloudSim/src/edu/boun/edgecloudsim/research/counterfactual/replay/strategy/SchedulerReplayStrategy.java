package edu.boun.edgecloudsim.research.counterfactual.replay.strategy;

import edu.boun.edgecloudsim.research.counterfactual.replay.SchedulerDecision;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.SchedulerReplayContext;

/**
 * Extension point for scheduler-specific replay behavior.
 *
 * The wider counterfactual framework interacts only with this
 * interface and therefore remains scheduler-agnostic.
 */
public interface SchedulerReplayStrategy {

    String getSupportedPolicy();

    SchedulerDecision replay(
            SchedulerReplayContext context);

    default boolean supports(
            String policyName) {

        return policyName != null
                && getSupportedPolicy()
                .equalsIgnoreCase(
                        policyName.trim());
    }
}
package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Evaluates whether the originally selected VM provides enough
 * processing cores for the task.
 *
 * This rule is diagnostic. A counterfactual may make the originally
 * selected VM infeasible while another candidate VM remains feasible.
 */
public final class CoreFeasibilityRule
        implements FeasibilityRule {

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .SELECTED_RESOURCE_CORE_CAPACITY;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        int requiredCores =
                replayState.getRequiredCores();

        int availableCores =
                replayState.getSelectedVmCoreCount();

        int margin =
                availableCores
                - requiredCores;

        boolean satisfied =
                availableCores >= 0
                && margin >= 0;

        String message;

        if(satisfied) {

            message =
                    "The referenced resource has sufficient core "
                    + "capacity. Available cores="
                    + availableCores
                    + ", required cores="
                    + requiredCores
                    + ".";
        }
        else if(availableCores < 0) {

            message =
                    "Core capacity cannot be evaluated because no "
                    + "valid selected VM core count is available.";
        }
        else {

            message =
                    "The referenced resource has insufficient core "
                    + "capacity. Available cores="
                    + availableCores
                    + ", required cores="
                    + requiredCores
                    + ". Another candidate resource may still "
                    + "be feasible.";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                satisfied,
                availableCores,
                requiredCores,
                margin,
                message);
    }
}
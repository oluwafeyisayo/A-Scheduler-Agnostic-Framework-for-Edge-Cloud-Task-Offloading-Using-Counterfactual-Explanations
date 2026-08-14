package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Evaluates whether the originally selected replay-state resource
 * retains sufficient CPU capacity for the task.
 *
 * This rule does not determine whether another candidate VM can
 * execute the task.
 */
public final class CpuFeasibilityRule
        implements FeasibilityRule {

    private static final double EPSILON =
            0.0001;

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .SELECTED_RESOURCE_CPU_CAPACITY;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        double requiredCpu =
                replayState
                .getRequiredCpuPercent();

        double availableCpu =
                replayState
                .getSelectedAvailableCpuPercent();

        double margin =
                availableCpu
                - requiredCpu;

        boolean satisfied =
                margin >= -EPSILON;

        String message;

        if(satisfied) {

            message =
                    "The referenced resource has sufficient CPU "
                    + "capacity. Available CPU is "
                    + availableCpu
                    + "% and required CPU is "
                    + requiredCpu
                    + "%.";
        }
        else {

            message =
                    "The referenced resource has insufficient CPU "
                    + "capacity. Available CPU is "
                    + availableCpu
                    + "% but required CPU is "
                    + requiredCpu
                    + "%. Another candidate resource may still "
                    + "be feasible.";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                satisfied,
                availableCpu,
                requiredCpu,
                margin,
                message);
    }
}
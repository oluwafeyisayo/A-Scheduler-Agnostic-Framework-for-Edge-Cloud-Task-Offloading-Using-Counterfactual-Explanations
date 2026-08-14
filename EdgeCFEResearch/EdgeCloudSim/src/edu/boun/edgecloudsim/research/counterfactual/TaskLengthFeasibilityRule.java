package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Evaluates whether the task computational length remains within
 * the configured schedulable limit.
 *
 * The threshold is temporary. It will later be obtained from the
 * application profile or destination-specific service requirement.
 */
public final class TaskLengthFeasibilityRule
        implements FeasibilityRule {

    private static final double EPSILON =
            0.000001;

    private final double maximumTaskLength;

    public TaskLengthFeasibilityRule(
            double maximumTaskLength) {

        if(Double.isNaN(maximumTaskLength)
                || Double.isInfinite(maximumTaskLength)
                || maximumTaskLength <= 0.0) {

            throw new IllegalArgumentException(
                    "Maximum task length must be finite "
                    + "and greater than zero.");
        }

        this.maximumTaskLength =
                maximumTaskLength;
    }

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .TASK_LENGTH;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        double observedTaskLength =
                replayState
                .getTaskLength();

        double margin =
                maximumTaskLength
                - observedTaskLength;

        boolean passed =
                observedTaskLength > 0.0
                && observedTaskLength
                <= maximumTaskLength
                + EPSILON;

        String explanation;

        if(passed) {

            explanation =
                    "Task length satisfies the configured "
                    + "computational limit. Task length="
                    + observedTaskLength
                    + ", maximum task length="
                    + maximumTaskLength
                    + ", remaining margin="
                    + margin
                    + ".";
        }
        else {

            explanation =
                    "Task length exceeds the configured "
                    + "computational limit. Task length="
                    + observedTaskLength
                    + ", maximum task length="
                    + maximumTaskLength
                    + ", excess="
                    + Math.abs(margin)
                    + ".";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                passed,
                observedTaskLength,
                maximumTaskLength,
                margin,
                explanation);
    }
}
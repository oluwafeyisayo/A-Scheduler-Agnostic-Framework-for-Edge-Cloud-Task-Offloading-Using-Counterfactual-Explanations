package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Immutable result for one feasibility constraint.
 */
public final class ConstraintEvaluation {

    private final FeasibilityConstraint constraint;
    private final boolean satisfied;
    private final double observedValue;
    private final double requiredValue;
    private final double margin;
    private final String message;

    public ConstraintEvaluation(
            FeasibilityConstraint constraint,
            boolean satisfied,
            double observedValue,
            double requiredValue,
            double margin,
            String message) {

        if(constraint == null) {

            throw new IllegalArgumentException(
                    "Feasibility constraint cannot be null.");
        }

        this.constraint =
                constraint;

        this.satisfied =
                satisfied;

        this.observedValue =
                observedValue;

        this.requiredValue =
                requiredValue;

        this.margin =
                margin;

        this.message =
                safeText(
                        message);
    }

    private static String safeText(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return "NO_MESSAGE";
        }

        return value.trim();
    }

    public FeasibilityConstraint getConstraint() {

        return constraint;
    }

    public boolean isSatisfied() {

        return satisfied;
    }

    public double getObservedValue() {

        return observedValue;
    }

    public double getRequiredValue() {

        return requiredValue;
    }

    public double getMargin() {

        return margin;
    }

    public String getMessage() {

        return message;
    }

    @Override
    public String toString() {

        return "ConstraintEvaluation{"
                + "constraint="
                + constraint
                + ", satisfied="
                + satisfied
                + ", observedValue="
                + observedValue
                + ", requiredValue="
                + requiredValue
                + ", margin="
                + margin
                + ", message='"
                + message
                + '\''
                + '}';
    }
}
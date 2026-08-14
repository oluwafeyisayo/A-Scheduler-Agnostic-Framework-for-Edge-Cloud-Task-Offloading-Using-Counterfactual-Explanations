package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Evaluates whether the estimated total network delay remains
 * within the maximum permitted latency.
 *
 * Total network delay is calculated by ReplayState as:
 *
 * network latency + upload transmission time
 */
public final class LatencyFeasibilityRule
        implements FeasibilityRule {

    private static final double EPSILON =
            0.000001;

    private final double maximumAllowedLatency;

    public LatencyFeasibilityRule(
            double maximumAllowedLatency) {

        if(Double.isNaN(maximumAllowedLatency)
                || Double.isInfinite(maximumAllowedLatency)
                || maximumAllowedLatency < 0.0) {

            throw new IllegalArgumentException(
                    "Maximum allowed latency must be finite "
                    + "and nonnegative.");
        }

        this.maximumAllowedLatency =
                maximumAllowedLatency;
    }

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .LATENCY;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        double estimatedNetworkDelay =
                replayState
                .estimateTotalNetworkDelay();

        double margin =
                maximumAllowedLatency
                - estimatedNetworkDelay;

        boolean passed =
                !Double.isInfinite(
                        estimatedNetworkDelay)
                && !Double.isNaN(
                        estimatedNetworkDelay)
                && estimatedNetworkDelay
                <= maximumAllowedLatency
                + EPSILON;

        String explanation;

        if(passed) {

            explanation =
                    "Estimated network delay satisfies the latency "
                    + "requirement. Estimated delay="
                    + estimatedNetworkDelay
                    + ", maximum allowed latency="
                    + maximumAllowedLatency
                    + ", remaining margin="
                    + margin
                    + ".";
        }
        else {

            explanation =
                    "Estimated network delay exceeds the latency "
                    + "requirement. Estimated delay="
                    + estimatedNetworkDelay
                    + ", maximum allowed latency="
                    + maximumAllowedLatency
                    + ", excess delay="
                    + Math.abs(
                            margin)
                    + ".";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                passed,
                estimatedNetworkDelay,
                maximumAllowedLatency,
                margin,
                explanation);
    }
}
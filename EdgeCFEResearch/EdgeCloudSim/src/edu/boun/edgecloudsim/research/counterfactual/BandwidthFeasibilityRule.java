package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Evaluates whether sufficient bandwidth is available to transmit
 * the task input.
 *
 * The required bandwidth value is currently supplied directly to
 * the rule. Later, it can be derived from task size, latency limits,
 * and EdgeCloudSim network conditions.
 */
public final class BandwidthFeasibilityRule
        implements FeasibilityRule {

    private static final double EPSILON =
            0.000001;

    private final double requiredBandwidth;

    public BandwidthFeasibilityRule(
            double requiredBandwidth) {

        if(Double.isNaN(requiredBandwidth)
                || Double.isInfinite(requiredBandwidth)
                || requiredBandwidth < 0.0) {

            throw new IllegalArgumentException(
                    "Required bandwidth must be finite and nonnegative.");
        }

        this.requiredBandwidth =
                requiredBandwidth;
    }

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .BANDWIDTH;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        double availableBandwidth =
                replayState
                .getAvailableBandwidth();

        double margin =
                availableBandwidth
                - requiredBandwidth;

        boolean passed =
                availableBandwidth + EPSILON
                >= requiredBandwidth;

        String explanation;

        if(passed) {

            explanation =
                    "Available bandwidth satisfies the network "
                    + "requirement. Available bandwidth="
                    + availableBandwidth
                    + ", required bandwidth="
                    + requiredBandwidth
                    + ", margin="
                    + margin
                    + ".";
        }
        else {

            explanation =
                    "Available bandwidth is insufficient. "
                    + "Available bandwidth="
                    + availableBandwidth
                    + ", required bandwidth="
                    + requiredBandwidth
                    + ", deficit="
                    + Math.abs(margin)
                    + ".";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                passed,
                availableBandwidth,
                requiredBandwidth,
                margin,
                explanation);
    }
}
package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Evaluates whether the task input size remains within the configured
 * upload limit.
 *
 * Upload size also influences transmission delay through ReplayState,
 * but this rule independently enforces a maximum transferable input size.
 */
public final class UploadSizeFeasibilityRule
        implements FeasibilityRule {

    private static final double EPSILON =
            0.000001;

    private final double maximumUploadSize;

    public UploadSizeFeasibilityRule(
            double maximumUploadSize) {

        if(Double.isNaN(maximumUploadSize)
                || Double.isInfinite(maximumUploadSize)
                || maximumUploadSize <= 0.0) {

            throw new IllegalArgumentException(
                    "Maximum upload size must be finite "
                    + "and greater than zero.");
        }

        this.maximumUploadSize =
                maximumUploadSize;
    }

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .UPLOAD_SIZE;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        double observedUploadSize =
                replayState
                .getUploadSize();

        double margin =
                maximumUploadSize
                - observedUploadSize;

        boolean passed =
                observedUploadSize > 0.0
                && observedUploadSize
                <= maximumUploadSize
                + EPSILON;

        String explanation;

        if(passed) {

            explanation =
                    "Upload size satisfies the configured transfer "
                    + "limit. Upload size="
                    + observedUploadSize
                    + ", maximum upload size="
                    + maximumUploadSize
                    + ", remaining margin="
                    + margin
                    + ".";
        }
        else {

            explanation =
                    "Upload size exceeds the configured transfer "
                    + "limit. Upload size="
                    + observedUploadSize
                    + ", maximum upload size="
                    + maximumUploadSize
                    + ", excess="
                    + Math.abs(margin)
                    + ".";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                passed,
                observedUploadSize,
                maximumUploadSize,
                margin,
                explanation);
    }
}
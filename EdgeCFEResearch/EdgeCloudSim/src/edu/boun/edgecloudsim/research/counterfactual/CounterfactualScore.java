package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Stores the objective-function score assigned to one
 * counterfactual replay state.
 *
 * Lower scores are preferred.
 *
 * totalScore =
 *     proximityWeight * proximity
 *   + sparsityWeight  * sparsity
 *   + effortWeight    * effort
 *   + networkWeight   * networkPenalty
 */
public final class CounterfactualScore
        implements Comparable<CounterfactualScore> {

    private final double proximity;
    private final double sparsity;
    private final double effort;
    private final double networkPenalty;
    private final double totalScore;

    public CounterfactualScore(
            double proximity,
            double sparsity,
            double effort,
            double networkPenalty,
            double totalScore) {

        validateNonNegativeFinite(
                proximity,
                "Proximity");

        validateNonNegativeFinite(
                sparsity,
                "Sparsity");

        validateNonNegativeFinite(
                effort,
                "Effort");

        validateNonNegativeFinite(
                networkPenalty,
                "Network penalty");

        validateNonNegativeFinite(
                totalScore,
                "Total score");

        this.proximity =
                proximity;

        this.sparsity =
                sparsity;

        this.effort =
                effort;

        this.networkPenalty =
                networkPenalty;

        this.totalScore =
                totalScore;
    }

    public double getProximity() {

        return proximity;
    }

    public double getSparsity() {

        return sparsity;
    }

    public double getEffort() {

        return effort;
    }

    public double getNetworkPenalty() {

        return networkPenalty;
    }

    public double getTotalScore() {

        return totalScore;
    }

    /**
     * Lower scores are considered better.
     */
    public boolean isBetterThan(
            CounterfactualScore other) {

        if(other == null) {

            return true;
        }

        return totalScore
                < other.totalScore;
    }

    @Override
    public int compareTo(
            CounterfactualScore other) {

        if(other == null) {

            return -1;
        }

        return Double.compare(
                this.totalScore,
                other.totalScore);
    }

    private static void validateNonNegativeFinite(
            double value,
            String fieldName) {

        if(Double.isNaN(value)
                || Double.isInfinite(value)
                || value < 0.0) {

            throw new IllegalArgumentException(
                    fieldName
                    + " must be finite and nonnegative.");
        }
    }

    @Override
    public String toString() {

        return "CounterfactualScore{"
                + "proximity="
                + proximity
                + ", sparsity="
                + sparsity
                + ", effort="
                + effort
                + ", networkPenalty="
                + networkPenalty
                + ", totalScore="
                + totalScore
                + '}';
    }
}
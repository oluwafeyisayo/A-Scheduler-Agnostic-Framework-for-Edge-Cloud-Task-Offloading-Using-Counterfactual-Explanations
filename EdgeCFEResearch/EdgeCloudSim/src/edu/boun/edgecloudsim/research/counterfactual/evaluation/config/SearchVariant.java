package edu.boun.edgecloudsim.research.counterfactual.evaluation.config;

/**
 * Identifies the counterfactual-search configuration used by an
 * experiment profile.
 */
public enum SearchVariant {

    ADAPTIVE_ONLY(
            "ADAPTIVE_ONLY",
            true,
            false,
            false),

    ADAPTIVE_PLUS_NEIGHBORHOOD(
            "ADAPTIVE_PLUS_NEIGHBORHOOD",
            true,
            true,
            false),

    FIXED_EXHAUSTIVE(
            "FIXED_EXHAUSTIVE",
            false,
            false,
            true);

    private final String code;
    private final boolean adaptiveBudgetEnabled;
    private final boolean neighborhoodRefinementEnabled;
    private final boolean exhaustiveEvaluationEnabled;

    SearchVariant(
            String code,
            boolean adaptiveBudgetEnabled,
            boolean neighborhoodRefinementEnabled,
            boolean exhaustiveEvaluationEnabled) {

        this.code =
                requireText(
                        code,
                        "code");

        this.adaptiveBudgetEnabled =
                adaptiveBudgetEnabled;

        this.neighborhoodRefinementEnabled =
                neighborhoodRefinementEnabled;

        this.exhaustiveEvaluationEnabled =
                exhaustiveEvaluationEnabled;
    }

    public String getCode() {

        return code;
    }

    public boolean isAdaptiveBudgetEnabled() {

        return adaptiveBudgetEnabled;
    }

    public boolean isNeighborhoodRefinementEnabled() {

        return neighborhoodRefinementEnabled;
    }

    public boolean isExhaustiveEvaluationEnabled() {

        return exhaustiveEvaluationEnabled;
    }

    private static String requireText(
            String value,
            String fieldName) {

        if(value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                    + " cannot be null or empty.");
        }

        return value.trim();
    }
}
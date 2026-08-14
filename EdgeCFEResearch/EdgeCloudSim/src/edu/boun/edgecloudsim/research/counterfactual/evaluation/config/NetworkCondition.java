package edu.boun.edgecloudsim.research.counterfactual.evaluation.config;

/**
 * Defines the network condition associated with an evaluation profile.
 *
 * Stage B uses NORMAL. Stage C can later bind the remaining values to
 * dedicated simulator property files.
 */
public enum NetworkCondition {

    NORMAL(
            "NORMAL",
            "Normal network condition"),

    MODERATE_CONGESTION(
            "MODERATE_CONGESTION",
            "Moderately constrained bandwidth and increased delay"),

    HIGH_CONGESTION(
            "HIGH_CONGESTION",
            "Severely constrained bandwidth and increased delay");

    private final String code;
    private final String description;

    NetworkCondition(
            String code,
            String description) {

        this.code =
                requireText(
                        code,
                        "code");

        this.description =
                requireText(
                        description,
                        "description");
    }

    public String getCode() {

        return code;
    }

    public String getDescription() {

        return description;
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
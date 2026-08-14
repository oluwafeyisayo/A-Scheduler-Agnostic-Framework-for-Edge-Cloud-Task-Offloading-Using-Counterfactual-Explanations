package edu.boun.edgecloudsim.research.counterfactual.evaluation.config;

/**
 * Identifies a major evaluation stage and its primary independent
 * variable.
 */
public enum EvaluationStage {

    STAGE_A(
            "Functional Scheduler Comparison",
            "stage_a",
            "scheduler"),

    STAGE_B(
            "Scalability Evaluation",
            "stage_b",
            "device_count"),

    STAGE_C(
            "Network Sensitivity Evaluation",
            "stage_c",
            "network_condition"),

    STAGE_D(
            "Search Ablation Evaluation",
            "stage_d",
            "search_variant");

    private final String displayName;
    private final String folderName;
    private final String independentVariable;

    EvaluationStage(
            String displayName,
            String folderName,
            String independentVariable) {

        this.displayName =
                requireText(
                        displayName,
                        "displayName");

        this.folderName =
                requireText(
                        folderName,
                        "folderName");

        this.independentVariable =
                requireText(
                        independentVariable,
                        "independentVariable");
    }

    public String getDisplayName() {

        return displayName;
    }

    public String getFolderName() {

        return folderName;
    }

    public String getIndependentVariable() {

        return independentVariable;
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
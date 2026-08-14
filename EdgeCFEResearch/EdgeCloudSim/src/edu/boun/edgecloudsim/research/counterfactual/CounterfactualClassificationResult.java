package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Stores the semantic classification of one evaluated counterfactual.
 */
public final class CounterfactualClassificationResult {

    private final long taskId;
    private final long candidateId;

    private final CounterfactualClassification classification;

    private final boolean candidateValid;
    private final boolean replaySuccessful;
    private final boolean targetAchieved;
    private final boolean operationallyFeasible;

    private final String reason;

    public CounterfactualClassificationResult(
            long taskId,
            long candidateId,
            CounterfactualClassification classification,
            boolean candidateValid,
            boolean replaySuccessful,
            boolean targetAchieved,
            boolean operationallyFeasible,
            String reason) {

        if(classification == null) {

            throw new IllegalArgumentException(
                    "Counterfactual classification cannot be null.");
        }

        this.taskId =
                taskId;

        this.candidateId =
                candidateId;

        this.classification =
                classification;

        this.candidateValid =
                candidateValid;

        this.replaySuccessful =
                replaySuccessful;

        this.targetAchieved =
                targetAchieved;

        this.operationallyFeasible =
                operationallyFeasible;

        this.reason =
                reason == null
                ? ""
                : reason;
    }

    public long getTaskId() {

        return taskId;
    }

    public long getCandidateId() {

        return candidateId;
    }

    public CounterfactualClassification getClassification() {

        return classification;
    }

    public boolean isCandidateValid() {

        return candidateValid;
    }

    public boolean isReplaySuccessful() {

        return replaySuccessful;
    }

    public boolean isTargetAchieved() {

        return targetAchieved;
    }

    public boolean isOperationallyFeasible() {

        return operationallyFeasible;
    }

    /**
     * Returns true only for an actionable counterfactual explanation.
     */
    public boolean isAcceptedCounterfactual() {

        return classification
                == CounterfactualClassification.FEASIBLE_EFFECTIVE;
    }

    public String getReason() {

        return reason;
    }

    @Override
    public String toString() {

        return "CounterfactualClassificationResult{"
                + "taskId=" + taskId
                + ", candidateId=" + candidateId
                + ", classification=" + classification
                + ", candidateValid=" + candidateValid
                + ", replaySuccessful=" + replaySuccessful
                + ", targetAchieved=" + targetAchieved
                + ", operationallyFeasible="
                + operationallyFeasible
                + ", acceptedCounterfactual="
                + isAcceptedCounterfactual()
                + ", reason='" + reason + '\''
                + '}';
    }
}
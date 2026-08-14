package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Immutable CSV representation of one RG-FCS experiment result.
 */
public final class CounterfactualResultRecord {

    private final int taskId;
    private final String schedulerPolicy;
    private final String searchStatus;
    private final boolean searchSuccessful;

    private final int adaptiveGeneratedCandidateCount;
    private final int adaptiveEvaluatedCandidateCount;
    private final int adaptiveAcceptedCandidateCount;

    private final int neighborhoodGeneratedCandidateCount;
    private final int neighborhoodEvaluatedCandidateCount;
    private final int neighborhoodAcceptedCandidateCount;

    private final int overallGeneratedCandidateCount;
    private final int overallEvaluatedCandidateCount;
    private final int overallAcceptedCandidateCount;

    private final boolean improvedByNeighborhoodRefinement;
    private final String bestCandidateSource;

    private final int candidateId;
    private final int changedFeatureCount;
    private final String changedFeatures;

    private final double proximityScore;
    private final double sparsityScore;
    private final double effortScore;
    private final double networkPenalty;
    private final double totalScore;

    private final boolean candidateAccepted;
    private final String resultReason;

    private CounterfactualResultRecord(
            Builder builder) {

        this.taskId =
                builder.taskId;

        this.schedulerPolicy =
                safeString(
                        builder.schedulerPolicy);

        this.searchStatus =
                safeString(
                        builder.searchStatus);

        this.searchSuccessful =
                builder.searchSuccessful;

        this.adaptiveGeneratedCandidateCount =
                builder.adaptiveGeneratedCandidateCount;

        this.adaptiveEvaluatedCandidateCount =
                builder.adaptiveEvaluatedCandidateCount;

        this.adaptiveAcceptedCandidateCount =
                builder.adaptiveAcceptedCandidateCount;

        this.neighborhoodGeneratedCandidateCount =
                builder.neighborhoodGeneratedCandidateCount;

        this.neighborhoodEvaluatedCandidateCount =
                builder.neighborhoodEvaluatedCandidateCount;

        this.neighborhoodAcceptedCandidateCount =
                builder.neighborhoodAcceptedCandidateCount;

        this.overallGeneratedCandidateCount =
                builder.overallGeneratedCandidateCount;

        this.overallEvaluatedCandidateCount =
                builder.overallEvaluatedCandidateCount;

        this.overallAcceptedCandidateCount =
                builder.overallAcceptedCandidateCount;

        this.improvedByNeighborhoodRefinement =
                builder.improvedByNeighborhoodRefinement;

        this.bestCandidateSource =
                safeString(
                        builder.bestCandidateSource);

        this.candidateId =
                builder.candidateId;

        this.changedFeatureCount =
                builder.changedFeatureCount;

        this.changedFeatures =
                safeString(
                        builder.changedFeatures);

        this.proximityScore =
                builder.proximityScore;

        this.sparsityScore =
                builder.sparsityScore;

        this.effortScore =
                builder.effortScore;

        this.networkPenalty =
                builder.networkPenalty;

        this.totalScore =
                builder.totalScore;

        this.candidateAccepted =
                builder.candidateAccepted;

        this.resultReason =
                safeString(
                        builder.resultReason);
    }

    public static Builder builder(
            int taskId) {

        return new Builder(
                taskId);
    }

    public String toCsvRow() {

        StringBuilder row =
                new StringBuilder();

        appendValue(row, Integer.toString(taskId));
        appendValue(row, schedulerPolicy);
        appendValue(row, searchStatus);
        appendValue(row, Boolean.toString(searchSuccessful));

        appendValue(
                row,
                Integer.toString(
                        adaptiveGeneratedCandidateCount));

        appendValue(
                row,
                Integer.toString(
                        adaptiveEvaluatedCandidateCount));

        appendValue(
                row,
                Integer.toString(
                        adaptiveAcceptedCandidateCount));

        appendValue(
                row,
                Integer.toString(
                        neighborhoodGeneratedCandidateCount));

        appendValue(
                row,
                Integer.toString(
                        neighborhoodEvaluatedCandidateCount));

        appendValue(
                row,
                Integer.toString(
                        neighborhoodAcceptedCandidateCount));

        appendValue(
                row,
                Integer.toString(
                        overallGeneratedCandidateCount));

        appendValue(
                row,
                Integer.toString(
                        overallEvaluatedCandidateCount));

        appendValue(
                row,
                Integer.toString(
                        overallAcceptedCandidateCount));

        appendValue(
                row,
                Boolean.toString(
                        improvedByNeighborhoodRefinement));

        appendValue(
                row,
                bestCandidateSource);

        appendValue(
                row,
                Integer.toString(
                        candidateId));

        appendValue(
                row,
                Integer.toString(
                        changedFeatureCount));

        appendValue(
                row,
                changedFeatures);

        appendValue(
                row,
                Double.toString(
                        proximityScore));

        appendValue(
                row,
                Double.toString(
                        sparsityScore));

        appendValue(
                row,
                Double.toString(
                        effortScore));

        appendValue(
                row,
                Double.toString(
                        networkPenalty));

        appendValue(
                row,
                Double.toString(
                        totalScore));

        appendValue(
                row,
                Boolean.toString(
                        candidateAccepted));

        appendValue(
                row,
                resultReason);

        return row.toString();
    }

    private static void appendValue(
            StringBuilder row,
            String value) {

        if(row.length() > 0) {

            row.append(',');
        }

        row.append(
                escapeCsv(
                        value));
    }

    private static String escapeCsv(
            String value) {

        String safeValue =
                safeString(
                        value);

        boolean requiresQuotes =
                safeValue.contains(",")
                || safeValue.contains("\"")
                || safeValue.contains("\n")
                || safeValue.contains("\r");

        if(!requiresQuotes) {

            return safeValue;
        }

        return "\""
                + safeValue.replace(
                        "\"",
                        "\"\"")
                + "\"";
    }

    private static String safeString(
            String value) {

        return value == null
                ? ""
                : value;
    }

    public static final class Builder {

        private final int taskId;

        private String schedulerPolicy =
                "";

        private String searchStatus =
                "";

        private boolean searchSuccessful;

        private int adaptiveGeneratedCandidateCount;
        private int adaptiveEvaluatedCandidateCount;
        private int adaptiveAcceptedCandidateCount;

        private int neighborhoodGeneratedCandidateCount;
        private int neighborhoodEvaluatedCandidateCount;
        private int neighborhoodAcceptedCandidateCount;

        private int overallGeneratedCandidateCount;
        private int overallEvaluatedCandidateCount;
        private int overallAcceptedCandidateCount;

        private boolean improvedByNeighborhoodRefinement;

        private String bestCandidateSource =
                "NONE";

        private int candidateId =
                -1;

        private int changedFeatureCount;

        private String changedFeatures =
                "";

        private double proximityScore =
                Double.NaN;

        private double sparsityScore =
                Double.NaN;

        private double effortScore =
                Double.NaN;

        private double networkPenalty =
                Double.NaN;

        private double totalScore =
                Double.NaN;

        private boolean candidateAccepted;

        private String resultReason =
                "";

        private Builder(
                int taskId) {

            this.taskId =
                    taskId;
        }

        public Builder withSchedulerPolicy(
                String schedulerPolicy) {

            this.schedulerPolicy =
                    schedulerPolicy;

            return this;
        }

        public Builder withSearchStatus(
                String searchStatus) {

            this.searchStatus =
                    searchStatus;

            return this;
        }

        public Builder withSearchSuccessful(
                boolean searchSuccessful) {

            this.searchSuccessful =
                    searchSuccessful;

            return this;
        }

        public Builder withAdaptiveGeneratedCandidateCount(
                int count) {

            this.adaptiveGeneratedCandidateCount =
                    count;

            return this;
        }

        public Builder withAdaptiveEvaluatedCandidateCount(
                int count) {

            this.adaptiveEvaluatedCandidateCount =
                    count;

            return this;
        }

        public Builder withAdaptiveAcceptedCandidateCount(
                int count) {

            this.adaptiveAcceptedCandidateCount =
                    count;

            return this;
        }

        public Builder withNeighborhoodGeneratedCandidateCount(
                int count) {

            this.neighborhoodGeneratedCandidateCount =
                    count;

            return this;
        }

        public Builder withNeighborhoodEvaluatedCandidateCount(
                int count) {

            this.neighborhoodEvaluatedCandidateCount =
                    count;

            return this;
        }

        public Builder withNeighborhoodAcceptedCandidateCount(
                int count) {

            this.neighborhoodAcceptedCandidateCount =
                    count;

            return this;
        }

        public Builder withOverallGeneratedCandidateCount(
                int count) {

            this.overallGeneratedCandidateCount =
                    count;

            return this;
        }

        public Builder withOverallEvaluatedCandidateCount(
                int count) {

            this.overallEvaluatedCandidateCount =
                    count;

            return this;
        }

        public Builder withOverallAcceptedCandidateCount(
                int count) {

            this.overallAcceptedCandidateCount =
                    count;

            return this;
        }

        public Builder withImprovedByNeighborhoodRefinement(
                boolean improved) {

            this.improvedByNeighborhoodRefinement =
                    improved;

            return this;
        }

        public Builder withBestCandidateSource(
                String bestCandidateSource) {

            this.bestCandidateSource =
                    bestCandidateSource;

            return this;
        }

        public Builder withCandidateId(
                int candidateId) {

            this.candidateId =
                    candidateId;

            return this;
        }

        public Builder withChangedFeatureCount(
                int changedFeatureCount) {

            this.changedFeatureCount =
                    changedFeatureCount;

            return this;
        }

        public Builder withChangedFeatures(
                String changedFeatures) {

            this.changedFeatures =
                    changedFeatures;

            return this;
        }

        public Builder withProximityScore(
                double proximityScore) {

            this.proximityScore =
                    proximityScore;

            return this;
        }

        public Builder withSparsityScore(
                double sparsityScore) {

            this.sparsityScore =
                    sparsityScore;

            return this;
        }

        public Builder withEffortScore(
                double effortScore) {

            this.effortScore =
                    effortScore;

            return this;
        }

        public Builder withNetworkPenalty(
                double networkPenalty) {

            this.networkPenalty =
                    networkPenalty;

            return this;
        }

        public Builder withTotalScore(
                double totalScore) {

            this.totalScore =
                    totalScore;

            return this;
        }

        public Builder withCandidateAccepted(
                boolean candidateAccepted) {

            this.candidateAccepted =
                    candidateAccepted;

            return this;
        }

        public Builder withResultReason(
                String resultReason) {

            this.resultReason =
                    resultReason;

            return this;
        }

        public CounterfactualResultRecord build() {

            return new CounterfactualResultRecord(
                    this);
        }
    }
}
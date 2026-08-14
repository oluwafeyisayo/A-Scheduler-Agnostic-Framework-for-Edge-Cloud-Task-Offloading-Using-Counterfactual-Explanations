package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Immutable summary of one RG-FCS batch experiment.
 *
 * The summary preserves:
 *
 * 1. Adaptive-stage search cost
 * 2. Neighborhood-refinement cost
 * 3. Complete RG-FCS cost
 * 4. Final-best-candidate source
 * 5. Neighborhood-improvement frequency
 */
public final class CounterfactualExperimentSummary {

    private final int submittedDecisionCount;
    private final int processedDecisionCount;
    private final int successfulSearchCount;
    private final int unsuccessfulSearchCount;
    private final int failedExecutionCount;

    private final long totalAdaptiveGeneratedCandidateCount;
    private final long totalAdaptiveEvaluatedCandidateCount;
    private final long totalAdaptiveAcceptedCandidateCount;

    private final long totalNeighborhoodGeneratedCandidateCount;
    private final long totalNeighborhoodEvaluatedCandidateCount;
    private final long totalNeighborhoodAcceptedCandidateCount;

    private final long totalOverallGeneratedCandidateCount;
    private final long totalOverallEvaluatedCandidateCount;
    private final long totalOverallAcceptedCandidateCount;

    private final int adaptiveBestCandidateCount;
    private final int neighborhoodBestCandidateCount;
    private final int noBestCandidateCount;

    private final int neighborhoodImprovedSearchCount;

    private final long experimentDurationMilliseconds;

    public CounterfactualExperimentSummary(
            int submittedDecisionCount,
            int processedDecisionCount,
            int successfulSearchCount,
            int unsuccessfulSearchCount,
            int failedExecutionCount,

            long totalAdaptiveGeneratedCandidateCount,
            long totalAdaptiveEvaluatedCandidateCount,
            long totalAdaptiveAcceptedCandidateCount,

            long totalNeighborhoodGeneratedCandidateCount,
            long totalNeighborhoodEvaluatedCandidateCount,
            long totalNeighborhoodAcceptedCandidateCount,

            long totalOverallGeneratedCandidateCount,
            long totalOverallEvaluatedCandidateCount,
            long totalOverallAcceptedCandidateCount,

            int adaptiveBestCandidateCount,
            int neighborhoodBestCandidateCount,
            int noBestCandidateCount,
            int neighborhoodImprovedSearchCount,

            long experimentDurationMilliseconds) {

        this.submittedDecisionCount =
                submittedDecisionCount;

        this.processedDecisionCount =
                processedDecisionCount;

        this.successfulSearchCount =
                successfulSearchCount;

        this.unsuccessfulSearchCount =
                unsuccessfulSearchCount;

        this.failedExecutionCount =
                failedExecutionCount;

        this.totalAdaptiveGeneratedCandidateCount =
                totalAdaptiveGeneratedCandidateCount;

        this.totalAdaptiveEvaluatedCandidateCount =
                totalAdaptiveEvaluatedCandidateCount;

        this.totalAdaptiveAcceptedCandidateCount =
                totalAdaptiveAcceptedCandidateCount;

        this.totalNeighborhoodGeneratedCandidateCount =
                totalNeighborhoodGeneratedCandidateCount;

        this.totalNeighborhoodEvaluatedCandidateCount =
                totalNeighborhoodEvaluatedCandidateCount;

        this.totalNeighborhoodAcceptedCandidateCount =
                totalNeighborhoodAcceptedCandidateCount;

        this.totalOverallGeneratedCandidateCount =
                totalOverallGeneratedCandidateCount;

        this.totalOverallEvaluatedCandidateCount =
                totalOverallEvaluatedCandidateCount;

        this.totalOverallAcceptedCandidateCount =
                totalOverallAcceptedCandidateCount;

        this.adaptiveBestCandidateCount =
                adaptiveBestCandidateCount;

        this.neighborhoodBestCandidateCount =
                neighborhoodBestCandidateCount;

        this.noBestCandidateCount =
                noBestCandidateCount;

        this.neighborhoodImprovedSearchCount =
                neighborhoodImprovedSearchCount;

        this.experimentDurationMilliseconds =
                experimentDurationMilliseconds;
    }

    public int getSubmittedDecisionCount() {

        return submittedDecisionCount;
    }

    public int getProcessedDecisionCount() {

        return processedDecisionCount;
    }

    public int getSuccessfulSearchCount() {

        return successfulSearchCount;
    }

    public int getUnsuccessfulSearchCount() {

        return unsuccessfulSearchCount;
    }

    public int getFailedExecutionCount() {

        return failedExecutionCount;
    }

    public long getTotalAdaptiveGeneratedCandidateCount() {

        return totalAdaptiveGeneratedCandidateCount;
    }

    public long getTotalAdaptiveEvaluatedCandidateCount() {

        return totalAdaptiveEvaluatedCandidateCount;
    }

    public long getTotalAdaptiveAcceptedCandidateCount() {

        return totalAdaptiveAcceptedCandidateCount;
    }

    public long getTotalNeighborhoodGeneratedCandidateCount() {

        return totalNeighborhoodGeneratedCandidateCount;
    }

    public long getTotalNeighborhoodEvaluatedCandidateCount() {

        return totalNeighborhoodEvaluatedCandidateCount;
    }

    public long getTotalNeighborhoodAcceptedCandidateCount() {

        return totalNeighborhoodAcceptedCandidateCount;
    }

    public long getTotalOverallGeneratedCandidateCount() {

        return totalOverallGeneratedCandidateCount;
    }

    public long getTotalOverallEvaluatedCandidateCount() {

        return totalOverallEvaluatedCandidateCount;
    }

    public long getTotalOverallAcceptedCandidateCount() {

        return totalOverallAcceptedCandidateCount;
    }

    public int getAdaptiveBestCandidateCount() {

        return adaptiveBestCandidateCount;
    }

    public int getNeighborhoodBestCandidateCount() {

        return neighborhoodBestCandidateCount;
    }

    public int getNoBestCandidateCount() {

        return noBestCandidateCount;
    }

    public int getNeighborhoodImprovedSearchCount() {

        return neighborhoodImprovedSearchCount;
    }

    public long getExperimentDurationMilliseconds() {

        return experimentDurationMilliseconds;
    }

    public double getSearchSuccessRate() {

        if(processedDecisionCount == 0) {

            return 0.0;
        }

        return (double) successfulSearchCount
                / processedDecisionCount;
    }

    public double getNeighborhoodImprovementRate() {

        if(successfulSearchCount == 0) {

            return 0.0;
        }

        return (double) neighborhoodImprovedSearchCount
                / successfulSearchCount;
    }

    public double getAdaptiveBestCandidateRate() {

        if(successfulSearchCount == 0) {

            return 0.0;
        }

        return (double) adaptiveBestCandidateCount
                / successfulSearchCount;
    }

    public double getNeighborhoodBestCandidateRate() {

        if(successfulSearchCount == 0) {

            return 0.0;
        }

        return (double) neighborhoodBestCandidateCount
                / successfulSearchCount;
    }

    public double getAverageAdaptiveGeneratedCandidates() {

        return average(
                totalAdaptiveGeneratedCandidateCount);
    }

    public double getAverageAdaptiveEvaluatedCandidates() {

        return average(
                totalAdaptiveEvaluatedCandidateCount);
    }

    public double getAverageAdaptiveAcceptedCandidates() {

        return average(
                totalAdaptiveAcceptedCandidateCount);
    }

    public double getAverageNeighborhoodGeneratedCandidates() {

        return average(
                totalNeighborhoodGeneratedCandidateCount);
    }

    public double getAverageNeighborhoodEvaluatedCandidates() {

        return average(
                totalNeighborhoodEvaluatedCandidateCount);
    }

    public double getAverageNeighborhoodAcceptedCandidates() {

        return average(
                totalNeighborhoodAcceptedCandidateCount);
    }

    public double getAverageOverallGeneratedCandidates() {

        return average(
                totalOverallGeneratedCandidateCount);
    }

    public double getAverageOverallEvaluatedCandidates() {

        return average(
                totalOverallEvaluatedCandidateCount);
    }

    public double getAverageOverallAcceptedCandidates() {

        return average(
                totalOverallAcceptedCandidateCount);
    }

    /*
     * Backward-compatible getters.
     *
     * These now represent the complete RG-FCS search rather than only
     * the adaptive stage.
     */
    public long getTotalGeneratedCandidateCount() {

        return totalOverallGeneratedCandidateCount;
    }

    public long getTotalEvaluatedCandidateCount() {

        return totalOverallEvaluatedCandidateCount;
    }

    public long getTotalAcceptedCandidateCount() {

        return totalOverallAcceptedCandidateCount;
    }

    public double getAverageGeneratedCandidates() {

        return getAverageOverallGeneratedCandidates();
    }

    public double getAverageEvaluatedCandidates() {

        return getAverageOverallEvaluatedCandidates();
    }

    private double average(
            long total) {

        if(processedDecisionCount == 0) {

            return 0.0;
        }

        return (double) total
                / processedDecisionCount;
    }

    @Override
    public String toString() {

        return "CounterfactualExperimentSummary{"
                + "submittedDecisionCount="
                + submittedDecisionCount
                + ", processedDecisionCount="
                + processedDecisionCount
                + ", successfulSearchCount="
                + successfulSearchCount
                + ", unsuccessfulSearchCount="
                + unsuccessfulSearchCount
                + ", failedExecutionCount="
                + failedExecutionCount
                + ", searchSuccessRate="
                + getSearchSuccessRate()

                + ", totalAdaptiveGeneratedCandidateCount="
                + totalAdaptiveGeneratedCandidateCount
                + ", totalAdaptiveEvaluatedCandidateCount="
                + totalAdaptiveEvaluatedCandidateCount
                + ", totalAdaptiveAcceptedCandidateCount="
                + totalAdaptiveAcceptedCandidateCount

                + ", totalNeighborhoodGeneratedCandidateCount="
                + totalNeighborhoodGeneratedCandidateCount
                + ", totalNeighborhoodEvaluatedCandidateCount="
                + totalNeighborhoodEvaluatedCandidateCount
                + ", totalNeighborhoodAcceptedCandidateCount="
                + totalNeighborhoodAcceptedCandidateCount

                + ", totalOverallGeneratedCandidateCount="
                + totalOverallGeneratedCandidateCount
                + ", totalOverallEvaluatedCandidateCount="
                + totalOverallEvaluatedCandidateCount
                + ", totalOverallAcceptedCandidateCount="
                + totalOverallAcceptedCandidateCount

                + ", averageAdaptiveGeneratedCandidates="
                + getAverageAdaptiveGeneratedCandidates()
                + ", averageAdaptiveEvaluatedCandidates="
                + getAverageAdaptiveEvaluatedCandidates()
                + ", averageAdaptiveAcceptedCandidates="
                + getAverageAdaptiveAcceptedCandidates()

                + ", averageNeighborhoodGeneratedCandidates="
                + getAverageNeighborhoodGeneratedCandidates()
                + ", averageNeighborhoodEvaluatedCandidates="
                + getAverageNeighborhoodEvaluatedCandidates()
                + ", averageNeighborhoodAcceptedCandidates="
                + getAverageNeighborhoodAcceptedCandidates()

                + ", averageOverallGeneratedCandidates="
                + getAverageOverallGeneratedCandidates()
                + ", averageOverallEvaluatedCandidates="
                + getAverageOverallEvaluatedCandidates()
                + ", averageOverallAcceptedCandidates="
                + getAverageOverallAcceptedCandidates()

                + ", adaptiveBestCandidateCount="
                + adaptiveBestCandidateCount
                + ", neighborhoodBestCandidateCount="
                + neighborhoodBestCandidateCount
                + ", noBestCandidateCount="
                + noBestCandidateCount
                + ", neighborhoodImprovedSearchCount="
                + neighborhoodImprovedSearchCount
                + ", neighborhoodImprovementRate="
                + getNeighborhoodImprovementRate()
                + ", adaptiveBestCandidateRate="
                + getAdaptiveBestCandidateRate()
                + ", neighborhoodBestCandidateRate="
                + getNeighborhoodBestCandidateRate()

                + ", experimentDurationMilliseconds="
                + experimentDurationMilliseconds
                + '}';
    }
}
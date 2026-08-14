package edu.boun.edgecloudsim.research.counterfactual;

import java.io.IOException;
import java.util.List;

/**
 * Runs RG-FCS over multiple DecisionState records.
 *
 * For every valid decision:
 *
 * 1. Execute RG-FCS
 * 2. Convert the result to a CSV record
 * 3. Write the record
 * 4. Update aggregate experiment metrics
 */
public final class CounterfactualExperimentRunner {

    private final CounterfactualSearchExecutor searchExecutor;
    private final CounterfactualResultLogger resultLogger;

    private final String schedulerPolicy;
    private final boolean debugEnabled;

    public CounterfactualExperimentRunner(
            CounterfactualSearchExecutor searchExecutor,
            CounterfactualResultLogger resultLogger,
            String schedulerPolicy) {

        this(
                searchExecutor,
                resultLogger,
                schedulerPolicy,
                false);
    }

    public CounterfactualExperimentRunner(
            CounterfactualSearchExecutor searchExecutor,
            CounterfactualResultLogger resultLogger,
            String schedulerPolicy,
            boolean debugEnabled) {

        if(searchExecutor == null) {

            throw new IllegalArgumentException(
                    "Search executor cannot be null.");
        }

        if(resultLogger == null) {

            throw new IllegalArgumentException(
                    "Result logger cannot be null.");
        }

        if(schedulerPolicy == null
                || schedulerPolicy.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Scheduler policy cannot be empty.");
        }

        this.searchExecutor =
                searchExecutor;

        this.resultLogger =
                resultLogger;

        this.schedulerPolicy =
                schedulerPolicy.trim();

        this.debugEnabled =
                debugEnabled;
    }

    public CounterfactualExperimentSummary run(
            List<DecisionState> decisionStates)
            throws IOException {

        if(decisionStates == null) {

            throw new IllegalArgumentException(
                    "Decision-state list cannot be null.");
        }

        long experimentStartTime =
                System.currentTimeMillis();

        int submittedDecisionCount =
                decisionStates.size();

        int processedDecisionCount =
                0;

        int successfulSearchCount =
                0;

        int unsuccessfulSearchCount =
                0;

        int failedExecutionCount =
                0;

        /*
         * Adaptive-stage totals.
         */
        long totalAdaptiveGeneratedCandidateCount =
                0;

        long totalAdaptiveEvaluatedCandidateCount =
                0;

        long totalAdaptiveAcceptedCandidateCount =
                0;

        /*
         * Neighborhood-stage totals.
         */
        long totalNeighborhoodGeneratedCandidateCount =
                0;

        long totalNeighborhoodEvaluatedCandidateCount =
                0;

        long totalNeighborhoodAcceptedCandidateCount =
                0;

        /*
         * Complete RG-FCS totals.
         */
        long totalOverallGeneratedCandidateCount =
                0;

        long totalOverallEvaluatedCandidateCount =
                0;

        long totalOverallAcceptedCandidateCount =
                0;

        /*
         * Final-best-source and refinement-effectiveness totals.
         */
        int adaptiveBestCandidateCount =
                0;

        int neighborhoodBestCandidateCount =
                0;

        int noBestCandidateCount =
                0;

        int neighborhoodImprovedSearchCount =
                0;

        for(int index = 0;
                index < decisionStates.size();
                index++) {

            DecisionState decisionState =
                    decisionStates.get(
                            index);

            if(decisionState == null) {

                failedExecutionCount++;

                printDebug(
                        "Skipped null decision at index "
                        + index
                        + ".");

                continue;
            }

            try {

                CounterfactualSearchResult searchResult =
                        searchExecutor.search(
                                decisionState);

                if(searchResult == null) {

                    failedExecutionCount++;

                    printDebug(
                            "RG-FCS returned null for decision "
                            + index
                            + ".");

                    continue;
                }

                processedDecisionCount++;

                if(searchResult.isSearchSuccessful()) {

                    successfulSearchCount++;
                }
                else {

                    unsuccessfulSearchCount++;
                }

                /*
                 * Aggregate adaptive-stage cost.
                 */
                totalAdaptiveGeneratedCandidateCount +=
                        searchResult
                        .getGeneratedCandidateCount();

                totalAdaptiveEvaluatedCandidateCount +=
                        searchResult
                        .getEvaluatedCandidateCount();

                totalAdaptiveAcceptedCandidateCount +=
                        searchResult
                        .getAcceptedCandidateCount();

                /*
                 * Aggregate neighborhood-stage cost.
                 */
                totalNeighborhoodGeneratedCandidateCount +=
                        searchResult
                        .getNeighborhoodGeneratedCandidateCount();

                totalNeighborhoodEvaluatedCandidateCount +=
                        searchResult
                        .getNeighborhoodEvaluatedCandidateCount();

                totalNeighborhoodAcceptedCandidateCount +=
                        searchResult
                        .getNeighborhoodAcceptedCandidateCount();

                /*
                 * Aggregate complete RG-FCS cost.
                 */
                totalOverallGeneratedCandidateCount +=
                        searchResult
                        .getOverallGeneratedCandidateCount();

                totalOverallEvaluatedCandidateCount +=
                        searchResult
                        .getOverallEvaluatedCandidateCount();

                totalOverallAcceptedCandidateCount +=
                        searchResult
                        .getOverallAcceptedCandidateCount();
                
                if(searchResult
                        .wasImprovedByNeighborhoodRefinement()) {

                    neighborhoodImprovedSearchCount++;
                }

                String bestCandidateSource =
                        searchResult
                        .getBestCandidateSource();

                if("NEIGHBORHOOD".equals(
                        bestCandidateSource)) {

                    neighborhoodBestCandidateCount++;
                }
                else if("ADAPTIVE".equals(
                        bestCandidateSource)) {

                    adaptiveBestCandidateCount++;
                }
                else {

                    noBestCandidateCount++;
                }

                CounterfactualResultRecord record =
                        CounterfactualResultRecordMapper.from(
                                searchResult,
                                schedulerPolicy);

                resultLogger.log(
                        record);

                printDebug(
                        "Processed decision "
                        + processedDecisionCount
                        + " of "
                        + submittedDecisionCount
                        + ": taskId="
                        + searchResult.getTaskId()
                        + ", status="
                        + searchResult.getStatus()
                        + ", bestSource="
                        + bestCandidateSource
                        + ", overallEvaluated="
                        + searchResult
                        .getOverallEvaluatedCandidateCount());

            }
//            catch(RuntimeException exception) {
//
//                failedExecutionCount++;
//
//                printDebug(
//                        "RG-FCS failed at decision index "
//                        + index
//                        + ": "
//                        + exception.getMessage());
//            }
            catch(RuntimeException exception) {

                failedExecutionCount++;

                System.err.println(
                        "[RG-FCS Batch Failure] "
                        + "policy="
                        + schedulerPolicy
                        + ", decisionIndex="
                        + index
                        + ", taskId="
                        + decisionState.getTaskId()
                        + ", exception="
                        + exception.getClass().getName()
                        + ", message="
                        + exception.getMessage());

                exception.printStackTrace();
            }
        }

        long experimentDurationMilliseconds =
                System.currentTimeMillis()
                - experimentStartTime;

        return new CounterfactualExperimentSummary(
                submittedDecisionCount,
                processedDecisionCount,
                successfulSearchCount,
                unsuccessfulSearchCount,
                failedExecutionCount,

                totalAdaptiveGeneratedCandidateCount,
                totalAdaptiveEvaluatedCandidateCount,
                totalAdaptiveAcceptedCandidateCount,

                totalNeighborhoodGeneratedCandidateCount,
                totalNeighborhoodEvaluatedCandidateCount,
                totalNeighborhoodAcceptedCandidateCount,

                totalOverallGeneratedCandidateCount,
                totalOverallEvaluatedCandidateCount,
                totalOverallAcceptedCandidateCount,

                adaptiveBestCandidateCount,
                neighborhoodBestCandidateCount,
                noBestCandidateCount,
                neighborhoodImprovedSearchCount,

                experimentDurationMilliseconds);
    }

    private void printDebug(
            String message) {

        if(debugEnabled) {

            System.out.println(
                    "[RG-FCS Batch] "
                    + message);
        }
    }
}
package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

/**
 * Converts an RG-FCS search result into a flat record suitable
 * for CSV logging.
 */
public final class CounterfactualResultRecordMapper {

    private CounterfactualResultRecordMapper() {

        /*
         * Utility class.
         */
    }

    public static CounterfactualResultRecord from(
            CounterfactualSearchResult searchResult,
            String schedulerPolicy) {

        if(searchResult == null) {

            throw new IllegalArgumentException(
                    "Counterfactual search result cannot be null.");
        }

        CounterfactualResultRecord.Builder recordBuilder =
                CounterfactualResultRecord
                .builder(
                        searchResult.getTaskId())

                .withSchedulerPolicy(
                        schedulerPolicy)

                .withSearchStatus(
                        searchResult
                        .getStatus()
                        .name())

                .withSearchSuccessful(
                        searchResult
                        .isSearchSuccessful())

                /*
                 * Adaptive-stage statistics.
                 */
                .withAdaptiveGeneratedCandidateCount(
                        searchResult
                        .getGeneratedCandidateCount())

                .withAdaptiveEvaluatedCandidateCount(
                        searchResult
                        .getEvaluatedCandidateCount())

                .withAdaptiveAcceptedCandidateCount(
                        searchResult
                        .getAcceptedCandidateCount())

                /*
                 * Neighborhood-stage statistics.
                 */
                .withNeighborhoodGeneratedCandidateCount(
                        searchResult
                        .getNeighborhoodGeneratedCandidateCount())

                .withNeighborhoodEvaluatedCandidateCount(
                        searchResult
                        .getNeighborhoodEvaluatedCandidateCount())

                .withNeighborhoodAcceptedCandidateCount(
                        searchResult
                        .getNeighborhoodAcceptedCandidateCount())

                /*
                 * Complete RG-FCS statistics.
                 */
                .withOverallGeneratedCandidateCount(
                        searchResult
                        .getOverallGeneratedCandidateCount())

                .withOverallEvaluatedCandidateCount(
                        searchResult
                        .getOverallEvaluatedCandidateCount())

                .withOverallAcceptedCandidateCount(
                        searchResult
                        .getOverallAcceptedCandidateCount())

                .withImprovedByNeighborhoodRefinement(
                        searchResult
                        .wasImprovedByNeighborhoodRefinement())

                .withBestCandidateSource(
                        searchResult
                        .getBestCandidateSource())

                .withResultReason(
                        searchResult
                        .getReason());

        CounterfactualCandidate bestCandidate =
                searchResult.getBestCandidate();

        if(bestCandidate == null) {

            return recordBuilder.build();
        }

        recordBuilder
                .withCandidateId(
                        bestCandidate
                        .getCandidateId())

                .withChangedFeatureCount(
                        bestCandidate
                        .getChangedFeatureCount())

                .withChangedFeatures(
                        formatFeatureChanges(
                                bestCandidate
                                .getFeatureChanges()))

                .withCandidateAccepted(
                        bestCandidate
                        .isAccepted());

        CounterfactualScore score =
                bestCandidate.getScore();

        if(score != null) {

            recordBuilder
                    .withProximityScore(
                            score.getProximity())

                    .withSparsityScore(
                            score.getSparsity())

                    .withEffortScore(
                            score.getEffort())

                    .withNetworkPenalty(
                            score.getNetworkPenalty())

                    .withTotalScore(
                            score.getTotalScore());
        }

        return recordBuilder.build();
    }

    private static String formatFeatureChanges(
            List<FeatureChange> featureChanges) {

        if(featureChanges == null
                || featureChanges.isEmpty()) {

            return "";
        }

        StringBuilder text =
                new StringBuilder();

        for(FeatureChange change
                : featureChanges) {

            if(change == null) {

                continue;
            }

            if(text.length() > 0) {

                text.append(';');
            }

            text.append(
                    change.getFeatureName());

            text.append(':');

            text.append(
                    change.getOriginalValue());

            text.append("->");

            text.append(
                    change.getCounterfactualValue());
        }

        return text.toString();
    }
}
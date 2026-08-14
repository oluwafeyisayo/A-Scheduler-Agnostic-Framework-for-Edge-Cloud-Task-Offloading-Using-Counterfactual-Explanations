package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.boun.edgecloudsim.research.counterfactual.verification
        .CounterfactualVerificationResult;

/**
 * Represents the outcome of a feasible counterfactual search.
 *
 * A successful search contains the best accepted counterfactual candidate
 * found according to the configured search objective.
 *
 * The result also records the classification distribution of every
 * evaluated candidate.
 */
public final class CounterfactualSearchResult {

    public enum SearchStatus {

        FOUND,
        NOT_FOUND,
        INVALID_REQUEST
    }

    private final int taskId;

    private final SearchStatus status;

    private final CounterfactualCandidate bestCandidate;

    private final CounterfactualVerificationResult
            verificationResult;

    private final int generatedCandidateCount;

    private final int evaluatedCandidateCount;

    private final int acceptedCandidateCount;
    
    private final int neighborhoodGeneratedCandidateCount;

    private final int neighborhoodEvaluatedCandidateCount;

    private final int neighborhoodAcceptedCandidateCount;

    private final boolean improvedByNeighborhoodRefinement;

    private final String bestCandidateSource;

    private final List<CounterfactualCandidate>
            acceptedCandidates;

    private final Map<CounterfactualClassification, Integer>
            classificationCounts;
    
    private final Map<Integer, Integer>
	generatedCountsByChangedFeatureCount;

	private final Map<Integer, Integer>
		evaluatedCountsByChangedFeatureCount;
	
	private final Map<Integer, Integer>
		acceptedCountsByChangedFeatureCount;

    private final String reason;

    private CounterfactualSearchResult(
            Builder builder) {

        this.taskId =
                builder.taskId;

        this.status =
                builder.status;

        this.bestCandidate =
                builder.bestCandidate;

        this.verificationResult =
                builder.verificationResult;

        this.generatedCandidateCount =
                builder.generatedCandidateCount;

        this.evaluatedCandidateCount =
                builder.evaluatedCandidateCount;

        this.acceptedCandidateCount =
                builder.acceptedCandidates.size();

        this.acceptedCandidates =
                Collections.unmodifiableList(
                        new ArrayList<CounterfactualCandidate>(
                                builder.acceptedCandidates));

        this.classificationCounts =
                Collections.unmodifiableMap(
                        new EnumMap<CounterfactualClassification, Integer>(
                                builder.classificationCounts));
        this.generatedCountsByChangedFeatureCount =
                immutableIntegerCountMap(
                        builder.generatedCountsByChangedFeatureCount);

        this.evaluatedCountsByChangedFeatureCount =
                immutableIntegerCountMap(
                        builder.evaluatedCountsByChangedFeatureCount);

        this.acceptedCountsByChangedFeatureCount =
                immutableIntegerCountMap(
                        builder.acceptedCountsByChangedFeatureCount);
        
        this.neighborhoodGeneratedCandidateCount =
                builder.neighborhoodGeneratedCandidateCount;

        this.neighborhoodEvaluatedCandidateCount =
                builder.neighborhoodEvaluatedCandidateCount;

        this.neighborhoodAcceptedCandidateCount =
                builder.neighborhoodAcceptedCandidateCount;

        this.improvedByNeighborhoodRefinement =
                builder.improvedByNeighborhoodRefinement;

        this.bestCandidateSource =
                safeBestCandidateSource(
                        builder.bestCandidateSource);

        this.reason =
                safeText(
                        builder.reason);
       
        }
    
    private static String safeBestCandidateSource(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return "NONE";
        }

        return value.trim()
                .toUpperCase();
    }
    
    private static Map<Integer, Integer> immutableIntegerCountMap(
            Map<Integer, Integer> source) {

        return Collections.unmodifiableMap(
                new TreeMap<Integer, Integer>(
                        source));
    }

    private static String safeText(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return "No search explanation was provided.";
        }

        return value.trim();
    }

    public int getTaskId() {

        return taskId;
    }

    public SearchStatus getStatus() {

        return status;
    }

    public CounterfactualCandidate getBestCandidate() {

        return bestCandidate;
    }

    /**
     * Compatibility getter for the automated RG-FCS search.
     *
     * The accepted candidate and best candidate refer to the same
     * lowest-scoring accepted counterfactual.
     */
    public CounterfactualCandidate getAcceptedCandidate() {

        return bestCandidate;
    }

    public boolean hasAcceptedCandidate() {

        return bestCandidate != null
                && bestCandidate.isAccepted();
    }

    public CounterfactualVerificationResult
            getVerificationResult() {

        return verificationResult;
    }

    public int getGeneratedCandidateCount() {

        return generatedCandidateCount;
    }

    public int getEvaluatedCandidateCount() {

        return evaluatedCandidateCount;
    }

    public int getAcceptedCandidateCount() {

        return acceptedCandidateCount;
    }
    
    public int getNeighborhoodGeneratedCandidateCount() {

        return neighborhoodGeneratedCandidateCount;
    }

    public int getNeighborhoodEvaluatedCandidateCount() {

        return neighborhoodEvaluatedCandidateCount;
    }

    public int getNeighborhoodAcceptedCandidateCount() {

        return neighborhoodAcceptedCandidateCount;
    }

    public int getOverallGeneratedCandidateCount() {

        return generatedCandidateCount
                + neighborhoodGeneratedCandidateCount;
    }

    public int getOverallEvaluatedCandidateCount() {

        return evaluatedCandidateCount
                + neighborhoodEvaluatedCandidateCount;
    }

    public int getOverallAcceptedCandidateCount() {

        return acceptedCandidateCount
                + neighborhoodAcceptedCandidateCount;
    }

    public boolean wasImprovedByNeighborhoodRefinement() {

        return improvedByNeighborhoodRefinement;
    }

    public String getBestCandidateSource() {

        return bestCandidateSource;
    }

    public List<CounterfactualCandidate>
            getAcceptedCandidates() {

        return acceptedCandidates;
    }
    
    public Map<Integer, Integer>
    getGeneratedCountsByChangedFeatureCount() {
    	return generatedCountsByChangedFeatureCount;
	}
	
	public Map<Integer, Integer>
	    getEvaluatedCountsByChangedFeatureCount() {
	
	return evaluatedCountsByChangedFeatureCount;
	}
	
	public Map<Integer, Integer>
	    getAcceptedCountsByChangedFeatureCount() {
	
	return acceptedCountsByChangedFeatureCount;
	}
	
	public int getGeneratedCandidateCountForCardinality(
	    int changedFeatureCount) {
	
	return getCardinalityCount(
	        generatedCountsByChangedFeatureCount,
	        changedFeatureCount);
	}
	
	public int getEvaluatedCandidateCountForCardinality(
	    int changedFeatureCount) {
	
	return getCardinalityCount(
	        evaluatedCountsByChangedFeatureCount,
	        changedFeatureCount);
	}
	
	public int getAcceptedCandidateCountForCardinality(
	    int changedFeatureCount) {
	
	return getCardinalityCount(
	        acceptedCountsByChangedFeatureCount,
	        changedFeatureCount);
	}
	
	public int getGeneratedMultiFeatureCandidateCount() {
	
	return sumMultiFeatureCounts(
	        generatedCountsByChangedFeatureCount);
	}
	
	public int getEvaluatedMultiFeatureCandidateCount() {
	
	return sumMultiFeatureCounts(
	        evaluatedCountsByChangedFeatureCount);
	}
	
	public int getAcceptedMultiFeatureCandidateCount() {
	
	return sumMultiFeatureCounts(
	        acceptedCountsByChangedFeatureCount);
	}
	
	public int getBestCandidateChangedFeatureCount() {
	
	return bestCandidate == null
	        ? 0
	        : bestCandidate.getChangedFeatureCount();
	}
	
	private static int getCardinalityCount(
	    Map<Integer, Integer> counts,
	    int changedFeatureCount) {
	
	Integer count =
	        counts.get(
	                Integer.valueOf(
	                        changedFeatureCount));
	
	return count == null
	        ? 0
	        : count.intValue();
	}
	
	private static int sumMultiFeatureCounts(
	    Map<Integer, Integer> counts) {
	
	int total =
	        0;
	
	for(Map.Entry<Integer, Integer> entry
	        : counts.entrySet()) {
	
	    Integer cardinality =
	            entry.getKey();
	
	    Integer count =
	            entry.getValue();
	
	    if(cardinality != null
	            && cardinality.intValue() > 1
	            && count != null) {
	
	        total +=
	                count.intValue();
	    }
	}
	
	return total;
	}

    /**
     * Returns the number of evaluated candidates assigned to the
     * specified classification.
     */
    public int getClassificationCount(
            CounterfactualClassification classification) {

        if(classification == null) {

            return 0;
        }

        Integer count =
                classificationCounts.get(
                        classification);

        return count == null
                ? 0
                : count.intValue();
    }
    

    public int getFeasibleEffectiveCount() {

        return getClassificationCount(
                CounterfactualClassification
                .FEASIBLE_EFFECTIVE);
    }

    public int getFeasibleIneffectiveCount() {

        return getClassificationCount(
                CounterfactualClassification
                .FEASIBLE_INEFFECTIVE);
    }

    public int getInfeasibleEffectiveCount() {

        return getClassificationCount(
                CounterfactualClassification
                .INFEASIBLE_EFFECTIVE);
    }

    public int getInfeasibleIneffectiveCount() {

        return getClassificationCount(
                CounterfactualClassification
                .INFEASIBLE_INEFFECTIVE);
    }

    public int getInvalidCount() {

        return getClassificationCount(
                CounterfactualClassification
                .INVALID);
    }

    /**
     * Returns an immutable map containing all classification counts.
     */
    public Map<CounterfactualClassification, Integer>
            getClassificationCounts() {

        return classificationCounts;
    }

    /**
     * Returns the total number of classified candidates.
     */
    public int getTotalClassifiedCandidateCount() {

        int total =
                0;

        for(Integer count
                : classificationCounts.values()) {

            if(count != null) {

                total +=
                        count.intValue();
            }
        }

        return total;
    }

    public String getReason() {

        return reason;
    }

    public boolean isFound() {

        return status == SearchStatus.FOUND
                && bestCandidate != null
                && bestCandidate.isAccepted();
    }

    public boolean isSearchSuccessful() {

        return isFound();
    }

    @Override
    public String toString() {

        return "CounterfactualSearchResult{"
                + "taskId="
                + taskId
                + ", status="
                + status
                + ", generatedCandidateCount="
                + generatedCandidateCount
                + ", evaluatedCandidateCount="
                + evaluatedCandidateCount
                + ", acceptedCandidateCount="
                + acceptedCandidateCount
                + ", neighborhoodGeneratedCandidateCount="
                + neighborhoodGeneratedCandidateCount
                + ", neighborhoodEvaluatedCandidateCount="
                + neighborhoodEvaluatedCandidateCount
                + ", neighborhoodAcceptedCandidateCount="
                + neighborhoodAcceptedCandidateCount
                + ", overallGeneratedCandidateCount="
                + getOverallGeneratedCandidateCount()
                + ", overallEvaluatedCandidateCount="
                + getOverallEvaluatedCandidateCount()
                + ", overallAcceptedCandidateCount="
                + getOverallAcceptedCandidateCount()
                + ", improvedByNeighborhoodRefinement="
                + improvedByNeighborhoodRefinement
                + ", bestCandidateSource="
                + bestCandidateSource
                + ", feasibleEffectiveCount="
                + getFeasibleEffectiveCount()
                + ", feasibleIneffectiveCount="
                + getFeasibleIneffectiveCount()
                + ", infeasibleEffectiveCount="
                + getInfeasibleEffectiveCount()
                + ", infeasibleIneffectiveCount="
                + getInfeasibleIneffectiveCount()
                + ", invalidCount="
                + getInvalidCount()
                + ", generatedCardinality="
                + generatedCountsByChangedFeatureCount
                + ", evaluatedCardinality="
                + evaluatedCountsByChangedFeatureCount
                + ", acceptedCardinality="
                + acceptedCountsByChangedFeatureCount
                + ", generatedMultiFeatureCount="
                + getGeneratedMultiFeatureCandidateCount()
                + ", evaluatedMultiFeatureCount="
                + getEvaluatedMultiFeatureCandidateCount()
                + ", acceptedMultiFeatureCount="
                + getAcceptedMultiFeatureCandidateCount()
                + ", bestChangedFeatureCount="
                + getBestCandidateChangedFeatureCount()
                + ", found="
                + isFound()
                + ", bestCandidate="
                + bestCandidate
                + ", reason='"
                + reason
                + '\''
                + '}';
    }

    public static final class Builder {

        private final int taskId;

        private SearchStatus status =
                SearchStatus.NOT_FOUND;

        private CounterfactualCandidate bestCandidate;

        private CounterfactualVerificationResult
                verificationResult;

        private int generatedCandidateCount;

        private int evaluatedCandidateCount;
        
        private int neighborhoodGeneratedCandidateCount;

        private int neighborhoodEvaluatedCandidateCount;

        private int neighborhoodAcceptedCandidateCount;

        private boolean improvedByNeighborhoodRefinement;

        private String bestCandidateSource =
                "NONE";

        private final List<CounterfactualCandidate>
                acceptedCandidates =
                new ArrayList<CounterfactualCandidate>();

        private final EnumMap<CounterfactualClassification, Integer>
                classificationCounts =
                new EnumMap<CounterfactualClassification, Integer>(
                        CounterfactualClassification.class);
        private final Map<Integer, Integer>
        generatedCountsByChangedFeatureCount =
        new TreeMap<Integer, Integer>();

		private final Map<Integer, Integer>
		        evaluatedCountsByChangedFeatureCount =
		        new TreeMap<Integer, Integer>();
		
		private final Map<Integer, Integer>
		        acceptedCountsByChangedFeatureCount =
        new TreeMap<Integer, Integer>();
		

        private String reason;

        public Builder(
                int taskId) {

            if(taskId < 0) {

                throw new IllegalArgumentException(
                        "Task ID cannot be negative.");
            }

            this.taskId =
                    taskId;

            initializeClassificationCounts();
        }
        
        public Builder incrementGeneratedCardinalityCount(
                int changedFeatureCount) {

            incrementCardinalityCount(
                    generatedCountsByChangedFeatureCount,
                    changedFeatureCount);

            return this;
        }

        public Builder incrementEvaluatedCardinalityCount(
                int changedFeatureCount) {

            incrementCardinalityCount(
                    evaluatedCountsByChangedFeatureCount,
                    changedFeatureCount);

            return this;
        }

        public Builder incrementAcceptedCardinalityCount(
                int changedFeatureCount) {

            incrementCardinalityCount(
                    acceptedCountsByChangedFeatureCount,
                    changedFeatureCount);

            return this;
        }

        private void incrementCardinalityCount(
                Map<Integer, Integer> counts,
                int changedFeatureCount) {

            if(changedFeatureCount <= 0) {

                throw new IllegalArgumentException(
                        "Changed-feature count must be positive.");
            }

            Integer key =
                    Integer.valueOf(
                            changedFeatureCount);

            Integer current =
                    counts.get(
                            key);

            counts.put(
                    key,
                    Integer.valueOf(
                            current == null
                                    ? 1
                                    : current.intValue() + 1));
        }
        
        private int sumBuilderCounts(
                Map<Integer, Integer> counts) {

            int total =
                    0;

            for(Integer count
                    : counts.values()) {

                if(count != null) {

                    total +=
                            count.intValue();
                }
            }

            return total;
        }

        private void initializeClassificationCounts() {

            for(CounterfactualClassification classification
                    : CounterfactualClassification.values()) {

                classificationCounts.put(
                        classification,
                        Integer.valueOf(0));
            }
        }

        public Builder withStatus(
                SearchStatus status) {

            if(status == null) {

                throw new IllegalArgumentException(
                        "Search status cannot be null.");
            }

            this.status =
                    status;

            return this;
        }

        public Builder withBestCandidate(
                CounterfactualCandidate candidate) {

            this.bestCandidate =
                    candidate;

            if(candidate != null
                    && candidate.isAccepted()
                    && !acceptedCandidates.contains(
                            candidate)) {

                acceptedCandidates.add(
                        candidate);
            }

            return this;
        }
        
        /**
         * Sets the final best candidate without adding it to the adaptive-search
         * accepted-candidate collection.
         *
         * This is used when a bounded post-search refinement stage produces a
         * better accepted candidate whose evaluation statistics are tracked
         * separately from the main search.
         */
        public Builder withRefinedBestCandidate(
                CounterfactualCandidate candidate) {

            if(candidate != null) {

                if(!candidate.isAccepted()) {

                    throw new IllegalArgumentException(
                            "The refined best candidate must be accepted.");
                }

                if(candidate.getClassification()
                        != CounterfactualClassification
                        .FEASIBLE_EFFECTIVE) {

                    throw new IllegalArgumentException(
                            "The refined best candidate must be classified "
                            + "as FEASIBLE_EFFECTIVE.");
                }
            }

            this.bestCandidate =
                    candidate;

            return this;
        }

        /**
         * Stores the best accepted candidate.
         */
        public Builder withAcceptedCandidate(
                CounterfactualCandidate candidate) {

            this.bestCandidate =
                    candidate;

            if(candidate != null
                    && candidate.isAccepted()
                    && !acceptedCandidates.contains(
                            candidate)) {

                acceptedCandidates.add(
                        candidate);
            }

            return this;
        }

        public Builder withVerificationResult(
                CounterfactualVerificationResult result) {

            this.verificationResult =
                    result;

            return this;
        }

        public Builder withGeneratedCandidateCount(
                int count) {

            if(count < 0) {

                throw new IllegalArgumentException(
                        "Generated-candidate count cannot "
                        + "be negative.");
            }

            this.generatedCandidateCount =
                    count;

            return this;
        }

        public Builder withEvaluatedCandidateCount(
                int count) {

            if(count < 0) {

                throw new IllegalArgumentException(
                        "Evaluated-candidate count cannot "
                        + "be negative.");
            }

            this.evaluatedCandidateCount =
                    count;

            return this;
        }
        
        /**
         * Records the bounded neighborhood-refinement stage separately from the
         * adaptive-search statistics.
         */
        public Builder withNeighborhoodRefinementStatistics(
                int generatedCount,
                int evaluatedCount,
                int acceptedCount,
                boolean improved,
                String candidateSource) {

            if(generatedCount < 0) {

                throw new IllegalArgumentException(
                        "Neighborhood generated count cannot be negative.");
            }

            if(evaluatedCount < 0) {

                throw new IllegalArgumentException(
                        "Neighborhood evaluated count cannot be negative.");
            }

            if(acceptedCount < 0) {

                throw new IllegalArgumentException(
                        "Neighborhood accepted count cannot be negative.");
            }

            if(evaluatedCount > generatedCount) {

                throw new IllegalArgumentException(
                        "Neighborhood evaluated count cannot exceed "
                        + "the neighborhood generated count.");
            }

            if(acceptedCount > evaluatedCount) {

                throw new IllegalArgumentException(
                        "Neighborhood accepted count cannot exceed "
                        + "the neighborhood evaluated count.");
            }

            this.neighborhoodGeneratedCandidateCount =
                    generatedCount;

            this.neighborhoodEvaluatedCandidateCount =
                    evaluatedCount;

            this.neighborhoodAcceptedCandidateCount =
                    acceptedCount;

            this.improvedByNeighborhoodRefinement =
                    improved;

            this.bestCandidateSource =
                    candidateSource == null
                            || candidateSource.trim().isEmpty()
                    ? "NONE"
                    : candidateSource.trim();

            return this;
        }

        /**
         * Increments the count for one evaluated candidate.
         */
        public Builder incrementClassificationCount(
                CounterfactualClassification classification) {

            if(classification == null) {

                throw new IllegalArgumentException(
                        "Counterfactual classification cannot be null.");
            }

            int currentCount =
                    getBuilderClassificationCount(
                            classification);

            classificationCounts.put(
                    classification,
                    Integer.valueOf(
                            currentCount + 1));

            return this;
        }

        /**
         * Increments the count using a classification result.
         */
        public Builder addClassificationResult(
                CounterfactualClassificationResult result) {

            if(result == null) {

                throw new IllegalArgumentException(
                        "Classification result cannot be null.");
            }

            return incrementClassificationCount(
                    result.getClassification());
        }

        /**
         * Allows a complete classification count to be assigned directly.
         */
        public Builder withClassificationCount(
                CounterfactualClassification classification,
                int count) {

            if(classification == null) {

                throw new IllegalArgumentException(
                        "Counterfactual classification cannot be null.");
            }

            if(count < 0) {

                throw new IllegalArgumentException(
                        "Classification count cannot be negative.");
            }

            classificationCounts.put(
                    classification,
                    Integer.valueOf(count));

            return this;
        }

        private int getBuilderClassificationCount(
                CounterfactualClassification classification) {

            Integer count =
                    classificationCounts.get(
                            classification);

            return count == null
                    ? 0
                    : count.intValue();
        }

        public Builder withSearchSuccessful(
                boolean successful) {

            if(successful) {

                this.status =
                        SearchStatus.FOUND;
            }
            else if(this.status
                    != SearchStatus.INVALID_REQUEST) {

                this.status =
                        SearchStatus.NOT_FOUND;
            }

            return this;
        }

        public Builder addAcceptedCandidate(
                CounterfactualCandidate candidate) {

            if(candidate == null) {

                throw new IllegalArgumentException(
                        "Accepted candidate cannot be null.");
            }

            if(!candidate.isAccepted()) {

                throw new IllegalArgumentException(
                        "Only accepted counterfactual "
                        + "candidates may be stored.");
            }

            if(candidate.getClassification()
                    != CounterfactualClassification
                    .FEASIBLE_EFFECTIVE) {

                throw new IllegalArgumentException(
                        "Only FEASIBLE_EFFECTIVE candidates "
                        + "may be accepted.");
            }

            if(!acceptedCandidates.contains(
                    candidate)) {

                acceptedCandidates.add(
                        candidate);
            }

            return this;
        }

        public Builder addAcceptedCandidates(
                List<CounterfactualCandidate> candidates) {

            if(candidates == null) {

                return this;
            }

            for(CounterfactualCandidate candidate
                    : candidates) {

                addAcceptedCandidate(
                        candidate);
            }

            return this;
        }

        public Builder withReason(
                String reason) {

            this.reason =
                    reason;

            return this;
        }

        public CounterfactualSearchResult build() {

            validate();

            return new CounterfactualSearchResult(
                    this);
        }

        private void validate() {

            /*
             * Basic search-count consistency.
             */
            if(evaluatedCandidateCount
                    > generatedCandidateCount) {

                throw new IllegalStateException(
                        "Evaluated-candidate count cannot exceed "
                        + "generated-candidate count.");
            }

            if(acceptedCandidates.size()
                    > evaluatedCandidateCount) {

                throw new IllegalStateException(
                        "Accepted-candidate count cannot exceed "
                        + "evaluated-candidate count.");
            }

            /*
             * Validate the classification counts and calculate their
             * total.
             */
            int totalClassifiedCandidateCount =
                    0;

            for(Map.Entry<CounterfactualClassification, Integer> entry
                    : classificationCounts.entrySet()) {

                Integer count =
                        entry.getValue();

                if(count == null
                        || count.intValue() < 0) {

                    throw new IllegalStateException(
                            "Classification counts cannot be "
                            + "null or negative.");
                }

                totalClassifiedCandidateCount +=
                        count.intValue();
            }

            /*
             * Every evaluated candidate must receive exactly one
             * classification.
             */
            if(totalClassifiedCandidateCount
                    != evaluatedCandidateCount) {

                throw new IllegalStateException(
                        "The total classification count must equal "
                        + "the evaluated-candidate count. "
                        + "Classified="
                        + totalClassifiedCandidateCount
                        + ", evaluated="
                        + evaluatedCandidateCount
                        + ".");
            }

            /*
             * Validate all candidates already stored in the accepted
             * collection.
             */
            for(CounterfactualCandidate candidate
                    : acceptedCandidates) {

                if(candidate == null
                        || !candidate.isAccepted()) {

                    throw new IllegalStateException(
                            "The accepted-candidate list contains "
                            + "an invalid or unaccepted candidate.");
                }

                if(candidate.getClassification()
                        != CounterfactualClassification
                        .FEASIBLE_EFFECTIVE) {

                    throw new IllegalStateException(
                            "Every accepted candidate must be "
                            + "classified as FEASIBLE_EFFECTIVE.");
                }
            }

            /*
             * Validate successful-search semantics and ensure that the
             * best candidate is represented in the accepted collection.
             */
            if(status == SearchStatus.FOUND) {

                if(bestCandidate == null) {

                    throw new IllegalStateException(
                            "A successful search must contain "
                            + "a best candidate.");
                }

                if(!bestCandidate.isAccepted()) {

                    throw new IllegalStateException(
                            "The best candidate must be accepted.");
                }

                if(bestCandidate.getClassification()
                        != CounterfactualClassification
                        .FEASIBLE_EFFECTIVE) {

                    throw new IllegalStateException(
                            "The best candidate must be classified "
                            + "as FEASIBLE_EFFECTIVE.");
                }
            }

            if(status == SearchStatus.NOT_FOUND
                    && bestCandidate != null
                    && bestCandidate.isAccepted()) {

                throw new IllegalStateException(
                        "A search containing an accepted best "
                        + "candidate cannot have NOT_FOUND status.");
            }
            

            /*
             * Semantic consistency rule:
             *
             * Every FEASIBLE_EFFECTIVE candidate is accepted, and every
             * accepted candidate must be FEASIBLE_EFFECTIVE.
             *
             * This check is intentionally performed after the successful
             * search block above, because that block may add the best
             * candidate to the accepted collection.
             */
            int feasibleEffectiveCount =
                    getBuilderClassificationCount(
                            CounterfactualClassification
                            .FEASIBLE_EFFECTIVE);

            if(acceptedCandidates.size()
                    != feasibleEffectiveCount) {

                throw new IllegalStateException(
                        "Accepted-candidate count must equal the "
                        + "FEASIBLE_EFFECTIVE classification count. "
                        + "Accepted="
                        + acceptedCandidates.size()
                        + ", feasibleEffective="
                        + feasibleEffectiveCount
                        + ".");
            }
            
            if(neighborhoodEvaluatedCandidateCount
                    > neighborhoodGeneratedCandidateCount) {

                throw new IllegalStateException(
                        "Neighborhood evaluated count cannot exceed "
                        + "neighborhood generated count.");
            }

            if(neighborhoodAcceptedCandidateCount
                    > neighborhoodEvaluatedCandidateCount) {

                throw new IllegalStateException(
                        "Neighborhood accepted count cannot exceed "
                        + "neighborhood evaluated count.");
            }

            if(improvedByNeighborhoodRefinement
                    && !"NEIGHBORHOOD".equals(
                            bestCandidateSource)) {

                throw new IllegalStateException(
                        "An improvement produced by neighborhood refinement "
                        + "must identify NEIGHBORHOOD as the best-candidate source.");
            }

            if(status == SearchStatus.NOT_FOUND
                    && !"NONE".equals(
                            bestCandidateSource)) {

                throw new IllegalStateException(
                        "A NOT_FOUND result must use NONE as the "
                        + "best-candidate source.");
            }

            /*
             * A search with one or more FEASIBLE_EFFECTIVE candidates
             * must be marked FOUND and must contain a best candidate.
             */
            if(feasibleEffectiveCount > 0
                    && status != SearchStatus.FOUND) {

                throw new IllegalStateException(
                        "A result containing FEASIBLE_EFFECTIVE "
                        + "candidates must have FOUND status.");
            }

            /*
             * A FOUND result must have at least one
             * FEASIBLE_EFFECTIVE candidate.
             */
            if(status == SearchStatus.FOUND
                    && feasibleEffectiveCount == 0) {

                throw new IllegalStateException(
                        "A FOUND result must contain at least one "
                        + "FEASIBLE_EFFECTIVE candidate.");
            }

            /*
             * A NOT_FOUND result cannot contain accepted candidates.
             */
            if(status == SearchStatus.NOT_FOUND
                    && !acceptedCandidates.isEmpty()) {

                throw new IllegalStateException(
                        "A NOT_FOUND result cannot contain accepted "
                        + "counterfactual candidates.");
            }
            if(status == SearchStatus.FOUND
                    && "NONE".equals(
                            bestCandidateSource)) {

                throw new IllegalStateException(
                        "A FOUND result must identify ADAPTIVE or "
                        + "NEIGHBORHOOD as the best-candidate source.");
            }

            if(status == SearchStatus.FOUND
                    && !"ADAPTIVE".equals(
                            bestCandidateSource)
                    && !"NEIGHBORHOOD".equals(
                            bestCandidateSource)) {

                throw new IllegalStateException(
                        "Unsupported best-candidate source for a "
                        + "FOUND result: "
                        + bestCandidateSource);
            }

            
            int generatedCardinalityTotal =
                    sumBuilderCounts(
                            generatedCountsByChangedFeatureCount);

            int evaluatedCardinalityTotal =
                    sumBuilderCounts(
                            evaluatedCountsByChangedFeatureCount);

            int acceptedCardinalityTotal =
                    sumBuilderCounts(
                            acceptedCountsByChangedFeatureCount);

            if(generatedCardinalityTotal
                    != generatedCandidateCount) {

                throw new IllegalStateException(
                        "Generated cardinality total must equal "
                        + "generated-candidate count. CardinalityTotal="
                        + generatedCardinalityTotal
                        + ", generated="
                        + generatedCandidateCount
                        + ".");
            }

            if(evaluatedCardinalityTotal
                    != evaluatedCandidateCount) {

                throw new IllegalStateException(
                        "Evaluated cardinality total must equal "
                        + "evaluated-candidate count. CardinalityTotal="
                        + evaluatedCardinalityTotal
                        + ", evaluated="
                        + evaluatedCandidateCount
                        + ".");
            }

            if(acceptedCardinalityTotal
                    != acceptedCandidates.size()) {

                throw new IllegalStateException(
                        "Accepted cardinality total must equal "
                        + "accepted-candidate count. CardinalityTotal="
                        + acceptedCardinalityTotal
                        + ", accepted="
                        + acceptedCandidates.size()
                        + ".");
            }
        }
    }
}
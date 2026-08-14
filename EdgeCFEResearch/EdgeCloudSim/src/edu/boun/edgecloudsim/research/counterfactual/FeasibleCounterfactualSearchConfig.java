package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.features
        .CounterfactualFeatureNames;

/**
 * Configuration for the Feasible Counterfactual Search algorithm.
 *
 * The configuration defines:
 *
 * 1. Which features may be modified
 * 2. The permitted engineering-unit range for each feature
 * 3. The perturbation step size
 * 4. The objective-function weights
 * 5. The maximum number of candidates evaluated
 *
 * Standard engineering units:
 *
 * CPU values                 = percent
 * available bandwidth       = megabits per second
 * network latency           = milliseconds
 * task length               = million instructions
 * upload size               = kilobytes
 *
 * This class is immutable.
 */
public final class FeasibleCounterfactualSearchConfig {

    /*
     * Searchable feature names.
     *
     * These aliases are retained so existing code that references
     * FeasibleCounterfactualSearchConfig.FEATURE_NAME continues
     * compiling.
     */
    public static final String REQUIRED_CPU_PERCENT =
            CounterfactualFeatureNames
            .REQUIRED_CPU_PERCENT;
    
    public static final String REQUIRED_CORES =
    		CounterfactualFeatureNames.
    		REQUIRED_CORES;

    public static final String SELECTED_AVAILABLE_CPU_PERCENT =
            CounterfactualFeatureNames
            .SELECTED_AVAILABLE_CPU_PERCENT;

    public static final String BEST_AVAILABLE_CPU_PERCENT =
            CounterfactualFeatureNames
            .BEST_AVAILABLE_CPU_PERCENT;

    public static final String AVAILABLE_BANDWIDTH_MBPS =
            CounterfactualFeatureNames
            .AVAILABLE_BANDWIDTH_MBPS;

    public static final String NETWORK_LATENCY_MS =
            CounterfactualFeatureNames
            .NETWORK_LATENCY_MS;

    public static final String TASK_LENGTH_MI =
            CounterfactualFeatureNames
            .TASK_LENGTH_MI;

    public static final String UPLOAD_SIZE_KB =
            CounterfactualFeatureNames
            .UPLOAD_SIZE_KB;
    

    /*
     * Deprecated aliases for older code.
     */
    @Deprecated
    public static final String AVAILABLE_BANDWIDTH =
            AVAILABLE_BANDWIDTH_MBPS;

    @Deprecated
    public static final String NETWORK_LATENCY =
            NETWORK_LATENCY_MS;

    @Deprecated
    public static final String TASK_LENGTH =
            TASK_LENGTH_MI;

    @Deprecated
    public static final String UPLOAD_SIZE =
            UPLOAD_SIZE_KB;

    /*
     * Candidate-generation configuration.
     */
    private final List<FeatureSearchSpec>
            featureSearchSpecifications;

    private final int maximumGeneratedCandidates;
    private final int maximumEvaluatedCandidates;

    /*
     * A valid counterfactual must modify at least this many features.
     */
    private final int minimumChangedFeatures;

    /*
     * A counterfactual may modify up to this many configured searchable
     * features.
     */
    private final int maximumChangedFeatures;

    /*
     * Adaptive search-budget bounds.
     *
     * These values are used only when the configured search mode is
     * ADAPTIVE_BEST_FIRST.
     */
    private final int minimumAdaptiveEvaluationBudget;
    private final int maximumAdaptiveEvaluationBudget;
    
    /*
     * Number of additional candidate evaluations granted by each
     * adaptive search extension.
     */
    private final int adaptiveBudgetExtensionSize;

    /*
     * Maximum number of adaptive extensions permitted for one search.
     */
    private final int maximumAdaptiveBudgetExtensions;
    /*
     * Replay-guided neighborhood refinement settings.
     */
    private final boolean neighborhoodRefinementEnabled;

    private final int maximumBoundarySeeds;

    private final int neighborhoodSamplesPerSeed;

    private final int maximumNeighborhoodCandidates;

    /*
     * Objective-function weights.
     */
    private final double proximityWeight;
    private final double sparsityWeight;
    private final double effortWeight;
    private final double networkWeight;
    private final CounterfactualSearchMode searchMode;

    /*
     * Search behavior.
     */
    private final boolean stopAtFirstAcceptedCandidate;

    private FeasibleCounterfactualSearchConfig(
            Builder builder) {

        this.featureSearchSpecifications =
                Collections.unmodifiableList(
                        new ArrayList<FeatureSearchSpec>(
                                builder
                                .featureSearchSpecifications));

        this.maximumGeneratedCandidates =
                builder.maximumGeneratedCandidates;

        this.maximumEvaluatedCandidates =
                builder.maximumEvaluatedCandidates;

        this.minimumChangedFeatures =
                builder.minimumChangedFeatures;

        /*
         * Integer.MAX_VALUE means "use every configured searchable feature."
         *
         * The resolved value is stored in the immutable configuration so
         * downstream classes do not need to interpret the sentinel value.
         */
        this.maximumChangedFeatures =
                Math.min(
                        builder.maximumChangedFeatures,
                        builder.featureSearchSpecifications.size());
        
        this.minimumAdaptiveEvaluationBudget =
                builder.minimumAdaptiveEvaluationBudget;

        this.maximumAdaptiveEvaluationBudget =
                builder.maximumAdaptiveEvaluationBudget;
        
        this.adaptiveBudgetExtensionSize =
                builder.adaptiveBudgetExtensionSize;

        this.maximumAdaptiveBudgetExtensions =
                builder.maximumAdaptiveBudgetExtensions;
        
        this.neighborhoodRefinementEnabled =
                builder.neighborhoodRefinementEnabled;

        this.maximumBoundarySeeds =
                builder.maximumBoundarySeeds;

        this.neighborhoodSamplesPerSeed =
                builder.neighborhoodSamplesPerSeed;

        this.maximumNeighborhoodCandidates =
                builder.maximumNeighborhoodCandidates;

        this.proximityWeight =
                builder.proximityWeight;

        this.sparsityWeight =
                builder.sparsityWeight;

        this.effortWeight =
                builder.effortWeight;

        this.networkWeight =
                builder.networkWeight;
        
        this.searchMode =
                builder.searchMode;

        this.stopAtFirstAcceptedCandidate =
                builder.stopAtFirstAcceptedCandidate;
    }

    /**
     * Returns the default single-feature RG-FCS configuration.
     *
     * The ranges are expressed using standardized engineering units.
     *
     * Upload-size generation will later use relative reductions such
     * as 5%, 10%, 15%, and so forth. The upload step below remains as
     * a fallback for generic step-based generation.
     */
    public static FeasibleCounterfactualSearchConfig
    createDefault() {

		return createDefault(
		        CounterfactualSearchMode
		        .FIXED_EXHAUSTIVE);
		}
		
    /**
     * Returns the default RG-FCS configuration.
     *
     * A counterfactual must modify at least one feature and may modify any
     * number of configured searchable features.
		*/
		public static FeasibleCounterfactualSearchConfig
		    createDefault(
		            CounterfactualSearchMode searchMode) {
		
		if(searchMode == null) {
		
		    throw new IllegalArgumentException(
		            "Counterfactual search mode cannot be null.");
		}
		
		return new Builder()
				
				/*
				 * Task CPU requirement:
				 * 0-100 percent in increments of five percentage points.
				 *
				 * This feature is essential for feasibility restoration because
				 * reducing task demand may make an originally unschedulable task
				 * compatible with one or more candidate VMs.
				 */
				.addFeatureSearchSpec(
				        new FeatureSearchSpec(
				                REQUIRED_CPU_PERCENT,
				                0.0,
				                100.0,
				                5.0,
				                100.0,
				                1.5,
				                NormalizationMode
				                        .FIXED_RANGE))

                /*
                 * Selected VM CPU availability:
                 * 0-100 percent in increments of five percentage
                 * points.
                 */
                .addFeatureSearchSpec(
                        new FeatureSearchSpec(
                                SELECTED_AVAILABLE_CPU_PERCENT,
                                0.0,
                                100.0,
                                5.0,
                                100.0,
                                1.0,
                                NormalizationMode
                                        .FIXED_RANGE))

                /*
                 * Best candidate VM CPU availability:
                 * 0-100 percent in increments of five percentage
                 * points.
                 */
                .addFeatureSearchSpec(
                        new FeatureSearchSpec(
                                BEST_AVAILABLE_CPU_PERCENT,
                                0.0,
                                100.0,
                                5.0,
                                100.0,
                                1.0,
                                NormalizationMode
                                        .FIXED_RANGE))

                /*
                 * Available bandwidth in Mbps.
                 *
                 * The range supports WLAN, WAN, and higher-capacity
                 * experimental network configurations.
                 */
                .addFeatureSearchSpec(
                        new FeatureSearchSpec(
                                AVAILABLE_BANDWIDTH_MBPS,
                                1.0,
                                1000.0,
                                10.0,
                                1000.0,
                                1.5,
                                NormalizationMode
                                        .FIXED_RANGE))

                /*
                 * Network latency in milliseconds.
                 *
                 * The 0-1000 ms range permits low-latency edge links,
                 * WAN links, and degraded-network experiments.
                 */
                .addFeatureSearchSpec(
                        new FeatureSearchSpec(
                                NETWORK_LATENCY_MS,
                                0.0,
                                1000.0,
                                5.0,
                                1000.0,
                                2.0,
                                NormalizationMode
                                        .FIXED_RANGE))

                /*
                 * Task length in million instructions.
                 *
                 * Relative normalization is used because a change of
                 * 500 MI has a different practical meaning for a
                 * 1,000-MI task than for a 50,000-MI task.
                 */
                .addFeatureSearchSpec(
                        new FeatureSearchSpec(
                                TASK_LENGTH_MI,
                                1.0,
                                100000.0,
                                500.0,
                                100000.0,
                                2.0,
                                NormalizationMode
                                        .RELATIVE_TO_ORIGINAL))

                /*
                 * Upload size in kilobytes.
                 *
                 * The earlier 1-100 range was invalid for logged values
                 * such as 1,355 KB, 2,568 KB, and 4,715 KB.
                 *
                 * The broad 1-10,000 KB bound safely contains the
                 * current experimental workload. Relative percentage
                 * reductions will be generated in CounterfactualGenerator.
                 */
                .addFeatureSearchSpec(
                        new FeatureSearchSpec(
                                UPLOAD_SIZE_KB,
                                1.0,
                                25000.0,
                                100.0,
                                25000.0,
                                2.0,
                                NormalizationMode
                                        .RELATIVE_TO_ORIGINAL))
                
                /*
                 * Required task core count.
                 *
                 * Core demand is discrete. This feature is especially
                 * important for feasibility restoration when a task
                 * originally requires more cores than available VMs expose.
                 */
                .addFeatureSearchSpec(
                        new FeatureSearchSpec(
                                REQUIRED_CORES,
                                1.0,
                                4.0,
                                1.0,
                                4.0,
                                2.0,
                                NormalizationMode
                                        .FIXED_RANGE))

                .withMaximumGeneratedCandidates(
                        500)
                
                .withMaximumEvaluatedCandidates(
                        500)

                
                .withMinimumAdaptiveEvaluationBudget(
                        50)

                .withMaximumAdaptiveEvaluationBudget(
                        500)
                
                /*
                 * Extend an unsuccessful adaptive search in blocks of 50 replay
                 * evaluations, for at most four additional stages.
                 */
                .withAdaptiveBudgetExtensionSize(
                        50)

                .withMaximumAdaptiveBudgetExtensions(
                        4)
                
                /*
                 * Every counterfactual must modify at least one feature.
                 */
                .withMinimumChangedFeatures(
                        1)

                /*
                 * Allow any number of configured searchable features to be modified.
                 *
                 * The sparsity objective still prefers candidates with fewer changes,
                 * but multi-feature counterfactuals are not prohibited.
                 */
                .withMaximumChangedFeatures(
                        Integer.MAX_VALUE)

                /*
                 * Initial objective:
                 *
                 * score =
                 * proximity
                 * + sparsity
                 * + effort
                 * + network penalty
                 */
                .withProximityWeight(
                        1.0)

                .withSparsityWeight(
                        1.0)

                .withEffortWeight(
                        1.0)

                .withNetworkWeight(
                        1.0)

                /*
                 * Evaluate all accepted candidates and retain the
                 * lowest-scoring result.
                 */
                .withStopAtFirstAcceptedCandidate(
                        false)
                
                .withSearchMode(
                        searchMode)
                

                .build();
    }
    
    

    public List<FeatureSearchSpec>
            getFeatureSearchSpecifications() {

        return featureSearchSpecifications;
    }

    public FeatureSearchSpec findFeatureSearchSpec(
            String featureName) {

        if(featureName == null) {

            return null;
        }

        String normalizedFeatureName =
                normalizeFeatureName(
                        featureName);

        for(FeatureSearchSpec specification
                : featureSearchSpecifications) {

            if(specification
                    .getFeatureName()
                    .equals(
                            normalizedFeatureName)) {

                return specification;
            }
        }

        return null;
    }

    public int getMaximumGeneratedCandidates() {

        return maximumGeneratedCandidates;
    }

    public int getMaximumEvaluatedCandidates() {

        return maximumEvaluatedCandidates;
    }
    
    /**
     * Returns the minimum number of changed features required for a valid
     * generated counterfactual candidate.
     */
    public int getMinimumChangedFeatures() {

        return minimumChangedFeatures;
    }

    public int getMaximumChangedFeatures() {

        return maximumChangedFeatures;
    }
    
    public int getMinimumAdaptiveEvaluationBudget() {

        return minimumAdaptiveEvaluationBudget;
    }

    public int getMaximumAdaptiveEvaluationBudget() {

        return maximumAdaptiveEvaluationBudget;
    }
    
    public int getAdaptiveBudgetExtensionSize() {

        return adaptiveBudgetExtensionSize;
    }

    public int getMaximumAdaptiveBudgetExtensions() {

        return maximumAdaptiveBudgetExtensions;
        
    }
    
    /**
     * Returns whether replay-guided neighborhood refinement is enabled.
     */
    public boolean isNeighborhoodRefinementEnabled() {

        return neighborhoodRefinementEnabled;
    }

    /**
     * Returns the maximum number of replay-boundary candidates that may
     * be used as local-refinement seeds.
     */
    public int getMaximumBoundarySeeds() {

        return maximumBoundarySeeds;
    }

    /**
     * Returns the maximum number of local neighbors generated from each
     * selected boundary seed.
     */
    public int getNeighborhoodSamplesPerSeed() {

        return neighborhoodSamplesPerSeed;
    }

    /**
     * Returns the global maximum number of locally refined candidates
     * generated for one counterfactual search.
     */
    public int getMaximumNeighborhoodCandidates() {

        return maximumNeighborhoodCandidates;
    }

    public double getProximityWeight() {

        return proximityWeight;
    }

    public double getSparsityWeight() {

        return sparsityWeight;
    }

    public double getEffortWeight() {

        return effortWeight;
    }

    public double getNetworkWeight() {

        return networkWeight;
    }
    
    public CounterfactualSearchMode getSearchMode() {

        return searchMode;
    }
    
    public boolean isFixedExhaustiveSearch() {

        return searchMode
                == CounterfactualSearchMode
                .FIXED_EXHAUSTIVE;
    }

    public boolean isAdaptiveBestFirstSearch() {

        return searchMode
                == CounterfactualSearchMode
                .ADAPTIVE_BEST_FIRST;
    }

    public boolean isStopAtFirstAcceptedCandidate() {

        return stopAtFirstAcceptedCandidate;
    }

    public int getSearchableFeatureCount() {

        return featureSearchSpecifications
                .size();
    }

    private static String normalizeFeatureName(
            String featureName) {

        if(featureName == null
                || featureName.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Feature name cannot be null or empty.");
        }

        return featureName
                .trim()
                .toLowerCase()
                .replace(' ', '_')
                .replace('-', '_');
    }

    private static boolean isFinite(
            double value) {

        return !Double.isNaN(value)
                && !Double.isInfinite(value);
    }

    @Override
    public String toString() {

        return "FeasibleCounterfactualSearchConfig{"
                + "searchMode="
                + searchMode

                + ", searchableFeatureCount="
                + featureSearchSpecifications.size()

                + ", maximumGeneratedCandidates="
                + maximumGeneratedCandidates

                + ", maximumEvaluatedCandidates="
                + maximumEvaluatedCandidates

                + ", minimumAdaptiveEvaluationBudget="
                + minimumAdaptiveEvaluationBudget

                + ", maximumAdaptiveEvaluationBudget="
                + maximumAdaptiveEvaluationBudget
                
                + ", adaptiveBudgetExtensionSize="
                + adaptiveBudgetExtensionSize

                + ", maximumAdaptiveBudgetExtensions="
                + maximumAdaptiveBudgetExtensions
                
                + ", neighborhoodRefinementEnabled="
                + neighborhoodRefinementEnabled

                + ", maximumBoundarySeeds="
                + maximumBoundarySeeds

                + ", neighborhoodSamplesPerSeed="
                + neighborhoodSamplesPerSeed

                + ", maximumNeighborhoodCandidates="
                + maximumNeighborhoodCandidates

                + ", minimumChangedFeatures="
                + minimumChangedFeatures

                + ", maximumChangedFeatures="
                + maximumChangedFeatures

                + ", proximityWeight="
                + proximityWeight

                + ", sparsityWeight="
                + sparsityWeight

                + ", effortWeight="
                + effortWeight

                + ", networkWeight="
                + networkWeight

                + ", stopAtFirstAcceptedCandidate="
                + stopAtFirstAcceptedCandidate

                + '}';
    }

    /**
     * Determines how feature changes are normalized.
     */
    public enum NormalizationMode {

        /*
         * |counterfactual - original| / configured range
         */
        FIXED_RANGE,

        /*
         * |counterfactual - original| / |original|
         */
        RELATIVE_TO_ORIGINAL
    }

    /**
     * Defines how one feature is explored during search.
     */
    public static final class FeatureSearchSpec {

        private static final double MINIMUM_DENOMINATOR =
                0.000001;

        private final String featureName;

        private final double minimumValue;
        private final double maximumValue;
        private final double stepSize;

        /*
         * Used when normalizationMode is FIXED_RANGE.
         */
        private final double normalizationRange;

        /*
         * Relative effort associated with modifying the feature.
         */
        private final double effortCost;

        private final NormalizationMode normalizationMode;

        /**
         * Backward-compatible constructor.
         *
         * Uses fixed-range normalization.
         */
        public FeatureSearchSpec(
                String featureName,
                double minimumValue,
                double maximumValue,
                double stepSize,
                double normalizationRange,
                double effortCost) {

            this(
                    featureName,
                    minimumValue,
                    maximumValue,
                    stepSize,
                    normalizationRange,
                    effortCost,
                    NormalizationMode
                            .FIXED_RANGE);
        }

        /**
         * Creates a feature-search specification.
         */
        public FeatureSearchSpec(
                String featureName,
                double minimumValue,
                double maximumValue,
                double stepSize,
                double normalizationRange,
                double effortCost,
                NormalizationMode normalizationMode) {

            this.featureName =
                    normalizeFeatureName(
                            featureName);

            validateFinite(
                    minimumValue,
                    "Minimum value");

            validateFinite(
                    maximumValue,
                    "Maximum value");

            validateFinite(
                    stepSize,
                    "Step size");

            validateFinite(
                    normalizationRange,
                    "Normalization range");

            validateFinite(
                    effortCost,
                    "Effort cost");

            if(normalizationMode == null) {

                throw new IllegalArgumentException(
                        "Normalization mode cannot be null.");
            }

            if(maximumValue < minimumValue) {

                throw new IllegalArgumentException(
                        "Maximum value cannot be smaller "
                        + "than minimum value.");
            }

            if(stepSize <= 0.0) {

                throw new IllegalArgumentException(
                        "Step size must be greater than zero.");
            }

            if(normalizationRange <= 0.0) {

                throw new IllegalArgumentException(
                        "Normalization range must be greater "
                        + "than zero.");
            }

            if(effortCost < 0.0) {

                throw new IllegalArgumentException(
                        "Effort cost cannot be negative.");
            }
            

            this.minimumValue =
                    minimumValue;

            this.maximumValue =
                    maximumValue;

            this.stepSize =
                    stepSize;

            this.normalizationRange =
                    normalizationRange;

            this.effortCost =
                    effortCost;

            this.normalizationMode =
                    normalizationMode;
        }

        public String getFeatureName() {

            return featureName;
        }

        public double getMinimumValue() {

            return minimumValue;
        }

        public double getMaximumValue() {

            return maximumValue;
        }

        public double getStepSize() {

            return stepSize;
        }

        public double getNormalizationRange() {

            return normalizationRange;
        }

        public double getEffortCost() {

            return effortCost;
        }

        public NormalizationMode getNormalizationMode() {

            return normalizationMode;
        }

        public boolean usesRelativeNormalization() {

            return normalizationMode
                    == NormalizationMode
                    .RELATIVE_TO_ORIGINAL;
        }

        public boolean contains(
                double value) {

            return isFinite(value)
                    && value >= minimumValue
                    && value <= maximumValue;
        }

        /**
         * Verifies that an original value lies within the configured
         * search range.
         *
         * This prevents silent errors such as searching upload size
         * between 1 and 100 when the original value is 4,715 KB.
         */
        public void validateOriginalValue(
                double originalValue) {

            if(!isFinite(originalValue)) {

                throw new IllegalStateException(
                        "Original value for "
                        + featureName
                        + " must be finite.");
            }

            if(!contains(originalValue)) {

                throw new IllegalStateException(
                        "Original value "
                        + originalValue
                        + " for feature "
                        + featureName
                        + " is outside the configured range ["
                        + minimumValue
                        + ", "
                        + maximumValue
                        + "].");
            }
        }

        public double normalizeChange(
                double originalValue,
                double counterfactualValue) {

            if(!isFinite(originalValue)
                    || !isFinite(counterfactualValue)) {

                return Double.POSITIVE_INFINITY;
            }

            double absoluteChange =
                    Math.abs(
                            counterfactualValue
                            - originalValue);

            if(normalizationMode
                    == NormalizationMode
                    .RELATIVE_TO_ORIGINAL) {

                double denominator =
                        Math.max(
                                Math.abs(
                                        originalValue),
                                MINIMUM_DENOMINATOR);

                return absoluteChange
                        / denominator;
            }

            return absoluteChange
                    / normalizationRange;
        }

        public double calculateEffort(
                double originalValue,
                double counterfactualValue) {

            return normalizeChange(
                    originalValue,
                    counterfactualValue)
                    * effortCost;
        }

        private static void validateFinite(
                double value,
                String fieldName) {

            if(!isFinite(value)) {

                throw new IllegalArgumentException(
                        fieldName
                        + " must be finite.");
            }
        }

        @Override
        public String toString() {

            return "FeatureSearchSpec{"
                    + "featureName='"
                    + featureName
                    + '\''

                    + ", minimumValue="
                    + minimumValue

                    + ", maximumValue="
                    + maximumValue

                    + ", stepSize="
                    + stepSize

                    + ", normalizationRange="
                    + normalizationRange

                    + ", effortCost="
                    + effortCost

                    + ", normalizationMode="
                    + normalizationMode

                    +'}';
        }
    }

    public static final class Builder {

        private final List<FeatureSearchSpec>
                featureSearchSpecifications =
                new ArrayList<FeatureSearchSpec>();

        private int maximumGeneratedCandidates =
                500;

        private int maximumEvaluatedCandidates =
                500;
        
        /*
         * A valid counterfactual must modify at least one feature.
         */
        private int minimumChangedFeatures =
                1;

        /*
         * Integer.MAX_VALUE means that all configured searchable features may
         * be modified.
         *
         * The value is resolved to the actual searchable-feature count when
         * the immutable configuration is constructed.
         */
        private int maximumChangedFeatures =
                Integer.MAX_VALUE;
        
        private int minimumAdaptiveEvaluationBudget =
                50;

        private int maximumAdaptiveEvaluationBudget =
                500;
        
        private int adaptiveBudgetExtensionSize =
                50;

        private int maximumAdaptiveBudgetExtensions =
                4;
        /*
         * Replay-guided neighborhood refinement defaults.
         */
        private boolean neighborhoodRefinementEnabled =
                true;

        private int maximumBoundarySeeds =
                4;

        private int neighborhoodSamplesPerSeed =
                8;

        private int maximumNeighborhoodCandidates =
                32;

        private double proximityWeight =
                1.0;

        private double sparsityWeight =
                1.0;

        private double effortWeight =
                1.0;

        private double networkWeight =
                1.0;

        private boolean stopAtFirstAcceptedCandidate =
                false;
        
        private CounterfactualSearchMode searchMode =
                CounterfactualSearchMode.ADAPTIVE_BEST_FIRST;
       

        public Builder addFeatureSearchSpec(
                FeatureSearchSpec specification) {

            if(specification == null) {

                throw new IllegalArgumentException(
                        "Feature search specification "
                        + "cannot be null.");
            }

            if(containsFeature(
                    specification
                    .getFeatureName())) {

                throw new IllegalArgumentException(
                        "Duplicate feature search specification: "
                        + specification
                        .getFeatureName());
            }

            featureSearchSpecifications.add(
                    specification);

            return this;
        }

        public Builder withMaximumGeneratedCandidates(
                int value) {

            this.maximumGeneratedCandidates =
                    value;

            return this;
        }

        public Builder withMaximumEvaluatedCandidates(
                int value) {

            this.maximumEvaluatedCandidates =
                    value;

            return this;
        }
        
        public Builder withAdaptiveBudgetExtensionSize(
                int value) {

            this.adaptiveBudgetExtensionSize =
                    value;

            return this;
        }

        public Builder withMaximumAdaptiveBudgetExtensions(
                int value) {

            this.maximumAdaptiveBudgetExtensions =
                    value;

            return this;
        }
        
        /**
         * Sets the minimum number of features that must be changed in one
         * generated counterfactual candidate.
         */
        public Builder withMinimumChangedFeatures(
                int value) {

            this.minimumChangedFeatures =
                    value;

            return this;
        }

        /**
         * Sets the maximum number of searchable features that may be changed
         * in one generated counterfactual candidate.
         *
         * Integer.MAX_VALUE means all configured searchable features.
         */
        public Builder withMaximumChangedFeatures(
                int value) {

            this.maximumChangedFeatures =
                    value;

            return this;
        }
        
        public Builder withMinimumAdaptiveEvaluationBudget(
                int value) {

            this.minimumAdaptiveEvaluationBudget =
                    value;

            return this;
        }

        public Builder withMaximumAdaptiveEvaluationBudget(
                int value) {

            this.maximumAdaptiveEvaluationBudget =
                    value;

            return this;
        }
        
        /**
         * Enables or disables replay-guided neighborhood refinement.
         */
        public Builder withNeighborhoodRefinementEnabled(
                boolean neighborhoodRefinementEnabled) {

            this.neighborhoodRefinementEnabled =
                    neighborhoodRefinementEnabled;

            return this;
        }

        /**
         * Sets the maximum number of boundary candidates used as refinement
         * seeds.
         */
        public Builder withMaximumBoundarySeeds(
                int maximumBoundarySeeds) {

            this.maximumBoundarySeeds =
                    maximumBoundarySeeds;

            return this;
        }

        /**
         * Sets the maximum number of local candidates generated from each
         * boundary seed.
         */
        public Builder withNeighborhoodSamplesPerSeed(
                int neighborhoodSamplesPerSeed) {

            this.neighborhoodSamplesPerSeed =
                    neighborhoodSamplesPerSeed;

            return this;
        }

        /**
         * Sets the global maximum number of local neighborhood candidates.
         */
        public Builder withMaximumNeighborhoodCandidates(
                int maximumNeighborhoodCandidates) {

            this.maximumNeighborhoodCandidates =
                    maximumNeighborhoodCandidates;

            return this;
        }

        public Builder withProximityWeight(
                double value) {

            this.proximityWeight =
                    value;

            return this;
        }

        public Builder withSparsityWeight(
                double value) {

            this.sparsityWeight =
                    value;

            return this;
        }

        public Builder withEffortWeight(
                double value) {

            this.effortWeight =
                    value;

            return this;
        }

        public Builder withNetworkWeight(
                double value) {

            this.networkWeight =
                    value;

            return this;
        }

        public Builder withStopAtFirstAcceptedCandidate(
                boolean value) {

            this.stopAtFirstAcceptedCandidate =
                    value;

            return this;
        }
        
        public Builder withSearchMode(
                CounterfactualSearchMode searchMode) {

            if(searchMode == null) {

                throw new IllegalArgumentException(
                        "Counterfactual search mode cannot be null.");
            }

            this.searchMode =
                    searchMode;

            return this;
        }

        public FeasibleCounterfactualSearchConfig build() {

            validate();

            return new FeasibleCounterfactualSearchConfig(
                    this);
        }

        private boolean containsFeature(
                String featureName) {

            for(FeatureSearchSpec specification
                    : featureSearchSpecifications) {

                if(specification
                        .getFeatureName()
                        .equals(
                                featureName)) {

                    return true;
                }
            }

            return false;
        }

        private void validate() {
        	
        	if(searchMode == null) {

        	    throw new IllegalStateException(
        	            "Counterfactual search mode cannot be null.");
        	}

            if(featureSearchSpecifications.isEmpty()) {

                throw new IllegalStateException(
                        "At least one searchable feature "
                        + "must be configured.");
            }

            if(maximumGeneratedCandidates <= 0) {

                throw new IllegalStateException(
                        "Maximum generated-candidate count "
                        + "must be greater than zero.");
            }

            if(maximumEvaluatedCandidates <= 0) {

                throw new IllegalStateException(
                        "Maximum evaluated-candidate count "
                        + "must be greater than zero.");
            }

            if(maximumEvaluatedCandidates
                    > maximumGeneratedCandidates) {

                throw new IllegalStateException(
                        "Maximum evaluated-candidate count cannot "
                        + "exceed maximum generated-candidate count.");
            }

            if(minimumChangedFeatures <= 0) {

                throw new IllegalStateException(
                        "Minimum changed-feature count must "
                        + "be greater than zero.");
            }

            if(maximumChangedFeatures <= 0) {

                throw new IllegalStateException(
                        "Maximum changed-feature count must "
                        + "be greater than zero.");
            }

            int resolvedMaximumChangedFeatures =
                    Math.min(
                            maximumChangedFeatures,
                            featureSearchSpecifications.size());

            if(minimumChangedFeatures
                    > resolvedMaximumChangedFeatures) {

                throw new IllegalStateException(
                        "Minimum changed-feature count cannot exceed "
                        + "the effective maximum changed-feature count.");
            }
            
            if(minimumAdaptiveEvaluationBudget <= 0) {

                throw new IllegalStateException(
                        "Minimum adaptive evaluation budget must "
                        + "be greater than zero.");
            }

            if(maximumAdaptiveEvaluationBudget <= 0) {

                throw new IllegalStateException(
                        "Maximum adaptive evaluation budget must "
                        + "be greater than zero.");
            }

            if(minimumAdaptiveEvaluationBudget
                    > maximumAdaptiveEvaluationBudget) {

                throw new IllegalStateException(
                        "Minimum adaptive evaluation budget cannot "
                        + "exceed maximum adaptive evaluation budget.");
            }
            
            if(adaptiveBudgetExtensionSize <= 0) {

                throw new IllegalStateException(
                        "Adaptive budget extension size must be "
                        + "greater than zero.");
            }

            if(maximumAdaptiveBudgetExtensions < 0) {

                throw new IllegalStateException(
                        "Maximum adaptive budget-extension count "
                        + "cannot be negative.");
            }
            
            if(maximumBoundarySeeds <= 0) {

                throw new IllegalArgumentException(
                        "Maximum boundary seeds must be greater than zero.");
            }

            if(neighborhoodSamplesPerSeed <= 0) {

                throw new IllegalArgumentException(
                        "Neighborhood samples per seed must be greater than zero.");
            }

            if(maximumNeighborhoodCandidates <= 0) {

                throw new IllegalArgumentException(
                        "Maximum neighborhood candidates must be greater than zero.");
            }

            long maximumRequestedNeighborhoodCandidates =
                    (long) maximumBoundarySeeds
                    * neighborhoodSamplesPerSeed;

            if(maximumNeighborhoodCandidates
                    > maximumRequestedNeighborhoodCandidates) {

                throw new IllegalArgumentException(
                        "Maximum neighborhood candidates cannot exceed "
                        + "maximumBoundarySeeds multiplied by "
                        + "neighborhoodSamplesPerSeed.");
            }
            
            long largestConfiguredAdaptiveBudget =
                    (long) minimumAdaptiveEvaluationBudget
                    + ((long) adaptiveBudgetExtensionSize
                    * maximumAdaptiveBudgetExtensions);

            if(largestConfiguredAdaptiveBudget
                    < minimumAdaptiveEvaluationBudget) {

                throw new IllegalStateException(
                        "Adaptive budget configuration overflowed.");
            }

            if(maximumAdaptiveEvaluationBudget
                    > maximumGeneratedCandidates) {

                throw new IllegalStateException(
                        "Maximum adaptive evaluation budget cannot "
                        + "exceed maximum generated-candidate count.");
            }

            validateNonNegativeWeight(
                    proximityWeight,
                    "Proximity weight");

            validateNonNegativeWeight(
                    sparsityWeight,
                    "Sparsity weight");

            validateNonNegativeWeight(
                    effortWeight,
                    "Effort weight");

            validateNonNegativeWeight(
                    networkWeight,
                    "Network weight");

            if(proximityWeight == 0.0
                    && sparsityWeight == 0.0
                    && effortWeight == 0.0
                    && networkWeight == 0.0) {

                throw new IllegalStateException(
                        "At least one objective-function weight "
                        + "must be greater than zero.");
            }
        }

        private static void validateNonNegativeWeight(
                double value,
                String fieldName) {

            if(!isFinite(value)
                    || value < 0.0) {

                throw new IllegalStateException(
                        fieldName
                        + " must be finite and nonnegative.");
            }
        }
    }
}
package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.boun.edgecloudsim.research.counterfactual.features
        .CounterfactualFeatureNames;

/**
 * Generates feature perturbations and complete candidate change sets
 * for Replay-Guided Feasible Counterfactual Search (RG-FCS).
 *
 * Standard engineering units:
 *
 * CPU values                 = percent
 * available bandwidth       = megabits per second
 * network latency           = milliseconds
 * task length               = million instructions
 * upload size               = kilobytes
 *
 * Most features are generated using the configured step size.
 * Upload-size candidates are generated using percentage reductions
 * because compression and payload reduction are naturally expressed
 * relative to the original task size.
 *
 * Candidate values are generated deterministically so experiments
 * can be reproduced.
 */
public final class CounterfactualCandidateGenerator {

    private static final double EPSILON =
            0.000001;

    /**
     * Relative payload reductions evaluated for upload-size
     * counterfactuals.
     *
     * For example, an original payload of 2,000 KB produces:
     *
     * 5% reduction  = 1,900 KB
     * 10% reduction = 1,800 KB
     * 25% reduction = 1,500 KB
     * 50% reduction = 1,000 KB
     */
    private static final double[] UPLOAD_REDUCTION_RATIOS = {
            0.05,
            0.10,
            0.15,
            0.20,
            0.25,
            0.30,
            0.40,
            0.50
    };

    private final FeasibleCounterfactualSearchConfig configuration;

    private final boolean debugEnabled;

    public CounterfactualCandidateGenerator(
            FeasibleCounterfactualSearchConfig configuration) {

        this(
                configuration,
                false);
    }

    public CounterfactualCandidateGenerator(
            FeasibleCounterfactualSearchConfig configuration,
            boolean debugEnabled) {

        if(configuration == null) {

            throw new IllegalArgumentException(
                    "Search configuration cannot be null.");
        }

        this.configuration =
                configuration;

        this.debugEnabled =
                debugEnabled;
    }

    public boolean isDebugEnabled() {

        return debugEnabled;
    }

    public FeasibleCounterfactualSearchConfig
            getConfiguration() {

        return configuration;
    }

    /**
     * Generates individual single-feature changes from a base replay
     * state.
     *
     * This method is retained for backward compatibility and for producing
     * the per-feature perturbation pools used by complete candidate
     * generation.
     */
    public List<FeatureChange> generateSingleFeatureChanges(
            ReplayState baseState) {

        if(baseState == null) {

            throw new IllegalArgumentException(
                    "Base ReplayState cannot be null.");
        }

        /*
         * Generate candidates separately for each feature first.
         *
         * This prevents features appearing earlier in the configuration
         * from consuming the entire global candidate budget.
         */
        List<List<FeatureChange>> candidatesByFeature =
                new ArrayList<List<FeatureChange>>();

        for(FeasibleCounterfactualSearchConfig.FeatureSearchSpec specification
                : configuration.getFeatureSearchSpecifications()) {

            List<FeatureChange> featureCandidates =
                    new ArrayList<FeatureChange>();

            generateChangesForFeature(
                    baseState,
                    specification,
                    featureCandidates);

            candidatesByFeature.add(
                    featureCandidates);
        }

        /*
         * Merge the feature-specific candidate lists in round-robin
         * order. This guarantees balanced feature coverage.
         */
        List<FeatureChange> generatedChanges =
                interleaveFeatureCandidates(
                        candidatesByFeature);

        if(debugEnabled) {

            printGenerationSummary(
                    generatedChanges);
        }

        return Collections.unmodifiableList(
                generatedChanges);
    }
    
    /**
     * Generates complete counterfactual candidate change sets.
     *
     * Candidate generation is bounded and deterministic. Feature changes
     * are first organized into one perturbation pool per searchable
     * feature. Candidate sets are then generated in distance layers.
     *
     * Layer zero combines the nearest generated perturbation from each
     * selected feature. Layer one combines the second-nearest
     * perturbations, and so forth.
     *
     * Within every layer, sparse candidates are generated before denser
     * candidates:
     *
     * 1 changed feature,
     * 2 changed features,
     * 3 changed features,
     * ...
     * maximum configured changed features.
     *
     * This avoids construction of the full Cartesian product while still
     * providing deterministic coverage of multiple feature subsets and
     * perturbation distances.
     */
    public List<List<FeatureChange>> generateCandidateChangeSets(
            ReplayState baseState) {

        if(baseState == null) {

            throw new IllegalArgumentException(
                    "Base ReplayState cannot be null.");
        }

        List<List<FeatureChange>> candidatesByFeature =
                generateFeatureCandidatePools(
                        baseState);

        List<List<FeatureChange>> candidateChangeSets =
                new ArrayList<List<FeatureChange>>();

        Map<String, Boolean> encounteredCandidateKeys =
                new LinkedHashMap<String, Boolean>();

        int maximumCandidateCount =
                configuration.getMaximumGeneratedCandidates();

        int minimumChangedFeatureCount =
                configuration.getMinimumChangedFeatures();

        int maximumChangedFeatureCount =
                Math.min(
                        configuration.getMaximumChangedFeatures(),
                        candidatesByFeature.size());

        int maximumLayerCount =
                findMaximumCandidatePoolSize(
                        candidatesByFeature);

        /*
         * Progress outward from the original state.
         *
         * Candidates in earlier layers use changes that are nearer to the
         * original feature values.
         */
        for(int layerIndex = 0;
                layerIndex < maximumLayerCount
                && candidateChangeSets.size()
                < maximumCandidateCount;
                layerIndex++) {

            /*
             * Generate sparse candidates before denser candidates within
             * every perturbation layer.
             */
            for(int changedFeatureCount =
                    minimumChangedFeatureCount;

                    changedFeatureCount
                    <= maximumChangedFeatureCount
                    && candidateChangeSets.size()
                    < maximumCandidateCount;

                    changedFeatureCount++) {

                generateLayerCombinations(
                        candidatesByFeature,
                        layerIndex,
                        changedFeatureCount,
                        0,
                        new ArrayList<FeatureChange>(),
                        candidateChangeSets,
                        encounteredCandidateKeys,
                        maximumCandidateCount);
            }
        }

        if(debugEnabled) {

            printCandidateChangeSetSummary(
                    candidateChangeSets);
        }

        return Collections.unmodifiableList(
                candidateChangeSets);
    }
    
    /**
     * Generates one ordered perturbation pool for every configured
     * searchable feature.
     *
     * The feature order is the order declared in the search
     * configuration. The change order inside each pool is the existing
     * deterministic generation order.
     */
    private List<List<FeatureChange>> generateFeatureCandidatePools(
            ReplayState baseState) {

        List<List<FeatureChange>> candidatesByFeature =
                new ArrayList<List<FeatureChange>>();

        for(FeasibleCounterfactualSearchConfig.FeatureSearchSpec specification
                : configuration.getFeatureSearchSpecifications()) {

            List<FeatureChange> featureCandidates =
                    new ArrayList<FeatureChange>();

            generateChangesForFeature(
                    baseState,
                    specification,
                    featureCandidates);

            candidatesByFeature.add(
                    featureCandidates);
        }

        return candidatesByFeature;
    }
    /**
     * Generates combinations containing exactly the requested number of
     * changed features from one perturbation layer.
     *
     * One feature contributes at most one FeatureChange to a candidate.
     * Feature order is preserved, making candidate IDs and duplicate keys
     * deterministic.
     */
    private void generateLayerCombinations(
            List<List<FeatureChange>> candidatesByFeature,
            int layerIndex,
            int targetChangedFeatureCount,
            int nextFeatureIndex,
            List<FeatureChange> currentChangeSet,
            List<List<FeatureChange>> generatedChangeSets,
            Map<String, Boolean> encounteredCandidateKeys,
            int maximumCandidateCount) {

        if(generatedChangeSets.size()
                >= maximumCandidateCount) {

            return;
        }

        /*
         * A complete combination has been constructed.
         */
        if(currentChangeSet.size()
                == targetChangedFeatureCount) {

            addCandidateChangeSetIfValid(
                    currentChangeSet,
                    generatedChangeSets,
                    encounteredCandidateKeys,
                    maximumCandidateCount);

            return;
        }

        int remainingRequiredFeatures =
                targetChangedFeatureCount
                - currentChangeSet.size();

        int remainingAvailableFeatures =
                candidatesByFeature.size()
                - nextFeatureIndex;

        /*
         * Stop when too few features remain to complete the requested
         * candidate cardinality.
         */
        if(remainingAvailableFeatures
                < remainingRequiredFeatures) {

            return;
        }

        for(int featureIndex = nextFeatureIndex;
                featureIndex < candidatesByFeature.size();
                featureIndex++) {

            if(generatedChangeSets.size()
                    >= maximumCandidateCount) {

                return;
            }

            int featuresRemainingAfterCurrent =
                    candidatesByFeature.size()
                    - featureIndex
                    - 1;

            int featuresStillRequiredAfterCurrent =
                    targetChangedFeatureCount
                    - currentChangeSet.size()
                    - 1;

            if(featuresRemainingAfterCurrent
                    < featuresStillRequiredAfterCurrent) {

                break;
            }

            List<FeatureChange> featureCandidates =
                    candidatesByFeature.get(
                            featureIndex);

            /*
             * This feature has no candidate at the current distance layer.
             */
            if(featureCandidates == null
                    || layerIndex
                    >= featureCandidates.size()) {

                continue;
            }

            FeatureChange featureChange =
                    featureCandidates.get(
                            layerIndex);

            currentChangeSet.add(
                    featureChange);

            generateLayerCombinations(
                    candidatesByFeature,
                    layerIndex,
                    targetChangedFeatureCount,
                    featureIndex + 1,
                    currentChangeSet,
                    generatedChangeSets,
                    encounteredCandidateKeys,
                    maximumCandidateCount);

            currentChangeSet.remove(
                    currentChangeSet.size() - 1);
        }
    }
    
    /**
     * Adds one complete change set when it is valid, unique, and inside
     * the global candidate-generation budget.
     */
    private void addCandidateChangeSetIfValid(
            List<FeatureChange> featureChanges,
            List<List<FeatureChange>> generatedChangeSets,
            Map<String, Boolean> encounteredCandidateKeys,
            int maximumCandidateCount) {

        if(generatedChangeSets.size()
                >= maximumCandidateCount) {

            return;
        }

        if(!isValidCandidateChangeSet(
                featureChanges)) {

            return;
        }

        String candidateKey =
                createCandidateChangeSetKey(
                        featureChanges);

        if(candidateKey.length() == 0
                || encounteredCandidateKeys
                .containsKey(candidateKey)) {

            return;
        }

        generatedChangeSets.add(
                createImmutableChangeSet(
                        featureChanges));

        encounteredCandidateKeys.put(
                candidateKey,
                Boolean.TRUE);
    }
    
    /**
     * Returns the greatest number of perturbations generated for any
     * searchable feature.
     */
    private int findMaximumCandidatePoolSize(
            List<List<FeatureChange>> candidatesByFeature) {

        int maximumPoolSize =
                0;

        if(candidatesByFeature == null
                || candidatesByFeature.isEmpty()) {

            return maximumPoolSize;
        }

        for(List<FeatureChange> featureCandidates
                : candidatesByFeature) {

            if(featureCandidates != null) {

                maximumPoolSize =
                        Math.max(
                                maximumPoolSize,
                                featureCandidates.size());
            }
        }

        return maximumPoolSize;
    }
    
    /**
     * Determines whether a complete candidate change set satisfies the
     * configured changed-feature bounds and contains no duplicate feature
     * names.
     *
     * This helper is not yet used by the existing single-feature path. It
     * will be used when bounded multi-feature generation is enabled.
     */
    private boolean isValidCandidateChangeSet(
            List<FeatureChange> featureChanges) {

        if(featureChanges == null
                || featureChanges.isEmpty()) {

            return false;
        }

        int changedFeatureCount =
                featureChanges.size();

        if(changedFeatureCount
                < configuration.getMinimumChangedFeatures()) {

            return false;
        }

        if(changedFeatureCount
                > configuration.getMaximumChangedFeatures()) {

            return false;
        }

        Map<String, Boolean> encounteredFeatureNames =
                new LinkedHashMap<String, Boolean>();

        for(FeatureChange featureChange
                : featureChanges) {

            if(featureChange == null) {

                return false;
            }

            if(!featureChange.isMutable()
                    || !featureChange.isActionable()
                    || !featureChange.hasChanged()
                    || !featureChange.isValidChange()) {

                return false;
            }

            String featureName =
                    featureChange.getFeatureName();

            if(encounteredFeatureNames.containsKey(
                    featureName)) {

                return false;
            }

            encounteredFeatureNames.put(
                    featureName,
                    Boolean.TRUE);
        }

        return true;
    }
    
    /**
     * Creates a deterministic key for duplicate detection.
     *
     * Feature changes are expected to follow configuration order when
     * generated. The key therefore preserves the supplied order.
     */
    private String createCandidateChangeSetKey(
            List<FeatureChange> featureChanges) {

        if(featureChanges == null
                || featureChanges.isEmpty()) {

            return "";
        }

        StringBuilder keyBuilder =
                new StringBuilder();

        for(FeatureChange featureChange
                : featureChanges) {

            if(keyBuilder.length() > 0) {

                keyBuilder.append('|');
            }

            keyBuilder
            .append(
                    featureChange.getFeatureName())
            .append('=')
            .append(
                    Double.toString(
                            featureChange
                            .getCounterfactualValue()));
        }

        return keyBuilder.toString();
    }
    
    /**
     * Creates an immutable defensive copy of one complete candidate
     * change set.
     */
    private List<FeatureChange> createImmutableChangeSet(
            List<FeatureChange> featureChanges) {

        if(!isValidCandidateChangeSet(
                featureChanges)) {

            throw new IllegalArgumentException(
                    "Candidate change set is invalid.");
        }

        return Collections.unmodifiableList(
                new ArrayList<FeatureChange>(
                        featureChanges));
    }
    /**
     * Interleaves candidates from each searchable feature.
     *
     * One candidate is taken from every feature in each round until the
     * global candidate limit is reached or all feature lists are empty.
     */
    private List<FeatureChange> interleaveFeatureCandidates(
            List<List<FeatureChange>> candidatesByFeature) {

        List<FeatureChange> balancedCandidates =
                new ArrayList<FeatureChange>();

        if(candidatesByFeature == null
                || candidatesByFeature.isEmpty()) {

            return balancedCandidates;
        }

        int maximumCandidateCount =
                configuration.getMaximumGeneratedCandidates();

        int candidateIndex =
                0;

        boolean candidateAdded =
                true;

        while(candidateAdded
                && balancedCandidates.size()
                < maximumCandidateCount) {

            candidateAdded =
                    false;

            for(List<FeatureChange> featureCandidates :
                    candidatesByFeature) {

                if(balancedCandidates.size()
                        >= maximumCandidateCount) {

                    break;
                }

                if(candidateIndex
                        < featureCandidates.size()) {

                    balancedCandidates.add(
                            featureCandidates.get(
                                    candidateIndex));

                    candidateAdded =
                            true;
                }
            }

            candidateIndex++;
        }

        return balancedCandidates;
    }

    /**
     * Generates candidate changes for one feature.
     *
     * Upload size uses relative reductions.
     * Other features use configured fixed steps.
     */
    private void generateChangesForFeature(
            ReplayState baseState,
            FeasibleCounterfactualSearchConfig.FeatureSearchSpec specification,
            List<FeatureChange> generatedChanges) {

        String featureName =
                specification
                .getFeatureName();

        double originalValue =
                getFeatureValue(
                        baseState,
                        featureName);

        if(!isFinite(originalValue)) {

            return;
        }

        /*
         * Prevent invalid search configurations from silently
         * generating candidates outside the original feature scale.
         */
        if(!specification.contains(
                originalValue)) {

            if(debugEnabled) {

                System.out.println(
                        "[Candidate Generation] "
                        + "Skipping feature "
                        + featureName
                        + " because original value "
                        + originalValue
                        + " is outside configured range ["
                        + specification.getMinimumValue()
                        + ", "
                        + specification.getMaximumValue()
                        + "].");
            }

            return;
        }

        /*
         * Required cores are discrete and should be generated only as
         * whole-number reductions.
         */
        if(CounterfactualFeatureNames
                .REQUIRED_CORES
                .equals(featureName)) {

            generateRequiredCoreChanges(
                    originalValue,
                    specification,
                    generatedChanges);

            return;
        }

        /*
         * Upload size is generated through relative payload
         * reductions instead of arbitrary fixed KB values.
         */
        if(CounterfactualFeatureNames
                .UPLOAD_SIZE_KB
                .equals(featureName)) {

            generateUploadSizeChanges(
                    originalValue,
                    specification,
                    generatedChanges);

            return;
        }

        generateStepBasedChanges(
                originalValue,
                specification,
                generatedChanges);
    }

    /**
     * Generates percentage-based upload-size reductions.
     *
     * Upload size is actionable through compression, filtering,
     * aggregation, sampling, or feature reduction.
     *
     * Only reductions are generated because increasing the upload
     * payload would generally worsen communication cost and does not
     * represent useful application-level recourse.
     */
    private void generateUploadSizeChanges(
            double originalUploadSizeKb,
            FeasibleCounterfactualSearchConfig.FeatureSearchSpec specification,
            List<FeatureChange> generatedChanges) {

        if(originalUploadSizeKb <= 0.0) {

            return;
        }

        for(double reductionRatio :
                UPLOAD_REDUCTION_RATIOS) {

            if(reachedGenerationLimit(
                    generatedChanges)) {

                return;
            }

            double counterfactualUploadSizeKb =
                    originalUploadSizeKb
                    * (1.0 - reductionRatio);

            /*
             * Payload sizes are represented as whole kilobytes.
             */
            counterfactualUploadSizeKb =
                    Math.round(
                            counterfactualUploadSizeKb);

            counterfactualUploadSizeKb =
                    normalizeBoundaryValue(
                            counterfactualUploadSizeKb,
                            specification);

            addChangeIfValid(
                    specification,
                    originalUploadSizeKb,
                    counterfactualUploadSizeKb,
                    generatedChanges);
        }
    }

    /**
     * Generates ordinary fixed-step changes for CPU, bandwidth,
     * latency, and task length.
     */
    private void generateStepBasedChanges(
            double originalValue,
            FeasibleCounterfactualSearchConfig.FeatureSearchSpec specification,
            List<FeatureChange> generatedChanges) {

        /*
         * Generate nearest lower values first.
         */
        double lowerValue =
                originalValue
                - specification
                .getStepSize();

        while(lowerValue
                >= specification
                .getMinimumValue()
                - EPSILON) {

            if(reachedGenerationLimit(
                    generatedChanges)) {

                return;
            }

            double boundedValue =
                    normalizeBoundaryValue(
                            lowerValue,
                            specification);

            addChangeIfValid(
                    specification,
                    originalValue,
                    boundedValue,
                    generatedChanges);

            lowerValue -=
                    specification
                    .getStepSize();
        }

        /*
         * Generate nearest higher values second.
         */
        double higherValue =
                originalValue
                + specification
                .getStepSize();

        while(higherValue
                <= specification
                .getMaximumValue()
                + EPSILON) {

            if(reachedGenerationLimit(
                    generatedChanges)) {

                return;
            }

            double boundedValue =
                    normalizeBoundaryValue(
                            higherValue,
                            specification);

            addChangeIfValid(
                    specification,
                    originalValue,
                    boundedValue,
                    generatedChanges);

            higherValue +=
                    specification
                    .getStepSize();
        }
    }

    /**
     * Adds a generated feature change when it is finite, different
     * from the original value, and inside the configured range.
     */
    private void addChangeIfValid(
            FeasibleCounterfactualSearchConfig.FeatureSearchSpec specification,
            double originalValue,
            double counterfactualValue,
            List<FeatureChange> generatedChanges) {

        if(!isFinite(counterfactualValue)) {

            return;
        }

        /*
         * The counterfactual value must differ from the original.
         */
        if(Math.abs(
                originalValue
                - counterfactualValue)
                <= EPSILON) {

            return;
        }

        /*
         * The generated value must remain inside the feature's
         * configured engineering-unit range.
         */
        if(!specification.contains(
                counterfactualValue)) {

            return;
        }

        FeatureChange featureChange =
                new FeatureChange(
                        specification
                        .getFeatureName(),

                        resolveFeatureCategory(
                                specification
                                .getFeatureName()),

                        originalValue,

                        counterfactualValue,

                        specification
                        .getMinimumValue(),

                        specification
                        .getMaximumValue(),

                        true,

                        true);

        generatedChanges.add(
                featureChange);

        if(debugEnabled
                && CounterfactualFeatureNames
                .UPLOAD_SIZE_KB
                .equals(
                        specification
                        .getFeatureName())) {

            double reductionPercent =
                    calculateReductionPercent(
                            originalValue,
                            counterfactualValue);

            System.out.println(
                    "[Upload Candidate] "
                    + originalValue
                    + " KB -> "
                    + counterfactualValue
                    + " KB"
                    + " ("
                    + reductionPercent
                    + "% reduction)");
        }
    }

    private boolean reachedGenerationLimit(
            List<FeatureChange> generatedChanges) {

        return generatedChanges.size()
                >= configuration
                .getMaximumGeneratedCandidates();
    }

    /**
     * Returns the current value of a searchable feature using
     * standardized engineering-unit accessors.
     */
    private double getFeatureValue(
            ReplayState replayState,
            String featureName) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        String normalizedFeatureName =
                normalizeFeatureName(
                        featureName);

        if(CounterfactualFeatureNames
                .REQUIRED_CPU_PERCENT
                .equals(normalizedFeatureName)) {

            return replayState.getRequiredCpuPercent();
        }

        if(CounterfactualFeatureNames
                .REQUIRED_CORES
                .equals(normalizedFeatureName)) {

            return replayState.getRequiredCores();
        }

        if(CounterfactualFeatureNames
                .SELECTED_AVAILABLE_CPU_PERCENT
                .equals(normalizedFeatureName)) {

            return replayState.getSelectedAvailableCpuPercent();
        }

        if(CounterfactualFeatureNames
                .BEST_AVAILABLE_CPU_PERCENT
                .equals(normalizedFeatureName)) {

            return replayState.getBestAvailableCpuPercent();
        }

        if(CounterfactualFeatureNames
                .AVAILABLE_BANDWIDTH_MBPS
                .equals(normalizedFeatureName)) {

            return replayState.getAvailableBandwidthMbps();
        }

        if(CounterfactualFeatureNames
                .NETWORK_LATENCY_MS
                .equals(normalizedFeatureName)) {

            return replayState.getNetworkLatencyMs();
        }

        if(CounterfactualFeatureNames
                .TASK_LENGTH_MI
                .equals(normalizedFeatureName)) {

            return replayState.getTaskLengthMi();
        }

        if(CounterfactualFeatureNames
                .UPLOAD_SIZE_KB
                .equals(normalizedFeatureName)) {

            return replayState.getUploadSizeKb();
        }

        throw new IllegalArgumentException(
                "Unsupported searchable feature: "
                + featureName);
    }

    /**
     * Assigns each feature to its conceptual category.
     */
    private FeatureCategory resolveFeatureCategory(
            String featureName) {

        if(CounterfactualFeatureNames
                .REQUIRED_CPU_PERCENT
                .equals(featureName)
                || CounterfactualFeatureNames
                .REQUIRED_CORES
                .equals(featureName)
                || CounterfactualFeatureNames
                .SELECTED_AVAILABLE_CPU_PERCENT
                .equals(featureName)
                || CounterfactualFeatureNames
                .BEST_AVAILABLE_CPU_PERCENT
                .equals(featureName)) {

            return FeatureCategory.COMPUTE;
        }

        if(CounterfactualFeatureNames
                .AVAILABLE_BANDWIDTH_MBPS
                .equals(featureName)
                || CounterfactualFeatureNames
                .NETWORK_LATENCY_MS
                .equals(featureName)) {

            return FeatureCategory.NETWORK;
        }

        if(CounterfactualFeatureNames
                .TASK_LENGTH_MI
                .equals(featureName)
                || CounterfactualFeatureNames
                .UPLOAD_SIZE_KB
                .equals(featureName)) {

            return FeatureCategory.TASK;
        }

        throw new IllegalArgumentException(
                "No feature category exists for: "
                + featureName);
    }

    /**
     * Normalizes values that are extremely close to a configured
     * minimum or maximum.
     */
    private double normalizeBoundaryValue(
            double value,
            FeasibleCounterfactualSearchConfig.FeatureSearchSpec specification) {

        if(Math.abs(
                value
                - specification
                .getMinimumValue())
                <= EPSILON) {

            return specification
                    .getMinimumValue();
        }

        if(Math.abs(
                value
                - specification
                .getMaximumValue())
                <= EPSILON) {

            return specification
                    .getMaximumValue();
        }

        return value;
    }

    private double calculateReductionPercent(
            double originalValue,
            double counterfactualValue) {

        if(originalValue <= EPSILON) {

            return 0.0;
        }

        double reductionRatio =
                (originalValue
                        - counterfactualValue)
                / originalValue;

        return Math.round(
                reductionRatio
                * 10000.0)
                / 100.0;
    }

    /**
     * Prints the number of generated changes for every configured
     * searchable feature.
     */
    private void printGenerationSummary(
            List<FeatureChange> generatedChanges) {

        Map<String, Integer> countsByFeature =
                new LinkedHashMap<String, Integer>();

        /*
         * Initialize every configured searchable feature with zero.
         */
        for(FeasibleCounterfactualSearchConfig.FeatureSearchSpec specification
                : configuration
                .getFeatureSearchSpecifications()) {

            countsByFeature.put(
                    specification
                    .getFeatureName(),
                    0);
        }

        /*
         * Count generated changes by feature.
         */
        for(FeatureChange change :
                generatedChanges) {

            String featureName =
                    change
                    .getFeatureName();

            Integer currentCount =
                    countsByFeature
                    .get(featureName);

            if(currentCount == null) {

                countsByFeature.put(
                        featureName,
                        1);
            }
            else {

                countsByFeature.put(
                        featureName,
                        currentCount + 1);
            }
        }

        System.out.println();
        System.out.println(
                "Generated candidates by feature:");

        for(Map.Entry<String, Integer> entry :
                countsByFeature.entrySet()) {

            System.out.printf(
                    "%-40s : %d%n",
                    entry.getKey(),
                    entry.getValue());
        }

        System.out.println(
                "------------------------------------------------");

        System.out.printf(
                "%-40s : %d%n",
                "Total",
                generatedChanges.size());

        System.out.println();
    }
    
    /**
     * Prints the distribution of complete candidate change sets by their
     * number of changed features.
     */
    private void printCandidateChangeSetSummary(
            List<List<FeatureChange>> candidateChangeSets) {

        Map<Integer, Integer> countsByChangedFeatureCount =
                new LinkedHashMap<Integer, Integer>();

        for(int changedFeatureCount =
                configuration.getMinimumChangedFeatures();

                changedFeatureCount
                <= configuration.getMaximumChangedFeatures();

                changedFeatureCount++) {

            countsByChangedFeatureCount.put(
                    changedFeatureCount,
                    0);
        }

        for(List<FeatureChange> candidateChangeSet
                : candidateChangeSets) {

            int changedFeatureCount =
                    candidateChangeSet.size();

            Integer existingCount =
                    countsByChangedFeatureCount.get(
                            changedFeatureCount);

            if(existingCount == null) {

                countsByChangedFeatureCount.put(
                        changedFeatureCount,
                        1);
            }
            else {

                countsByChangedFeatureCount.put(
                        changedFeatureCount,
                        existingCount + 1);
            }
        }

        System.out.println();
        System.out.println(
                "Generated candidate sets by changed-feature count:");

        for(Map.Entry<Integer, Integer> entry
                : countsByChangedFeatureCount.entrySet()) {

            System.out.printf(
                    "%d changed feature(s)                  : %d%n",
                    entry.getKey(),
                    entry.getValue());
        }

        System.out.println(
                "------------------------------------------------");

        System.out.printf(
                "%-38s : %d%n",
                "Total candidate sets",
                candidateChangeSets.size());

        System.out.println();
    }
    
    /**
     * Generates whole-number reductions in required task cores.
     */
    private void generateRequiredCoreChanges(
            double originalValue,
            FeasibleCounterfactualSearchConfig
                    .FeatureSearchSpec specification,
            List<FeatureChange> generatedChanges) {

        int originalCoreCount =
                toPositiveInteger(
                        originalValue,
                        CounterfactualFeatureNames.REQUIRED_CORES);

        int minimumCoreCount =
                (int)Math.ceil(
                        specification.getMinimumValue());

        for(int newCoreCount = originalCoreCount - 1;
                newCoreCount >= minimumCoreCount;
                newCoreCount--) {

            if(reachedGenerationLimit(
                    generatedChanges)) {

                return;
            }

            addChangeIfValid(
                    specification,
                    originalValue,
                    newCoreCount,
                    generatedChanges);
        }
    }

    private static int toPositiveInteger(
            double value,
            String featureName) {

        if(!isFinite(value)) {

            throw new IllegalArgumentException(
                    featureName
                    + " must be finite.");
        }

        if(value < 1.0) {

            throw new IllegalArgumentException(
                    featureName
                    + " must be at least 1.");
        }

        if(value != Math.floor(value)) {

            throw new IllegalArgumentException(
                    featureName
                    + " must be an integer.");
        }

        if(value > Integer.MAX_VALUE) {

            throw new IllegalArgumentException(
                    featureName
                    + " exceeds the supported integer range.");
        }

        return (int)value;
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
}
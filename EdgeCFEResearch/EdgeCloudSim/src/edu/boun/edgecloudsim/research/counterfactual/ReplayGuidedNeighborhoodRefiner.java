package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.boun.edgecloudsim.research.counterfactual
        .FeasibleCounterfactualSearchConfig
        .FeatureSearchSpec;

/**
 * Generates bounded local counterfactual neighborhoods around
 * replay-boundary candidates.
 *
 * A replay-boundary candidate is an evaluated candidate with the
 * INFEASIBLE_EFFECTIVE classification:
 *
 * 1. The requested scheduler decision change occurred.
 * 2. The resulting state was not operationally feasible.
 *
 * A boundary candidate may contain one or more feature changes.
 *
 * The refiner generates:
 *
 * 1. Joint refinements:
 *    all changed features move toward their original values.
 *
 * 2. Coordinate-wise refinements:
 *    one changed feature moves toward its original value while the
 *    other feature changes remain at their boundary values.
 *
 * This class performs candidate generation only. It does not apply
 * changes to ReplayState, evaluate feasibility, replay the scheduler,
 * classify candidates, or calculate objective scores.
 */
public final class ReplayGuidedNeighborhoodRefiner {

    private static final double VALUE_TOLERANCE =
            0.000000001;

    /**
     * Identifies how a neighborhood candidate was generated.
     */
    public enum RefinementType {

        JOINT,
        COORDINATE_WISE
    }

    /**
     * Immutable description of one locally generated neighborhood
     * candidate.
     *
     * Each specification preserves:
     *
     * 1. The boundary-seed candidate ID.
     * 2. The complete list of feature changes.
     * 3. The type of local refinement.
     * 4. The coordinate feature for coordinate-wise refinement.
     */
    public static final class NeighborhoodCandidateSpec {

        private final int neighborhoodCandidateIndex;

        private final int seedCandidateId;

        private final List<FeatureChange>
                featureChanges;

        private final RefinementType
                refinementType;

        private final String
                refinedFeatureName;

        private NeighborhoodCandidateSpec(
                int neighborhoodCandidateIndex,
                int seedCandidateId,
                List<FeatureChange> featureChanges,
                RefinementType refinementType,
                String refinedFeatureName) {

            if(neighborhoodCandidateIndex < 0) {

                throw new IllegalArgumentException(
                        "Neighborhood candidate index cannot "
                        + "be negative.");
            }

            if(seedCandidateId < 0) {

                throw new IllegalArgumentException(
                        "Seed candidate ID cannot be negative.");
            }

            if(featureChanges == null
                    || featureChanges.isEmpty()) {

                throw new IllegalArgumentException(
                        "Neighborhood feature-change list cannot "
                        + "be null or empty.");
            }

            if(refinementType == null) {

                throw new IllegalArgumentException(
                        "Refinement type cannot be null.");
            }

            this.neighborhoodCandidateIndex =
                    neighborhoodCandidateIndex;

            this.seedCandidateId =
                    seedCandidateId;

            this.featureChanges =
                    Collections.unmodifiableList(
                            new ArrayList<FeatureChange>(
                                    featureChanges));

            this.refinementType =
                    refinementType;

            this.refinedFeatureName =
                    normalizeOptionalText(
                            refinedFeatureName);
        }

        public int getNeighborhoodCandidateIndex() {

            return neighborhoodCandidateIndex;
        }

        public int getSeedCandidateId() {

            return seedCandidateId;
        }

        /**
         * Returns every feature change in the locally generated
         * counterfactual candidate.
         */
        public List<FeatureChange> getFeatureChanges() {

            return featureChanges;
        }

        public int getChangedFeatureCount() {

            return featureChanges.size();
        }

        public RefinementType getRefinementType() {

            return refinementType;
        }

        /**
         * Returns the individually refined feature for a coordinate-wise
         * candidate.
         *
         * For joint refinement, this returns "ALL_FEATURES".
         */
        public String getRefinedFeatureName() {

            return refinedFeatureName;
        }

        private static String normalizeOptionalText(
                String value) {

            if(value == null
                    || value.trim().isEmpty()) {

                return "ALL_FEATURES";
            }

            return value.trim();
        }

        @Override
        public String toString() {

            return "NeighborhoodCandidateSpec{"
                    + "neighborhoodCandidateIndex="
                    + neighborhoodCandidateIndex

                    + ", seedCandidateId="
                    + seedCandidateId

                    + ", changedFeatureCount="
                    + featureChanges.size()

                    + ", refinementType="
                    + refinementType

                    + ", refinedFeatureName='"
                    + refinedFeatureName
                    + '\''

                    + ", featureChanges="
                    + featureChanges

                    + '}';
        }
    }

    /**
     * Generates a bounded neighborhood around the best available
     * INFEASIBLE_EFFECTIVE boundary candidates.
     *
     * Boundary candidates are ranked using the natural ordering of
     * CounterfactualCandidate. Lower-scoring candidates are preferred.
     */
    public List<NeighborhoodCandidateSpec> refine(
            List<CounterfactualCandidate> evaluatedCandidates,
            FeasibleCounterfactualSearchConfig configuration) {

        validateInputs(
                evaluatedCandidates,
                configuration);

        if(!configuration
                .isNeighborhoodRefinementEnabled()) {

            return Collections.emptyList();
        }

        List<CounterfactualCandidate> boundarySeeds =
                selectBoundarySeeds(
                        evaluatedCandidates,
                        configuration);

        if(boundarySeeds.isEmpty()) {

            return Collections.emptyList();
        }

        List<NeighborhoodCandidateSpec> generatedNeighborhood =
                new ArrayList<NeighborhoodCandidateSpec>();

        /*
         * Prevent identical multidimensional counterfactual states from
         * being generated by different seeds or refinement paths.
         */
        Set<String> generatedCandidateKeys =
                new LinkedHashSet<String>();

        int neighborhoodCandidateIndex =
                0;

        for(CounterfactualCandidate boundarySeed
                : boundarySeeds) {

            if(generatedNeighborhood.size()
                    >= configuration
                    .getMaximumNeighborhoodCandidates()) {

                break;
            }

            int remainingGlobalCapacity =
                    configuration
                    .getMaximumNeighborhoodCandidates()
                    - generatedNeighborhood.size();

            int requestedForSeed =
                    Math.min(
                            configuration
                            .getNeighborhoodSamplesPerSeed(),
                            remainingGlobalCapacity);

            List<GeneratedNeighborhood> seedNeighborhood =
                    generateSeedNeighborhood(
                            boundarySeed,
                            configuration,
                            requestedForSeed);

            for(GeneratedNeighborhood generatedCandidate
                    : seedNeighborhood) {

                if(generatedNeighborhood.size()
                        >= configuration
                        .getMaximumNeighborhoodCandidates()) {

                    break;
                }

                String candidateKey =
                        createCandidateKey(
                                generatedCandidate
                                .featureChanges);

                if(!generatedCandidateKeys.add(
                        candidateKey)) {

                    continue;
                }

                generatedNeighborhood.add(
                        new NeighborhoodCandidateSpec(
                                neighborhoodCandidateIndex,

                                boundarySeed
                                .getCandidateId(),

                                generatedCandidate
                                .featureChanges,

                                generatedCandidate
                                .refinementType,

                                generatedCandidate
                                .refinedFeatureName));

                neighborhoodCandidateIndex++;
            }
        }

        return Collections.unmodifiableList(
                generatedNeighborhood);
    }

    /**
     * Selects the best eligible replay-boundary candidates.
     *
     * A seed may contain one or more feature changes.
     */
    private List<CounterfactualCandidate> selectBoundarySeeds(
            List<CounterfactualCandidate> evaluatedCandidates,
            FeasibleCounterfactualSearchConfig configuration) {

        List<CounterfactualCandidate> eligibleSeeds =
                new ArrayList<CounterfactualCandidate>();

        Set<String> uniqueSeedKeys =
                new LinkedHashSet<String>();

        for(CounterfactualCandidate candidate
                : evaluatedCandidates) {

            if(!isEligibleBoundarySeed(
                    candidate,
                    configuration)) {

                continue;
            }

            String seedKey =
                    createCandidateKey(
                            candidate
                            .getFeatureChanges());

            /*
             * Retain only one boundary seed representing an identical
             * multidimensional counterfactual state.
             */
            if(!uniqueSeedKeys.add(
                    seedKey)) {

                continue;
            }

            eligibleSeeds.add(
                    candidate);
        }

        Collections.sort(
                eligibleSeeds,
                new Comparator<CounterfactualCandidate>() {

                    @Override
                    public int compare(
                            CounterfactualCandidate first,
                            CounterfactualCandidate second) {

                        return first.compareTo(
                                second);
                    }
                });

        int selectedSeedCount =
                Math.min(
                        configuration
                        .getMaximumBoundarySeeds(),
                        eligibleSeeds.size());

        if(selectedSeedCount <= 0) {

            return Collections.emptyList();
        }

        return new ArrayList<CounterfactualCandidate>(
                eligibleSeeds.subList(
                        0,
                        selectedSeedCount));
    }

    /**
     * Determines whether a candidate can act as a neighborhood seed.
     *
     * All changes in the candidate must be:
     *
     * 1. Real changes.
     * 2. Mutable.
     * 3. Actionable.
     * 4. Within a configured searchable feature.
     */
    private boolean isEligibleBoundarySeed(
            CounterfactualCandidate candidate,
            FeasibleCounterfactualSearchConfig configuration) {

        if(candidate == null) {

            return false;
        }

        if(!candidate.isEvaluated()) {

            return false;
        }

        if(candidate.getClassification()
                != CounterfactualClassification
                .INFEASIBLE_EFFECTIVE) {

            return false;
        }

        /*
         * A valid counterfactual must change at least one feature.
         *
         * There is no single-feature-only restriction.
         */
        if(candidate.getChangedFeatureCount()
                < 1) {

            return false;
        }

        for(FeatureChange featureChange
                : candidate.getFeatureChanges()) {

            if(featureChange == null) {

                return false;
            }

            if(!featureChange.hasChanged()) {

                return false;
            }

            if(!featureChange.isMutable()) {

                return false;
            }

            if(!featureChange.isActionable()) {

                return false;
            }

            FeatureSearchSpec specification =
                    configuration
                    .findFeatureSearchSpec(
                            featureChange
                            .getFeatureName());

            if(specification == null) {

                return false;
            }

            if(!specification.contains(
                    featureChange
                    .getOriginalValue())) {

                return false;
            }

            if(!specification.contains(
                    featureChange
                    .getCounterfactualValue())) {

                return false;
            }
        }

        return true;
    }

    /**
     * Generates both joint and coordinate-wise candidates for one
     * boundary seed.
     */
    private List<GeneratedNeighborhood> generateSeedNeighborhood(
            CounterfactualCandidate boundarySeed,
            FeasibleCounterfactualSearchConfig configuration,
            int requestedCandidateCount) {

        if(requestedCandidateCount <= 0) {

            return Collections.emptyList();
        }

        List<FeatureChange> seedChanges =
                boundarySeed
                .getFeatureChanges();

        if(seedChanges == null
                || seedChanges.isEmpty()) {

            return Collections.emptyList();
        }

        List<GeneratedNeighborhood> generated =
                new ArrayList<GeneratedNeighborhood>();

        Set<String> localCandidateKeys =
                new LinkedHashSet<String>();

        /*
         * For a single-feature seed, joint and coordinate-wise
         * interpolation produce the same states. Generate only joint
         * refinements to avoid duplicates.
         */
        if(seedChanges.size() == 1) {

            generateJointRefinements(
                    seedChanges,
                    configuration,
                    requestedCandidateCount,
                    generated,
                    localCandidateKeys);

            return generated;
        }

        /*
         * For a multi-feature seed, divide the per-seed allowance
         * between:
         *
         * 1. Joint refinement.
         * 2. Coordinate-wise refinement.
         */
        int jointCandidateLimit =
                Math.max(
                        1,
                        requestedCandidateCount
                        / 2);

        int coordinateCandidateLimit =
                requestedCandidateCount
                - jointCandidateLimit;

        generateJointRefinements(
                seedChanges,
                configuration,
                jointCandidateLimit,
                generated,
                localCandidateKeys);

        generateCoordinateWiseRefinements(
                seedChanges,
                configuration,
                coordinateCandidateLimit,
                generated,
                localCandidateKeys);

        /*
         * If duplicate prevention or range checks caused either phase
         * to generate fewer candidates than requested, attempt
         * additional coordinate-wise candidates.
         */
        int remainingCapacity =
                requestedCandidateCount
                - generated.size();

        if(remainingCapacity > 0) {

            generateCoordinateWiseRefinements(
                    seedChanges,
                    configuration,
                    remainingCapacity,
                    generated,
                    localCandidateKeys);
        }

        /*
         * If capacity still remains, attempt additional joint values
         * using a denser interpolation grid.
         */
        remainingCapacity =
                requestedCandidateCount
                - generated.size();

        if(remainingCapacity > 0) {

            generateJointRefinements(
                    seedChanges,
                    configuration,
                    remainingCapacity,
                    generated,
                    localCandidateKeys);
        }

        if(generated.size()
                > requestedCandidateCount) {

            return new ArrayList<GeneratedNeighborhood>(
                    generated.subList(
                            0,
                            requestedCandidateCount));
        }

        return generated;
    }

    /**
     * Joint refinement moves every changed feature toward its original
     * value using a shared interpolation fraction.
     *
     * Example:
     *
     * CPU seed:       100 -> 60
     * latency seed:    20 -> 40
     *
     * At fraction 0.5:
     *
     * CPU:             100 -> 80
     * latency:          20 -> 30
     */
    private void generateJointRefinements(
            List<FeatureChange> seedChanges,
            FeasibleCounterfactualSearchConfig configuration,
            int requestedCount,
            List<GeneratedNeighborhood> output,
            Set<String> generatedKeys) {

        if(requestedCount <= 0) {

            return;
        }

        int denominator =
                requestedCount
                + 1;

        for(int sampleIndex = 1;
                sampleIndex <= requestedCount;
                sampleIndex++) {

            double fractionTowardOriginal =
                    sampleIndex
                    / (double) denominator;

            List<FeatureChange> refinedChanges =
                    new ArrayList<FeatureChange>();

            boolean validCandidate =
                    true;

            for(FeatureChange seedChange
                    : seedChanges) {

                FeatureChange refinedChange =
                        interpolateChange(
                                seedChange,
                                configuration,
                                fractionTowardOriginal);

                if(refinedChange == null
                        || !refinedChange.hasChanged()
                        || !refinedChange.isValidChange()) {

                    validCandidate =
                            false;

                    break;
                }

                refinedChanges.add(
                        refinedChange);
            }

            if(!validCandidate
                    || refinedChanges.isEmpty()) {

                continue;
            }

            String candidateKey =
                    createCandidateKey(
                            refinedChanges);

            if(!generatedKeys.add(
                    candidateKey)) {

                continue;
            }

            output.add(
                    new GeneratedNeighborhood(
                            refinedChanges,
                            RefinementType.JOINT,
                            "ALL_FEATURES"));
        }
    }

    /**
     * Coordinate-wise refinement moves one changed feature toward its
     * original value while preserving all other seed changes.
     *
     * Features are refined in round-robin order so one feature cannot
     * consume the complete per-seed neighborhood allowance.
     */
    private void generateCoordinateWiseRefinements(
            List<FeatureChange> seedChanges,
            FeasibleCounterfactualSearchConfig configuration,
            int requestedCount,
            List<GeneratedNeighborhood> output,
            Set<String> generatedKeys) {

        if(requestedCount <= 0
                || seedChanges.isEmpty()) {

            return;
        }

        int generatedDuringMethod =
                0;

        int featureCount =
                seedChanges.size();

        /*
         * Several rounds may be necessary when the requested candidate
         * count exceeds the number of changed features.
         */
        int maximumRounds =
                requestedCount
                + featureCount
                + 1;

        for(int roundIndex = 1;
                roundIndex <= maximumRounds
                && generatedDuringMethod < requestedCount;
                roundIndex++) {

            double fractionTowardOriginal =
                    roundIndex
                    / (double) (maximumRounds + 1);

            for(int featureIndex = 0;
                    featureIndex < featureCount
                    && generatedDuringMethod < requestedCount;
                    featureIndex++) {

                List<FeatureChange> refinedChanges =
                        new ArrayList<FeatureChange>();

                boolean validCandidate =
                        true;

                for(int changeIndex = 0;
                        changeIndex < featureCount;
                        changeIndex++) {

                    FeatureChange seedChange =
                            seedChanges
                            .get(changeIndex);

                    FeatureChange candidateChange;

                    if(changeIndex == featureIndex) {

                        candidateChange =
                                interpolateChange(
                                        seedChange,
                                        configuration,
                                        fractionTowardOriginal);
                    }
                    else {

                        candidateChange =
                                copyFeatureChange(
                                        seedChange,
                                        configuration);
                    }

                    if(candidateChange == null
                            || !candidateChange.hasChanged()
                            || !candidateChange.isValidChange()) {

                        validCandidate =
                                false;

                        break;
                    }

                    refinedChanges.add(
                            candidateChange);
                }

                if(!validCandidate
                        || refinedChanges.isEmpty()) {

                    continue;
                }

                String candidateKey =
                        createCandidateKey(
                                refinedChanges);

                if(!generatedKeys.add(
                        candidateKey)) {

                    continue;
                }

                output.add(
                        new GeneratedNeighborhood(
                                refinedChanges,

                                RefinementType
                                .COORDINATE_WISE,

                                seedChanges
                                .get(featureIndex)
                                .getFeatureName()));

                generatedDuringMethod++;
            }
        }
    }

    /**
     * Creates one feature change at an interior point between the seed
     * value and the original value.
     */
    private FeatureChange interpolateChange(
            FeatureChange seedChange,
            FeasibleCounterfactualSearchConfig configuration,
            double fractionTowardOriginal) {

        if(seedChange == null) {

            return null;
        }

        FeatureSearchSpec specification =
                configuration
                .findFeatureSearchSpec(
                        seedChange
                        .getFeatureName());

        if(specification == null) {

            return null;
        }

        double boundedFraction =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                fractionTowardOriginal));

        double seedValue =
                seedChange
                .getCounterfactualValue();

        double originalValue =
                seedChange
                .getOriginalValue();

        double refinedValue =
                seedValue
                + ((originalValue
                - seedValue)
                * boundedFraction);

        refinedValue =
                clamp(
                        refinedValue,
                        specification
                        .getMinimumValue(),
                        specification
                        .getMaximumValue());

        /*
         * The original endpoint is not a changed feature and must not
         * be returned as a counterfactual modification.
         */
        if(isApproximatelyEqual(
                refinedValue,
                originalValue)) {

            return null;
        }

        /*
         * The seed endpoint has already been evaluated.
         */
        if(isApproximatelyEqual(
                refinedValue,
                seedValue)) {

            return null;
        }

        if(!specification.contains(
                refinedValue)) {

            return null;
        }

        return new FeatureChange(
                seedChange
                .getFeatureName(),

                seedChange
                .getCategory(),

                originalValue,

                refinedValue,

                specification
                .getMinimumValue(),

                specification
                .getMaximumValue(),

                seedChange
                .isMutable(),

                seedChange
                .isActionable());
    }

    /**
     * Copies an unchanged seed coordinate into a newly generated
     * multidimensional neighborhood candidate.
     */
    private FeatureChange copyFeatureChange(
            FeatureChange seedChange,
            FeasibleCounterfactualSearchConfig configuration) {

        if(seedChange == null) {

            return null;
        }

        FeatureSearchSpec specification =
                configuration
                .findFeatureSearchSpec(
                        seedChange
                        .getFeatureName());

        if(specification == null) {

            return null;
        }

        return new FeatureChange(
                seedChange
                .getFeatureName(),

                seedChange
                .getCategory(),

                seedChange
                .getOriginalValue(),

                seedChange
                .getCounterfactualValue(),

                specification
                .getMinimumValue(),

                specification
                .getMaximumValue(),

                seedChange
                .isMutable(),

                seedChange
                .isActionable());
    }

    private static double clamp(
            double value,
            double minimum,
            double maximum) {

        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value));
    }

    private static boolean isApproximatelyEqual(
            double first,
            double second) {

        return Math.abs(
                first
                - second)
                <= VALUE_TOLERANCE;
    }

    /**
     * Generates a stable key for a complete multidimensional
     * counterfactual candidate.
     *
     * Feature changes are sorted by feature name so equivalent states
     * have the same key regardless of list order.
     */
    private static String createCandidateKey(
            List<FeatureChange> featureChanges) {

        if(featureChanges == null
                || featureChanges.isEmpty()) {

            return "EMPTY";
        }

        List<FeatureChange> sortedChanges =
                new ArrayList<FeatureChange>(
                        featureChanges);

        Collections.sort(
                sortedChanges,
                new Comparator<FeatureChange>() {

                    @Override
                    public int compare(
                            FeatureChange first,
                            FeatureChange second) {

                        return first
                                .getFeatureName()
                                .compareTo(
                                        second
                                        .getFeatureName());
                    }
                });

        StringBuilder keyBuilder =
                new StringBuilder();

        for(FeatureChange featureChange
                : sortedChanges) {

            if(keyBuilder.length() > 0) {

                keyBuilder.append(
                        '|');
            }

            keyBuilder.append(
                    featureChange
                    .getFeatureName());

            keyBuilder.append(
                    ':');

            keyBuilder.append(
                    createNumericKey(
                            featureChange
                            .getCounterfactualValue()));
        }

        return keyBuilder.toString();
    }

    private static String createNumericKey(
            double value) {

        long roundedValue =
                Math.round(
                        value
                        * 1000000000.0);

        return Long.toString(
                roundedValue);
    }

    private void validateInputs(
            List<CounterfactualCandidate> evaluatedCandidates,
            FeasibleCounterfactualSearchConfig configuration) {

        if(evaluatedCandidates == null) {

            throw new IllegalArgumentException(
                    "Evaluated candidate list cannot be null.");
        }

        if(configuration == null) {

            throw new IllegalArgumentException(
                    "Counterfactual search configuration "
                    + "cannot be null.");
        }
    }

    /**
     * Internal representation used while generating a neighborhood.
     */
    private static final class GeneratedNeighborhood {

        private final List<FeatureChange>
                featureChanges;

        private final RefinementType
                refinementType;

        private final String
                refinedFeatureName;

        private GeneratedNeighborhood(
                List<FeatureChange> featureChanges,
                RefinementType refinementType,
                String refinedFeatureName) {

            this.featureChanges =
                    new ArrayList<FeatureChange>(
                            featureChanges);

            this.refinementType =
                    refinementType;

            this.refinedFeatureName =
                    refinedFeatureName;
        }
    }
}
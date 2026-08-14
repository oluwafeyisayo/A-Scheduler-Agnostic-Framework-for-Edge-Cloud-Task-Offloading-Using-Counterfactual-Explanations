package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates bounded local counterfactual candidates around promising
 * feasible-effective seed candidates.
 *
 * Refinement moves each changed feature proportionally from the seed
 * counterfactual value toward its original value.
 *
 * For example:
 *
 * original = 100
 * seed     = 80
 *
 * Local refinement may generate:
 *
 * 100 -> 97.78
 * 100 -> 95.56
 * 100 -> 93.33
 * ...
 *
 * The generator does not perform feasibility checking or scheduler
 * replay. It only constructs deterministic local feature-change sets.
 */
public final class NeighborhoodRefinementGenerator {

    private static final double CHANGE_EPSILON =
            0.0001;

    private final FeasibleCounterfactualSearchConfig
            config;

    public NeighborhoodRefinementGenerator(
            FeasibleCounterfactualSearchConfig config) {

        if(config == null) {

            throw new IllegalArgumentException(
                    "Search configuration cannot be null.");
        }

        this.config =
                config;
    }

    /**
     * Generates local candidate change sets from the best accepted
     * counterfactual candidates.
     *
     * Candidate generation is bounded by:
     *
     * 1. maximumBoundarySeeds
     * 2. neighborhoodSamplesPerSeed
     * 3. maximumNeighborhoodCandidates
     */
    public List<List<FeatureChange>>
            generateNeighborhoodChangeSets(
                    List<CounterfactualCandidate>
                            acceptedCandidates) {

        if(!config.isNeighborhoodRefinementEnabled()) {

            return Collections.emptyList();
        }

        if(acceptedCandidates == null
                || acceptedCandidates.isEmpty()) {

            return Collections.emptyList();
        }

        List<CounterfactualCandidate> seeds =
                selectBoundarySeeds(
                        acceptedCandidates);

        if(seeds.isEmpty()) {

            return Collections.emptyList();
        }

        List<List<FeatureChange>> generatedChangeSets =
                new ArrayList<List<FeatureChange>>();

        Set<String> generatedKeys =
                new HashSet<String>();

        for(CounterfactualCandidate seed
                : seeds) {

            if(generatedChangeSets.size()
                    >= config
                    .getMaximumNeighborhoodCandidates()) {

                break;
            }

            generateFromSeed(
                    seed,
                    generatedChangeSets,
                    generatedKeys);
        }

        return Collections.unmodifiableList(
                generatedChangeSets);
    }

    /**
     * Selects the lowest-scoring accepted candidates as local-search
     * seeds.
     *
     * Candidate comparison already prefers:
     *
     * 1. lower objective score;
     * 2. fewer changed features;
     * 3. lower candidate ID.
     */
    private List<CounterfactualCandidate>
            selectBoundarySeeds(
                    List<CounterfactualCandidate>
                            acceptedCandidates) {

        List<CounterfactualCandidate> validCandidates =
                new ArrayList<CounterfactualCandidate>();

        for(CounterfactualCandidate candidate
                : acceptedCandidates) {

            if(candidate == null) {

                continue;
            }

            if(!candidate.isAccepted()
                    || !candidate.isEvaluated()
                    || !candidate.hasScore()
                    || candidate
                    .getFeatureChanges()
                    .isEmpty()) {

                continue;
            }

            validCandidates.add(
                    candidate);
        }

        Collections.sort(
                validCandidates,
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
                        config.getMaximumBoundarySeeds(),
                        validCandidates.size());

        return new ArrayList<CounterfactualCandidate>(
                validCandidates.subList(
                        0,
                        selectedSeedCount));
    }

    private void generateFromSeed(
            CounterfactualCandidate seed,
            List<List<FeatureChange>> generatedChangeSets,
            Set<String> generatedKeys) {

        int sampleCount =
                config.getNeighborhoodSamplesPerSeed();

        /*
         * Samples are generated from closest to the original state
         * toward the seed.
         *
         * For eight samples, interpolation ratios are:
         *
         * 1/9, 2/9, ..., 8/9.
         *
         * Ratio 0 would reproduce the unchanged original state.
         * Ratio 1 would reproduce the existing seed.
         * Both endpoints are therefore excluded.
         */
        for(int sampleIndex = 1;
                sampleIndex <= sampleCount;
                sampleIndex++) {

            if(generatedChangeSets.size()
                    >= config
                    .getMaximumNeighborhoodCandidates()) {

                return;
            }

            double interpolationRatio =
                    (double)sampleIndex
                    / (double)(sampleCount + 1);

            List<FeatureChange> refinedChanges =
                    refineSeedChanges(
                            seed.getFeatureChanges(),
                            interpolationRatio);

            if(!isValidRefinedChangeSet(
                    refinedChanges)) {

                continue;
            }

            String key =
                    createChangeSetKey(
                            refinedChanges);

            if(generatedKeys.add(
                    key)) {

                generatedChangeSets.add(
                        Collections.unmodifiableList(
                                refinedChanges));
            }
        }
    }

    /**
     * Moves every feature change proportionally toward its original
     * value.
     *
     * All features in a multi-feature seed use the same interpolation
     * ratio. This preserves the direction and relative composition of
     * the seed while reducing its total perturbation.
     */
    private List<FeatureChange> refineSeedChanges(
            List<FeatureChange> seedChanges,
            double interpolationRatio) {

        List<FeatureChange> refinedChanges =
                new ArrayList<FeatureChange>();

        for(FeatureChange seedChange
                : seedChanges) {

            if(seedChange == null
                    || !seedChange.hasChanged()) {

                continue;
            }

            double originalValue =
                    seedChange.getOriginalValue();

            double seedValue =
                    seedChange.getCounterfactualValue();

            double refinedValue =
                    originalValue
                    + ((seedValue - originalValue)
                    * interpolationRatio);

            refinedValue =
                    clamp(
                            refinedValue,
                            seedChange
                            .getMinimumAllowedValue(),
                            seedChange
                            .getMaximumAllowedValue());

            FeatureChange refinedChange =
                    new FeatureChange(
                            seedChange.getFeatureName(),
                            seedChange.getCategory(),
                            originalValue,
                            refinedValue,
                            seedChange
                            .getMinimumAllowedValue(),
                            seedChange
                            .getMaximumAllowedValue(),
                            seedChange.isMutable(),
                            seedChange.isActionable());

            if(refinedChange.hasChanged()) {

                refinedChanges.add(
                        refinedChange);
            }
        }

        return refinedChanges;
    }

    private boolean isValidRefinedChangeSet(
            List<FeatureChange> changes) {

        if(changes == null
                || changes.isEmpty()) {

            return false;
        }

        if(changes.size()
                < config.getMinimumChangedFeatures()) {

            return false;
        }

        if(changes.size()
                > config.getMaximumChangedFeatures()) {

            return false;
        }

        Set<String> featureNames =
                new HashSet<String>();

        for(FeatureChange change
                : changes) {

            if(change == null
                    || !change.hasChanged()
                    || !change.isValidChange()) {

                return false;
            }

            String normalizedFeatureName =
                    normalizeFeatureName(
                            change.getFeatureName());

            if(!featureNames.add(
                    normalizedFeatureName)) {

                return false;
            }
        }

        return true;
    }

    /**
     * Creates a deterministic key for duplicate prevention.
     */
    private String createChangeSetKey(
            List<FeatureChange> changes) {

        List<String> components =
                new ArrayList<String>();

        for(FeatureChange change
                : changes) {

            String component =
                    normalizeFeatureName(
                            change.getFeatureName())
                    + "="
                    + canonicalValue(
                            change
                            .getCounterfactualValue());

            components.add(
                    component);
        }

        Collections.sort(
                components);

        StringBuilder key =
                new StringBuilder();

        for(String component
                : components) {

            if(key.length() > 0) {

                key.append(
                        "|");
            }

            key.append(
                    component);
        }

        return key.toString();
    }

    /**
     * Rounds only for duplicate-key construction.
     *
     * The actual FeatureChange retains the full double value.
     */
    private String canonicalValue(
            double value) {

        double rounded =
                Math.rint(
                        value * 1000000.0)
                / 1000000.0;

        return Double.toString(
                rounded);
    }

    private double clamp(
            double value,
            double minimumValue,
            double maximumValue) {

        return Math.max(
                minimumValue,
                Math.min(
                        maximumValue,
                        value));
    }

    private String normalizeFeatureName(
            String featureName) {

        if(featureName == null) {

            return "";
        }

        return featureName
                .trim()
                .toLowerCase()
                .replace(' ', '_')
                .replace('-', '_');
    }
}
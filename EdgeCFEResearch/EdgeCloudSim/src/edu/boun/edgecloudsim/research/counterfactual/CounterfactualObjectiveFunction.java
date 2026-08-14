package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

/**
 * Calculates the weighted objective score used by the
 * Feasible Counterfactual Search algorithm.
 *
 * The current objective evaluates:
 *
 * 1. Proximity:
 *    Magnitude of normalized feature changes.
 *
 * 2. Sparsity:
 *    Number of changed features.
 *
 * 3. Effort:
 *    Relative difficulty associated with modifying each feature.
 *
 * 4. Network penalty:
 *    Additional cost for counterfactual changes that worsen network
 *    conditions.
 *
 * Lower scores are preferred.
 */
public final class CounterfactualObjectiveFunction {

    private static final double EPSILON =
            0.000001;

    private final FeasibleCounterfactualSearchConfig configuration;

    public CounterfactualObjectiveFunction(
            FeasibleCounterfactualSearchConfig configuration) {

        if(configuration == null) {

            throw new IllegalArgumentException(
                    "Search configuration cannot be null.");
        }

        this.configuration =
                configuration;
    }

    public FeasibleCounterfactualSearchConfig
            getConfiguration() {

        return configuration;
    }

    /**
     * Scores a modified ReplayState.
     */
    public CounterfactualScore score(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        List<FeatureChange> changes =
                replayState.getAppliedChanges();

        double proximity =
                calculateProximity(
                        changes);

        double sparsity =
                calculateSparsity(
                        changes);

        double effort =
                calculateEffort(
                        changes);

        double networkPenalty =
                calculateNetworkPenalty(
                        changes);

        double totalScore =
                configuration.getProximityWeight()
                * proximity
                + configuration.getSparsityWeight()
                * sparsity
                + configuration.getEffortWeight()
                * effort
                + configuration.getNetworkWeight()
                * networkPenalty;

        return new CounterfactualScore(
                proximity,
                sparsity,
                effort,
                networkPenalty,
                totalScore);
    }

    /**
     * Calculates normalized feature-change distance.
     */
    private double calculateProximity(
            List<FeatureChange> changes) {

        if(changes == null
                || changes.isEmpty()) {

            return 0.0;
        }

        double proximity =
                0.0;

        for(FeatureChange change
                : changes) {

            if(change == null
                    || !change.hasChanged()) {

                continue;
            }

            FeasibleCounterfactualSearchConfig
                    .FeatureSearchSpec specification =
                    configuration.findFeatureSearchSpec(
                            change.getFeatureName());

            if(specification == null) {

                throw new IllegalArgumentException(
                        "No search specification exists for feature: "
                        + change.getFeatureName());
            }

            proximity +=
                    specification.normalizeChange(
                            change.getOriginalValue(),
                            change.getCounterfactualValue());
        }

        return proximity;
    }

    /**
     * Sparsity is represented as the proportion of searchable
     * features modified by the counterfactual.
     *
     * A one-feature explanation therefore receives a lower sparsity
     * penalty than a multi-feature explanation.
     */
    private double calculateSparsity(
            List<FeatureChange> changes) {

        int changedFeatureCount =
                0;

        if(changes != null) {

            for(FeatureChange change
                    : changes) {

                if(change != null
                        && change.hasChanged()) {

                    changedFeatureCount++;
                }
            }
        }

        if(configuration.getSearchableFeatureCount()
                <= 0) {

            return 0.0;
        }

        return (double)changedFeatureCount
                / configuration
                .getSearchableFeatureCount();
    }

    /**
     * Calculates feature-specific modification effort.
     */
    private double calculateEffort(
            List<FeatureChange> changes) {

        if(changes == null
                || changes.isEmpty()) {

            return 0.0;
        }

        double effort =
                0.0;

        for(FeatureChange change
                : changes) {

            if(change == null
                    || !change.hasChanged()) {

                continue;
            }

            FeasibleCounterfactualSearchConfig
                    .FeatureSearchSpec specification =
                    configuration.findFeatureSearchSpec(
                            change.getFeatureName());

            if(specification == null) {

                throw new IllegalArgumentException(
                        "No search specification exists for feature: "
                        + change.getFeatureName());
            }

            effort +=
                    specification.calculateEffort(
                            change.getOriginalValue(),
                            change.getCounterfactualValue());
        }

        return effort;
    }

    /**
     * Penalizes changes that worsen network conditions.
     *
     * The current interpretation is:
     *
     * - Reducing bandwidth creates a penalty.
     * - Increasing latency creates a penalty.
     * - Increasing upload size creates a penalty.
     *
     * Improvements to these values do not incur a network penalty.
     */
    private double calculateNetworkPenalty(
            List<FeatureChange> changes) {

        if(changes == null
                || changes.isEmpty()) {

            return 0.0;
        }

        double penalty =
                0.0;

        for(FeatureChange change
                : changes) {

            if(change == null
                    || !change.hasChanged()) {

                continue;
            }

            String featureName =
                    normalizeFeatureName(
                            change.getFeatureName());

            double originalValue =
                    change.getOriginalValue();

            double counterfactualValue =
                    change.getCounterfactualValue();

            FeasibleCounterfactualSearchConfig
                    .FeatureSearchSpec specification =
                    configuration.findFeatureSearchSpec(
                            featureName);

            if(specification == null) {

                continue;
            }

            if(FeasibleCounterfactualSearchConfig
                    .AVAILABLE_BANDWIDTH
                    .equals(featureName)) {

                if(counterfactualValue
                        < originalValue - EPSILON) {

                    penalty +=
                            specification.normalizeChange(
                                    originalValue,
                                    counterfactualValue);
                }
            }
            else if(FeasibleCounterfactualSearchConfig
                    .NETWORK_LATENCY
                    .equals(featureName)) {

                if(counterfactualValue
                        > originalValue + EPSILON) {

                    penalty +=
                            specification.normalizeChange(
                                    originalValue,
                                    counterfactualValue);
                }
            }
            else if(FeasibleCounterfactualSearchConfig
                    .UPLOAD_SIZE
                    .equals(featureName)) {

                if(counterfactualValue
                        > originalValue + EPSILON) {

                    penalty +=
                            specification.normalizeChange(
                                    originalValue,
                                    counterfactualValue);
                }
            }
        }

        return penalty;
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
}
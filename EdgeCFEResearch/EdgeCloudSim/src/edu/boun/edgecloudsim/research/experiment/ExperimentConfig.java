package edu.boun.edgecloudsim.research.experiment;

import edu.boun.edgecloudsim.research.counterfactual
        .FeasibleCounterfactualSearchConfig;

/**
 * Immutable metadata describing one reproducible simulation or
 * counterfactual experiment run.
 *
 * This class records seeds and configuration. Actual simulator seed
 * injection is introduced separately because the current EdgeCloudSim
 * random-number utility does not yet expose a global seed setter.
 */
public final class ExperimentConfig {

    private final String experimentId;
    private final String runId;
    private final String scenario;
    private final String schedulerPolicy;

    private final int deviceCount;
    private final int iterationNumber;

    private final long simulationSeed;
    private final long searchSeed;

    private final double simulationDurationSeconds;

    private final String searchMode;
    private final int maximumGeneratedCandidates;
    private final int maximumEvaluatedCandidates;
    private final int minimumAdaptiveEvaluationBudget;
    private final int maximumAdaptiveEvaluationBudget;
    private final int adaptiveBudgetExtensionSize;
    private final int maximumAdaptiveBudgetExtensions;

    private final boolean neighborhoodRefinementEnabled;
    private final int maximumBoundarySeeds;
    private final int neighborhoodSamplesPerSeed;
    private final int maximumNeighborhoodCandidates;

    private final String ppoModelId;
    private final String outputDirectory;

    private ExperimentConfig(
            Builder builder) {

        this.experimentId =
                requireText(
                        builder.experimentId,
                        "experimentId");

        this.runId =
                requireText(
                        builder.runId,
                        "runId");

        this.scenario =
                requireText(
                        builder.scenario,
                        "scenario");

        this.schedulerPolicy =
                requireText(
                        builder.schedulerPolicy,
                        "schedulerPolicy");

        if(builder.deviceCount < 1) {

            throw new IllegalArgumentException(
                    "deviceCount must be positive.");
        }

        if(builder.iterationNumber < 1) {

            throw new IllegalArgumentException(
                    "iterationNumber must be positive.");
        }

        this.deviceCount =
                builder.deviceCount;

        this.iterationNumber =
                builder.iterationNumber;

        this.simulationSeed =
                builder.simulationSeed;

        this.searchSeed =
                builder.searchSeed;

        this.simulationDurationSeconds =
                builder.simulationDurationSeconds;

        this.searchMode =
                safeText(
                        builder.searchMode,
                        "NOT_APPLICABLE");

        this.maximumGeneratedCandidates =
                builder.maximumGeneratedCandidates;

        this.maximumEvaluatedCandidates =
                builder.maximumEvaluatedCandidates;

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

        this.ppoModelId =
                safeText(
                        builder.ppoModelId,
                        "NOT_APPLICABLE");

        this.outputDirectory =
                requireText(
                        builder.outputDirectory,
                        "outputDirectory");
    }

    public static Builder builder() {

        return new Builder();
    }

    public static Builder counterfactualBuilder(
            FeasibleCounterfactualSearchConfig searchConfig) {

        if(searchConfig == null) {

            throw new IllegalArgumentException(
                    "Search configuration cannot be null.");
        }

        return new Builder()
                .withSearchMode(
                        String.valueOf(
                                searchConfig.getSearchMode()))
                .withMaximumGeneratedCandidates(
                        searchConfig
                        .getMaximumGeneratedCandidates())
                .withMaximumEvaluatedCandidates(
                        searchConfig
                        .getMaximumEvaluatedCandidates())
                .withMinimumAdaptiveEvaluationBudget(
                        searchConfig
                        .getMinimumAdaptiveEvaluationBudget())
                .withMaximumAdaptiveEvaluationBudget(
                        searchConfig
                        .getMaximumAdaptiveEvaluationBudget())
                .withAdaptiveBudgetExtensionSize(
                        searchConfig
                        .getAdaptiveBudgetExtensionSize())
                .withMaximumAdaptiveBudgetExtensions(
                        searchConfig
                        .getMaximumAdaptiveBudgetExtensions())
                .withNeighborhoodRefinementEnabled(
                        searchConfig
                        .isNeighborhoodRefinementEnabled())
                .withMaximumBoundarySeeds(
                        searchConfig
                        .getMaximumBoundarySeeds())
                .withNeighborhoodSamplesPerSeed(
                        searchConfig
                        .getNeighborhoodSamplesPerSeed())
                .withMaximumNeighborhoodCandidates(
                        searchConfig
                        .getMaximumNeighborhoodCandidates());
    }

    private static String requireText(
            String value,
            String fieldName) {

        if(value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                    + " cannot be empty.");
        }

        return value.trim();
    }

    private static String safeText(
            String value,
            String fallback) {

        return value == null
                || value.trim().isEmpty()
                ? fallback
                : value.trim();
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getRunId() {
        return runId;
    }

    public String getScenario() {
        return scenario;
    }

    public String getSchedulerPolicy() {
        return schedulerPolicy;
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    public int getIterationNumber() {
        return iterationNumber;
    }

    public long getSimulationSeed() {
        return simulationSeed;
    }

    public long getSearchSeed() {
        return searchSeed;
    }

    public double getSimulationDurationSeconds() {
        return simulationDurationSeconds;
    }

    public String getSearchMode() {
        return searchMode;
    }

    public int getMaximumGeneratedCandidates() {
        return maximumGeneratedCandidates;
    }

    public int getMaximumEvaluatedCandidates() {
        return maximumEvaluatedCandidates;
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

    public boolean isNeighborhoodRefinementEnabled() {
        return neighborhoodRefinementEnabled;
    }

    public int getMaximumBoundarySeeds() {
        return maximumBoundarySeeds;
    }

    public int getNeighborhoodSamplesPerSeed() {
        return neighborhoodSamplesPerSeed;
    }

    public int getMaximumNeighborhoodCandidates() {
        return maximumNeighborhoodCandidates;
    }

    public String getPpoModelId() {
        return ppoModelId;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public static final class Builder {

        private String experimentId =
                "UNSPECIFIED_EXPERIMENT";

        private String runId =
                "UNSPECIFIED_RUN";

        private String scenario =
                "UNKNOWN";

        private String schedulerPolicy =
                "UNKNOWN";

        private int deviceCount =
                1;

        private int iterationNumber =
                1;

        private long simulationSeed =
                1001L;

        private long searchSeed =
                2001L;

        private double simulationDurationSeconds =
                -1.0;

        private String searchMode =
                "NOT_APPLICABLE";

        private int maximumGeneratedCandidates =
                0;

        private int maximumEvaluatedCandidates =
                0;

        private int minimumAdaptiveEvaluationBudget =
                0;

        private int maximumAdaptiveEvaluationBudget =
                0;

        private int adaptiveBudgetExtensionSize =
                0;

        private int maximumAdaptiveBudgetExtensions =
                0;

        private boolean neighborhoodRefinementEnabled =
                false;

        private int maximumBoundarySeeds =
                0;

        private int neighborhoodSamplesPerSeed =
                0;

        private int maximumNeighborhoodCandidates =
                0;

        private String ppoModelId =
                "NOT_APPLICABLE";

        private String outputDirectory =
                ".";

        public Builder withExperimentId(String value) {
            this.experimentId = value;
            return this;
        }

        public Builder withRunId(String value) {
            this.runId = value;
            return this;
        }

        public Builder withScenario(String value) {
            this.scenario = value;
            return this;
        }

        public Builder withSchedulerPolicy(String value) {
            this.schedulerPolicy = value;
            return this;
        }

        public Builder withDeviceCount(int value) {
            this.deviceCount = value;
            return this;
        }

        public Builder withIterationNumber(int value) {
            this.iterationNumber = value;
            return this;
        }

        public Builder withSimulationSeed(long value) {
            this.simulationSeed = value;
            return this;
        }

        public Builder withSearchSeed(long value) {
            this.searchSeed = value;
            return this;
        }

        public Builder withSimulationDurationSeconds(double value) {
            this.simulationDurationSeconds = value;
            return this;
        }

        public Builder withSearchMode(String value) {
            this.searchMode = value;
            return this;
        }

        public Builder withMaximumGeneratedCandidates(int value) {
            this.maximumGeneratedCandidates = value;
            return this;
        }

        public Builder withMaximumEvaluatedCandidates(int value) {
            this.maximumEvaluatedCandidates = value;
            return this;
        }

        public Builder withMinimumAdaptiveEvaluationBudget(int value) {
            this.minimumAdaptiveEvaluationBudget = value;
            return this;
        }

        public Builder withMaximumAdaptiveEvaluationBudget(int value) {
            this.maximumAdaptiveEvaluationBudget = value;
            return this;
        }

        public Builder withAdaptiveBudgetExtensionSize(int value) {
            this.adaptiveBudgetExtensionSize = value;
            return this;
        }

        public Builder withMaximumAdaptiveBudgetExtensions(int value) {
            this.maximumAdaptiveBudgetExtensions = value;
            return this;
        }

        public Builder withNeighborhoodRefinementEnabled(boolean value) {
            this.neighborhoodRefinementEnabled = value;
            return this;
        }

        public Builder withMaximumBoundarySeeds(int value) {
            this.maximumBoundarySeeds = value;
            return this;
        }

        public Builder withNeighborhoodSamplesPerSeed(int value) {
            this.neighborhoodSamplesPerSeed = value;
            return this;
        }

        public Builder withMaximumNeighborhoodCandidates(int value) {
            this.maximumNeighborhoodCandidates = value;
            return this;
        }

        public Builder withPpoModelId(String value) {
            this.ppoModelId = value;
            return this;
        }

        public Builder withOutputDirectory(String value) {
            this.outputDirectory = value;
            return this;
        }

        public ExperimentConfig build() {

            return new ExperimentConfig(
                    this);
        }
    }
}
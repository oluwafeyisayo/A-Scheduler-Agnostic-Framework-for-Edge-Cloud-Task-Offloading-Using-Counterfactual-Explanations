package edu.boun.edgecloudsim.research.counterfactual.evaluation.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.boun.edgecloudsim.research.counterfactual.comparison
        .DecisionChangeType;

/**
 * Immutable configuration for one evaluation profile.
 *
 * A profile defines what should be varied and where all simulator,
 * replay, and reporting inputs are located. The configuration does not
 * execute simulations; it is consumed by the generic evaluation
 * runner introduced in the next phase.
 */
public final class EvaluationExperimentConfig {

    private final String experimentId;
    private final EvaluationStage stage;
    private final String scenario;

    private final File simulationConfigFile;
    private final File edgeDevicesFile;
    private final File applicationsFile;
    private final File outputRoot;

    private final NetworkCondition networkCondition;
    private final SearchVariant searchVariant;

    private final List<Integer> deviceCounts;
    private final int startIteration;
    private final int endIteration;

    private final List<String> schedulerPolicies;
    private final List<DecisionChangeType> objectives;

    private final int maximumSelectedDecisionsPerObjective;
    private final int maximumRestorationDecisionsPerObjective;

    private final String ppoModelId;

    private EvaluationExperimentConfig(
            Builder builder) {

        this.experimentId =
                requireText(
                        builder.experimentId,
                        "experimentId");

        if(builder.stage == null) {

            throw new IllegalArgumentException(
                    "Evaluation stage cannot be null.");
        }

        this.stage =
                builder.stage;

        this.scenario =
                requireText(
                        builder.scenario,
                        "scenario");

        this.simulationConfigFile =
                requireFileReference(
                        builder.simulationConfigFile,
                        "simulationConfigFile");

        this.edgeDevicesFile =
                requireFileReference(
                        builder.edgeDevicesFile,
                        "edgeDevicesFile");

        this.applicationsFile =
                requireFileReference(
                        builder.applicationsFile,
                        "applicationsFile");

        if(builder.outputRoot == null) {

            throw new IllegalArgumentException(
                    "outputRoot cannot be null.");
        }

        this.outputRoot =
                builder.outputRoot;

        if(builder.networkCondition == null) {

            throw new IllegalArgumentException(
                    "Network condition cannot be null.");
        }

        this.networkCondition =
                builder.networkCondition;

        if(builder.searchVariant == null) {

            throw new IllegalArgumentException(
                    "Search variant cannot be null.");
        }

        this.searchVariant =
                builder.searchVariant;

        this.deviceCounts =
                immutablePositiveIntegerList(
                        builder.deviceCounts,
                        "deviceCounts");

        if(builder.startIteration < 1) {

            throw new IllegalArgumentException(
                    "startIteration must be positive.");
        }

        if(builder.endIteration
                < builder.startIteration) {

            throw new IllegalArgumentException(
                    "endIteration must be greater than or equal "
                    + "to startIteration.");
        }

        this.startIteration =
                builder.startIteration;

        this.endIteration =
                builder.endIteration;

        this.schedulerPolicies =
                immutableTextList(
                        builder.schedulerPolicies,
                        "schedulerPolicies");

        this.objectives =
                immutableObjectiveList(
                        builder.objectives);

        if(builder.maximumSelectedDecisionsPerObjective < 1) {

            throw new IllegalArgumentException(
                    "maximumSelectedDecisionsPerObjective "
                    + "must be positive.");
        }

        if(builder.maximumRestorationDecisionsPerObjective < 1) {

            throw new IllegalArgumentException(
                    "maximumRestorationDecisionsPerObjective "
                    + "must be positive.");
        }

        this.maximumSelectedDecisionsPerObjective =
                builder.maximumSelectedDecisionsPerObjective;

        this.maximumRestorationDecisionsPerObjective =
                builder.maximumRestorationDecisionsPerObjective;

        this.ppoModelId =
                requireText(
                        builder.ppoModelId,
                        "ppoModelId");
    }

    public static Builder builder() {

        return new Builder();
    }

    /**
     * Creates the Stage B scalability profile used by the current
     * evaluation plan.
     *
     * A separate instance is still required for each Stage B property
     * file because each file fixes one device count.
     */
    public static EvaluationExperimentConfig createStageB(
            File simulationConfigFile,
            int deviceCount) {

        return builder()
                .withExperimentId(
                        "STAGE_B_"
                        + deviceCount
                        + "_DEVICES")
                .withStage(
                        EvaluationStage.STAGE_B)
                .withScenario(
                        "TWO_TIER_WITH_EO")
                .withSimulationConfigFile(
                        simulationConfigFile)
                .withEdgeDevicesFile(
                        new File(
                                "scripts/counterfactual_offloading/"
                                + "config/edge_devices_research.xml"))
                .withApplicationsFile(
                        new File(
                                "scripts/counterfactual_offloading/"
                                + "config/applications_research.xml"))
                .withOutputRoot(
                        new File(
                                "evaluation_results"))
                .withNetworkCondition(
                        NetworkCondition.NORMAL)
                .withSearchVariant(
                        SearchVariant
                        .ADAPTIVE_PLUS_NEIGHBORHOOD)
                .withDeviceCounts(
                        Collections.singletonList(
                                Integer.valueOf(
                                        deviceCount)))
                .withIterationRange(
                        1,
                        5)
                .withSchedulerPolicies(
                        defaultSchedulerPolicies())
                .withObjectives(
                        defaultObjectives())
                .withMaximumSelectedDecisionsPerObjective(
                        100)
                .withMaximumRestorationDecisionsPerObjective(
                        100)
                .withPpoModelId(
                        "PPO_BOOTSTRAP_V1")
                .build();
    }

    public static List<String> defaultSchedulerPolicies() {

        List<String> policies =
                new ArrayList<String>();

        policies.add(
                "RANDOM");

        policies.add(
                "ROUND_ROBIN");

        policies.add(
                "LEAST_LOADED");

        policies.add(
                "MULTI_OBJECTIVE");

        policies.add(
                "PPO");

        return Collections.unmodifiableList(
                policies);
    }

    public static List<DecisionChangeType> defaultObjectives() {

        List<DecisionChangeType> objectives =
                new ArrayList<DecisionChangeType>();

        objectives.add(
                DecisionChangeType
                .EXECUTION_TARGET_CHANGE);

        objectives.add(
                DecisionChangeType
                .DESTINATION_CHANGE);

        objectives.add(
                DecisionChangeType
                .FEASIBILITY_RESTORATION);

        return Collections.unmodifiableList(
                objectives);
    }

    private static File requireFileReference(
            File file,
            String fieldName) {

        if(file == null) {

            throw new IllegalArgumentException(
                    fieldName
                    + " cannot be null.");
        }

        return file;
    }

    private static List<Integer> immutablePositiveIntegerList(
            List<Integer> values,
            String fieldName) {

        if(values == null
                || values.isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                    + " cannot be null or empty.");
        }

        Set<Integer> uniqueValues =
                new LinkedHashSet<Integer>();

        for(Integer value
                : values) {

            if(value == null
                    || value.intValue() < 1) {

                throw new IllegalArgumentException(
                        fieldName
                        + " must contain only positive integers.");
            }

            uniqueValues.add(
                    value);
        }

        return Collections.unmodifiableList(
                new ArrayList<Integer>(
                        uniqueValues));
    }

    private static List<String> immutableTextList(
            List<String> values,
            String fieldName) {

        if(values == null
                || values.isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                    + " cannot be null or empty.");
        }

        Set<String> uniqueValues =
                new LinkedHashSet<String>();

        for(String value
                : values) {

            uniqueValues.add(
                    requireText(
                            value,
                            fieldName));
        }

        return Collections.unmodifiableList(
                new ArrayList<String>(
                        uniqueValues));
    }

    private static List<DecisionChangeType>
            immutableObjectiveList(
                    List<DecisionChangeType> values) {

        if(values == null
                || values.isEmpty()) {

            throw new IllegalArgumentException(
                    "objectives cannot be null or empty.");
        }

        Set<DecisionChangeType> uniqueValues =
                new LinkedHashSet<DecisionChangeType>();

        for(DecisionChangeType value
                : values) {

            if(value == null) {

                throw new IllegalArgumentException(
                        "objectives cannot contain null.");
            }

            uniqueValues.add(
                    value);
        }

        return Collections.unmodifiableList(
                new ArrayList<DecisionChangeType>(
                        uniqueValues));
    }

    private static String requireText(
            String value,
            String fieldName) {

        if(value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                    + " cannot be null or empty.");
        }

        return value.trim();
    }

    public String getExperimentId() {

        return experimentId;
    }

    public EvaluationStage getStage() {

        return stage;
    }

    public String getScenario() {

        return scenario;
    }

    public File getSimulationConfigFile() {

        return simulationConfigFile;
    }

    public File getEdgeDevicesFile() {

        return edgeDevicesFile;
    }

    public File getApplicationsFile() {

        return applicationsFile;
    }

    public File getOutputRoot() {

        return outputRoot;
    }

    public NetworkCondition getNetworkCondition() {

        return networkCondition;
    }

    public SearchVariant getSearchVariant() {

        return searchVariant;
    }

    public List<Integer> getDeviceCounts() {

        return deviceCounts;
    }

    public int getStartIteration() {

        return startIteration;
    }

    public int getEndIteration() {

        return endIteration;
    }

    public int getIterationCount() {

        return endIteration
                - startIteration
                + 1;
    }

    public List<String> getSchedulerPolicies() {

        return schedulerPolicies;
    }

    public List<DecisionChangeType> getObjectives() {

        return objectives;
    }

    public int getMaximumSelectedDecisionsPerObjective() {

        return maximumSelectedDecisionsPerObjective;
    }

    public int getMaximumRestorationDecisionsPerObjective() {

        return maximumRestorationDecisionsPerObjective;
    }

    public String getPpoModelId() {

        return ppoModelId;
    }

    @Override
    public String toString() {

        return "EvaluationExperimentConfig{"
                + "experimentId='"
                + experimentId
                + '\''
                + ", stage="
                + stage
                + ", scenario='"
                + scenario
                + '\''
                + ", deviceCounts="
                + deviceCounts
                + ", iterations="
                + startIteration
                + "-"
                + endIteration
                + ", networkCondition="
                + networkCondition
                + ", searchVariant="
                + searchVariant
                + ", schedulerPolicies="
                + schedulerPolicies
                + ", objectives="
                + objectives
                + '}';
    }

    public static final class Builder {

        private String experimentId =
                "UNSPECIFIED_EXPERIMENT";

        private EvaluationStage stage =
                EvaluationStage.STAGE_B;

        private String scenario =
                "TWO_TIER_WITH_EO";

        private File simulationConfigFile =
                new File(
                        "scripts/counterfactual_offloading/"
                        + "config/default_config.properties");

        private File edgeDevicesFile =
                new File(
                        "scripts/counterfactual_offloading/"
                        + "config/edge_devices_research.xml");

        private File applicationsFile =
                new File(
                        "scripts/counterfactual_offloading/"
                        + "config/applications_research.xml");

        private File outputRoot =
                new File(
                        "evaluation_results");

        private NetworkCondition networkCondition =
                NetworkCondition.NORMAL;

        private SearchVariant searchVariant =
                SearchVariant
                .ADAPTIVE_PLUS_NEIGHBORHOOD;

        private List<Integer> deviceCounts =
                Collections.singletonList(
                        Integer.valueOf(
                                500));

        private int startIteration =
                1;

        private int endIteration =
                5;

        private List<String> schedulerPolicies =
                defaultSchedulerPolicies();

        private List<DecisionChangeType> objectives =
                defaultObjectives();

        private int maximumSelectedDecisionsPerObjective =
                100;

        private int maximumRestorationDecisionsPerObjective =
                100;

        private String ppoModelId =
                "PPO_BOOTSTRAP_V1";

        public Builder withExperimentId(
                String value) {

            this.experimentId =
                    value;

            return this;
        }

        public Builder withStage(
                EvaluationStage value) {

            this.stage =
                    value;

            return this;
        }

        public Builder withScenario(
                String value) {

            this.scenario =
                    value;

            return this;
        }

        public Builder withSimulationConfigFile(
                File value) {

            this.simulationConfigFile =
                    value;

            return this;
        }

        public Builder withEdgeDevicesFile(
                File value) {

            this.edgeDevicesFile =
                    value;

            return this;
        }

        public Builder withApplicationsFile(
                File value) {

            this.applicationsFile =
                    value;

            return this;
        }

        public Builder withOutputRoot(
                File value) {

            this.outputRoot =
                    value;

            return this;
        }

        public Builder withNetworkCondition(
                NetworkCondition value) {

            this.networkCondition =
                    value;

            return this;
        }

        public Builder withSearchVariant(
                SearchVariant value) {

            this.searchVariant =
                    value;

            return this;
        }

        public Builder withDeviceCounts(
                List<Integer> values) {

            this.deviceCounts =
                    values;

            return this;
        }

        public Builder withIterationRange(
                int start,
                int end) {

            this.startIteration =
                    start;

            this.endIteration =
                    end;

            return this;
        }

        public Builder withSchedulerPolicies(
                List<String> values) {

            this.schedulerPolicies =
                    values;

            return this;
        }

        public Builder withObjectives(
                List<DecisionChangeType> values) {

            this.objectives =
                    values;

            return this;
        }

        public Builder withMaximumSelectedDecisionsPerObjective(
                int value) {

            this.maximumSelectedDecisionsPerObjective =
                    value;

            return this;
        }

        public Builder withMaximumRestorationDecisionsPerObjective(
                int value) {

            this.maximumRestorationDecisionsPerObjective =
                    value;

            return this;
        }

        public Builder withPpoModelId(
                String value) {

            this.ppoModelId =
                    value;

            return this;
        }

        public EvaluationExperimentConfig build() {

            return new EvaluationExperimentConfig(
                    this);
        }
    }
}
package edu.boun.edgecloudsim.research.counterfactual.evaluation.directory;

import java.io.File;
import java.io.IOException;

import edu.boun.edgecloudsim.research.counterfactual.evaluation.config
        .EvaluationExperimentConfig;

/**
 * Centralizes all filesystem paths used by one evaluation profile.
 *
 * No runner, replay test, or report writer should construct evaluation
 * paths independently once this manager is in use.
 */
public final class EvaluationDirectoryManager {

    private final EvaluationExperimentConfig config;

    private final File stageRoot;
    private final File profileRoot;
    private final File aggregateDirectory;
    private final File summaryDirectory;
    private final File executionLogDirectory;

    public EvaluationDirectoryManager(
            EvaluationExperimentConfig config)
            throws IOException {

        if(config == null) {

            throw new IllegalArgumentException(
                    "EvaluationExperimentConfig cannot be null.");
        }

        this.config =
                config;

        this.stageRoot =
                new File(
                        config.getOutputRoot(),
                        config.getStage()
                        .getFolderName());

        this.profileRoot =
                new File(
                        stageRoot,
                        sanitize(
                                config.getExperimentId()));

        this.aggregateDirectory =
                new File(
                        profileRoot,
                        "aggregate");

        this.summaryDirectory =
                new File(
                        profileRoot,
                        "summary");

        this.executionLogDirectory =
                new File(
                        profileRoot,
                        "execution_logs");

        createDirectory(
                stageRoot);

        createDirectory(
                profileRoot);

        createDirectory(
                aggregateDirectory);

        createDirectory(
                summaryDirectory);

        createDirectory(
                executionLogDirectory);
    }

    public EvaluationExperimentConfig getConfig() {

        return config;
    }

    public File getStageRoot() {

        return stageRoot;
    }

    public File getProfileRoot() {

        return profileRoot;
    }

    public File getAggregateDirectory() {

        return aggregateDirectory;
    }

    public File getSummaryDirectory() {

        return summaryDirectory;
    }

    public File getExecutionLogDirectory() {

        return executionLogDirectory;
    }

    /**
     * Returns the root directory for one device-count profile.
     *
     * Stage B uses this as the independent-variable directory.
     */
    public File getDeviceCountDirectory(
            int deviceCount)
            throws IOException {

        validateDeviceCount(
                deviceCount);

        File directory =
                new File(
                        profileRoot,
                        deviceCount
                        + "_devices");

        createDirectory(
                directory);

        return directory;
    }

    public File getIterationDirectory(
            int deviceCount,
            int iteration)
            throws IOException {

        validateIteration(
                iteration);

        File directory =
                new File(
                        getDeviceCountDirectory(
                                deviceCount),
                        "ite"
                        + iteration);

        createDirectory(
                directory);

        return directory;
    }

    /**
     * MainApp writes simulator outputs into this iteration directory.
     */
    public File getSimulationOutputDirectory(
            int deviceCount,
            int iteration)
            throws IOException {

        return getIterationDirectory(
                deviceCount,
                iteration);
    }

    public File getDecisionLogDirectory(
            int deviceCount,
            int iteration)
            throws IOException {

        File directory =
                new File(
                        getIterationDirectory(
                                deviceCount,
                                iteration),
                        "decision_logs");

        createDirectory(
                directory);

        return directory;
    }

    public File getCounterfactualResultDirectory(
            int deviceCount,
            int iteration)
            throws IOException {

        File directory =
                new File(
                        getIterationDirectory(
                                deviceCount,
                                iteration),
                        "counterfactual_results");

        createDirectory(
                directory);

        return directory;
    }

    public File getSchedulerSpecificResultDirectory(
            int deviceCount,
            int iteration)
            throws IOException {

        File directory =
                new File(
                        getCounterfactualResultDirectory(
                                deviceCount,
                                iteration),
                        "scheduler_specific");

        createDirectory(
                directory);

        return directory;
    }

    public File getReportDirectory(
            int deviceCount,
            int iteration)
            throws IOException {

        File directory =
                new File(
                        getCounterfactualResultDirectory(
                                deviceCount,
                                iteration),
                        "reports");

        createDirectory(
                directory);

        return directory;
    }

    public File getSimulationExecutionLog(
            int deviceCount,
            int iteration) {

        return new File(
                executionLogDirectory,
                "device"
                + deviceCount
                + "_ite"
                + iteration
                + "_simulation.log");
    }

    public File getCounterfactualExecutionLog(
            int deviceCount,
            int iteration) {

        return new File(
                executionLogDirectory,
                "device"
                + deviceCount
                + "_ite"
                + iteration
                + "_counterfactual.log");
    }

    public File getDecisionCsvFile(
            int deviceCount,
            int iteration,
            String schedulerPolicy)
            throws IOException {

        return new File(
                getDecisionLogDirectory(
                        deviceCount,
                        iteration),
                createDecisionFilePrefix(
                        deviceCount,
                        iteration,
                        schedulerPolicy)
                + ".csv");
    }

    public File getCandidateCsvFile(
            int deviceCount,
            int iteration,
            String schedulerPolicy)
            throws IOException {

        return new File(
                getDecisionLogDirectory(
                        deviceCount,
                        iteration),
                createDecisionFilePrefix(
                        deviceCount,
                        iteration,
                        schedulerPolicy)
                + "_candidates.csv");
    }

    public File getObjectiveSummaryFile(
            int deviceCount,
            int iteration)
            throws IOException {

        return new File(
                getReportDirectory(
                        deviceCount,
                        iteration),
                "objective_summary.csv");
    }

    public File getEligibilitySummaryFile(
            int deviceCount,
            int iteration)
            throws IOException {

        return new File(
                getReportDirectory(
                        deviceCount,
                        iteration),
                "eligibility_summary.csv");
    }

    public File getObjectiveSeparationSummaryFile(
            int deviceCount,
            int iteration)
            throws IOException {

        return new File(
                getReportDirectory(
                        deviceCount,
                        iteration),
                "objective_separation_summary.csv");
    }

    public File getExperimentConfigurationFile(
            int deviceCount,
            int iteration)
            throws IOException {

        return new File(
                getReportDirectory(
                        deviceCount,
                        iteration),
                "experiment_configuration.csv");
    }

    public File getCombinedObjectiveSummaryFile() {

        return new File(
                aggregateDirectory,
                "all_objective_summaries.csv");
    }

    public File getCombinedEligibilitySummaryFile() {

        return new File(
                aggregateDirectory,
                "all_eligibility_summaries.csv");
    }

    public File getCombinedObjectiveSeparationFile() {

        return new File(
                aggregateDirectory,
                "all_objective_separation_summaries.csv");
    }

    public File getAggregateStatisticsFile() {

        return new File(
                aggregateDirectory,
                "aggregate_statistics.csv");
    }

    public File getEvaluationSummaryFile() {

        return new File(
                summaryDirectory,
                "evaluation_summary.md");
    }

    public String createDecisionFilePrefix(
            int deviceCount,
            int iteration,
            String schedulerPolicy) {

        validateDeviceCount(
                deviceCount);

        validateIteration(
                iteration);

        String policy =
                requireText(
                        schedulerPolicy,
                        "schedulerPolicy");

        return "offloading_decisions_"
                + config.getScenario()
                + "_"
                + policy
                + "_"
                + deviceCount
                + "DEVICES_ITE"
                + iteration;
    }

    private void validateDeviceCount(
            int deviceCount) {

        if(deviceCount < 1) {

            throw new IllegalArgumentException(
                    "deviceCount must be positive.");
        }

        if(!config.getDeviceCounts()
                .contains(
                        Integer.valueOf(
                                deviceCount))) {

            throw new IllegalArgumentException(
                    "Device count "
                    + deviceCount
                    + " is not part of evaluation profile "
                    + config.getExperimentId()
                    + ".");
        }
    }

    private void validateIteration(
            int iteration) {

        if(iteration < config.getStartIteration()
                || iteration > config.getEndIteration()) {

            throw new IllegalArgumentException(
                    "Iteration "
                    + iteration
                    + " is outside configured range "
                    + config.getStartIteration()
                    + "-"
                    + config.getEndIteration()
                    + ".");
        }
    }

    private static void createDirectory(
            File directory)
            throws IOException {

        if(directory.exists()) {

            if(!directory.isDirectory()) {

                throw new IOException(
                        "Path exists but is not a directory: "
                        + directory.getAbsolutePath());
            }

            return;
        }

        if(!directory.mkdirs()) {

            throw new IOException(
                    "Unable to create evaluation directory: "
                    + directory.getAbsolutePath());
        }
    }

    private static String sanitize(
            String value) {

        return requireText(
                value,
                "value")
                .replaceAll(
                        "[^A-Za-z0-9._-]",
                        "_");
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

    @Override
    public String toString() {

        return "EvaluationDirectoryManager{"
                + "stageRoot="
                + stageRoot.getPath()
                + ", profileRoot="
                + profileRoot.getPath()
                + ", aggregateDirectory="
                + aggregateDirectory.getPath()
                + ", summaryDirectory="
                + summaryDirectory.getPath()
                + '}';
    }
}
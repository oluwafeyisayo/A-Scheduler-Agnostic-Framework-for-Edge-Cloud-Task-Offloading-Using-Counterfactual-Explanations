package edu.boun.edgecloudsim.research.counterfactual.evaluation.runner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.evaluation.config
        .EvaluationExperimentConfig;
import edu.boun.edgecloudsim.research.counterfactual.evaluation.directory
        .EvaluationDirectoryManager;
import edu.boun.edgecloudsim.research.counterfactual.evaluation.reporting
        .EvaluationReportAggregator;

/**
 * Stage B scalability runner.
 *
 * This runner deliberately reuses the already validated execution path:
 *
 * 1. MainApp generates decision and candidate CSV files.
 * 2. EvaluationSchedulerReplayEngineTest performs replay and RG-FCS.
 * 3. EvaluationReportAggregator combines the iteration reports.
 *
 * Only the device count, iteration number, configuration file, and
 * output directory vary across runs.
 */
public final class StageBExperimentRunner {

    private static final int[] DEVICE_COUNTS = {
            100,
            250,
            500,
            750,
            1000
    };

    private static final int DEFAULT_START_ITERATION =
            1;

    private static final int DEFAULT_END_ITERATION =
            5;

    private static final String MAIN_APP_CLASS =
            "edu.boun.edgecloudsim.research.counterfactual.MainApp";

    private static final String REPLAY_TEST_CLASS =
            "edu.boun.edgecloudsim.research.counterfactual.test."
            + "EvaluationSchedulerReplayEngineTest";

    private static final String CONFIG_DIRECTORY =
            "scripts/counterfactual_offloading/config";

    private StageBExperimentRunner() {
    }

    public static void main(
            String[] args) {

        int minimumDeviceCount =
                parseOptionalInteger(
                        args,
                        0,
                        100);

        int maximumDeviceCount =
                parseOptionalInteger(
                        args,
                        1,
                        1000);

        int startIteration =
                parseOptionalInteger(
                        args,
                        2,
                        DEFAULT_START_ITERATION);

        int endIteration =
                parseOptionalInteger(
                        args,
                        3,
                        DEFAULT_END_ITERATION);

        validateRange(
                minimumDeviceCount,
                maximumDeviceCount,
                startIteration,
                endIteration);

        try {

            for(int deviceCount
                    : DEVICE_COUNTS) {

                if(deviceCount < minimumDeviceCount
                        || deviceCount > maximumDeviceCount) {

                    continue;
                }

                executeDeviceProfile(
                        deviceCount,
                        startIteration,
                        endIteration);
            }

            System.out.println();
            System.out.println(
                    "========================================");

            System.out.println(
                    "Stage B scalability evaluation completed.");

            System.out.println(
                    "Device range: "
                    + minimumDeviceCount
                    + "-"
                    + maximumDeviceCount);

            System.out.println(
                    "Iteration range: "
                    + startIteration
                    + "-"
                    + endIteration);

            System.out.println(
                    "========================================");
        }
        catch(Exception exception) {

            System.err.println(
                    "Stage B evaluation failed: "
                    + exception.getMessage());

            exception.printStackTrace(
                    System.err);

            throw new RuntimeException(
                    exception);
        }
    }

    private static void executeDeviceProfile(
            int deviceCount,
            int startIteration,
            int endIteration)
            throws IOException,
            InterruptedException {

        File simulationConfigFile =
                new File(
                        CONFIG_DIRECTORY,
                        "default_config_stage_b_"
                        + deviceCount
                        + ".properties");

        requireInputFile(
                simulationConfigFile);

        EvaluationExperimentConfig config =
                createStageBConfig(
                        simulationConfigFile,
                        deviceCount,
                        startIteration,
                        endIteration);

        EvaluationDirectoryManager directoryManager =
                new EvaluationDirectoryManager(
                        config);

        System.out.println();
        System.out.println(
                "========================================");

        System.out.println(
                "Stage B profile: "
                + deviceCount
                + " devices");

        System.out.println(
                "Iterations: "
                + startIteration
                + "-"
                + endIteration);

        System.out.println(
                "========================================");

        for(int iteration = startIteration;
                iteration <= endIteration;
                iteration++) {

            executeIteration(
                    config,
                    directoryManager,
                    deviceCount,
                    iteration);
        }

        EvaluationReportAggregator.aggregate(
                config,
                directoryManager);

        System.out.println(
                "Aggregated Stage B profile for "
                + deviceCount
                + " devices.");
    }

    private static EvaluationExperimentConfig createStageBConfig(
            File simulationConfigFile,
            int deviceCount,
            int startIteration,
            int endIteration) {

        EvaluationExperimentConfig baseConfig =
                EvaluationExperimentConfig
                .createStageB(
                        simulationConfigFile,
                        deviceCount);

        return EvaluationExperimentConfig
                .builder()
                .withExperimentId(
                        baseConfig.getExperimentId())
                .withStage(
                        baseConfig.getStage())
                .withScenario(
                        baseConfig.getScenario())
                .withSimulationConfigFile(
                        baseConfig.getSimulationConfigFile())
                .withEdgeDevicesFile(
                        baseConfig.getEdgeDevicesFile())
                .withApplicationsFile(
                        baseConfig.getApplicationsFile())
                .withOutputRoot(
                        baseConfig.getOutputRoot())
                .withNetworkCondition(
                        baseConfig.getNetworkCondition())
                .withSearchVariant(
                        baseConfig.getSearchVariant())
                .withDeviceCounts(
                        baseConfig.getDeviceCounts())
                .withIterationRange(
                        startIteration,
                        endIteration)
                .withSchedulerPolicies(
                        baseConfig.getSchedulerPolicies())
                .withObjectives(
                        baseConfig.getObjectives())
                .withMaximumSelectedDecisionsPerObjective(
                        baseConfig
                        .getMaximumSelectedDecisionsPerObjective())
                .withMaximumRestorationDecisionsPerObjective(
                        baseConfig
                        .getMaximumRestorationDecisionsPerObjective())
                .withPpoModelId(
                        baseConfig.getPpoModelId())
                .build();
    }

    private static void executeIteration(
            EvaluationExperimentConfig config,
            EvaluationDirectoryManager directoryManager,
            int deviceCount,
            int iteration)
            throws IOException,
            InterruptedException {

        System.out.println();
        System.out.println(
                "Starting "
                + deviceCount
                + "-device iteration "
                + iteration
                + ".");

        File simulationOutputDirectory =
                directoryManager
                .getSimulationOutputDirectory(
                        deviceCount,
                        iteration);

        List<String> simulationArguments =
                new ArrayList<String>();

        simulationArguments.add(
                config.getSimulationConfigFile()
                .getPath());

        simulationArguments.add(
                config.getEdgeDevicesFile()
                .getPath());

        simulationArguments.add(
                config.getApplicationsFile()
                .getPath());

        simulationArguments.add(
                simulationOutputDirectory
                .getPath());

        simulationArguments.add(
                String.valueOf(
                        iteration));

        executeJavaClass(
                MAIN_APP_CLASS,
                simulationArguments,
                directoryManager
                .getSimulationExecutionLog(
                        deviceCount,
                        iteration));

        verifySimulationOutputs(
                config,
                directoryManager,
                deviceCount,
                iteration);

        List<String> replayArguments =
                new ArrayList<String>();

        replayArguments.add(
                String.valueOf(
                        iteration));

        replayArguments.add(
                String.valueOf(
                        deviceCount));

        replayArguments.add(
                config.getSimulationConfigFile()
                .getPath());

        executeJavaClass(
                REPLAY_TEST_CLASS,
                replayArguments,
                directoryManager
                .getCounterfactualExecutionLog(
                        deviceCount,
                        iteration));

        verifyReportOutputs(
                directoryManager,
                deviceCount,
                iteration);

        System.out.println(
                "Completed "
                + deviceCount
                + "-device iteration "
                + iteration
                + ".");
    }

    private static void verifySimulationOutputs(
            EvaluationExperimentConfig config,
            EvaluationDirectoryManager directoryManager,
            int deviceCount,
            int iteration)
            throws IOException {

        for(String schedulerPolicy
                : config.getSchedulerPolicies()) {

            requireOutputFile(
                    directoryManager
                    .getDecisionCsvFile(
                            deviceCount,
                            iteration,
                            schedulerPolicy));

            requireOutputFile(
                    directoryManager
                    .getCandidateCsvFile(
                            deviceCount,
                            iteration,
                            schedulerPolicy));
        }
    }

    private static void verifyReportOutputs(
            EvaluationDirectoryManager directoryManager,
            int deviceCount,
            int iteration)
            throws IOException {

        requireOutputFile(
                directoryManager
                .getExperimentConfigurationFile(
                        deviceCount,
                        iteration));

        requireOutputFile(
                directoryManager
                .getEligibilitySummaryFile(
                        deviceCount,
                        iteration));

        requireOutputFile(
                directoryManager
                .getObjectiveSummaryFile(
                        deviceCount,
                        iteration));

        requireOutputFile(
                directoryManager
                .getObjectiveSeparationSummaryFile(
                        deviceCount,
                        iteration));
    }

    private static void executeJavaClass(
            String className,
            List<String> arguments,
            File logFile)
            throws IOException,
            InterruptedException {

        File parentDirectory =
                logFile.getParentFile();

        if(parentDirectory != null
                && !parentDirectory.exists()
                && !parentDirectory.mkdirs()) {

            throw new IOException(
                    "Unable to create execution-log directory: "
                    + parentDirectory.getAbsolutePath());
        }

        List<String> command =
                new ArrayList<String>();

        command.add(
                resolveJavaExecutable());

        command.add(
                "-cp");

        command.add(
                System.getProperty(
                        "java.class.path"));

        command.add(
                className);

        command.addAll(
                arguments);

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        command);

        processBuilder.directory(
                new File(
                        System.getProperty(
                                "user.dir")));

        processBuilder.redirectErrorStream(
                true);

        Process process =
                processBuilder.start();

        try(
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        process.getInputStream()));

                BufferedWriter writer =
                        new BufferedWriter(
                                new FileWriter(
                                        logFile,
                                        false))) {

            String line;

            while((line =
                    reader.readLine())
                    != null) {

                System.out.println(
                        line);

                writer.write(
                        line);

                writer.newLine();
            }
        }

        int exitCode =
                process.waitFor();

        if(exitCode != 0) {

            throw new IllegalStateException(
                    "Child Java process failed for "
                    + className
                    + " with exit code "
                    + exitCode
                    + ". See "
                    + logFile.getAbsolutePath());
        }
    }

    private static void requireInputFile(
            File file) {

        if(file == null
                || !file.exists()
                || !file.isFile()) {

            throw new IllegalStateException(
                    "Required Stage B input file is missing: "
                    + (
                            file == null
                            ? "null"
                            : file.getAbsolutePath()
                    ));
        }
    }

    private static void requireOutputFile(
            File file) {

        if(file == null
                || !file.exists()
                || !file.isFile()
                || file.length() == 0L) {

            throw new IllegalStateException(
                    "Required Stage B output file is missing: "
                    + (
                            file == null
                            ? "null"
                            : file.getAbsolutePath()
                    ));
        }
    }

    private static String resolveJavaExecutable() {

        String executableName =
                System.getProperty(
                        "os.name")
                .toLowerCase()
                .contains(
                        "win")
                ? "java.exe"
                : "java";

        return new File(
                new File(
                        System.getProperty(
                                "java.home"),
                        "bin"),
                executableName)
                .getAbsolutePath();
    }

    private static int parseOptionalInteger(
            String[] args,
            int index,
            int defaultValue) {

        if(args == null
                || args.length <= index) {

            return defaultValue;
        }

        try {

            return Integer.parseInt(
                    args[index]);
        }
        catch(NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Stage B runner arguments must be integers.",
                    exception);
        }
    }

    private static void validateRange(
            int minimumDeviceCount,
            int maximumDeviceCount,
            int startIteration,
            int endIteration) {

        if(minimumDeviceCount < 1
                || maximumDeviceCount
                < minimumDeviceCount) {

            throw new IllegalArgumentException(
                    "Invalid device-count range.");
        }

        if(startIteration < 1
                || endIteration < startIteration) {

            throw new IllegalArgumentException(
                    "Invalid iteration range.");
        }
    }
}
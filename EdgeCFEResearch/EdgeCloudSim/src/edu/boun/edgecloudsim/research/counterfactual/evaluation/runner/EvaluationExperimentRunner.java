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
 * Executes the Stage B scalability matrix.
 *
 * Every simulation and replay evaluation runs in a separate JVM to
 * prevent CloudSim singleton state and open writers from leaking
 * across iterations or workload sizes.
 */
public final class EvaluationExperimentRunner {

    private static final int[] STAGE_B_DEVICE_COUNTS = {
            100,
            250,
            500,
            750,
            1000
    };

    private static final String MAIN_CLASS =
            "edu.boun.edgecloudsim.research.counterfactual.MainApp";

    private static final String REPLAY_CLASS =
            "edu.boun.edgecloudsim.research.counterfactual.test."
            + "EvaluationSchedulerReplayEngineTest";

    private static final String CONFIG_DIRECTORY =
            "scripts/counterfactual_offloading/config";

    private EvaluationExperimentRunner() {
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
                        1);

        int endIteration =
                parseOptionalInteger(
                        args,
                        3,
                        5);

        try {

            for(int deviceCount
                    : STAGE_B_DEVICE_COUNTS) {

                if(deviceCount < minimumDeviceCount
                        || deviceCount > maximumDeviceCount) {

                    continue;
                }

                runDeviceProfile(
                        deviceCount,
                        startIteration,
                        endIteration);
            }

            System.out.println();
            System.out.println(
                    "Stage B scalability evaluation completed.");
        }
        catch(Exception exception) {

            exception.printStackTrace();

            throw new RuntimeException(
                    "Stage B evaluation failed.",
                    exception);
        }
    }

    private static void runDeviceProfile(
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

        EvaluationExperimentConfig config =
                EvaluationExperimentConfig
                .createStageB(
                        simulationConfigFile,
                        deviceCount);

        if(startIteration != config.getStartIteration()
                || endIteration != config.getEndIteration()) {

            config =
                    EvaluationExperimentConfig
                    .builder()
                    .withExperimentId(
                            config.getExperimentId())
                    .withStage(
                            config.getStage())
                    .withScenario(
                            config.getScenario())
                    .withSimulationConfigFile(
                            config.getSimulationConfigFile())
                    .withEdgeDevicesFile(
                            config.getEdgeDevicesFile())
                    .withApplicationsFile(
                            config.getApplicationsFile())
                    .withOutputRoot(
                            config.getOutputRoot())
                    .withNetworkCondition(
                            config.getNetworkCondition())
                    .withSearchVariant(
                            config.getSearchVariant())
                    .withDeviceCounts(
                            config.getDeviceCounts())
                    .withIterationRange(
                            startIteration,
                            endIteration)
                    .withSchedulerPolicies(
                            config.getSchedulerPolicies())
                    .withObjectives(
                            config.getObjectives())
                    .withMaximumSelectedDecisionsPerObjective(
                            config
                            .getMaximumSelectedDecisionsPerObjective())
                    .withMaximumRestorationDecisionsPerObjective(
                            config
                            .getMaximumRestorationDecisionsPerObjective())
                    .withPpoModelId(
                            config.getPpoModelId())
                    .build();
        }

        EvaluationDirectoryManager directoryManager =
                new EvaluationDirectoryManager(
                        config);

        for(int iteration = startIteration;
                iteration <= endIteration;
                iteration++) {

            runIteration(
                    config,
                    directoryManager,
                    deviceCount,
                    iteration);
        }

        EvaluationReportAggregator.aggregate(
                config,
                directoryManager);

        System.out.println(
                "Aggregated Stage B profile: "
                + deviceCount
                + " devices.");
    }

    private static void runIteration(
            EvaluationExperimentConfig config,
            EvaluationDirectoryManager directoryManager,
            int deviceCount,
            int iteration)
            throws IOException,
            InterruptedException {

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
                MAIN_CLASS,
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
                REPLAY_CLASS,
                replayArguments,
                directoryManager
                .getCounterfactualExecutionLog(
                        deviceCount,
                        iteration));

        verifyReports(
                directoryManager,
                deviceCount,
                iteration);
    }

    private static void verifySimulationOutputs(
            EvaluationExperimentConfig config,
            EvaluationDirectoryManager directoryManager,
            int deviceCount,
            int iteration)
            throws IOException {

        for(String schedulerPolicy
                : config.getSchedulerPolicies()) {

            requireFile(
                    directoryManager
                    .getDecisionCsvFile(
                            deviceCount,
                            iteration,
                            schedulerPolicy));

            requireFile(
                    directoryManager
                    .getCandidateCsvFile(
                            deviceCount,
                            iteration,
                            schedulerPolicy));
        }
    }

    private static void verifyReports(
            EvaluationDirectoryManager directoryManager,
            int deviceCount,
            int iteration)
            throws IOException {

        requireFile(
                directoryManager
                .getExperimentConfigurationFile(
                        deviceCount,
                        iteration));

        requireFile(
                directoryManager
                .getEligibilitySummaryFile(
                        deviceCount,
                        iteration));

        requireFile(
                directoryManager
                .getObjectiveSummaryFile(
                        deviceCount,
                        iteration));

        requireFile(
                directoryManager
                .getObjectiveSeparationSummaryFile(
                        deviceCount,
                        iteration));
    }

    private static void requireFile(
            File file) {

        if(file == null
                || !file.exists()
                || !file.isFile()
                || file.length() == 0L) {

            throw new IllegalStateException(
                    "Required evaluation file is missing: "
                    + (
                            file == null
                            ? "null"
                            : file.getAbsolutePath()
                    ));
        }
    }

    private static void executeJavaClass(
            String className,
            List<String> arguments,
            File logFile)
            throws IOException,
            InterruptedException {

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
                    "Child process failed for "
                    + className
                    + " with exit code "
                    + exitCode
                    + ". See "
                    + logFile.getAbsolutePath());
        }
    }

    private static String resolveJavaExecutable() {

        String executable =
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
                executable)
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
                    "Runner arguments must be integers.",
                    exception);
        }
    }
}
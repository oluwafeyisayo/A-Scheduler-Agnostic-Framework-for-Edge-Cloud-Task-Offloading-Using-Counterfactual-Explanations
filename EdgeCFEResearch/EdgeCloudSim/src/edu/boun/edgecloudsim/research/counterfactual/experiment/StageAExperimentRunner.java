package edu.boun.edgecloudsim.research.counterfactual.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Runs Stage A for a configurable iteration range in separate JVMs. */
public final class StageAExperimentRunner {

    private static final int DEFAULT_START_ITERATION = 1;
    private static final int DEFAULT_END_ITERATION = 5;

    private static final String MAIN_CLASS =
            "edu.boun.edgecloudsim.research.counterfactual.MainApp";

    private static final String REPLAY_TEST_CLASS =
            "edu.boun.edgecloudsim.research.counterfactual.test."
            + "SchedulerReplayEngineTest";

    private static final String CONFIG_FILE =
            "scripts/counterfactual_offloading/config/"
            + "default_config.properties";

    private static final String EDGE_DEVICES_FILE =
            "scripts/counterfactual_offloading/config/"
            + "edge_devices_research.xml";

    private static final String APPLICATIONS_FILE =
            "scripts/counterfactual_offloading/config/"
            + "applications_research.xml";


    private static final String[] SCHEDULER_POLICIES = {
            "RANDOM",
            "ROUND_ROBIN",
            "LEAST_LOADED",
            "MULTI_OBJECTIVE",
            "PPO"
    };

    private static final String SCENARIO =
            "TWO_TIER_WITH_EO";

    private static final int DEVICE_COUNT =
            500;

    private StageAExperimentRunner() {
    }

    public static void main(String[] args) {

        int startIteration = parseArgument(
                args,
                0,
                DEFAULT_START_ITERATION);

        int endIteration = parseArgument(
                args,
                1,
                DEFAULT_END_ITERATION);

        if(startIteration < 1 || endIteration < startIteration) {
            throw new IllegalArgumentException(
                    "Expected a positive range where start <= end.");
        }

        try {
            File executionLogDirectory = new File(
                    "sim_results/stage_a_reports/execution_logs");

            ensureDirectory(executionLogDirectory);

            for(int iteration = startIteration;
                    iteration <= endIteration;
                    iteration++) {

                runIteration(iteration, executionLogDirectory);
            }

            StageAReportAggregator.aggregate(
                    startIteration,
                    endIteration,
                    new File("sim_results/stage_a_reports"));

            System.out.println();
            System.out.println("========================================");
            System.out.println("Stage A completed successfully.");
            System.out.println(
                    "Iterations: " + startIteration + "-" + endIteration);
            System.out.println(
                    "Combined reports: "
                    + new File("sim_results/stage_a_reports")
                    .getAbsolutePath());
            System.out.println("========================================");
        }
        catch(Exception exception) {
            System.err.println(
                    "Stage A experiment failed: "
                    + exception.getMessage());
            exception.printStackTrace(System.err);
            throw new RuntimeException(exception);
        }
    }

    private static void runIteration(
            int iteration,
            File executionLogDirectory)
            throws IOException, InterruptedException {

        System.out.println();
        System.out.println("========================================");
        System.out.println("Starting Stage A iteration " + iteration);
        System.out.println("========================================");

        String outputFolder = "sim_results/ite" + iteration;

        /*
         * MainApp expects the output folder to exist when an explicit
         * path is supplied. Iteration 1 often already exists from manual
         * testing, but later iteration folders do not.
         */
        ensureDirectory(
                new File(
                        outputFolder));

        List<String> simulationArguments = new ArrayList<String>();
        simulationArguments.add(CONFIG_FILE);
        simulationArguments.add(EDGE_DEVICES_FILE);
        simulationArguments.add(APPLICATIONS_FILE);
        simulationArguments.add(outputFolder);
        simulationArguments.add(String.valueOf(iteration));

        executeJavaClass(
                MAIN_CLASS,
                simulationArguments,
                new File(
                        executionLogDirectory,
                        "ite" + iteration + "_simulation.log"));

        /*
         * MainApp currently exits with status 0 for some simulation
         * failures. Verify the expected decision and candidate files
         * before starting replay so a missing simulation dataset is
         * reported at its real source.
         */
        verifySimulationOutputs(
                iteration);

        List<String> replayArguments = new ArrayList<String>();
        replayArguments.add(String.valueOf(iteration));

        executeJavaClass(
                REPLAY_TEST_CLASS,
                replayArguments,
                new File(
                        executionLogDirectory,
                        "ite" + iteration + "_counterfactual.log"));

        verifyIterationReports(iteration);

        System.out.println(
                "Completed Stage A iteration " + iteration + ".");
    }

    private static void executeJavaClass(
            String className,
            List<String> arguments,
            File logFile)
            throws IOException, InterruptedException {

        List<String> command = new ArrayList<String>();
        command.add(resolveJavaExecutable());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(className);
        command.addAll(arguments);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(new File(System.getProperty("user.dir")));
        builder.redirectErrorStream(true);

        Process process = builder.start();

        try(BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                BufferedWriter writer = new BufferedWriter(
                    new FileWriter(logFile, false))) {

            String line;
            while((line = reader.readLine()) != null) {
                System.out.println(line);
                writer.write(line);
                writer.newLine();
            }
        }

        int exitCode = process.waitFor();

        if(exitCode != 0) {
            throw new IllegalStateException(
                    "Child JVM failed for "
                    + className
                    + " with exit code "
                    + exitCode
                    + ". See "
                    + logFile.getAbsolutePath());
        }
    }

    private static void verifySimulationOutputs(
            int iteration) {

        File decisionDirectory =
                new File(
                        "sim_results/ite"
                        + iteration
                        + "/decision_logs");

        if(!decisionDirectory.exists()
                || !decisionDirectory.isDirectory()) {

            throw new IllegalStateException(
                    "Simulation did not create the decision-log "
                    + "directory for iteration "
                    + iteration
                    + ": "
                    + decisionDirectory.getAbsolutePath());
        }

        for(String schedulerPolicy
                : SCHEDULER_POLICIES) {

            String filePrefix =
                    "offloading_decisions_"
                    + SCENARIO
                    + "_"
                    + schedulerPolicy
                    + "_"
                    + DEVICE_COUNT
                    + "DEVICES_ITE"
                    + iteration;

            requireSimulationFile(
                    new File(
                            decisionDirectory,
                            filePrefix
                            + ".csv"));

            requireSimulationFile(
                    new File(
                            decisionDirectory,
                            filePrefix
                            + "_candidates.csv"));
        }
    }

    private static void requireSimulationFile(
            File file) {

        if(!file.exists()
                || !file.isFile()
                || file.length() == 0L) {

            throw new IllegalStateException(
                    "Simulation output was not generated: "
                    + file.getAbsolutePath());
        }
    }

    private static void verifyIterationReports(int iteration) {

        File reportDirectory = new File(
                "sim_results/ite"
                + iteration
                + "/counterfactual_results/reports");

        requireFile(new File(
                reportDirectory,
                "experiment_configuration.csv"));

        requireFile(new File(
                reportDirectory,
                "eligibility_summary.csv"));

        requireFile(new File(
                reportDirectory,
                "objective_summary.csv"));

        requireFile(new File(
                reportDirectory,
                "objective_separation_summary.csv"));
    }

    private static void requireFile(File file) {

        if(!file.exists() || !file.isFile() || file.length() == 0L) {
            throw new IllegalStateException(
                    "Required report was not generated: "
                    + file.getAbsolutePath());
        }
    }

    private static String resolveJavaExecutable() {

        String executableName = System.getProperty("os.name")
                .toLowerCase()
                .contains("win")
                ? "java.exe"
                : "java";

        return new File(
                new File(System.getProperty("java.home"), "bin"),
                executableName)
                .getAbsolutePath();
    }

    private static int parseArgument(
            String[] args,
            int index,
            int defaultValue) {

        if(args == null || args.length <= index) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(args[index]);
        }
        catch(NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Iteration arguments must be integers.",
                    exception);
        }
    }

    private static void ensureDirectory(File directory)
            throws IOException {

        if(directory.exists()) {
            if(!directory.isDirectory()) {
                throw new IOException(
                        "Path is not a directory: "
                        + directory.getAbsolutePath());
            }
            return;
        }

        if(!directory.mkdirs()) {
            throw new IOException(
                    "Unable to create directory: "
                    + directory.getAbsolutePath());
        }
    }
}
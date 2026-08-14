package edu.boun.edgecloudsim.research.counterfactual.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs Stage C counterfactual generation for CONSTRAINED and SEVERE
 * using the provenance pairs already produced by the Stage C simulation.
 *
 * NORMAL is reused from Stage A and is not regenerated.
 */
public final class StageCCounterfactualExperimentRunner {

    private static final int DEFAULT_START_ITERATION = 1;
    private static final int DEFAULT_END_ITERATION = 5;

    private static final String REPLAY_TEST_CLASS =
            "edu.boun.edgecloudsim.research.counterfactual.test."
            + "SchedulerReplayEngineTest";

    private static final String[] CONDITIONS = {
            "constrained",
            "severe"
    };

    private static final String[] SCHEDULER_POLICIES = {
            "RANDOM",
            "ROUND_ROBIN",
            "LEAST_LOADED",
            "MULTI_OBJECTIVE",
            "PPO"
    };

    private static final String SCENARIO = "TWO_TIER_WITH_EO";
    private static final int DEVICE_COUNT = 500;

    private StageCCounterfactualExperimentRunner() {
    }

    public static void main(String[] args) {

        int startIteration = parseArgument(
                args, 0, DEFAULT_START_ITERATION);

        int endIteration = parseArgument(
                args, 1, DEFAULT_END_ITERATION);

        if(startIteration < 1 || endIteration < startIteration) {
            throw new IllegalArgumentException(
                    "Expected a positive range where start <= end.");
        }

        try {
            File executionLogDirectory =
                    new File("sim_results/stage_c/evaluation/execution_logs");

            ensureDirectory(executionLogDirectory);

            for(String condition : CONDITIONS) {
                for(int iteration = startIteration;
                        iteration <= endIteration;
                        iteration++) {

                    runConditionIteration(
                            condition,
                            iteration,
                            executionLogDirectory);
                }
            }

            System.out.println();
            System.out.println("========================================");
            System.out.println(
                    "Stage C counterfactual generation completed.");
            System.out.println(
                    "Conditions: CONSTRAINED and SEVERE");
            System.out.println(
                    "Iterations: " + startIteration + "-" + endIteration);
            System.out.println(
                    "Outputs: "
                    + new File("sim_results/stage_c/evaluation")
                    .getAbsolutePath());
            System.out.println("========================================");
        }
        catch(Exception exception) {
            System.err.println(
                    "Stage C counterfactual generation failed: "
                    + exception.getMessage());
            exception.printStackTrace(System.err);
            throw new RuntimeException(exception);
        }
    }

    private static void runConditionIteration(
            String condition,
            int iteration,
            File executionLogDirectory)
            throws IOException, InterruptedException {

        File iterationInputRoot =
                new File(
                        "sim_results/stage_c/"
                        + condition
                        + "/ite"
                        + iteration);

        File decisionDirectory =
                new File(iterationInputRoot, "decision_logs");

        verifyProvenanceFiles(
                decisionDirectory,
                iteration);

        File counterfactualOutputRoot =
                new File(
                        "sim_results/stage_c/evaluation/"
                        + condition
                        + "/ite"
                        + iteration
                        + "/counterfactual_results");

        ensureDirectory(counterfactualOutputRoot);

        List<String> replayArguments =
                new ArrayList<String>();

        replayArguments.add(String.valueOf(iteration));
        replayArguments.add(iterationInputRoot.getPath());
        replayArguments.add(counterfactualOutputRoot.getPath());
        replayArguments.add(condition.toUpperCase());

        File logFile =
                new File(
                        executionLogDirectory,
                        condition
                        + "_ite"
                        + iteration
                        + "_counterfactual.log");

        System.out.println();
        System.out.println("========================================");
        System.out.println(
                "Starting Stage C "
                + condition.toUpperCase()
                + ", iteration "
                + iteration);
        System.out.println("Input: "
                + decisionDirectory.getAbsolutePath());
        System.out.println("Output: "
                + counterfactualOutputRoot.getAbsolutePath());
        System.out.println("========================================");

        executeJavaClass(
                REPLAY_TEST_CLASS,
                replayArguments,
                logFile);

        verifyCounterfactualReports(
                counterfactualOutputRoot);

        System.out.println(
                "Completed "
                + condition.toUpperCase()
                + ", iteration "
                + iteration
                + ".");
    }

    private static void verifyProvenanceFiles(
            File decisionDirectory,
            int iteration) {

        if(!decisionDirectory.isDirectory()) {
            throw new IllegalStateException(
                    "Decision-log directory was not found: "
                    + decisionDirectory.getAbsolutePath());
        }

        for(String scheduler : SCHEDULER_POLICIES) {

            String prefix =
                    "offloading_decisions_"
                    + SCENARIO
                    + "_"
                    + scheduler
                    + "_"
                    + DEVICE_COUNT
                    + "DEVICES_ITE"
                    + iteration;

            requireFile(
                    new File(
                            decisionDirectory,
                            prefix + ".csv"));

            requireFile(
                    new File(
                            decisionDirectory,
                            prefix + "_candidates.csv"));
        }
    }

    private static void verifyCounterfactualReports(
            File outputRoot) {

        File reportDirectory =
                new File(outputRoot, "reports");

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

    private static void executeJavaClass(
            String className,
            List<String> arguments,
            File logFile)
            throws IOException, InterruptedException {

        ensureDirectory(logFile.getParentFile());

        List<String> command = new ArrayList<String>();
        command.add(resolveJavaExecutable());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(className);
        command.addAll(arguments);

        ProcessBuilder builder =
                new ProcessBuilder(command);

        builder.directory(
                new File(System.getProperty("user.dir")));
        builder.redirectErrorStream(true);

        Process process = builder.start();

        try(BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream()));
                BufferedWriter writer =
                    new BufferedWriter(
                            new FileWriter(
                                    logFile,
                                    false))) {

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

    private static void requireFile(File file) {
        if(!file.isFile() || file.length() == 0L) {
            throw new IllegalStateException(
                    "Required file was not generated: "
                    + file.getAbsolutePath());
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

    private static String resolveJavaExecutable() {

        String executableName =
                System.getProperty("os.name")
                .toLowerCase()
                .contains("win")
                ? "java.exe"
                : "java";

        return new File(
                new File(
                        System.getProperty("java.home"),
                        "bin"),
                executableName)
                .getAbsolutePath();
    }
}
package edu.boun.edgecloudsim.research.experiment;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Writes a one-row CSV manifest for an experiment run.
 */
public final class ExperimentManifest
        implements Closeable {

    private static final String HEADER =
            "experiment_id,"
            + "run_id,"
            + "scenario,"
            + "scheduler_policy,"
            + "device_count,"
            + "iteration_number,"
            + "simulation_seed,"
            + "search_seed,"
            + "simulation_duration_seconds,"
            + "search_mode,"
            + "maximum_generated_candidates,"
            + "maximum_evaluated_candidates,"
            + "minimum_adaptive_evaluation_budget,"
            + "maximum_adaptive_evaluation_budget,"
            + "adaptive_budget_extension_size,"
            + "maximum_adaptive_budget_extensions,"
            + "neighborhood_refinement_enabled,"
            + "maximum_boundary_seeds,"
            + "neighborhood_samples_per_seed,"
            + "maximum_neighborhood_candidates,"
            + "ppo_model_id,"
            + "output_directory,"
            + "start_epoch_millis,"
            + "end_epoch_millis,"
            + "status,"
            + "failure_message";

    private final File outputFile;
    private final ExperimentConfig config;
    private final long startEpochMillis;

    private boolean completed;

    public ExperimentManifest(
            File outputFile,
            ExperimentConfig config)
            throws IOException {

        if(outputFile == null) {

            throw new IllegalArgumentException(
                    "Manifest file cannot be null.");
        }

        if(config == null) {

            throw new IllegalArgumentException(
                    "ExperimentConfig cannot be null.");
        }

        File parent =
                outputFile.getParentFile();

        if(parent != null
                && !parent.exists()
                && !parent.mkdirs()) {

            throw new IOException(
                    "Unable to create manifest directory: "
                    + parent.getAbsolutePath());
        }

        this.outputFile =
                outputFile;

        this.config =
                config;

        this.startEpochMillis =
                new Date().getTime();

        write(
                -1L,
                "RUNNING",
                "");
    }

    public synchronized void markCompleted()
            throws IOException {

        completed =
                true;

        write(
                new Date().getTime(),
                "COMPLETED",
                "");
    }

    public synchronized void markFailed(
            Throwable throwable)
            throws IOException {

        completed =
                true;

        write(
                new Date().getTime(),
                "FAILED",
                throwable == null
                ? "UNKNOWN_FAILURE"
                : throwable.getClass().getName()
                  + ": "
                  + String.valueOf(
                            throwable.getMessage()));
    }

    private void write(
            long endEpochMillis,
            String status,
            String failureMessage)
            throws IOException {

        try(BufferedWriter writer =
                new BufferedWriter(
                        new FileWriter(
                                outputFile,
                                false))) {

            writer.write(
                    HEADER);

            writer.newLine();

            writer.write(
                    csv(config.getExperimentId()));
            writer.write(',');
            writer.write(
                    csv(config.getRunId()));
            writer.write(',');
            writer.write(
                    csv(config.getScenario()));
            writer.write(',');
            writer.write(
                    csv(config.getSchedulerPolicy()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getDeviceCount()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getIterationNumber()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getSimulationSeed()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getSearchSeed()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getSimulationDurationSeconds()));
            writer.write(',');
            writer.write(
                    csv(config.getSearchMode()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getMaximumGeneratedCandidates()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getMaximumEvaluatedCandidates()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getMinimumAdaptiveEvaluationBudget()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getMaximumAdaptiveEvaluationBudget()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getAdaptiveBudgetExtensionSize()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getMaximumAdaptiveBudgetExtensions()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.isNeighborhoodRefinementEnabled()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getMaximumBoundarySeeds()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getNeighborhoodSamplesPerSeed()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            config.getMaximumNeighborhoodCandidates()));
            writer.write(',');
            writer.write(
                    csv(config.getPpoModelId()));
            writer.write(',');
            writer.write(
                    csv(config.getOutputDirectory()));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            startEpochMillis));
            writer.write(',');
            writer.write(
                    String.valueOf(
                            endEpochMillis));
            writer.write(',');
            writer.write(
                    csv(status));
            writer.write(',');
            writer.write(
                    csv(failureMessage));
            writer.newLine();
        }
    }

    private static String csv(
            String value) {

        String safe =
                value == null
                ? ""
                : value;

        return "\""
                + safe.replace(
                        "\"",
                        "\"\"")
                + "\"";
    }

    @Override
    public synchronized void close()
            throws IOException {

        if(!completed) {

            markCompleted();
        }
    }
}
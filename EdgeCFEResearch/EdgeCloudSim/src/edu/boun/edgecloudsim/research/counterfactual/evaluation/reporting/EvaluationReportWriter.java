package edu.boun.edgecloudsim.research.counterfactual.evaluation.reporting;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualExperimentSummary;
import edu.boun.edgecloudsim.research.counterfactual
        .FeasibleCounterfactualSearchConfig;
import edu.boun.edgecloudsim.research.counterfactual
        .PerDecisionCounterfactualSearchExecutor;
import edu.boun.edgecloudsim.research.counterfactual.comparison
        .DecisionChangeType;
import edu.boun.edgecloudsim.research.counterfactual.evaluation.config
        .EvaluationExperimentConfig;
import edu.boun.edgecloudsim.research.counterfactual.evaluation.directory
        .EvaluationDirectoryManager;

/**
 * Generic evaluation report writer used by Stage B and later stages.
 *
 * One instance writes the complete report set for one device-count and
 * iteration combination.
 */
public final class EvaluationReportWriter
        implements Closeable {

    private final EvaluationExperimentConfig config;
    private final EvaluationDirectoryManager directoryManager;

    private final int deviceCount;
    private final int iteration;

    private final BufferedWriter eligibilityWriter;
    private final BufferedWriter objectiveWriter;
    private final BufferedWriter separationWriter;

    private boolean closed;

    public EvaluationReportWriter(
            EvaluationExperimentConfig config,
            EvaluationDirectoryManager directoryManager,
            int deviceCount,
            int iteration,
            FeasibleCounterfactualSearchConfig searchConfig)
            throws IOException {

        if(config == null) {

            throw new IllegalArgumentException(
                    "EvaluationExperimentConfig cannot be null.");
        }

        if(directoryManager == null) {

            throw new IllegalArgumentException(
                    "EvaluationDirectoryManager cannot be null.");
        }

        if(searchConfig == null) {

            throw new IllegalArgumentException(
                    "FeasibleCounterfactualSearchConfig cannot be null.");
        }

        this.config =
                config;

        this.directoryManager =
                directoryManager;

        this.deviceCount =
                deviceCount;

        this.iteration =
                iteration;

        writeConfiguration(
                searchConfig);

        this.eligibilityWriter =
                createWriter(
                        directoryManager
                        .getEligibilitySummaryFile(
                                deviceCount,
                                iteration),
                        "phase,experiment_id,scenario,device_count,"
                        + "iteration,network_condition,search_variant,"
                        + "scheduler,loaded_decisions,"
                        + "eligible_selected_decisions,"
                        + "eligible_restoration_decisions,"
                        + "selected_sample_size,"
                        + "restoration_sample_size,"
                        + "missing_candidate_snapshots,"
                        + "invalid_scheduler_provenance");

        this.objectiveWriter =
                createWriter(
                        directoryManager
                        .getObjectiveSummaryFile(
                                deviceCount,
                                iteration),
                        "phase,experiment_id,scenario,device_count,"
                        + "iteration,network_condition,search_variant,"
                        + "scheduler,objective,submitted,processed,"
                        + "successful,unsuccessful,failed_executions,"
                        + "success_rate,adaptive_generated,"
                        + "adaptive_evaluated,adaptive_accepted,"
                        + "neighborhood_generated,"
                        + "neighborhood_evaluated,"
                        + "neighborhood_accepted,overall_generated,"
                        + "overall_evaluated,overall_accepted,"
                        + "average_adaptive_generated,"
                        + "average_adaptive_evaluated,"
                        + "average_adaptive_accepted,"
                        + "average_neighborhood_generated,"
                        + "average_neighborhood_evaluated,"
                        + "average_neighborhood_accepted,"
                        + "average_overall_generated,"
                        + "average_overall_evaluated,"
                        + "average_overall_accepted,"
                        + "adaptive_best_count,"
                        + "neighborhood_best_count,no_best_count,"
                        + "neighborhood_improved_count,"
                        + "neighborhood_improvement_rate,"
                        + "adaptive_best_rate,"
                        + "neighborhood_best_rate,duration_ms,"
                        + "average_duration_per_processed_ms");

        this.separationWriter =
                createWriter(
                        directoryManager
                        .getObjectiveSeparationSummaryFile(
                                deviceCount,
                                iteration),
                        "phase,experiment_id,scenario,device_count,"
                        + "iteration,network_condition,search_variant,"
                        + "scheduler,objective,successful_results,"
                        + "observed_destination_changes,"
                        + "observed_same_tier_vm_changes,"
                        + "observed_execution_target_changes,"
                        + "observed_feasibility_restorations,"
                        + "objective_mismatches");
    }

    public synchronized void writeEligibility(
            String scheduler,
            int loadedDecisionCount,
            int eligibleSelectedDecisionCount,
            int eligibleRestorationDecisionCount,
            int selectedSampleSize,
            int restorationSampleSize,
            int missingCandidateSnapshotCount,
            int invalidSchedulerProvenanceCount)
            throws IOException {

        ensureOpen();

        writeCommonPrefix(
                eligibilityWriter);

        writeText(
                eligibilityWriter,
                scheduler);

        writeNumber(
                eligibilityWriter,
                loadedDecisionCount);

        writeNumber(
                eligibilityWriter,
                eligibleSelectedDecisionCount);

        writeNumber(
                eligibilityWriter,
                eligibleRestorationDecisionCount);

        writeNumber(
                eligibilityWriter,
                selectedSampleSize);

        writeNumber(
                eligibilityWriter,
                restorationSampleSize);

        writeNumber(
                eligibilityWriter,
                missingCandidateSnapshotCount);

        writeNumber(
                eligibilityWriter,
                invalidSchedulerProvenanceCount);

        eligibilityWriter.newLine();
        eligibilityWriter.flush();
    }

    public synchronized void writeObjectiveSummary(
            String scheduler,
            DecisionChangeType objective,
            CounterfactualExperimentSummary summary)
            throws IOException {

        ensureOpen();

        if(objective == null) {

            throw new IllegalArgumentException(
                    "DecisionChangeType cannot be null.");
        }

        if(summary == null) {

            throw new IllegalArgumentException(
                    "CounterfactualExperimentSummary cannot be null.");
        }

        writeCommonPrefix(
                objectiveWriter);

        writeText(
                objectiveWriter,
                scheduler);

        objectiveWriter.write(',');

        writeText(
                objectiveWriter,
                String.valueOf(
                        objective));

        writeNumber(
                objectiveWriter,
                summary.getSubmittedDecisionCount());

        writeNumber(
                objectiveWriter,
                summary.getProcessedDecisionCount());

        writeNumber(
                objectiveWriter,
                summary.getSuccessfulSearchCount());

        writeNumber(
                objectiveWriter,
                summary.getUnsuccessfulSearchCount());

        writeNumber(
                objectiveWriter,
                summary.getFailedExecutionCount());

        writeNumber(
                objectiveWriter,
                summary.getSearchSuccessRate());

        writeNumber(
                objectiveWriter,
                summary
                .getTotalAdaptiveGeneratedCandidateCount());

        writeNumber(
                objectiveWriter,
                summary
                .getTotalAdaptiveEvaluatedCandidateCount());

        writeNumber(
                objectiveWriter,
                summary
                .getTotalAdaptiveAcceptedCandidateCount());

        writeNumber(
                objectiveWriter,
                summary
                .getTotalNeighborhoodGeneratedCandidateCount());

        writeNumber(
                objectiveWriter,
                summary
                .getTotalNeighborhoodEvaluatedCandidateCount());

        writeNumber(
                objectiveWriter,
                summary
                .getTotalNeighborhoodAcceptedCandidateCount());

        writeNumber(
                objectiveWriter,
                summary
                .getTotalOverallGeneratedCandidateCount());

        writeNumber(
                objectiveWriter,
                summary
                .getTotalOverallEvaluatedCandidateCount());

        writeNumber(
                objectiveWriter,
                summary
                .getTotalOverallAcceptedCandidateCount());

        writeNumber(
                objectiveWriter,
                summary
                .getAverageAdaptiveGeneratedCandidates());

        writeNumber(
                objectiveWriter,
                summary
                .getAverageAdaptiveEvaluatedCandidates());

        writeNumber(
                objectiveWriter,
                summary
                .getAverageAdaptiveAcceptedCandidates());

        writeNumber(
                objectiveWriter,
                summary
                .getAverageNeighborhoodGeneratedCandidates());

        writeNumber(
                objectiveWriter,
                summary
                .getAverageNeighborhoodEvaluatedCandidates());

        writeNumber(
                objectiveWriter,
                summary
                .getAverageNeighborhoodAcceptedCandidates());

        writeNumber(
                objectiveWriter,
                summary
                .getAverageOverallGeneratedCandidates());

        writeNumber(
                objectiveWriter,
                summary
                .getAverageOverallEvaluatedCandidates());

        writeNumber(
                objectiveWriter,
                summary
                .getAverageOverallAcceptedCandidates());

        writeNumber(
                objectiveWriter,
                summary.getAdaptiveBestCandidateCount());

        writeNumber(
                objectiveWriter,
                summary.getNeighborhoodBestCandidateCount());

        writeNumber(
                objectiveWriter,
                summary.getNoBestCandidateCount());

        writeNumber(
                objectiveWriter,
                summary.getNeighborhoodImprovedSearchCount());

        writeNumber(
                objectiveWriter,
                summary.getNeighborhoodImprovementRate());

        writeNumber(
                objectiveWriter,
                summary.getAdaptiveBestCandidateRate());

        writeNumber(
                objectiveWriter,
                summary.getNeighborhoodBestCandidateRate());

        writeNumber(
                objectiveWriter,
                summary.getExperimentDurationMilliseconds());

        double averageDurationPerProcessedDecision =
                summary.getProcessedDecisionCount() == 0
                ? 0.0
                : (double) summary
                    .getExperimentDurationMilliseconds()
                  / summary.getProcessedDecisionCount();

        writeNumber(
                objectiveWriter,
                averageDurationPerProcessedDecision);

        objectiveWriter.newLine();
        objectiveWriter.flush();
    }

    public synchronized void writeObjectiveSeparation(
            String scheduler,
            DecisionChangeType objective,
            PerDecisionCounterfactualSearchExecutor executor)
            throws IOException {

        if(executor == null) {

            throw new IllegalArgumentException(
                    "PerDecisionCounterfactualSearchExecutor "
                    + "cannot be null.");
        }

        writeObjectiveSeparation(
                scheduler,
                objective,
                executor.getDiagnosticSuccessfulResultCount(),
                executor.getDiagnosticDestinationChangeCount(),
                executor.getDiagnosticSameTierVmChangeCount(),
                executor.getDiagnosticExecutionTargetChangeCount(),
                executor.getDiagnosticFeasibilityRestorationCount(),
                executor.getDiagnosticObjectiveMismatchCount());
    }

    /**
     * Writes a direct diagnostic row. This overload is used for
     * objective combinations with zero eligible decisions.
     */
    public synchronized void writeObjectiveSeparation(
            String scheduler,
            DecisionChangeType objective,
            int successfulResults,
            int observedDestinationChanges,
            int observedSameTierVmChanges,
            int observedExecutionTargetChanges,
            int observedFeasibilityRestorations,
            int objectiveMismatches)
            throws IOException {

        ensureOpen();

        if(objective == null) {

            throw new IllegalArgumentException(
                    "DecisionChangeType cannot be null.");
        }

        writeCommonPrefix(
                separationWriter);

        writeText(
                separationWriter,
                scheduler);

        separationWriter.write(',');

        writeText(
                separationWriter,
                String.valueOf(
                        objective));

        writeNumber(
                separationWriter,
                successfulResults);

        writeNumber(
                separationWriter,
                observedDestinationChanges);

        writeNumber(
                separationWriter,
                observedSameTierVmChanges);

        writeNumber(
                separationWriter,
                observedExecutionTargetChanges);

        writeNumber(
                separationWriter,
                observedFeasibilityRestorations);

        writeNumber(
                separationWriter,
                objectiveMismatches);

        separationWriter.newLine();
        separationWriter.flush();
    }

    private void writeConfiguration(
            FeasibleCounterfactualSearchConfig searchConfig)
            throws IOException {

        File outputFile =
                directoryManager
                .getExperimentConfigurationFile(
                        deviceCount,
                        iteration);

        try(BufferedWriter writer =
                createWriter(
                        outputFile,
                        "phase,experiment_id,scenario,device_count,"
                        + "iteration,network_condition,search_variant,"
                        + "maximum_selected_decisions,"
                        + "maximum_restoration_decisions,"
                        + "search_mode,maximum_generated_candidates,"
                        + "maximum_evaluated_candidates,"
                        + "minimum_adaptive_evaluation_budget,"
                        + "maximum_adaptive_evaluation_budget,"
                        + "adaptive_budget_extension_size,"
                        + "maximum_adaptive_budget_extensions,"
                        + "neighborhood_enabled,"
                        + "maximum_boundary_seeds,"
                        + "neighborhood_samples_per_seed,"
                        + "maximum_neighborhood_candidates,"
                        + "ppo_model_id")) {

            writeCommonPrefix(
                    writer);

            writer.write(
                    String.valueOf(
                            config
                            .getMaximumSelectedDecisionsPerObjective()));

            writeNumber(
                    writer,
                    config
                    .getMaximumRestorationDecisionsPerObjective());

            writer.write(',');

            writeText(
                    writer,
                    String.valueOf(
                            searchConfig.getSearchMode()));

            writeNumber(
                    writer,
                    searchConfig
                    .getMaximumGeneratedCandidates());

            writeNumber(
                    writer,
                    searchConfig
                    .getMaximumEvaluatedCandidates());

            writeNumber(
                    writer,
                    searchConfig
                    .getMinimumAdaptiveEvaluationBudget());

            writeNumber(
                    writer,
                    searchConfig
                    .getMaximumAdaptiveEvaluationBudget());

            writeNumber(
                    writer,
                    searchConfig
                    .getAdaptiveBudgetExtensionSize());

            writeNumber(
                    writer,
                    searchConfig
                    .getMaximumAdaptiveBudgetExtensions());

            writer.write(',');

            writer.write(
                    String.valueOf(
                            searchConfig
                            .isNeighborhoodRefinementEnabled()));

            writeNumber(
                    writer,
                    searchConfig.getMaximumBoundarySeeds());

            writeNumber(
                    writer,
                    searchConfig
                    .getNeighborhoodSamplesPerSeed());

            writeNumber(
                    writer,
                    searchConfig
                    .getMaximumNeighborhoodCandidates());

            writer.write(',');

            writeText(
                    writer,
                    config.getPpoModelId());

            writer.newLine();
        }
    }

    private void writeCommonPrefix(
            BufferedWriter writer)
            throws IOException {

        writeText(
                writer,
                config.getStage()
                .name());

        writer.write(',');

        writeText(
                writer,
                config.getExperimentId());

        writer.write(',');

        writeText(
                writer,
                config.getScenario());

        writeNumber(
                writer,
                deviceCount);

        writeNumber(
                writer,
                iteration);

        writer.write(',');

        writeText(
                writer,
                config.getNetworkCondition()
                .getCode());

        writer.write(',');

        writeText(
                writer,
                config.getSearchVariant()
                .getCode());

        writer.write(',');
    }

    private static BufferedWriter createWriter(
            File outputFile,
            String header)
            throws IOException {

        File parentDirectory =
                outputFile.getParentFile();

        if(parentDirectory != null
                && !parentDirectory.exists()
                && !parentDirectory.mkdirs()) {

            throw new IOException(
                    "Unable to create report directory: "
                    + parentDirectory.getAbsolutePath());
        }

        BufferedWriter writer =
                new BufferedWriter(
                        new FileWriter(
                                outputFile,
                                false));

        writer.write(
                header);

        writer.newLine();
        writer.flush();

        return writer;
    }

    private void ensureOpen() {

        if(closed) {

            throw new IllegalStateException(
                    "EvaluationReportWriter is already closed.");
        }
    }

    private static void writeText(
            BufferedWriter writer,
            String value)
            throws IOException {

        String safeValue =
                value == null
                ? ""
                : value;

        writer.write(
                "\""
                + safeValue.replace(
                        "\"",
                        "\"\"")
                + "\"");
    }

    private static void writeNumber(
            BufferedWriter writer,
            long value)
            throws IOException {

        writer.write(',');
        writer.write(
                String.valueOf(
                        value));
    }

    private static void writeNumber(
            BufferedWriter writer,
            double value)
            throws IOException {

        writer.write(',');
        writer.write(
                String.valueOf(
                        value));
    }

    @Override
    public synchronized void close()
            throws IOException {

        if(closed) {

            return;
        }

        IOException failure =
                null;

        try {

            eligibilityWriter.close();
        }
        catch(IOException exception) {

            failure =
                    exception;
        }

        try {

            objectiveWriter.close();
        }
        catch(IOException exception) {

            if(failure == null) {

                failure =
                        exception;
            }
        }

        try {

            separationWriter.close();
        }
        catch(IOException exception) {

            if(failure == null) {

                failure =
                        exception;
            }
        }

        closed =
                true;

        if(failure != null) {

            throw failure;
        }
    }
}
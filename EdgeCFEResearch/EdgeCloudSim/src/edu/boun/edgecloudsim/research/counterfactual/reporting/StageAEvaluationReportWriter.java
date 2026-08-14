package edu.boun.edgecloudsim.research.counterfactual.reporting;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.boun.edgecloudsim.research.counterfactual.CounterfactualExperimentSummary;
import edu.boun.edgecloudsim.research.counterfactual.FeasibleCounterfactualSearchConfig;
import edu.boun.edgecloudsim.research.counterfactual.PerDecisionCounterfactualSearchExecutor;
import edu.boun.edgecloudsim.research.counterfactual.comparison.DecisionChangeType;

public final class StageAEvaluationReportWriter implements Closeable {

    private final BufferedWriter eligibilityWriter;
    private final BufferedWriter objectiveWriter;
    private final BufferedWriter separationWriter;
    private final int iteration;
    private final String scenario;
    private final int deviceCount;
    private final String networkCondition;
    private final String searchVariant;
    private final File reportDirectory;

    public StageAEvaluationReportWriter(
            File reportDirectory,
            int iteration,
            String scenario,
            int deviceCount,
            String networkCondition,
            String searchVariant,
            FeasibleCounterfactualSearchConfig searchConfig,
            int maximumSelectedDecisions,
            int maximumRestorationDecisions)
            throws IOException {

        if(reportDirectory == null || searchConfig == null) {
            throw new IllegalArgumentException(
                    "Report directory and search configuration are required.");
        }

        this.reportDirectory = reportDirectory;
        this.iteration = iteration;
        this.scenario = scenario;
        this.deviceCount = deviceCount;
        this.networkCondition = networkCondition;
        this.searchVariant = searchVariant;

        if(!reportDirectory.exists() && !reportDirectory.mkdirs()) {
            throw new IOException(
                    "Unable to create report directory: "
                    + reportDirectory.getAbsolutePath());
        }

        writeConfiguration(
                searchConfig,
                maximumSelectedDecisions,
                maximumRestorationDecisions);

        eligibilityWriter = createWriter(
                "eligibility_summary.csv",
                "phase,scenario,device_count,iteration,network_condition,"
                + "search_variant,scheduler,loaded_decisions,"
                + "eligible_selected_decisions,eligible_restoration_decisions,"
                + "selected_sample_size,restoration_sample_size,"
                + "missing_candidate_snapshots,invalid_scheduler_provenance");

        objectiveWriter = createWriter(
                "objective_summary.csv",
                "phase,scenario,device_count,iteration,network_condition,"
                + "search_variant,scheduler,objective,submitted,processed,"
                + "successful,unsuccessful,failed_executions,success_rate,"
                + "adaptive_generated,adaptive_evaluated,adaptive_accepted,"
                + "neighborhood_generated,neighborhood_evaluated,"
                + "neighborhood_accepted,overall_generated,overall_evaluated,"
                + "overall_accepted,average_adaptive_generated,"
                + "average_adaptive_evaluated,average_adaptive_accepted,"
                + "average_neighborhood_generated,"
                + "average_neighborhood_evaluated,"
                + "average_neighborhood_accepted,average_overall_generated,"
                + "average_overall_evaluated,average_overall_accepted,"
                + "adaptive_best_count,neighborhood_best_count,no_best_count,"
                + "neighborhood_improved_count,neighborhood_improvement_rate,"
                + "adaptive_best_rate,neighborhood_best_rate,duration_ms,"
                + "average_duration_per_processed_ms");

        separationWriter = createWriter(
                "objective_separation_summary.csv",
                "phase,scenario,device_count,iteration,network_condition,"
                + "search_variant,scheduler,objective,successful_results,"
                + "observed_destination_changes,"
                + "observed_same_tier_vm_changes,"
                + "observed_execution_target_changes,"
                + "observed_feasibility_restorations,objective_mismatches");
    }

    public synchronized void writeEligibility(
            String scheduler,
            int loaded,
            int eligibleSelected,
            int eligibleRestoration,
            int selectedSample,
            int restorationSample,
            int missingCandidates,
            int invalidProvenance)
            throws IOException {

        prefix(eligibilityWriter);
        text(eligibilityWriter, scheduler);
        number(eligibilityWriter, loaded);
        number(eligibilityWriter, eligibleSelected);
        number(eligibilityWriter, eligibleRestoration);
        number(eligibilityWriter, selectedSample);
        number(eligibilityWriter, restorationSample);
        number(eligibilityWriter, missingCandidates);
        number(eligibilityWriter, invalidProvenance);
        eligibilityWriter.newLine();
        eligibilityWriter.flush();
    }

    public synchronized void writeObjectiveSummary(
            String scheduler,
            DecisionChangeType objective,
            CounterfactualExperimentSummary s)
            throws IOException {

        prefix(objectiveWriter);
        text(objectiveWriter, scheduler);
        objectiveWriter.write(',');
        text(objectiveWriter, String.valueOf(objective));

        number(objectiveWriter, s.getSubmittedDecisionCount());
        number(objectiveWriter, s.getProcessedDecisionCount());
        number(objectiveWriter, s.getSuccessfulSearchCount());
        number(objectiveWriter, s.getUnsuccessfulSearchCount());
        number(objectiveWriter, s.getFailedExecutionCount());
        number(objectiveWriter, s.getSearchSuccessRate());
        number(objectiveWriter, s.getTotalAdaptiveGeneratedCandidateCount());
        number(objectiveWriter, s.getTotalAdaptiveEvaluatedCandidateCount());
        number(objectiveWriter, s.getTotalAdaptiveAcceptedCandidateCount());
        number(objectiveWriter, s.getTotalNeighborhoodGeneratedCandidateCount());
        number(objectiveWriter, s.getTotalNeighborhoodEvaluatedCandidateCount());
        number(objectiveWriter, s.getTotalNeighborhoodAcceptedCandidateCount());
        number(objectiveWriter, s.getTotalOverallGeneratedCandidateCount());
        number(objectiveWriter, s.getTotalOverallEvaluatedCandidateCount());
        number(objectiveWriter, s.getTotalOverallAcceptedCandidateCount());
        number(objectiveWriter, s.getAverageAdaptiveGeneratedCandidates());
        number(objectiveWriter, s.getAverageAdaptiveEvaluatedCandidates());
        number(objectiveWriter, s.getAverageAdaptiveAcceptedCandidates());
        number(objectiveWriter, s.getAverageNeighborhoodGeneratedCandidates());
        number(objectiveWriter, s.getAverageNeighborhoodEvaluatedCandidates());
        number(objectiveWriter, s.getAverageNeighborhoodAcceptedCandidates());
        number(objectiveWriter, s.getAverageOverallGeneratedCandidates());
        number(objectiveWriter, s.getAverageOverallEvaluatedCandidates());
        number(objectiveWriter, s.getAverageOverallAcceptedCandidates());
        number(objectiveWriter, s.getAdaptiveBestCandidateCount());
        number(objectiveWriter, s.getNeighborhoodBestCandidateCount());
        number(objectiveWriter, s.getNoBestCandidateCount());
        number(objectiveWriter, s.getNeighborhoodImprovedSearchCount());
        number(objectiveWriter, s.getNeighborhoodImprovementRate());
        number(objectiveWriter, s.getAdaptiveBestCandidateRate());
        number(objectiveWriter, s.getNeighborhoodBestCandidateRate());
        number(objectiveWriter, s.getExperimentDurationMilliseconds());

        double avgDuration = s.getProcessedDecisionCount() == 0
                ? 0.0
                : (double)s.getExperimentDurationMilliseconds()
                  / s.getProcessedDecisionCount();

        number(objectiveWriter, avgDuration);
        objectiveWriter.newLine();
        objectiveWriter.flush();
    }

    public synchronized void writeObjectiveSeparation(
            String scheduler,
            DecisionChangeType objective,
            PerDecisionCounterfactualSearchExecutor executor)
            throws IOException {

        prefix(separationWriter);
        text(separationWriter, scheduler);
        separationWriter.write(',');
        text(separationWriter, String.valueOf(objective));
        number(separationWriter, executor.getDiagnosticSuccessfulResultCount());
        number(separationWriter, executor.getDiagnosticDestinationChangeCount());
        number(separationWriter, executor.getDiagnosticSameTierVmChangeCount());
        number(separationWriter, executor.getDiagnosticExecutionTargetChangeCount());
        number(separationWriter, executor.getDiagnosticFeasibilityRestorationCount());
        number(separationWriter, executor.getDiagnosticObjectiveMismatchCount());
        separationWriter.newLine();
        separationWriter.flush();
    }

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

        prefix(separationWriter);
        text(separationWriter, scheduler);
        separationWriter.write(',');
        text(separationWriter, String.valueOf(objective));
        number(separationWriter, successfulResults);
        number(separationWriter, observedDestinationChanges);
        number(separationWriter, observedSameTierVmChanges);
        number(separationWriter, observedExecutionTargetChanges);
        number(separationWriter, observedFeasibilityRestorations);
        number(separationWriter, objectiveMismatches);
        separationWriter.newLine();
        separationWriter.flush();
    }

    private void writeConfiguration(
            FeasibleCounterfactualSearchConfig c,
            int maxSelected,
            int maxRestoration)
            throws IOException {

        try(BufferedWriter writer = createWriter(
                "experiment_configuration.csv",
                "phase,scenario,device_count,iteration,network_condition,"
                + "search_variant,maximum_selected_decisions,"
                + "maximum_restoration_decisions,search_mode,"
                + "maximum_generated_candidates,maximum_evaluated_candidates,"
                + "minimum_adaptive_evaluation_budget,"
                + "maximum_adaptive_evaluation_budget,"
                + "adaptive_budget_extension_size,"
                + "maximum_adaptive_budget_extensions,"
                + "neighborhood_enabled,maximum_boundary_seeds,"
                + "neighborhood_samples_per_seed,"
                + "maximum_neighborhood_candidates")) {

            prefix(writer);

            writer.write(
                    String.valueOf(
                            maxSelected));

            number(
                    writer,
                    maxRestoration);

            writer.write(',');

            text(
                    writer,
                    String.valueOf(
                            c.getSearchMode()));
            number(writer, c.getMaximumGeneratedCandidates());
            number(writer, c.getMaximumEvaluatedCandidates());
            number(writer, c.getMinimumAdaptiveEvaluationBudget());
            number(writer, c.getMaximumAdaptiveEvaluationBudget());
            number(writer, c.getAdaptiveBudgetExtensionSize());
            number(writer, c.getMaximumAdaptiveBudgetExtensions());
            writer.write(',');
            writer.write(String.valueOf(
                    c.isNeighborhoodRefinementEnabled()));
            number(writer, c.getMaximumBoundarySeeds());
            number(writer, c.getNeighborhoodSamplesPerSeed());
            number(writer, c.getMaximumNeighborhoodCandidates());
            writer.newLine();
        }
    }

    private BufferedWriter createWriter(String name, String header)
            throws IOException {
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(reportDirectory, name), false));
        writer.write(header);
        writer.newLine();
        writer.flush();
        return writer;
    }

    private void prefix(BufferedWriter writer) throws IOException {
        text(writer, "STAGE_A_FUNCTIONAL_COMPARISON");
        writer.write(',');
        text(writer, scenario);
        number(writer, deviceCount);
        number(writer, iteration);
        writer.write(',');
        text(writer, networkCondition);
        writer.write(',');
        text(writer, searchVariant);
        writer.write(',');
    }
    private static void text(
            BufferedWriter writer,
            String value)
            throws IOException {

        String safe =
                value == null
                ? ""
                : value;

        writer.write(
                "\""
                + safe.replace(
                        "\"",
                        "\"\"")
                + "\"");
    }

    private static void number(BufferedWriter writer, long value)
            throws IOException {
        writer.write(',');
        writer.write(String.valueOf(value));
    }

    private static void number(BufferedWriter writer, double value)
            throws IOException {
        writer.write(',');
        writer.write(String.valueOf(value));
    }

    public File getReportDirectory() {
        return reportDirectory;
    }

    @Override
    public synchronized void close() throws IOException {
        eligibilityWriter.close();
        objectiveWriter.close();
        separationWriter.close();
    }
}
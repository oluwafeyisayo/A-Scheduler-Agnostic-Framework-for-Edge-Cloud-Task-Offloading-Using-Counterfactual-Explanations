package edu.boun.edgecloudsim.research.counterfactual.evaluation.reporting;

import java.io.File;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualExperimentSummary;
import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualSearchMode;
import edu.boun.edgecloudsim.research.counterfactual
        .FeasibleCounterfactualSearchConfig;
import edu.boun.edgecloudsim.research.counterfactual.comparison
        .DecisionChangeType;
import edu.boun.edgecloudsim.research.counterfactual.evaluation.config
        .EvaluationExperimentConfig;
import edu.boun.edgecloudsim.research.counterfactual.evaluation.directory
        .EvaluationDirectoryManager;

/**
 * Minimal report-writer smoke test using zero-valued summaries.
 */
public final class EvaluationReportWriterTest {

    private EvaluationReportWriterTest() {
    }

    public static void main(
            String[] args) {

        try {

            EvaluationExperimentConfig config =
                    EvaluationExperimentConfig
                    .createStageB(
                            new File(
                                    "scripts/counterfactual_offloading/"
                                    + "config/"
                                    + "default_config_stage_b_100.properties"),
                            100);

            EvaluationDirectoryManager directoryManager =
                    new EvaluationDirectoryManager(
                            config);

            FeasibleCounterfactualSearchConfig searchConfig =
                    FeasibleCounterfactualSearchConfig
                    .createDefault(
                            CounterfactualSearchMode
                            .ADAPTIVE_BEST_FIRST);

            CounterfactualExperimentSummary zeroSummary =
                    new CounterfactualExperimentSummary(
                            0,
                            0,
                            0,
                            0,
                            0,

                            0L,
                            0L,
                            0L,

                            0L,
                            0L,
                            0L,

                            0L,
                            0L,
                            0L,

                            0,
                            0,
                            0,
                            0,

                            0L);

            try(EvaluationReportWriter writer =
                    new EvaluationReportWriter(
                            config,
                            directoryManager,
                            100,
                            1,
                            searchConfig)) {

                writer.writeEligibility(
                        "PPO",
                        100,
                        100,
                        0,
                        100,
                        0,
                        0,
                        0);

                writer.writeObjectiveSummary(
                        "PPO",
                        DecisionChangeType
                        .FEASIBILITY_RESTORATION,
                        zeroSummary);

                writer.writeObjectiveSeparation(
                        "PPO",
                        DecisionChangeType
                        .FEASIBILITY_RESTORATION,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0);
            }

            System.out.println(
                    "EvaluationReportWriter test passed.");

            System.out.println(
                    "Reports: "
                    + directoryManager
                    .getReportDirectory(
                            100,
                            1)
                    .getAbsolutePath());
        }
        catch(Exception exception) {

            exception.printStackTrace();

            throw new RuntimeException(
                    exception);
        }
    }
}
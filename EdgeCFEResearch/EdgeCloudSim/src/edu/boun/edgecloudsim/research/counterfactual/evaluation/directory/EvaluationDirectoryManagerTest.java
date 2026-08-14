package edu.boun.edgecloudsim.research.counterfactual.evaluation.directory;

import java.io.File;

import edu.boun.edgecloudsim.research.counterfactual.evaluation.config
        .EvaluationExperimentConfig;

/**
 * Small compilation and path-construction test for the evaluation
 * directory manager.
 */
public final class EvaluationDirectoryManagerTest {

    private EvaluationDirectoryManagerTest() {
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

            EvaluationDirectoryManager manager =
                    new EvaluationDirectoryManager(
                            config);

            System.out.println(
                    "Profile root: "
                    + manager
                    .getProfileRoot()
                    .getAbsolutePath());

            System.out.println(
                    "Iteration root: "
                    + manager
                    .getIterationDirectory(
                            100,
                            1)
                    .getAbsolutePath());

            System.out.println(
                    "Decision CSV: "
                    + manager
                    .getDecisionCsvFile(
                            100,
                            1,
                            "PPO")
                    .getAbsolutePath());

            System.out.println(
                    "Candidate CSV: "
                    + manager
                    .getCandidateCsvFile(
                            100,
                            1,
                            "PPO")
                    .getAbsolutePath());

            System.out.println(
                    "Report directory: "
                    + manager
                    .getReportDirectory(
                            100,
                            1)
                    .getAbsolutePath());

            System.out.println(
                    "EvaluationDirectoryManager test passed.");
        }
        catch(Exception exception) {

            exception.printStackTrace();

            throw new RuntimeException(
                    exception);
        }
    }
}
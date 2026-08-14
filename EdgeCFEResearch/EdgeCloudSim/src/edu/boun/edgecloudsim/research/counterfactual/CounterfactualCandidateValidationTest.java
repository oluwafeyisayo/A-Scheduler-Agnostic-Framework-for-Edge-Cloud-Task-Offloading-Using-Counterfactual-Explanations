package edu.boun.edgecloudsim.research.counterfactual;

import java.io.IOException;
import java.util.List;

public class CounterfactualCandidateValidationTest {

    public static void main(String[] args) {

        String csvFilePath =
                "sim_results/ite1/decision_logs/"
                + "offloading_decisions_"
                + "TWO_TIER_WITH_EO_"
                + "LEAST_LOADED_"
                + "500DEVICES_ITE1.csv";

        DecisionReplayEngine replayEngine =
                new DecisionReplayEngine();

        try {

            List<DecisionState> decisions =
                    replayEngine.loadSelectedDecisions(
                            csvFilePath);

            if(decisions.isEmpty()) {

                System.out.println(
                        "No selected decisions found.");

                return;
            }

            DecisionState originalDecision =
                    decisions.get(0);

            FeatureChange cpuChange =
                    new FeatureChange(
                            "selected_available_cpu_percent",
                            FeatureCategory.COMPUTE,
                            originalDecision
                            .getSelectedAvailableCpuPercent(),
                            28.0,
                            0.0,
                            100.0,
                            true,
                            true);

            ReplayState replayState =
                    ReplayState
                    .from(originalDecision)
                    .applyFeatureChange(cpuChange)
                    .build();

            CounterfactualValidator validator =
                    new CounterfactualValidator();

            FeasibilityResult feasibilityResult =
                    validator.validate(
                            replayState);

            CounterfactualClassifier classifier =
                    new CounterfactualClassifier();

            CounterfactualClassification classification =
                    classifier.classify(
                            replayState.getAppliedChanges(),
                            feasibilityResult);

//            boolean accepted =
//                    feasibilityResult
//                    .isOperationallyFeasible();
            boolean accepted =
                    false;

            String candidateReason;

            if(feasibilityResult.isOperationallyFeasible()) {

                candidateReason =
                        "The candidate is operationally feasible, but it has "
                        + "not been accepted because scheduler replay and "
                        + "decision-change verification were not performed.";
            }
            else {

                candidateReason =
                        "The candidate is not operationally feasible. "
                        + "Primary failure: "
                        + feasibilityResult.getPrimaryFailureReason();
            }

            CounterfactualScore score =
                    new CounterfactualScore(
                            replayState.calculateNormalizedDistance(),
                            1.0,
                            replayState.calculateNormalizedDistance(),
                            0.0,
                            replayState.calculateNormalizedDistance()
                            + 1.0
                            + replayState.calculateNormalizedDistance());

            CounterfactualCandidate candidate =
                    new CounterfactualCandidate.Builder(
                            originalDecision.getTaskId(),
                            1)

                    .addFeatureChanges(
                            replayState.getAppliedChanges())

                    .withReplayState(
                            replayState)

                    .withFeasibilityResult(
                            feasibilityResult)

                    .withScore(
                            score)

                    .withEvaluated(
                            true)

                    .withAccepted(
                            accepted)

                    .withReason(
                            candidateReason)

                    .build();

            System.out.println(
                    "ReplayState:");

            System.out.println(
                    replayState);

            System.out.println();

            System.out.println(
                    "FeasibilityResult:");

            System.out.println(
                    feasibilityResult);

            System.out.println();

            System.out.println(
                    "CounterfactualClassification:");

            System.out.println(
                    classification);

            System.out.println();

            System.out.println(
                    "Automatic class: "
                    + classification
                    .getClassification());

            System.out.println(
                    "Primary cause: "
                    + classification
                    .getPrimaryCause());

            System.out.println(
                    "Changed categories: "
                    + classification
                    .getChangedCategories());

            System.out.println(
                    "Failed constraints: "
                    + classification
                    .getFailedConstraints());

            System.out.println(
                    "Classification confidence: "
                    + classification
                    .getConfidenceScore());

            System.out.println(
                    "Explanation: "
                    + classification
                    .getExplanationSummary());

            System.out.println();

            System.out.println(
                    "CounterfactualCandidate:");

            System.out.println(
                    candidate);

            System.out.println();

            System.out.println(
                    "Selected-resource CPU feasible: "
                    + candidate
                    .getFeasibilityResult()
                    .isSelectedResourceFeasible());

            System.out.println(
                    "Candidate-set feasible: "
                    + candidate
                    .getFeasibilityResult()
                    .isCandidateSetFeasible());

            System.out.println(
                    "Operationally feasible: "
                    + candidate
                    .getFeasibilityResult()
                    .isOperationallyFeasible());

            System.out.println(
                    "Overall feasible: "
                    + candidate
                    .getFeasibilityResult()
                    .isOverallFeasible());

            System.out.println(
                    "Feasibility failure: "
                    + candidate
                    .getFeasibilityResult()
                    .getPrimaryFailureReason());

            System.out.println(
                    "Candidate reason: "
                    + candidate
                    .getReason());

            System.out.println(
                    "Accepted counterfactual: "
                    + candidate
                    .isAccepted());

            System.out.println(
                    "Candidate and result use same task: "
                    + (originalDecision.getTaskId()
                    == candidate
                    .getFeasibilityResult()
                    .getTaskId()));
        }
        catch(IOException exception) {

            System.err.println(
                    "Test failed while reading CSV: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
        catch(IllegalArgumentException exception) {

            System.err.println(
                    "Invalid candidate input: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
        catch(IllegalStateException exception) {

            System.err.println(
                    "Candidate construction failed: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
    }
}
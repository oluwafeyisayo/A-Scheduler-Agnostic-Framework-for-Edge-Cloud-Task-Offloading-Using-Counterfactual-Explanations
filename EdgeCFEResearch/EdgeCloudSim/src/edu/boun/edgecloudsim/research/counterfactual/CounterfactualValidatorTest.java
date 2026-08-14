package edu.boun.edgecloudsim.research.counterfactual;

import java.io.IOException;
import java.util.List;

public class CounterfactualValidatorTest {

	public static void main(String[] args) {

		String csvFilePath = "sim_results/ite1/decision_logs/" + "offloading_decisions_" + "TWO_TIER_WITH_EO_"
				+ "LEAST_LOADED_" + "500DEVICES_ITE1.csv";

		DecisionReplayEngine replayEngine = new DecisionReplayEngine();

		try {

			List<DecisionState> decisions = replayEngine.loadSelectedDecisions(csvFilePath);

			if (decisions.isEmpty()) {

				System.out.println("No selected decisions found.");

				return;
			}

			DecisionState originalDecision = decisions.get(0);

			FeatureChange cpuChange = new FeatureChange("selected_available_cpu_percent", FeatureCategory.COMPUTE,
					originalDecision.getSelectedAvailableCpuPercent(), 28.0, 0.0, 100.0, true, true);

			ReplayState replayState = ReplayState.from(originalDecision).applyFeatureChange(cpuChange).build();

			CounterfactualValidator validator = new CounterfactualValidator();

			FeasibilityResult result = validator.validate(replayState);

			System.out.println("ReplayState:");

			System.out.println(replayState);

			System.out.println();

			System.out.println("FeasibilityResult:");

			System.out.println(result);

			System.out.println();

			System.out.println("Constraint evaluations:");

			for (ConstraintEvaluation evaluation : result.getEvaluations()) {

				System.out.println(evaluation);
			}

			System.out.println();

			System.out.println("Feature validity satisfied: "
					+ result.isConstraintSatisfied(FeasibilityConstraint.FEATURE_VALIDITY));

			System.out.println("Structural consistency satisfied: "
					+ result.isConstraintSatisfied(FeasibilityConstraint.STRUCTURAL_CONSISTENCY));

			System.out.println(
					"CPU capacity satisfied: " + result.isConstraintSatisfied(FeasibilityConstraint.CANDIDATE_SET_CPU_CAPACITY));

			System.out.println("Overall feasible: " + result.isOverallFeasible());

			System.out.println("Failed constraint count: " + result.getFailedConstraintCount());

			System.out.println("Primary failure reason: " + result.getPrimaryFailureReason());

			ConstraintEvaluation cpuEvaluation = result.getEvaluation(FeasibilityConstraint.CANDIDATE_SET_CPU_CAPACITY);

			if (cpuEvaluation != null) {

				System.out.println("CPU feasibility margin: " + cpuEvaluation.getMargin());

				System.out.println("CPU validation message: " + cpuEvaluation.getMessage());
			}
		} catch (IOException exception) {

			System.err.println("Validator test failed while reading CSV: " + exception.getMessage());

			exception.printStackTrace();
		} catch (IllegalArgumentException exception) {

			System.err.println("Invalid validator input: " + exception.getMessage());

			exception.printStackTrace();
		} catch (IllegalStateException exception) {

			System.err.println("Replay-state validation failed: " + exception.getMessage());

			exception.printStackTrace();
		}
	}
}
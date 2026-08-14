package edu.boun.edgecloudsim.research.counterfactual;

import java.io.IOException;
import java.util.List;

public class ReplayStateTest {

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
                    replayEngine
                    .loadSelectedDecisions(
                            csvFilePath);

            if(decisions.isEmpty()){

                System.out.println(
                        "No selected decisions found.");

                return;
            }

            DecisionState originalDecision =
                    decisions.get(0);

            System.out.println(
                    "Original DecisionState:");

            System.out.println(
                    originalDecision);

            System.out.println();

            /*
             * Create a valid CPU change object.
             *
             * The change itself is structurally valid because 28 is
             * within the allowed range of 0 to 100.
             *
             * However, the resulting replay state will be CPU-infeasible
             * because the task requires 30%.
             */
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

            ReplayState modifiedState =
                    ReplayState
                    .from(originalDecision)
                    .applyFeatureChange(cpuChange)
                    .build();

            System.out.println(
                    "Applied FeatureChange:");

            System.out.println(
                    cpuChange);

            System.out.println();

            System.out.println(
                    "Modified ReplayState:");

            System.out.println(
                    modifiedState);

            System.out.println();

            /*
             * Confirm that the original object was not modified.
             */
            System.out.println(
                    "Original available CPU: "
                    + originalDecision
                    .getSelectedAvailableCpuPercent());

            System.out.println(
                    "Replay available CPU: "
                    + modifiedState
                    .getSelectedAvailableCpuPercent());

            System.out.println(
                    "Required CPU: "
                    + modifiedState
                    .getRequiredCpuPercent());

            System.out.println(
                    "Replay CPU feasible: "
                    + modifiedState
                    .isCpuFeasible());

            System.out.println(
                    "CPU feasibility margin: "
                    + modifiedState
                    .getCpuFeasibilityMargin());

            System.out.println(
                    "Has applied changes: "
                    + modifiedState
                    .hasAppliedChanges());

            System.out.println(
                    "Changed feature count: "
                    + modifiedState
                    .getChangedFeatureCount());

            System.out.println(
                    "Valid applied changes: "
                    + modifiedState
                    .hasValidAppliedChanges());

            System.out.println(
                    "Normalized distance: "
                    + modifiedState
                    .calculateNormalizedDistance());

            System.out.println();

            /*
             * Verify that the original DecisionState remains unchanged.
             */
            boolean originalUnchanged =
                    originalDecision
                    .getSelectedAvailableCpuPercent()
                    == 100.0;

            System.out.println(
                    "Original DecisionState unchanged: "
                    + originalUnchanged);

            /*
             * Locate the applied feature by name.
             */
            FeatureChange foundChange =
                    modifiedState
                    .findAppliedChange(
                            "selected_available_cpu_percent");

            System.out.println(
                    "Applied feature found: "
                    + (foundChange != null));
        }
        catch(IOException exception){

            System.err.println(
                    "ReplayState test failed: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
        catch(IllegalArgumentException exception){

            System.err.println(
                    "Invalid replay-state input: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
        catch(IllegalStateException exception){

            System.err.println(
                    "ReplayState validation failed: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
    }
}
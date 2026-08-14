package edu.boun.edgecloudsim.research.counterfactual;

import java.io.IOException;
import java.util.List;

public class DecisionReplayTest {

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
                    replayEngine.loadDecisions(
                            csvFilePath);

            System.out.println(
                    "Total decisions loaded: "
                    + decisions.size());

            if(!decisions.isEmpty()){

                DecisionState firstDecision =
                        decisions.get(0);

                System.out.println(
                        "First decision:");

                System.out.println(
                        firstDecision);

                System.out.println(
                        "Selected decision: "
                        + firstDecision
                        .isSelectedDecision());

                System.out.println(
                        "CPU feasible: "
                        + firstDecision
                        .isCpuFeasible());

                System.out.println(
                        "Best available CPU selected: "
                        + firstDecision
                        .selectedVmHasBestAvailableCpu());
            }
        }
        catch(IOException exception){

            System.err.println(
                    "Replay failed: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
    }
}
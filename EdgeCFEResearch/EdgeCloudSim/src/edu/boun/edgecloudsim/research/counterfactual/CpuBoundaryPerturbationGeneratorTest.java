package edu.boun.edgecloudsim.research.counterfactual;

import java.io.IOException;
import java.util.List;

public class CpuBoundaryPerturbationGeneratorTest {

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

            CounterfactualPerturbationGenerator generator =
                    new CpuBoundaryPerturbationGenerator(
                            0.1);

            List<GeneratedReplayState> generatedStates =
                    generator.generate(
                            originalDecision);

            CounterfactualValidator validator =
                    new CounterfactualValidator();

            CounterfactualClassifier classifier =
                    new CounterfactualClassifier();

            System.out.println(
                    "Original DecisionState:");

            System.out.println(
                    originalDecision);

            System.out.println();

            System.out.println(
                    "Generator: "
                    + generator.getGeneratorName());

            System.out.println(
                    "Generated state count: "
                    + generatedStates.size());

            System.out.println();

            int candidateNumber = 1;

            for(GeneratedReplayState generatedState
                    : generatedStates){

                ReplayState replayState =
                        generatedState
                        .getReplayState();

                FeasibilityResult feasibilityResult =
                        validator.validate(
                                replayState);

                CounterfactualClassification classification =
                        classifier.classify(
                                replayState.getAppliedChanges(),
                                feasibilityResult);

                System.out.println(
                        "Generated candidate "
                        + candidateNumber
                        + ":");

                System.out.println(
                        generatedState);

                System.out.println(
                        replayState);

                System.out.println(
                        feasibilityResult);

                System.out.println(
                        classification);

                System.out.println(
                        "Generated CPU value: "
                        + replayState
                        .getSelectedAvailableCpuPercent());

                System.out.println(
                        "Required CPU value: "
                        + replayState
                        .getRequiredCpuPercent());

                System.out.println(
                        "CPU margin: "
                        + replayState
                        .getCpuFeasibilityMargin());

                System.out.println(
                        "Overall feasible: "
                        + feasibilityResult
                        .isOverallFeasible());

                System.out.println(
                        "Automatic class: "
                        + classification
                        .getClassification());

                System.out.println(
                        "Generation reason: "
                        + generatedState
                        .getGenerationReason());

                System.out.println(
                        "Scheduler decision replayed: false");

                System.out.println();

                candidateNumber++;
            }
        }
        catch(IOException exception){

            System.err.println(
                    "Unable to read decision CSV: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
        catch(IllegalArgumentException exception){

            System.err.println(
                    "Invalid generator input: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
        catch(IllegalStateException exception){

            System.err.println(
                    "Generated replay state is invalid: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
    }
}
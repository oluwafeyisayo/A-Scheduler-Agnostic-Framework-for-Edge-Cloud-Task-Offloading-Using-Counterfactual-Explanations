package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates CPU perturbations around the task's CPU feasibility boundary.
 *
 * For a required CPU value R, the generator creates:
 *
 * 1. R       — minimally feasible boundary;
 * 2. R-step  — minimally infeasible boundary.
 */
public final class CpuBoundaryPerturbationGenerator
        implements CounterfactualPerturbationGenerator {

    private static final double MIN_CPU_PERCENT =
            0.0;

    private static final double MAX_CPU_PERCENT =
            100.0;

    private final double boundaryStep;

    public CpuBoundaryPerturbationGenerator() {

        this(0.1);
    }

    public CpuBoundaryPerturbationGenerator(
            double boundaryStep) {

        if(Double.isNaN(boundaryStep)
                || Double.isInfinite(boundaryStep)
                || boundaryStep <= 0.0){

            throw new IllegalArgumentException(
                    "Boundary step must be greater than zero.");
        }

        this.boundaryStep =
                boundaryStep;
    }

    @Override
    public String getGeneratorName() {

        return "CPU_BOUNDARY_GENERATOR";
    }

    @Override
    public List<GeneratedReplayState> generate(
            DecisionState originalDecision) {

        if(originalDecision == null){

            throw new IllegalArgumentException(
                    "Original DecisionState cannot be null.");
        }

        List<GeneratedReplayState> generatedStates =
                new ArrayList<GeneratedReplayState>();

        double originalAvailableCpu =
                originalDecision
                .getSelectedAvailableCpuPercent();

        double requiredCpu =
                originalDecision
                .getRequiredCpuPercent();

        /*
         * Candidate 1: exact feasibility boundary.
         */
        addGeneratedState(
                generatedStates,
                originalDecision,
                originalAvailableCpu,
                requiredCpu,
                "CPU_BOUNDARY_FEASIBLE",
                "Available CPU was moved to the minimum "
                + "value that exactly satisfies the task requirement.");

        /*
         * Candidate 2: immediately below the feasibility boundary.
         */
        double belowBoundary =
                Math.max(
                        MIN_CPU_PERCENT,
                        requiredCpu - boundaryStep);

        addGeneratedState(
                generatedStates,
                originalDecision,
                originalAvailableCpu,
                belowBoundary,
                "CPU_BOUNDARY_INFEASIBLE",
                "Available CPU was moved immediately below "
                + "the task requirement.");

        return Collections.unmodifiableList(
                generatedStates);
    }

    private void addGeneratedState(
            List<GeneratedReplayState> generatedStates,
            DecisionState originalDecision,
            double originalAvailableCpu,
            double counterfactualAvailableCpu,
            String generationReasonCode,
            String generationDescription) {

        if(counterfactualAvailableCpu < MIN_CPU_PERCENT
                || counterfactualAvailableCpu > MAX_CPU_PERCENT){

            return;
        }

        /*
         * Do not generate an unchanged perturbation.
         */
        if(Double.compare(
                originalAvailableCpu,
                counterfactualAvailableCpu) == 0){

            return;
        }

        FeatureChange cpuChange =
                new FeatureChange(
                        "selected_available_cpu_percent",
                        FeatureCategory.COMPUTE,
                        originalAvailableCpu,
                        counterfactualAvailableCpu,
                        MIN_CPU_PERCENT,
                        MAX_CPU_PERCENT,
                        true,
                        true);

        ReplayState replayState =
                ReplayState
                .from(originalDecision)
                .applyFeatureChange(
                        cpuChange)
                .build();

        GeneratedReplayState generatedReplayState =
                new GeneratedReplayState(
                        replayState,
                        cpuChange,
                        getGeneratorName(),
                        generationReasonCode
                        + ": "
                        + generationDescription);

        generatedStates.add(
                generatedReplayState);
    }
}
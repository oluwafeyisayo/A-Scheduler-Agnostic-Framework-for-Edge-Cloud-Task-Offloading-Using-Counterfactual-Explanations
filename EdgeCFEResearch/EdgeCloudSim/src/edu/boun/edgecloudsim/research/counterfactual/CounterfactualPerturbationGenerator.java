package edu.boun.edgecloudsim.research.counterfactual;

import java.util.List;

/**
 * Common interface for automatic state-perturbation generators.
 */
public interface CounterfactualPerturbationGenerator {

    String getGeneratorName();

    List<GeneratedReplayState> generate(
            DecisionState originalDecision);
}
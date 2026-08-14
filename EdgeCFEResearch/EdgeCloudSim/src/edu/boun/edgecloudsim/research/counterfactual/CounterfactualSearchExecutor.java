package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Executes RG-FCS for one original decision state.
 *
 * This interface separates the batch experiment runner from the
 * exact constructor and method signature used by the RG-FCS engine.
 */
public interface CounterfactualSearchExecutor {

    CounterfactualSearchResult search(
            DecisionState decisionState);
}
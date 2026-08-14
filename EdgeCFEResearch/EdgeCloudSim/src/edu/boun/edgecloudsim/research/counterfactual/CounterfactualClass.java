package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Describes the primary cause of a counterfactual decision change.
 */
public enum CounterfactualClass {

    RESOURCE_DRIVEN,
    NETWORK_DRIVEN,
    CAPACITY_DRIVEN,
    MOBILITY_DRIVEN,
    SERVICE_DRIVEN,
    POLICY_DRIVEN,
    MULTI_FACTOR,
    NOT_CLASSIFIED
}
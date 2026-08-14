package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Feasibility constraints supported by the counterfactual
 * validation framework.
 */
public enum FeasibilityConstraint {

    /*
     * General state validation.
     */
    FEATURE_VALIDITY,

    STRUCTURAL_CONSISTENCY,

    /*
     * Compute-resource feasibility.
     */
    SELECTED_RESOURCE_CPU_CAPACITY,

    CANDIDATE_SET_CPU_CAPACITY,

    SELECTED_RESOURCE_CORE_CAPACITY,

    CANDIDATE_SET_CORE_CAPACITY,

    SELECTED_RESOURCE_PROCESSING_CAPACITY,

    CANDIDATE_SET_COMPUTE_FEASIBILITY,

    /*
     * Network feasibility.
     */
    BANDWIDTH,

    LATENCY,

    CONNECTIVITY,

    SELECTED_RESOURCE_NETWORK_PATH,

    CANDIDATE_SET_NETWORK_PATH,

    CANDIDATE_SET_COMPUTE_NETWORK_FEASIBILITY,
    
    CANDIDATE_SET_CONNECTIVITY,

    /*
     * Task-level feasibility.
     */
    TASK_LENGTH,

    UPLOAD_SIZE,

    /*
     * Constraints reserved for later framework versions.
     */
    
    SERVICE_LEVEL
}
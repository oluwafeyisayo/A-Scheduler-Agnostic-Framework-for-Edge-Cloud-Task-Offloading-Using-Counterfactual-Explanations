package edu.boun.edgecloudsim.research.counterfactual.verification;

/**
 * Final verification status assigned to a generated
 * counterfactual candidate.
 */
public enum CounterfactualVerificationStatus {

    /**
     * Replay succeeded, the candidate was feasible, and the
     * requested decision change was achieved.
     */
    VERIFIED,

    /**
     * The modified state violates one or more operational
     * feasibility constraints.
     */
    INFEASIBLE,

    /**
     * The scheduler could not be replayed deterministically.
     */
    REPLAY_INDETERMINATE,

    /**
     * Replay succeeded, but the requested decision change did not
     * occur.
     */
    TARGET_NOT_ACHIEVED,

    /**
     * The replayed decision did not change.
     */
    NO_DECISION_CHANGE,

    /**
     * Verification encountered inconsistent or invalid data.
     */
    ERROR
}
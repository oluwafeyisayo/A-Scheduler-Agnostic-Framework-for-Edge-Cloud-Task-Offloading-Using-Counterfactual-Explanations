package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Consolidated feasibility result for one ReplayState or
 * CounterfactualCandidate.
 *
 * Overall feasibility requires every evaluated constraint to pass.
 *
 * Operational feasibility allows the originally selected resource to
 * become infeasible, provided another candidate resource satisfies the
 * authoritative candidate-set compute-network model and all mandatory
 * task-level constraints pass.
 */
public final class FeasibilityResult {

    private final int taskId;

    private final List<ConstraintEvaluation>
            evaluations;

    private final boolean overallFeasible;

    private final String primaryFailureReason;

    private FeasibilityResult(
            Builder builder) {

        this.taskId =
                builder.taskId;

        this.evaluations =
                Collections.unmodifiableList(
                        new ArrayList<ConstraintEvaluation>(
                                builder.evaluations));

        this.overallFeasible =
                calculateOverallFeasibility(
                        evaluations);

        this.primaryFailureReason =
                determinePrimaryFailureReason(
                        evaluations);
    }

    public int getTaskId() {

        return taskId;
    }

    public List<ConstraintEvaluation>
            getEvaluations() {

        return evaluations;
    }

    public boolean isOverallFeasible() {

        return overallFeasible;
    }

    public String getPrimaryFailureReason() {

        return primaryFailureReason;
    }

    public int getEvaluationCount() {

        return evaluations.size();
    }

    /**
     * Returns the evaluation associated with a constraint.
     */
    public ConstraintEvaluation getEvaluation(
            FeasibilityConstraint constraint) {

        if(constraint == null) {

            return null;
        }

        for(ConstraintEvaluation evaluation
                : evaluations) {

            if(evaluation.getConstraint()
                    == constraint) {

                return evaluation;
            }
        }

        return null;
    }

    /**
     * Returns true only when the constraint was evaluated and passed.
     */
    public boolean isConstraintSatisfied(
            FeasibilityConstraint constraint) {

        ConstraintEvaluation evaluation =
                getEvaluation(
                        constraint);

        return evaluation != null
                && evaluation.isSatisfied();
    }

    /**
     * Returns true when a constraint was not evaluated, or when it was
     * evaluated and passed.
     */
    private boolean isSatisfiedWhenEvaluated(
            FeasibilityConstraint constraint) {

        ConstraintEvaluation evaluation =
                getEvaluation(
                        constraint);

        return evaluation == null
                || evaluation.isSatisfied();
    }

    /**
     * Returns true when the originally selected VM satisfies all
     * evaluated selected-resource compute and network constraints.
     */
    public boolean isSelectedResourceFeasible() {

        return isSatisfiedWhenEvaluated(
                FeasibilityConstraint
                .SELECTED_RESOURCE_CPU_CAPACITY)

                && isSatisfiedWhenEvaluated(
                        FeasibilityConstraint
                        .SELECTED_RESOURCE_CORE_CAPACITY)

                && isSatisfiedWhenEvaluated(
                        FeasibilityConstraint
                        .SELECTED_RESOURCE_PROCESSING_CAPACITY)

                && isSatisfiedWhenEvaluated(
                        FeasibilityConstraint
                        .SELECTED_RESOURCE_NETWORK_PATH);
    }

    /**
     * Returns true when at least one operational candidate satisfies
     * the complete compute-network feasibility model.
     *
     * The combined compute-network constraint is authoritative because
     * it verifies that the same VM satisfies CPU, cores, processing
     * capacity, operational availability, and network-path validity.
     *
     * Separate CPU, core, compute, and network candidate-set rules are
     * retained as diagnostics and do not independently veto a candidate
     * that passed the authoritative combined rule.
     */
    public boolean isCandidateSetFeasible() {

        ConstraintEvaluation combinedEvaluation =
                getEvaluation(
                        FeasibilityConstraint
                        .CANDIDATE_SET_COMPUTE_NETWORK_FEASIBILITY);

        /*
         * Prefer the authoritative combined rule when available.
         */
        if(combinedEvaluation != null) {

            if(!combinedEvaluation.isSatisfied()) {

                return false;
            }

            return isSatisfiedWhenEvaluated(
                    FeasibilityConstraint
                    .CANDIDATE_SET_CONNECTIVITY);
        }

        /*
         * Backward-compatible fallback for validators that do not
         * register the combined compute-network rule.
         */
        if(!isSatisfiedWhenEvaluated(
                FeasibilityConstraint
                .CANDIDATE_SET_COMPUTE_FEASIBILITY)) {

            return false;
        }

        if(!isSatisfiedWhenEvaluated(
                FeasibilityConstraint
                .CANDIDATE_SET_NETWORK_PATH)) {

            return false;
        }

        if(!isSatisfiedWhenEvaluated(
                FeasibilityConstraint
                .CANDIDATE_SET_CONNECTIVITY)) {

            return false;
        }

        return true;
    }

    /**
     * Returns true when the modified state remains operationally
     * schedulable.
     *
     * The originally selected VM may become infeasible. Another VM in
     * the synchronized candidate set may still satisfy the complete
     * compute-network feasibility model.
     */
    public boolean isOperationallyFeasible() {

        if(!isConstraintSatisfied(
                FeasibilityConstraint
                .FEATURE_VALIDITY)) {

            return false;
        }

        if(!isConstraintSatisfied(
                FeasibilityConstraint
                .STRUCTURAL_CONSISTENCY)) {

            return false;
        }

        if(!isCandidateSetFeasible()) {

            return false;
        }

        if(!isSatisfiedWhenEvaluated(
                FeasibilityConstraint
                .BANDWIDTH)) {

            return false;
        }

        if(!isSatisfiedWhenEvaluated(
                FeasibilityConstraint
                .LATENCY)) {

            return false;
        }

        if(!isSatisfiedWhenEvaluated(
                FeasibilityConstraint
                .CONNECTIVITY)) {

            return false;
        }

        if(!isSatisfiedWhenEvaluated(
                FeasibilityConstraint
                .TASK_LENGTH)) {

            return false;
        }

        if(!isSatisfiedWhenEvaluated(
                FeasibilityConstraint
                .UPLOAD_SIZE)) {

            return false;
        }

        if(!isSatisfiedWhenEvaluated(
                FeasibilityConstraint
                .SERVICE_LEVEL)) {

            return false;
        }

        return true;
    }

    /**
     * Returns the number of failed constraints.
     */
    public int getFailedConstraintCount() {

        int count =
                0;

        for(ConstraintEvaluation evaluation
                : evaluations) {

            if(!evaluation.isSatisfied()) {

                count++;
            }
        }

        return count;
    }

    /**
     * Returns all failed evaluations.
     */
    public List<ConstraintEvaluation>
            getFailedEvaluations() {

        List<ConstraintEvaluation> failed =
                new ArrayList<ConstraintEvaluation>();

        for(ConstraintEvaluation evaluation
                : evaluations) {

            if(!evaluation.isSatisfied()) {

                failed.add(
                        evaluation);
            }
        }

        return Collections.unmodifiableList(
                failed);
    }

    private static boolean calculateOverallFeasibility(
            List<ConstraintEvaluation> evaluations) {

        if(evaluations == null
                || evaluations.isEmpty()) {

            return false;
        }

        for(ConstraintEvaluation evaluation
                : evaluations) {

            if(evaluation == null
                    || !evaluation.isSatisfied()) {

                return false;
            }
        }

        return true;
    }

    private static String determinePrimaryFailureReason(
            List<ConstraintEvaluation> evaluations) {

        if(evaluations == null
                || evaluations.isEmpty()) {

            return "NO_CONSTRAINTS_EVALUATED";
        }

        FeasibilityConstraint[] operationalPriority = {

                FeasibilityConstraint
                .FEATURE_VALIDITY,

                FeasibilityConstraint
                .STRUCTURAL_CONSISTENCY,

                FeasibilityConstraint
                .CANDIDATE_SET_COMPUTE_NETWORK_FEASIBILITY,

                FeasibilityConstraint
                .CANDIDATE_SET_CONNECTIVITY,

                FeasibilityConstraint
                .CANDIDATE_SET_NETWORK_PATH,

                FeasibilityConstraint
                .CANDIDATE_SET_COMPUTE_FEASIBILITY,

                FeasibilityConstraint
                .CANDIDATE_SET_CPU_CAPACITY,

                FeasibilityConstraint
                .CANDIDATE_SET_CORE_CAPACITY,

                FeasibilityConstraint
                .BANDWIDTH,

                FeasibilityConstraint
                .LATENCY,

                FeasibilityConstraint
                .CONNECTIVITY,

                FeasibilityConstraint
                .TASK_LENGTH,

                FeasibilityConstraint
                .UPLOAD_SIZE,

                FeasibilityConstraint
                .SERVICE_LEVEL
        };

        for(FeasibilityConstraint priorityConstraint
                : operationalPriority) {

            for(ConstraintEvaluation evaluation
                    : evaluations) {

                if(!evaluation.isSatisfied()
                        && evaluation.getConstraint()
                                == priorityConstraint) {

                    return priorityConstraint.name();
                }
            }
        }

        /*
         * If only selected-resource diagnostic constraints failed,
         * report the first one.
         */
        for(ConstraintEvaluation evaluation
                : evaluations) {

            if(!evaluation.isSatisfied()) {

                return evaluation
                        .getConstraint()
                        .name();
            }
        }

        return "NONE";
    }

    @Override
    public String toString() {

        return "FeasibilityResult{"
                + "taskId="
                + taskId
                + ", evaluationCount="
                + evaluations.size()
                + ", failedConstraintCount="
                + getFailedConstraintCount()
                + ", overallFeasible="
                + overallFeasible
                + ", operationallyFeasible="
                + isOperationallyFeasible()
                + ", selectedResourceFeasible="
                + isSelectedResourceFeasible()
                + ", candidateSetFeasible="
                + isCandidateSetFeasible()
                + ", primaryFailureReason='"
                + primaryFailureReason
                + '\''
                + '}';
    }

    public static final class Builder {

        private final int taskId;

        private final List<ConstraintEvaluation>
                evaluations =
                new ArrayList<ConstraintEvaluation>();

        public Builder(
                int taskId) {

            if(taskId < 0) {

                throw new IllegalArgumentException(
                        "Task ID cannot be negative.");
            }

            this.taskId =
                    taskId;
        }

        public Builder addEvaluation(
                ConstraintEvaluation evaluation) {

            if(evaluation == null) {

                throw new IllegalArgumentException(
                        "Constraint evaluation cannot be null.");
            }

            evaluations.add(
                    evaluation);

            return this;
        }

        public Builder addEvaluations(
                List<ConstraintEvaluation> evaluations) {

            if(evaluations == null) {

                throw new IllegalArgumentException(
                        "Constraint-evaluation list cannot be null.");
            }

            for(ConstraintEvaluation evaluation
                    : evaluations) {

                addEvaluation(
                        evaluation);
            }

            return this;
        }

        public FeasibilityResult build() {

            return new FeasibilityResult(
                    this);
        }
    }
}
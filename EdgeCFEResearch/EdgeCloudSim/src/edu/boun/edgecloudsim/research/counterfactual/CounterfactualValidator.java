package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

/**
 * Evaluates reconstructed replay states and synchronized VM candidate
 * sets using registered feasibility rules.
 *
 * Validation is divided into:
 *
 * 1. Task-level and network-level ReplayState constraints.
 * 2. Selected-resource constraints used only when no candidate set is
 *    supplied.
 * 3. Candidate-set constraints used when synchronized VM snapshots are
 *    supplied.
 *
 * This separation prevents stale or absent selected-resource fields in
 * ReplayState from incorrectly invalidating counterfactual candidates.
 */
public final class CounterfactualValidator {

    private static final double MINIMUM_BANDWIDTH_MBPS =
            1.0;

    /**
     * General ReplayState constraints that remain valid regardless of
     * which VM candidate is selected.
     */
    private final List<FeasibilityRule>
            replayStateRules;

    /**
     * Rules that inspect the selected resource stored directly in
     * ReplayState.
     *
     * These rules are evaluated only by the backward-compatible
     * ReplayState-only validation method.
     */
    private final List<FeasibilityRule>
            selectedResourceRules;

    /**
     * Rules that evaluate the complete synchronized VM candidate set.
     */
    private final List<CandidateSetFeasibilityRule>
            candidateSetRules;

    /**
     * Creates the default counterfactual validator.
     */
    public CounterfactualValidator() {

        List<FeasibilityRule> defaultReplayStateRules =
                new ArrayList<FeasibilityRule>();

        /*
         * General state validity and consistency.
         */
        defaultReplayStateRules.add(
                new FeatureValidityRule());

        defaultReplayStateRules.add(
                new StructuralConsistencyRule());

        /*
         * Task-level network constraints.
         */
        defaultReplayStateRules.add(
                new BandwidthFeasibilityRule(
                        MINIMUM_BANDWIDTH_MBPS));

        defaultReplayStateRules.add(
                new ConnectivityFeasibilityRule());

        /*
         * Rules that depend on the selected resource stored in
         * ReplayState.
         *
         * These must not be evaluated when synchronized VM candidates
         * are available because ReplayState may still describe the
         * original selection or contain -1 identifiers for an
         * originally unschedulable task.
         */
        List<FeasibilityRule> defaultSelectedResourceRules =
                new ArrayList<FeasibilityRule>();

        defaultSelectedResourceRules.add(
                new CpuFeasibilityRule());

        defaultSelectedResourceRules.add(
                new CoreFeasibilityRule());

        defaultSelectedResourceRules.add(
                new ProcessingCapacityRule());

        defaultSelectedResourceRules.add(
                new SelectedResourceNetworkFeasibilityRule());

        /*
         * Candidate-set constraints.
         *
         * Each rule is registered exactly once.
         */
        List<CandidateSetFeasibilityRule>
                defaultCandidateSetRules =
                new ArrayList<CandidateSetFeasibilityRule>();

        defaultCandidateSetRules.add(
                new CandidateSetCpuFeasibilityRule());

        defaultCandidateSetRules.add(
                new CandidateSetCoreFeasibilityRule());

        defaultCandidateSetRules.add(
                new CandidateSetComputeFeasibilityRule());

        defaultCandidateSetRules.add(
                new CandidateSetNetworkFeasibilityRule());

        defaultCandidateSetRules.add(
                new CandidateSetConnectivityRule());

        defaultCandidateSetRules.add(
                new CandidateSetComputeNetworkFeasibilityRule());

        this.replayStateRules =
                Collections.unmodifiableList(
                        defaultReplayStateRules);

        this.selectedResourceRules =
                Collections.unmodifiableList(
                        defaultSelectedResourceRules);

        this.candidateSetRules =
                Collections.unmodifiableList(
                        defaultCandidateSetRules);
    }

    /**
     * Creates a validator using explicitly supplied ReplayState rules.
     *
     * The supplied rules are treated as general ReplayState rules.
     * No selected-resource or candidate-set rules are registered.
     */
    public CounterfactualValidator(
            List<FeasibilityRule> rules) {

        this(
                rules,
                Collections
                .<FeasibilityRule>emptyList(),
                Collections
                .<CandidateSetFeasibilityRule>emptyList());
    }

    /**
     * Creates a validator using explicitly supplied ReplayState and
     * candidate-set rules.
     *
     * The supplied ReplayState rules are treated as general rules.
     */
    public CounterfactualValidator(
            List<FeasibilityRule> replayStateRules,
            List<CandidateSetFeasibilityRule> candidateSetRules) {

        this(
                replayStateRules,
                Collections
                .<FeasibilityRule>emptyList(),
                candidateSetRules);
    }

    /**
     * Creates a validator using explicitly supplied general ReplayState,
     * selected-resource, and candidate-set rules.
     */
    public CounterfactualValidator(
            List<FeasibilityRule> replayStateRules,
            List<FeasibilityRule> selectedResourceRules,
            List<CandidateSetFeasibilityRule> candidateSetRules) {

        if(replayStateRules == null
                || replayStateRules.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one ReplayState feasibility rule "
                    + "is required.");
        }

        if(selectedResourceRules == null) {

            throw new IllegalArgumentException(
                    "Selected-resource feasibility-rule list "
                    + "cannot be null.");
        }

        if(candidateSetRules == null) {

            throw new IllegalArgumentException(
                    "Candidate-set feasibility-rule list cannot "
                    + "be null.");
        }

        this.replayStateRules =
                copyReplayStateRules(
                        replayStateRules,
                        "ReplayState");

        this.selectedResourceRules =
                copyReplayStateRules(
                        selectedResourceRules,
                        "Selected-resource");

        this.candidateSetRules =
                copyCandidateSetRules(
                        candidateSetRules);
    }

    /**
     * Backward-compatible validation method.
     *
     * Because no candidate snapshots are supplied, this method evaluates:
     *
     * - general ReplayState rules
     * - selected-resource ReplayState rules
     */
    public FeasibilityResult validate(
            ReplayState replayState) {

        return validateInternal(
                replayState,
                Collections
                .<VmCandidateSnapshot>emptyList(),
                true,
                false);
    }

    /**
     * Evaluates a counterfactual state using synchronized VM candidates.
     *
     * This method evaluates:
     *
     * - general ReplayState rules
     * - candidate-set rules
     *
     * It intentionally does not evaluate selected-resource rules against
     * ReplayState because the authoritative resource state is contained
     * in the synchronized candidate snapshots.
     */
    public FeasibilityResult validate(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidateSnapshots) {

        return validateInternal(
                replayState,
                candidateSnapshots,
                false,
                true);
    }

    private FeasibilityResult validateInternal(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidateSnapshots,
            boolean evaluateSelectedResourceRules,
            boolean evaluateCandidateSetRules) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        if(candidateSnapshots == null) {

            throw new IllegalArgumentException(
                    "VM candidate list cannot be null.");
        }

        if(evaluateCandidateSetRules
                && candidateSnapshots.isEmpty()) {

            throw new IllegalArgumentException(
                    "VM candidate list cannot be empty when "
                    + "candidate-set validation is requested.");
        }

        FeasibilityResult.Builder resultBuilder =
                new FeasibilityResult.Builder(
                        replayState.getTaskId());

        /*
         * Evaluate general task, structural, and network state rules.
         */
        evaluateReplayStateRules(
                replayStateRules,
                replayState,
                resultBuilder);

        /*
         * Evaluate selected-resource rules only when no authoritative
         * synchronized candidate set was supplied.
         */
        if(evaluateSelectedResourceRules) {

            evaluateReplayStateRules(
                    selectedResourceRules,
                    replayState,
                    resultBuilder);
        }

        /*
         * Evaluate the synchronized VM candidate set.
         */
        if(evaluateCandidateSetRules) {

            evaluateCandidateSetRules(
                    replayState,
                    candidateSnapshots,
                    resultBuilder);
        }

        return resultBuilder.build();
    }

    private void evaluateReplayStateRules(
            List<FeasibilityRule> rules,
            ReplayState replayState,
            FeasibilityResult.Builder resultBuilder) {

        for(FeasibilityRule rule
                : rules) {

            ConstraintEvaluation evaluation =
                    rule.evaluate(
                            replayState);

            validateEvaluation(
                    rule.getClass().getName(),
                    evaluation);

            resultBuilder.addEvaluation(
                    evaluation);
        }
    }

    private void evaluateCandidateSetRules(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidateSnapshots,
            FeasibilityResult.Builder resultBuilder) {

        for(CandidateSetFeasibilityRule rule
                : candidateSetRules) {

            ConstraintEvaluation evaluation =
                    rule.evaluate(
                            replayState,
                            candidateSnapshots);

            validateEvaluation(
                    rule.getClass().getName(),
                    evaluation);

            resultBuilder.addEvaluation(
                    evaluation);
        }
    }

    private void validateEvaluation(
            String ruleName,
            ConstraintEvaluation evaluation) {

        if(evaluation == null) {

            throw new IllegalStateException(
                    "Feasibility rule returned null: "
                    + ruleName);
        }
    }

    private static List<FeasibilityRule>
            copyReplayStateRules(
                    List<FeasibilityRule> source,
                    String description) {

        List<FeasibilityRule> copiedRules =
                new ArrayList<FeasibilityRule>();

        for(FeasibilityRule rule
                : source) {

            if(rule == null) {

                throw new IllegalArgumentException(
                        description
                        + " feasibility-rule list cannot "
                        + "contain null.");
            }

            copiedRules.add(
                    rule);
        }

        return Collections.unmodifiableList(
                copiedRules);
    }

    private static List<CandidateSetFeasibilityRule>
            copyCandidateSetRules(
                    List<CandidateSetFeasibilityRule> source) {

        List<CandidateSetFeasibilityRule> copiedRules =
                new ArrayList<CandidateSetFeasibilityRule>();

        for(CandidateSetFeasibilityRule rule
                : source) {

            if(rule == null) {

                throw new IllegalArgumentException(
                        "Candidate-set feasibility-rule list "
                        + "cannot contain null.");
            }

            copiedRules.add(
                    rule);
        }

        return Collections.unmodifiableList(
                copiedRules);
    }

    /**
     * Returns general ReplayState rules.
     */
    public List<FeasibilityRule> getRules() {

        return replayStateRules;
    }

    public List<FeasibilityRule> getReplayStateRules() {

        return replayStateRules;
    }

    public List<FeasibilityRule> getSelectedResourceRules() {

        return selectedResourceRules;
    }

    public List<CandidateSetFeasibilityRule>
            getCandidateSetRules() {

        return candidateSetRules;
    }

    public int getRuleCount() {

        return replayStateRules.size()
                + selectedResourceRules.size()
                + candidateSetRules.size();
    }
}
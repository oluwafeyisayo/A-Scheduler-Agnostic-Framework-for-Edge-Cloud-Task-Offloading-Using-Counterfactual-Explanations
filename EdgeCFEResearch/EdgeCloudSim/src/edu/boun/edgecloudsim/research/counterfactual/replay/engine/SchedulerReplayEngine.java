package edu.boun.edgecloudsim.research.counterfactual.replay.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay.VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.replay.state.RandomPolicyState;
import edu.boun.edgecloudsim.research.counterfactual.replay.state.RoundRobinPolicyState;
import edu.boun.edgecloudsim.research.counterfactual.replay.state.SchedulerPolicyState;

import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerDecision;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerReplayStatus;
import edu.boun.edgecloudsim.research.counterfactual.replay.context
        .SchedulerReplayContext;
import edu.boun.edgecloudsim.research.counterfactual.replay.strategy
        .SchedulerReplayStrategy;
import edu.boun.edgecloudsim.research.counterfactual.replay.destination
        .DestinationSelectionResult;
import edu.boun.edgecloudsim.research.counterfactual.replay.destination
        .StateAwareDestinationSelector;

/**
 * Scheduler-agnostic dispatcher for replay strategies.
 *
 * Normal counterfactual replay and original-baseline replay are kept
 * separate:
 *
 * - replay(...) evaluates a supplied replay state;
 * - replayBaseline(...) additionally verifies that an unmodified
 *   original state reproduces the logged original decision.
 */
public final class SchedulerReplayEngine {

    private final Map<String, SchedulerReplayStrategy>
            strategies;

    private final StateAwareDestinationSelector
            destinationSelector;

    public SchedulerReplayEngine() {

        this.strategies =
                new LinkedHashMap<String,
                        SchedulerReplayStrategy>();

        this.destinationSelector =
                new StateAwareDestinationSelector();
    }

    public SchedulerReplayEngine register(
            SchedulerReplayStrategy strategy) {

        if(strategy == null) {

            throw new IllegalArgumentException(
                    "Replay strategy cannot be null.");
        }

        String normalizedPolicy =
                normalize(
                        strategy.getSupportedPolicy());

        if(normalizedPolicy.isEmpty()) {

            throw new IllegalArgumentException(
                    "Replay strategy policy name "
                    + "cannot be empty.");
        }

        strategies.put(
                normalizedPolicy,
                strategy);

        return this;
    }

    public boolean supports(
            String policyName) {

        return strategies.containsKey(
                normalize(
                        policyName));
    }

    /**
     * Performs ordinary scheduler replay.
     *
     * This method must continue to be used for modified
     * counterfactual states because a counterfactual is expected to
     * differ from the original logged decision.
     */
    public SchedulerDecision replay(
            SchedulerReplayContext context) {

        if(context == null) {

            throw new IllegalArgumentException(
                    "SchedulerReplayContext cannot be null.");
        }

        ReplayState replayState =
                context.getReplayState();

        if(!context.hasCandidateSnapshots()) {

            return replayStrategy(
                    context);
        }

        DestinationSelectionResult destinationResult;

        try {

            destinationResult =
                    destinationSelector
                    .selectDestination(
                            replayState,
                            context.getCandidateSnapshots());
        }
        catch(RuntimeException exception) {

            return errorDecision(
                    replayState,
                    replayState.getOriginalPolicy(),
                    "Destination selection failed: "
                    + safeExceptionMessage(
                            exception));
        }

        if(destinationResult
                .isNoDestinationSelected()) {

            return new SchedulerDecision.Builder(
                    replayState.getTaskId(),
                    replayState.getOriginalPolicy())

                    .withReplayStatus(
                            SchedulerReplayStatus.REPLAYED)

                    .withDecisionOutcome(
                            "NO_FEASIBLE_VM")

                    .withSelectedDestination(
                            "NONE")

                    .withSelectedVmId(
                            -1)

                    .withSelectedHostId(
                            -1)

                    .withSelectedDatacenterId(
                            -1)

                    .withSelectedCandidateIndex(
                            -1)

                    .withRoundRobinStartingCandidateIndex(
                            -1)

                    .withRandomSampledCandidateIndex(
                            -1)

                    .withReason(
                            "STATE_AWARE_DESTINATION_NONE: "
                            + destinationResult.getReason())

                    .build();
        }

        SchedulerReplayContext restrictedContext =
                createModifiedRestrictedContext(
                        context,
                        destinationResult
                        .getSelectedDestination());

        SchedulerDecision replayedDecision =
                replayStrategy(
                        restrictedContext);

        if(!replayedDecision
                .wasSuccessfullyReplayed()) {

            return replayedDecision;
        }

        return copyDecisionWithReason(
                replayedDecision,
                "STATE_AWARE_DESTINATION="
                + destinationResult
                .getSelectedDestination()
                + "; EDGE_COST_MS="
                + destinationResult
                .getEdgeServiceCostMs()
                + "; CLOUD_COST_MS="
                + destinationResult
                .getCloudServiceCostMs()
                + "; "
                + destinationResult.getReason()
                + " | "
                + replayedDecision.getReason());
    }

    /**
     * Dispatches directly to the scheduler strategy without performing
     * destination selection. Baseline replay uses this method after
     * restricting candidates to the logged attempted destination.
     */
    private SchedulerDecision replayStrategy(
            SchedulerReplayContext context) {

        ReplayState replayState =
                context.getReplayState();

        String policy =
                replayState.getOriginalPolicy();

        SchedulerReplayStrategy strategy =
                strategies.get(
                        normalize(
                                policy));

        if(strategy == null) {

            return unsupportedPolicyDecision(
                    replayState,
                    policy);
        }

        try {

            SchedulerDecision replayedDecision =
                    strategy.replay(
                            context);

            if(replayedDecision == null) {

                return errorDecision(
                        replayState,
                        policy,
                        "Scheduler replay strategy returned null.");
            }

            return replayedDecision;
        }
        catch(RuntimeException exception) {

            return errorDecision(
                    replayState,
                    policy,
                    "Scheduler replay failed: "
                    + safeExceptionMessage(
                            exception));
        }
    }


    /**
     * Replays the original scheduler decision using only candidates from
     * the destination actually attempted by the live scheduler.
     *
     * This preserves the live two-stage scheduling semantics:
     *
     * destination selection
     *      -> VM selection within that destination
     */
    public SchedulerDecision replayBaseline(
            SchedulerReplayContext originalContext) {

        if(originalContext == null) {

            throw new IllegalArgumentException(
                    "Original SchedulerReplayContext cannot be null.");
        }

        ReplayState replayState =
                originalContext.getReplayState();

        SchedulerReplayContext restrictedContext =
                createBaselineRestrictedContext(
                        originalContext);

        SchedulerDecision replayedDecision =
                replayStrategy(
                        restrictedContext);

        if(!replayedDecision.wasSuccessfullyReplayed()) {

            return replayedDecision;
        }

        BaselineFidelity fidelity =
                evaluateBaselineFidelity(
                        replayState,
                        replayedDecision);

        if(fidelity.isFaithful()) {

            return replayedDecision;
        }

        return new SchedulerDecision.Builder(
                replayState.getTaskId(),
                replayState.getOriginalPolicy())

                .withReplayStatus(
                        SchedulerReplayStatus.INDETERMINATE)

                .withDecisionOutcome(
                        replayedDecision.getDecisionOutcome())

                .withSelectedDestination(
                        replayedDecision.getSelectedDestination())

                .withSelectedVmId(
                        replayedDecision.getSelectedVmId())

                .withSelectedHostId(
                        replayedDecision.getSelectedHostId())

                .withSelectedDatacenterId(
                        replayedDecision.getSelectedDatacenterId())

                .withSelectedCandidateIndex(
                        replayedDecision.getSelectedCandidateIndex())

                .withRoundRobinStartingCandidateIndex(
                        replayedDecision
                        .getRoundRobinStartingCandidateIndex())

                .withRandomSampledCandidateIndex(
                        replayedDecision
                        .getRandomSampledCandidateIndex())

                .withReason(
                        "BASELINE_REPLAY_MISMATCH: "
                        + fidelity.getReason())

                .build();
    }
    
    /**
     * Restricts modified replay to the destination selected from the
     * modified state.
     */
    private SchedulerReplayContext createModifiedRestrictedContext(
            SchedulerReplayContext originalContext,
            String selectedDestination) {

        List<VmCandidateSnapshot> restrictedCandidates =
                filterCandidatesByDestination(
                        originalContext
                        .getCandidateSnapshots(),
                        selectedDestination);

        SchedulerPolicyState restrictedPolicyState =
                remapPolicyStateForModifiedDestination(
                        originalContext,
                        restrictedCandidates);

        return new SchedulerReplayContext(
                originalContext.getReplayState(),
                restrictedCandidates,
                restrictedPolicyState);
    }

    private List<VmCandidateSnapshot> filterCandidatesByDestination(
            List<VmCandidateSnapshot> candidates,
            String destination) {

        List<VmCandidateSnapshot> restricted =
                new ArrayList<VmCandidateSnapshot>();

        if(candidates == null) {

            return restricted;
        }

        String normalizedDestination =
                normalizeDestination(
                        destination);

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate != null
                    && normalizedDestination.equals(
                            normalizeDestination(
                                    candidate.getDestination()))) {

                restricted.add(
                        candidate);
            }
        }

        return restricted;
    }

    /**
     * Remaps scheduler policy state when a modified replay selects a
     * destination that can differ from the original attempted tier.
     */
    private SchedulerPolicyState
            remapPolicyStateForModifiedDestination(
                    SchedulerReplayContext originalContext,
                    List<VmCandidateSnapshot> restrictedCandidates) {

        SchedulerPolicyState policyState =
                originalContext.getPolicyState();

        if(policyState == null
                || restrictedCandidates == null
                || restrictedCandidates.isEmpty()) {

            return policyState;
        }

        if(policyState instanceof RandomPolicyState) {

            RandomPolicyState randomState =
                    (RandomPolicyState) policyState;

            int sampledCandidateIndex =
                    randomState
                    .getSampledCandidateIndex();

            for(VmCandidateSnapshot candidate
                    : restrictedCandidates) {

                if(candidate.getCandidateIndex()
                        == sampledCandidateIndex) {

                    return new RandomPolicyState(
                            sampledCandidateIndex);
                }
            }

            /*
             * The destination changed, so the original sampled candidate
             * is outside the selected tier. Preserve deterministic RANDOM
             * semantics by mapping the original sample to a stable
             * position in the new destination-specific candidate list.
             */
            int mappedPosition =
                    Math.floorMod(
                            sampledCandidateIndex,
                            restrictedCandidates.size());

            return new RandomPolicyState(
                    restrictedCandidates
                    .get(mappedPosition)
                    .getCandidateIndex());
        }

        if(policyState instanceof RoundRobinPolicyState) {

            RoundRobinPolicyState roundRobinState =
                    (RoundRobinPolicyState) policyState;

            List<VmCandidateSnapshot> originalCandidates =
                    originalContext.getCandidateSnapshots();

            int originalStartingPosition =
                    roundRobinState
                    .getStartingCandidateIndex();

            if(originalStartingPosition >= 0
                    && originalStartingPosition
                    < originalCandidates.size()) {

                VmCandidateSnapshot startingCandidate =
                        originalCandidates.get(
                                originalStartingPosition);

                int restrictedPosition =
                        findCandidatePosition(
                                restrictedCandidates,
                                startingCandidate
                                .getCandidateIndex());

                if(restrictedPosition >= 0) {

                    return new RoundRobinPolicyState(
                            restrictedPosition);
                }
            }

            int deterministicPosition =
                    Math.floorMod(
                            originalStartingPosition,
                            restrictedCandidates.size());

            return new RoundRobinPolicyState(
                    deterministicPosition);
        }

        return policyState;
    }

    private SchedulerDecision copyDecisionWithReason(
            SchedulerDecision decision,
            String reason) {

        return new SchedulerDecision.Builder(
                decision.getTaskId(),
                decision.getSchedulerPolicy())

                .withReplayStatus(
                        decision.getReplayStatus())

                .withDecisionOutcome(
                        decision.getDecisionOutcome())

                .withSelectedDestination(
                        decision.getSelectedDestination())

                .withSelectedVmId(
                        decision.getSelectedVmId())

                .withSelectedHostId(
                        decision.getSelectedHostId())

                .withSelectedDatacenterId(
                        decision.getSelectedDatacenterId())

                .withSelectedCandidateIndex(
                        decision.getSelectedCandidateIndex())

                .withRoundRobinStartingCandidateIndex(
                        decision
                        .getRoundRobinStartingCandidateIndex())

                .withRandomSampledCandidateIndex(
                        decision
                        .getRandomSampledCandidateIndex())

                .withReason(
                        reason)

                .build();
    }

    /**
     * Restricts original baseline replay to the destination selected or
     * attempted by the live scheduler.
     *
     * Counterfactual replay does not call this method.
     */
    private SchedulerReplayContext createBaselineRestrictedContext(
            SchedulerReplayContext originalContext) {

        ReplayState replayState =
                originalContext.getReplayState();

        String attemptedDestination =
                normalizeDestination(
                        replayState.getAttemptedDestination());

        /*
         * Selected decisions should normally have both an attempted and
         * selected destination. Fall back to the original selected
         * destination when attemptedDestination is unavailable.
         */
        if("NONE".equals(attemptedDestination)) {

            attemptedDestination =
                    normalizeDestination(
                            replayState.getOriginalDestination());
        }

        /*
         * A pre-scheduling network rejection may have no candidate set.
         * Preserve the original context in that case.
         */
        if("NONE".equals(attemptedDestination)
                || !originalContext.hasCandidateSnapshots()) {

            return originalContext;
        }

        List<VmCandidateSnapshot> originalCandidates =
                originalContext.getCandidateSnapshots();

        List<VmCandidateSnapshot> restrictedCandidates =
                new ArrayList<VmCandidateSnapshot>();

        for(VmCandidateSnapshot candidate
                : originalCandidates) {

            if(candidate != null
                    && attemptedDestination.equals(
                            normalizeDestination(
                                    candidate.getDestination()))) {

                restrictedCandidates.add(
                        candidate);
            }
        }

        /*
         * Do not silently fall back to the cross-tier set. An empty
         * destination-specific candidate set indicates incomplete
         * provenance.
         */
        if(restrictedCandidates.isEmpty()) {

            return new SchedulerReplayContext(
                    replayState,
                    restrictedCandidates,
                    originalContext.getPolicyState());
        }

        SchedulerPolicyState restrictedPolicyState =
                remapPolicyStateForRestrictedCandidates(
                        originalContext,
                        restrictedCandidates);

        return new SchedulerReplayContext(
                replayState,
                restrictedCandidates,
                restrictedPolicyState);
    }
    /**
     * Remaps policy state after the full cross-tier candidate set is
     * restricted to one destination.
     */
    private SchedulerPolicyState
            remapPolicyStateForRestrictedCandidates(
                    SchedulerReplayContext originalContext,
                    List<VmCandidateSnapshot> restrictedCandidates) {

        SchedulerPolicyState policyState =
                originalContext.getPolicyState();

        if(policyState == null) {

            return null;
        }

        /*
         * RANDOM stores a candidate identifier, not a Java list position.
         * The identifier remains valid after destination filtering.
         */
        if(policyState instanceof RandomPolicyState) {

            RandomPolicyState randomState =
                    (RandomPolicyState) policyState;

            return new RandomPolicyState(
                    randomState.getSampledCandidateIndex());
        }

        /*
         * ROUND_ROBIN currently stores a list position. Identify the
         * original starting candidate and then find that same candidate's
         * position in the restricted destination-specific list.
         */
        if(policyState instanceof RoundRobinPolicyState) {

            RoundRobinPolicyState roundRobinState =
                    (RoundRobinPolicyState) policyState;

            List<VmCandidateSnapshot> originalCandidates =
                    originalContext.getCandidateSnapshots();

            int originalStartingPosition =
                    roundRobinState.getStartingCandidateIndex();

            if(originalStartingPosition < 0
                    || originalStartingPosition
                    >= originalCandidates.size()) {

                return new RoundRobinPolicyState(
                        0);
            }

            VmCandidateSnapshot originalStartingCandidate =
                    originalCandidates.get(
                            originalStartingPosition);

            int restrictedStartingPosition =
                    findCandidatePosition(
                            restrictedCandidates,
                            originalStartingCandidate
                            .getCandidateIndex());

            if(restrictedStartingPosition < 0) {

                /*
                 * This indicates that the stored cursor points to another
                 * destination, which should not occur for faithful live
                 * provenance.
                 */
                restrictedStartingPosition =
                        0;
            }

            return new RoundRobinPolicyState(
                    restrictedStartingPosition);
        }

        /*
         * LEAST_LOADED has no cursor or random-generator state.
         */
        return policyState;
    }
    private int findCandidatePosition(
            List<VmCandidateSnapshot> candidates,
            int candidateIndex) {

        if(candidates == null) {

            return -1;
        }

        for(int position = 0;
                position < candidates.size();
                position++) {

            VmCandidateSnapshot candidate =
                    candidates.get(
                            position);

            if(candidate != null
                    && candidate.getCandidateIndex()
                    == candidateIndex) {

                return position;
            }
        }

        return -1;
    }
    
    /**
     * Returns true when replay reproduces the logged original decision.
     *
     * This method is useful for diagnostics and test assertions.
     */
    public boolean isBaselineFaithful(
            SchedulerReplayContext originalContext) {

        if(originalContext == null) {

            throw new IllegalArgumentException(
                    "Original SchedulerReplayContext cannot be null.");
        }

        SchedulerReplayContext restrictedContext =
                createBaselineRestrictedContext(
                        originalContext);

        SchedulerDecision replayedDecision =
                replayStrategy(
                        restrictedContext);

        if(!replayedDecision.wasSuccessfullyReplayed()) {

            return false;
        }

        return evaluateBaselineFidelity(
                originalContext.getReplayState(),
                replayedDecision)
                .isFaithful();
    }

    /**
     * Produces a detailed baseline-fidelity diagnostic.
     */
    public String describeBaselineFidelity(
            SchedulerReplayContext originalContext) {

        if(originalContext == null) {

            throw new IllegalArgumentException(
                    "Original SchedulerReplayContext cannot be null.");
        }

        ReplayState replayState =
                originalContext.getReplayState();

        SchedulerReplayContext restrictedContext =
                createBaselineRestrictedContext(
                        originalContext);

        SchedulerDecision replayedDecision =
                replayStrategy(
                        restrictedContext);

        if(!replayedDecision
                .wasSuccessfullyReplayed()) {

            return "Baseline replay did not complete successfully. "
                    + "taskId="
                    + replayState.getTaskId()
                    + ", status="
                    + replayedDecision.getReplayStatus()
                    + ", reason="
                    + replayedDecision.getReason();
        }

        BaselineFidelity fidelity =
                evaluateBaselineFidelity(
                        replayState,
                        replayedDecision);

        return "BaselineReplayFidelity{"
                + "taskId="
                + replayState.getTaskId()
                + ", faithful="
                + fidelity.isFaithful()
                + ", loggedOutcome='"
                + replayState.getOriginalOutcome()
                + '\''
                + ", replayedOutcome='"
                + replayedDecision.getDecisionOutcome()
                + '\''
                + ", loggedDestination='"
                + replayState.getOriginalDestination()
                + '\''
                + ", replayedDestination='"
                + replayedDecision.getSelectedDestination()
                + '\''
                + ", loggedVmId="
                + replayState.getOriginalSelectedVmId()
                + ", replayedVmId="
                + replayedDecision.getSelectedVmId()
                + ", reason='"
                + fidelity.getReason()
                + '\''
                + '}';
    }

    private BaselineFidelity evaluateBaselineFidelity(
            ReplayState replayState,
            SchedulerDecision replayedDecision) {

        boolean outcomeMatches =
                sameOutcome(
                        replayState.getOriginalOutcome(),
                        replayedDecision
                        .getDecisionOutcome());

        boolean destinationMatches =
                sameDestination(
                        replayState.getOriginalDestination(),
                        replayedDecision
                        .getSelectedDestination());

        boolean vmMatches =
                replayState.getOriginalSelectedVmId()
                == replayedDecision.getSelectedVmId();

        /*
         * For unsuccessful decisions, VM identifiers are expected to be
         * absent in both original and replayed results.
         */
        boolean originalSelected =
                isSuccessfulSelection(
                        replayState.getOriginalOutcome(),
                        replayState.getOriginalDestination(),
                        replayState
                        .getOriginalSelectedVmId());

        boolean replaySelected =
                isSuccessfulSelection(
                        replayedDecision
                        .getDecisionOutcome(),
                        replayedDecision
                        .getSelectedDestination(),
                        replayedDecision
                        .getSelectedVmId());

        boolean selectionStatusMatches =
                originalSelected
                == replaySelected;

        boolean faithful =
                outcomeMatches
                && destinationMatches
                && vmMatches
                && selectionStatusMatches;

        if(faithful) {

            return new BaselineFidelity(
                    true,
                    "The replayed scheduler decision reproduces "
                    + "the logged original decision.");
        }

        StringBuilder reason =
                new StringBuilder();

        if(!selectionStatusMatches) {

            appendReason(
                    reason,
                    "selection status differs: originalSelected="
                    + originalSelected
                    + ", replaySelected="
                    + replaySelected);
        }

        if(!outcomeMatches) {

            appendReason(
                    reason,
                    "outcome differs: logged="
                    + replayState.getOriginalOutcome()
                    + ", replayed="
                    + replayedDecision
                    .getDecisionOutcome());
        }

        if(!destinationMatches) {

            appendReason(
                    reason,
                    "destination differs: logged="
                    + replayState.getOriginalDestination()
                    + ", replayed="
                    + replayedDecision
                    .getSelectedDestination());
        }

        if(!vmMatches) {

            appendReason(
                    reason,
                    "VM differs: logged="
                    + replayState
                    .getOriginalSelectedVmId()
                    + ", replayed="
                    + replayedDecision
                    .getSelectedVmId());
        }

        return new BaselineFidelity(
                false,
                reason.toString());
    }

    private SchedulerDecision unsupportedPolicyDecision(
            ReplayState replayState,
            String policy) {

        return new SchedulerDecision.Builder(
                replayState.getTaskId(),
                policy)

                .withReplayStatus(
                        SchedulerReplayStatus
                        .UNSUPPORTED_POLICY)

                .withReason(
                        "No scheduler replay strategy is "
                        + "registered for policy "
                        + policy
                        + ".")

                .build();
    }

    private SchedulerDecision errorDecision(
            ReplayState replayState,
            String policy,
            String reason) {

        return new SchedulerDecision.Builder(
                replayState.getTaskId(),
                policy)

                .withReplayStatus(
                        SchedulerReplayStatus.ERROR)

                .withReason(
                        reason)

                .build();
    }

    private static boolean sameOutcome(
            String first,
            String second) {

        return normalizeDecisionText(
                first).equals(
                        normalizeDecisionText(
                                second));
    }

    private static boolean sameDestination(
            String first,
            String second) {

        String normalizedFirst =
                normalizeDestination(
                        first);

        String normalizedSecond =
                normalizeDestination(
                        second);

        return normalizedFirst.equals(
                normalizedSecond);
    }

    private static boolean isSuccessfulSelection(
            String outcome,
            String destination,
            int selectedVmId) {

        return "SELECTED".equals(
                normalizeDecisionText(
                        outcome))

                && !"NONE".equals(
                        normalizeDestination(
                                destination))

                && selectedVmId >= 0;
    }

    private static String normalizeDestination(
            String destination) {

        String normalized =
                normalizeDecisionText(
                        destination);

        if(normalized.isEmpty()
                || "UNKNOWN".equals(normalized)
                || "UNSELECTED".equals(normalized)
                || "UNDETERMINED".equals(normalized)
                || "NO_DESTINATION".equals(normalized)) {

            return "NONE";
        }

        return normalized;
    }

    private static String normalizeDecisionText(
            String value) {

        if(value == null) {

            return "";
        }

        return value
                .trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');
    }

    private static void appendReason(
            StringBuilder reason,
            String value) {

        if(reason.length() > 0) {

            reason.append("; ");
        }

        reason.append(
                value);
    }

    private static String safeExceptionMessage(
            RuntimeException exception) {

        if(exception == null
                || exception.getMessage() == null
                || exception.getMessage()
                .trim()
                .isEmpty()) {

            return "UNKNOWN_REPLAY_ERROR";
        }

        return exception.getMessage()
                .trim();
    }

    public Set<String> getRegisteredPolicies() {

        return Collections.unmodifiableSet(
                strategies.keySet());
    }

    private static String normalize(
            String policyName) {

        if(policyName == null) {

            return "";
        }

        return policyName
                .trim()
                .toUpperCase();
    }

    /**
     * Internal immutable result describing whether baseline replay
     * reproduced the original logged decision.
     */
    private static final class BaselineFidelity {

        private final boolean faithful;
        private final String reason;

        private BaselineFidelity(
                boolean faithful,
                String reason) {

            this.faithful =
                    faithful;

            this.reason =
                    reason == null
                    || reason.trim().isEmpty()
                    ? "NO_REASON"
                    : reason.trim();
        }

        private boolean isFaithful() {

            return faithful;
        }

        private String getReason() {

            return reason;
        }
    }
}
package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.List;

import java.util.Collections;
import java.util.HashMap;

import java.util.Map;

import edu.boun.edgecloudsim.research.counterfactual.comparison
        .DecisionChangeType;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerDecision;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

import edu.boun.edgecloudsim.research.counterfactual.replay.context
        .SchedulerReplayContext;

import edu.boun.edgecloudsim.research.counterfactual.replay.engine
        .SchedulerReplayEngine;

/**
 * Executes RG-FCS independently for each DecisionState using the
 * candidate VM snapshots recorded for the corresponding task.
 */
public final class PerDecisionCounterfactualSearchExecutor
        implements CounterfactualSearchExecutor {

    private static final double DEFAULT_NETWORK_LATENCY_MS =
            5.0;

    private static final double DEFAULT_AVAILABLE_BANDWIDTH_MBPS =
            200.0;

    private final ReplayGuidedFeasibleCounterfactualSearch search;

    private final SchedulerReplayEngine schedulerReplayEngine;

    private final SchedulerReplayContextFactory contextFactory;

    private final DecisionChangeType requestedChangeType;
    
    private final Map<Integer, List<VmCandidateSnapshot>>
    candidateSnapshotsByTaskId;

    private int diagnosticSuccessfulResultCount;
    private int diagnosticDestinationChangeCount;
    private int diagnosticSameTierVmChangeCount;
    private int diagnosticExecutionTargetChangeCount;
    private int diagnosticFeasibilityRestorationCount;
    private int diagnosticObjectiveMismatchCount;
    
    

    public PerDecisionCounterfactualSearchExecutor(
            FeasibleCounterfactualSearchConfig config,
            SchedulerReplayEngine replayEngine,
            SchedulerReplayContextFactory contextFactory,
            DecisionChangeType requestedChangeType,
            Map<Integer, List<VmCandidateSnapshot>>
                    candidateSnapshotsByTaskId) {

        if(config == null) {

            throw new IllegalArgumentException(
                    "Search configuration cannot be null.");
        }

        if(replayEngine == null) {

            throw new IllegalArgumentException(
                    "Replay engine cannot be null.");
        }

        if(contextFactory == null) {

            throw new IllegalArgumentException(
                    "Replay-context factory cannot be null.");
        }

        if(requestedChangeType == null) {

            throw new IllegalArgumentException(
                    "Requested decision-change type cannot be null.");
        }

        if(candidateSnapshotsByTaskId == null) {

            throw new IllegalArgumentException(
                    "Candidate snapshot map cannot be null.");
        }

        this.schedulerReplayEngine =
                replayEngine;

        this.contextFactory =
                contextFactory;

        this.requestedChangeType =
                requestedChangeType;

        this.candidateSnapshotsByTaskId =
                copyCandidateSnapshotMap(
                        candidateSnapshotsByTaskId);

        this.search =
                new ReplayGuidedFeasibleCounterfactualSearch(
                        config,
                        replayEngine,
                        contextFactory);
    }
    
    
    private Map<Integer, List<VmCandidateSnapshot>>
    copyCandidateSnapshotMap(
            Map<Integer, List<VmCandidateSnapshot>> source) {

		Map<Integer, List<VmCandidateSnapshot>> copiedMap =
		        new HashMap<Integer, List<VmCandidateSnapshot>>();
		
		for(Map.Entry<Integer, List<VmCandidateSnapshot>> entry
		        : source.entrySet()) {
		
		    Integer taskId =
		            entry.getKey();
		
		    List<VmCandidateSnapshot> candidates =
		            entry.getValue();
		
		    if(taskId == null
		            || candidates == null) {
		
		        continue;
		    }
		
		    copiedMap.put(
		            taskId,
		            Collections.unmodifiableList(
		                    new ArrayList<VmCandidateSnapshot>(
		                            candidates)));
		}
		
		return Collections.unmodifiableMap(
		        copiedMap);
}

    public String getSchedulerPolicy() {

        return contextFactory
                .getSchedulerPolicy();
    }

    @Override
    public CounterfactualSearchResult search(
            DecisionState decisionState) {

        if(decisionState == null) {

            throw new IllegalArgumentException(
                    "Decision state cannot be null.");
        }

        /**
         * Retrieves the logged VM candidate snapshots for the current task.
         */
        ReplayState replayState =
                createReplayState(
                        decisionState);

        List<VmCandidateSnapshot> candidates =
                createVmCandidates(
                        replayState);
        
        if(candidates.isEmpty()) {

            throw new IllegalStateException(
                    "No candidate snapshots were loaded for task "
                    + replayState.getTaskId()
                    + ".");
        }

        SchedulerReplayContext replayContext =
                contextFactory.create(
                        replayState,
                        candidates);

        System.out.println(
                "[Replay State] "
                + "policy="
                + contextFactory.getSchedulerPolicy()
                + ", taskId="
                + decisionState.getTaskId()
                + ", latencyMs="
                + replayState.getNetworkLatencyMs()
                + ", bandwidthMbps="
                + replayState.getAvailableBandwidthMbps()
                + ", taskLengthMi="
                + replayState.getTaskLengthMi()
                + ", uploadSizeKb="
                + replayState.getUploadSizeKb()
                + ", policyState="
                + replayContext.getPolicyState());

        SchedulerDecision originalDecision =
                schedulerReplayEngine.replay(
                        replayContext);

        if(originalDecision == null) {

            throw new IllegalStateException(
                    "Scheduler replay returned null for task "
                    + decisionState.getTaskId()
                    + ".");
        }

        boolean originalReplaySuccessful =
                originalDecision
                .wasSuccessfullyReplayed();

        /*
         * A deterministic replay can still reproduce an unsuccessful
         * scheduling outcome. Selection success must therefore be
         * tracked separately from replay success.
         */
        boolean originalSelectionSuccessful =
                originalReplaySuccessful
                && originalDecision.isSelected()
                && originalDecision
                .getSelectedCandidateIndex() >= 0;

        boolean feasibilityRestorationRequested =
                requestedChangeType
                == DecisionChangeType
                .FEASIBILITY_RESTORATION;

        if(!originalReplaySuccessful) {

            throw new IllegalStateException(
                    "Original decision could not be replayed "
                    + "deterministically for task "
                    + decisionState.getTaskId()
                    + ": "
                    + originalDecision.getReason());
        }

        if(feasibilityRestorationRequested
                && !originalSelectionSuccessful) {

            System.out.println(
                    "[Feasibility Restoration] "
                    + "Task "
                    + decisionState.getTaskId()
                    + " was originally unschedulable. "
                    + "RG-FCS will search for a feasible state.");
        }

        if(feasibilityRestorationRequested
                && originalSelectionSuccessful) {

            throw new IllegalStateException(
                    "Feasibility restoration requires an originally "
                    + "unsuccessful decision, but task "
                    + decisionState.getTaskId()
                    + " replayed as SELECTED.");
        }

        /*
         * Synchronize the selected-CPU feature with the VM actually
         * selected by the scheduler-specific replay.
         *
         * This prevents ROUND_ROBIN and RANDOM decisions from
         * inheriting the CPU value of a different selected VM.
         */
        if(originalSelectionSuccessful) {

            replayState =
                    synchronizeSelectedCandidateState(
                            replayState,
                            candidates,
                            originalDecision);

            replayContext =
                    contextFactory.create(
                            replayState,
                            candidates);
        }
        else {

            /*
             * Feasibility-restoration searches begin from an originally
             * unsuccessful decision, so there is no selected candidate
             * whose state needs to be synchronized.
             */
            replayContext =
                    contextFactory.create(
                            replayState,
                            candidates);
        }                       

        System.out.println(
                "[Original Replay] "
                + "factoryPolicy="
                + contextFactory.getSchedulerPolicy()
                + ", taskId="
                + decisionState.getTaskId()
                + ", replaySuccessful="
                + originalReplaySuccessful
                + ", selectionSuccessful="
                + originalSelectionSuccessful
                + ", selectedVm="
                + originalDecision.getSelectedVmId()
                + ", selectedCandidate="
                + originalDecision.getSelectedCandidateIndex()
                + ", destination="
                + originalDecision.getSelectedDestination()
                + ", outcome="
                + originalDecision.getDecisionOutcome());

        /*
         * The policy-overridden and scheduler-synchronized ReplayState
         * is passed into RG-FCS.
         */
        CounterfactualSearchResult searchResult =
                search.search(
                        replayState,
                        candidates,
                        originalDecision,
                        requestedChangeType);

        recordObjectiveSeparationDiagnostic(
                decisionState,
                originalDecision,
                searchResult);

        return searchResult;
    }

    private void recordObjectiveSeparationDiagnostic(
            DecisionState loggedDecision,
            SchedulerDecision originalReplayDecision,
            CounterfactualSearchResult searchResult) {

        if(searchResult == null
                || !searchResult.hasAcceptedCandidate()) {

            System.out.println(
                    "[Objective Separation] policy="
                    + getSchedulerPolicy()
                    + ", requested="
                    + requestedChangeType
                    + ", taskId="
                    + loggedDecision.getTaskId()
                    + ", result=NOT_FOUND");

            return;
        }

        CounterfactualCandidate bestCandidate =
                searchResult.getBestCandidate();

        SchedulerDecision modifiedDecision =
                bestCandidate == null
                ? null
                : bestCandidate.getSchedulerDecision();

        if(modifiedDecision == null) {

            diagnosticObjectiveMismatchCount++;

            System.out.println(
                    "[Objective Separation] policy="
                    + getSchedulerPolicy()
                    + ", requested="
                    + requestedChangeType
                    + ", taskId="
                    + loggedDecision.getTaskId()
                    + ", result=INVALID_NO_MODIFIED_DECISION");

            return;
        }

        diagnosticSuccessfulResultCount++;

        boolean originalSelected =
                originalReplayDecision != null
                && originalReplayDecision.isSelected();

        boolean modifiedSelected =
                modifiedDecision.isSelected();

        boolean feasibilityRestored =
                !originalSelected
                && modifiedSelected;

        boolean destinationChanged =
                !sameText(
                        originalReplayDecision
                        .getSelectedDestination(),
                        modifiedDecision
                        .getSelectedDestination());

        boolean vmChanged =
                originalReplayDecision.getSelectedVmId()
                != modifiedDecision.getSelectedVmId();

        boolean hostChanged =
                originalReplayDecision.getSelectedHostId()
                != modifiedDecision.getSelectedHostId();

        boolean datacenterChanged =
                originalReplayDecision.getSelectedDatacenterId()
                != modifiedDecision.getSelectedDatacenterId();

        boolean executionTargetChanged =
                destinationChanged
                || vmChanged
                || hostChanged
                || datacenterChanged;

        boolean sameTierVmChange =
                !destinationChanged
                && (
                        vmChanged
                        || hostChanged
                        || datacenterChanged
                );

        if(destinationChanged) {
            diagnosticDestinationChangeCount++;
        }

        if(sameTierVmChange) {
            diagnosticSameTierVmChangeCount++;
        }

        if(executionTargetChanged) {
            diagnosticExecutionTargetChangeCount++;
        }

        if(feasibilityRestored) {
            diagnosticFeasibilityRestorationCount++;
        }

        boolean objectiveSatisfied =
                requestedChangeType
                == DecisionChangeType.DESTINATION_CHANGE
                ? destinationChanged
                : requestedChangeType
                == DecisionChangeType
                .FEASIBILITY_RESTORATION
                ? feasibilityRestored
                : executionTargetChanged;

        if(!objectiveSatisfied) {
            diagnosticObjectiveMismatchCount++;
        }

        String observedChangeType =
                feasibilityRestored
                ? "FEASIBILITY_RESTORATION"
                : destinationChanged
                ? "DESTINATION_CHANGE"
                : sameTierVmChange
                ? "EXECUTION_TARGET_CHANGE"
                : "NO_SUPPORTED_CHANGE";

        System.out.println(
                "[Objective Separation] policy="
                + getSchedulerPolicy()
                + ", requested="
                + requestedChangeType
                + ", taskId="
                + loggedDecision.getTaskId()
                + ", observed="
                + observedChangeType
                + ", originalDestination="
                + originalReplayDecision
                .getSelectedDestination()
                + ", modifiedDestination="
                + modifiedDecision
                .getSelectedDestination()
                + ", originalVm="
                + originalReplayDecision
                .getSelectedVmId()
                + ", modifiedVm="
                + modifiedDecision
                .getSelectedVmId()
                + ", destinationChanged="
                + destinationChanged
                + ", sameTierVmChange="
                + sameTierVmChange
                + ", objectiveSatisfied="
                + objectiveSatisfied
                + ", bestSource="
                + searchResult.getBestCandidateSource());
    }

    public int getDiagnosticSuccessfulResultCount() {
        return diagnosticSuccessfulResultCount;
    }

    public int getDiagnosticDestinationChangeCount() {
        return diagnosticDestinationChangeCount;
    }

    public int getDiagnosticSameTierVmChangeCount() {
        return diagnosticSameTierVmChangeCount;
    }

    public int getDiagnosticExecutionTargetChangeCount() {
        return diagnosticExecutionTargetChangeCount;
    }

    public int getDiagnosticFeasibilityRestorationCount() {
        return diagnosticFeasibilityRestorationCount;
    }

    public int getDiagnosticObjectiveMismatchCount() {
        return diagnosticObjectiveMismatchCount;
    }

    public void printObjectiveSeparationSummary() {

        System.out.println(
                "[Objective Separation Summary] policy="
                + getSchedulerPolicy()
                + ", requested="
                + requestedChangeType
                + ", successfulResults="
                + diagnosticSuccessfulResultCount
                + ", observedDestinationChanges="
                + diagnosticDestinationChangeCount
                + ", observedSameTierVmChanges="
                + diagnosticSameTierVmChangeCount
                + ", observedExecutionTargetChanges="
                + diagnosticExecutionTargetChangeCount
                + ", observedFeasibilityRestorations="
                + diagnosticFeasibilityRestorationCount
                + ", objectiveMismatches="
                + diagnosticObjectiveMismatchCount);
    }

    private static boolean sameText(
            String first,
            String second) {

        if(first == null || second == null) {
            return first == second;
        }

        return first.trim()
                .equalsIgnoreCase(
                        second.trim());
    }

    /**
     * Reconstructs the replay state for the scheduler currently being
     * evaluated.
     *
     * Logged network values are used when available. Temporary defaults
     * are used only when the logged values are zero or unavailable.
     */
    private ReplayState createReplayState(
            DecisionState decisionState) {

        return ReplayState
                .from(decisionState)

                .withOriginalPolicy(
                        contextFactory
                        .getSchedulerPolicy())

                .withNetworkLatencyMs(
                        resolveNetworkLatencyMs(
                                decisionState))

                .withAvailableBandwidthMbps(
                        resolveAvailableBandwidthMbps(
                                decisionState))

                .withTaskLengthMi(
                        decisionState
                        .getTaskLengthMi())

                .withUploadSizeKb(
                        decisionState
                        .getUploadSizeKb())

                .build();
    }

    /**
     * Uses the logged latency when it is positive.
     *
     * The default is 5 milliseconds.
     */
    private double resolveNetworkLatencyMs(
            DecisionState decisionState) {

        double loggedLatencyMs =
                decisionState
                .getNetworkLatencyMs();

        if(isFinite(loggedLatencyMs)
                && loggedLatencyMs > 0.0) {

            return loggedLatencyMs;
        }

        return DEFAULT_NETWORK_LATENCY_MS;
    }

    /**
     * Uses the logged bandwidth when it is positive.
     *
     * The default is 200 Mbps.
     */
    private double resolveAvailableBandwidthMbps(
            DecisionState decisionState) {

        double loggedBandwidthMbps =
                decisionState
                .getAvailableBandwidthMbps();

        if(isFinite(loggedBandwidthMbps)
                && loggedBandwidthMbps > 0.0) {

            return loggedBandwidthMbps;
        }

        return DEFAULT_AVAILABLE_BANDWIDTH_MBPS;
    }

    /**
     * Synchronizes selected_available_cpu_percent with the candidate
     * selected by the scheduler-specific original replay.
     */
    private ReplayState synchronizeSelectedCandidateState(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidates,
            SchedulerDecision originalDecision) {

        int selectedCandidateIndex =
                originalDecision
                .getSelectedCandidateIndex();

        VmCandidateSnapshot selectedCandidate =
                findCandidateByIndex(
                        candidates,
                        selectedCandidateIndex);

        if(selectedCandidate == null) {

            throw new IllegalStateException(
                    "Selected candidate index "
                    + selectedCandidateIndex
                    + " was not found for task "
                    + replayState.getTaskId()
                    + ".");
        }

        double selectedAvailableCpuPercent =
                selectedCandidate
                .getAvailableCpuPercent();

        double bestAvailableCpuPercent =
                findBestAvailableCpuPercent(
                        candidates);

        int feasibleVmCount =
                countFeasibleCandidates(
                        candidates,
                        replayState
                        .getRequiredCpuPercent());

        return ReplayState
                .from(replayState)

                .withSelectedAvailableCpuPercent(
                        selectedAvailableCpuPercent)

                .withBestAvailableCpuPercent(
                        bestAvailableCpuPercent)

                .withCandidateVmCount(
                        candidates.size())

                .withFeasibleVmCount(
                        feasibleVmCount)

                .build();
    }

    /**
     * Finds the candidate whose scheduler candidate index matches the
     * selected index returned by replay.
     */
    private VmCandidateSnapshot findCandidateByIndex(
            List<VmCandidateSnapshot> candidates,
            int selectedCandidateIndex) {

        if(candidates == null) {

            return null;
        }

        for(VmCandidateSnapshot candidate :
                candidates) {

            if(candidate == null) {
                continue;
            }

            if(candidate.getCandidateIndex()
                    == selectedCandidateIndex) {

                return candidate;
            }
        }

        return null;
    }

    /**
     * Returns the greatest available CPU percentage in the candidate
     * set.
     */
    private double findBestAvailableCpuPercent(
            List<VmCandidateSnapshot> candidates) {

        double bestAvailableCpuPercent =
                0.0;

        if(candidates == null) {

            return bestAvailableCpuPercent;
        }

        for(VmCandidateSnapshot candidate :
                candidates) {

            if(candidate == null) {
                continue;
            }

            bestAvailableCpuPercent =
                    Math.max(
                            bestAvailableCpuPercent,
                            candidate
                            .getAvailableCpuPercent());
        }

        return bestAvailableCpuPercent;
    }

    /**
     * Counts candidates whose available CPU satisfies the task's
     * required CPU percentage.
     */
    private int countFeasibleCandidates(
            List<VmCandidateSnapshot> candidates,
            double requiredCpuPercent) {

        int feasibleCount =
                0;

        if(candidates == null) {

            return feasibleCount;
        }

        for(VmCandidateSnapshot candidate :
                candidates) {

            if(candidate == null) {
                continue;
            }

            if(candidate.getAvailableCpuPercent()
                    >= requiredCpuPercent) {

                feasibleCount++;
            }
        }

        return feasibleCount;
    }

    /**
     * Creates the temporary three-VM replay environment.
     *
     * These synthetic snapshots should later be replaced with the
     * actual candidate VM states recorded when the original scheduler
     * decision was made.
     */
    private List<VmCandidateSnapshot> createVmCandidates(
            ReplayState replayState) {
    	

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        int taskId =
                replayState.getTaskId();

        List<VmCandidateSnapshot> candidates =
                candidateSnapshotsByTaskId.get(
                        taskId);

        if(candidates == null
                || candidates.isEmpty()) {

            System.err.println(
                    "[PerDecisionCounterfactualSearchExecutor] "
                    + "No candidate snapshots found for taskId="
                    + taskId
                    + ".");

            return Collections.emptyList();
        }

        return new ArrayList<VmCandidateSnapshot>(
                candidates);
    }

    private boolean isFinite(
            double value) {

        return !Double.isNaN(value)
                && !Double.isInfinite(value);
    }
}
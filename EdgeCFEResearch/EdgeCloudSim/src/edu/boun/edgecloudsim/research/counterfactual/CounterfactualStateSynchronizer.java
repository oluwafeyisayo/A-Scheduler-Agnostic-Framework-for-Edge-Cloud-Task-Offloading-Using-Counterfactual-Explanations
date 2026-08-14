package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.boun.edgecloudsim.research.counterfactual.comparison
        .DecisionChangeType;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerDecision;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

/**
 * Applies one or more counterfactual feature changes to ReplayState
 * and synchronizes VM candidate snapshots when a feature affects
 * scheduler-visible resource state.
 */
public final class CounterfactualStateSynchronizer {

    public static final class SynchronizedCounterfactualState {

        private final List<FeatureChange> featureChanges;

        private final ReplayState replayState;

        private final List<VmCandidateSnapshot> vmCandidates;

        private SynchronizedCounterfactualState(
                List<FeatureChange> featureChanges,
                ReplayState replayState,
                List<VmCandidateSnapshot> vmCandidates) {

            if(featureChanges == null
                    || featureChanges.isEmpty()) {

                throw new IllegalArgumentException(
                        "Feature-change list cannot be null "
                        + "or empty.");
            }

            if(replayState == null) {

                throw new IllegalArgumentException(
                        "Synchronized ReplayState cannot be null.");
            }

            if(vmCandidates == null
                    || vmCandidates.isEmpty()) {

                throw new IllegalArgumentException(
                        "Synchronized VM candidate list cannot "
                        + "be null or empty.");
            }

            this.featureChanges =
                    Collections.unmodifiableList(
                            new ArrayList<FeatureChange>(
                                    featureChanges));

            this.replayState =
                    replayState;

            this.vmCandidates =
                    Collections.unmodifiableList(
                            new ArrayList<VmCandidateSnapshot>(
                                    vmCandidates));
        }

        public List<FeatureChange> getFeatureChanges() {

            return featureChanges;
        }

        public int getChangedFeatureCount() {

            return featureChanges.size();
        }

        public FeatureChange getFeatureChange() {

            return featureChanges.get(0);
        }

        public ReplayState getReplayState() {

            return replayState;
        }

        public List<VmCandidateSnapshot> getVmCandidates() {

            return vmCandidates;
        }

        @Override
        public String toString() {

            return "SynchronizedCounterfactualState{"
                    + "changedFeatureCount="
                    + featureChanges.size()
                    + ", featureChanges="
                    + featureChanges
                    + ", candidateCount="
                    + vmCandidates.size()
                    + ", replayState="
                    + replayState
                    + '}';
        }
    }

//    /**
//     * Backward-compatible method.
//     *
//     * This method assumes a normal execution-target counterfactual.
//     */
    public SynchronizedCounterfactualState synchronize(
            ReplayState baseState,
            List<VmCandidateSnapshot> originalCandidates,
            SchedulerDecision originalDecision,
            FeatureChange featureChange) {

        return synchronize(
                baseState,
                originalCandidates,
                originalDecision,
                Collections.singletonList(
                        featureChange),
                DecisionChangeType.EXECUTION_TARGET_CHANGE);
    }
//
//    /**
//     * Backward-compatible multi-feature method.
//     */
    public SynchronizedCounterfactualState synchronize(
            ReplayState baseState,
            List<VmCandidateSnapshot> originalCandidates,
            SchedulerDecision originalDecision,
            List<FeatureChange> featureChanges) {

        return synchronize(
                baseState,
                originalCandidates,
                originalDecision,
                featureChanges,
                DecisionChangeType.EXECUTION_TARGET_CHANGE);
    }

    /**
     * Objective-aware synchronization method.
     *
     * For FEASIBILITY_RESTORATION, an original selected VM is not
     * required. When a selected-resource CPU feature is generated for
     * an originally unsuccessful decision, the change is applied to
     * the original best-CPU candidate instead.
     */
    public SynchronizedCounterfactualState synchronize(
            ReplayState baseState,
            List<VmCandidateSnapshot> originalCandidates,
            SchedulerDecision originalDecision,
            FeatureChange featureChange,
            DecisionChangeType requestedChangeType) {

        if(featureChange == null) {

            throw new IllegalArgumentException(
                    "Feature change cannot be null.");
        }

        return synchronize(
                baseState,
                originalCandidates,
                originalDecision,
                Collections.singletonList(
                        featureChange),
                requestedChangeType);
    }

    /**
     * Applies a complete objective-aware counterfactual change set.
     */
    public SynchronizedCounterfactualState synchronize(
            ReplayState baseState,
            List<VmCandidateSnapshot> originalCandidates,
            SchedulerDecision originalDecision,
            List<FeatureChange> featureChanges,
            DecisionChangeType requestedChangeType) {

        validateInputs(
                baseState,
                originalCandidates,
                originalDecision,
                featureChanges,
                requestedChangeType);

        ReplayState changedReplayState =
                applyChangesToReplayState(
                        baseState,
                        featureChanges);

        List<VmCandidateSnapshot> changedCandidates =
                synchronizeCandidates(
                        originalCandidates,
                        originalDecision,
                        featureChanges,
                        requestedChangeType);

        return new SynchronizedCounterfactualState(
                featureChanges,
                changedReplayState,
                changedCandidates);
    }

    private ReplayState applyChangesToReplayState(
            ReplayState baseState,
            List<FeatureChange> featureChanges) {

        ReplayState changedReplayState =
                baseState;

        for(FeatureChange featureChange
                : featureChanges) {

            changedReplayState =
                    ReplayState
                    .from(changedReplayState)
                    .applyFeatureChange(
                            featureChange)
                    .build();
        }

        return changedReplayState;
    }

    private List<VmCandidateSnapshot> synchronizeCandidates(
            List<VmCandidateSnapshot> originalCandidates,
            SchedulerDecision originalDecision,
            List<FeatureChange> featureChanges,
            DecisionChangeType requestedChangeType) {

        List<VmCandidateSnapshot> changedCandidates =
                new ArrayList<VmCandidateSnapshot>(
                        originalCandidates);

        boolean originalSelectionSuccessful =
                isSuccessfulSelection(
                        originalDecision);

        boolean feasibilityRestoration =
                requestedChangeType
                == DecisionChangeType
                .FEASIBILITY_RESTORATION;

        VmCandidateSnapshot originalBestCandidate =
                findBestCpuCandidate(
                        originalCandidates);

        int originalBestCandidateIndex =
                originalBestCandidate
                .getCandidateIndex();

        int selectedVmId =
                originalDecision
                .getSelectedVmId();

        for(FeatureChange featureChange
                : featureChanges) {

            String featureName =
                    featureChange
                    .getFeatureName();

            /*
             * A task CPU-requirement change affects every candidate VM.
             */
            if(FeasibleCounterfactualSearchConfig
                    .REQUIRED_CPU_PERCENT
                    .equals(featureName)) {

                changedCandidates =
                        updateRequiredCpuForAllCandidates(
                                changedCandidates,
                                featureChange
                                .getCounterfactualValue());

                continue;
            }

            /*
             * A task core-requirement change affects every candidate VM.
             */
            if(FeasibleCounterfactualSearchConfig
                    .REQUIRED_CORES
                    .equals(featureName)) {

                int newRequiredCoreCount =
                        toPositiveInteger(
                                featureChange
                                .getCounterfactualValue(),
                                featureName);

                changedCandidates =
                        updateRequiredCoresForAllCandidates(
                                changedCandidates,
                                newRequiredCoreCount);

                continue;
            }

            /*
             * A selected-VM CPU change normally affects the originally
             * selected VM.
             *
             * For feasibility restoration, no selected VM exists.
             * Therefore, the change is applied to the original candidate
             * with the greatest available CPU capacity.
             */
            if(FeasibleCounterfactualSearchConfig
                    .SELECTED_AVAILABLE_CPU_PERCENT
                    .equals(featureName)) {

                if(originalSelectionSuccessful) {

                    changedCandidates =
                            updateSelectedCandidateCpu(
                                    changedCandidates,
                                    selectedVmId,
                                    featureChange
                                    .getCounterfactualValue());
                }
                else if(feasibilityRestoration) {

                    changedCandidates =
                            updateCandidateCpuByIndex(
                                    changedCandidates,
                                    originalBestCandidateIndex,
                                    featureChange
                                    .getCounterfactualValue());
                }
                else {

                    throw new IllegalStateException(
                            "The original decision did not select a VM, "
                            + "so selected-resource CPU cannot be "
                            + "synchronized for objective "
                            + requestedChangeType
                            + ".");
                }

                continue;
            }

            /*
             * A best-candidate CPU change affects the candidate that had
             * the greatest original available CPU capacity.
             */
            if(FeasibleCounterfactualSearchConfig
                    .BEST_AVAILABLE_CPU_PERCENT
                    .equals(featureName)) {

                changedCandidates =
                        updateCandidateCpuByIndex(
                                changedCandidates,
                                originalBestCandidateIndex,
                                featureChange
                                .getCounterfactualValue());

                continue;
            }

            /*
             * Bandwidth, latency, task length, and upload-size changes are
             * represented in ReplayState and require no VM snapshot update.
             */
        }

        return changedCandidates;
    }
    
    private boolean isSuccessfulSelection(
            SchedulerDecision decision) {

        if(decision == null) {

            return false;
        }

        return "SELECTED".equalsIgnoreCase(
                decision.getDecisionOutcome())
                && decision.getSelectedVmId() >= 0
                && decision.getSelectedCandidateIndex() >= 0;
    }

    
    private List<VmCandidateSnapshot>
    updateRequiredCpuForAllCandidates(
            List<VmCandidateSnapshot> candidates,
            double newRequiredCpuPercent) {

	List<VmCandidateSnapshot> modifiedCandidates =
	        new ArrayList<VmCandidateSnapshot>();
	
	for(VmCandidateSnapshot candidate
	        : candidates) {
	
	    if(candidate == null) {
	
	        continue;
	    }
	
	    modifiedCandidates.add(
	            candidate.withRequiredCpuPercent(
	                    newRequiredCpuPercent));
	}
	
	if(modifiedCandidates.isEmpty()) {
	
	    throw new IllegalStateException(
	            "No VM candidates were available for "
	            + "required-CPU synchronization.");
	}
	
	return modifiedCandidates;
	}   
    
		    

    private List<VmCandidateSnapshot>
            updateRequiredCoresForAllCandidates(
                    List<VmCandidateSnapshot> candidates,
                    int newRequiredCoreCount) {

        List<VmCandidateSnapshot> modifiedCandidates =
                new ArrayList<VmCandidateSnapshot>();

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate == null) {

                continue;
            }

            modifiedCandidates.add(
                    candidate.withRequiredCoreCount(
                            newRequiredCoreCount));
        }

        if(modifiedCandidates.isEmpty()) {

            throw new IllegalStateException(
                    "No VM candidates were available for "
                    + "required-core synchronization.");
        }

        return modifiedCandidates;
    }

    private static int toPositiveInteger(
            double value,
            String featureName) {

        if(Double.isNaN(value)
                || Double.isInfinite(value)) {

            throw new IllegalArgumentException(
                    featureName
                    + " must be finite.");
        }

        if(value < 1.0) {

            throw new IllegalArgumentException(
                    featureName
                    + " must be at least 1.");
        }

        if(value != Math.floor(value)) {

            throw new IllegalArgumentException(
                    featureName
                    + " must be an integer.");
        }

        if(value > Integer.MAX_VALUE) {

            throw new IllegalArgumentException(
                    featureName
                    + " exceeds the supported integer range.");
        }

        return (int)value;
    }

    private List<VmCandidateSnapshot> updateSelectedCandidateCpu(
            List<VmCandidateSnapshot> candidates,
            int selectedVmId,
            double newAvailableCpuPercent) {

        List<VmCandidateSnapshot> modifiedCandidates =
                new ArrayList<VmCandidateSnapshot>();

        boolean candidateUpdated =
                false;

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate == null) {

                continue;
            }

            if(candidate.getVmId()
                    == selectedVmId) {

                modifiedCandidates.add(
                        candidate.withAvailableCpuPercent(
                                newAvailableCpuPercent));

                candidateUpdated =
                        true;
            }
            else {

                modifiedCandidates.add(
                        candidate);
            }
        }

        if(!candidateUpdated) {

            throw new IllegalStateException(
                    "The originally selected VM could not be "
                    + "found in the candidate list. VM ID: "
                    + selectedVmId);
        }

        return modifiedCandidates;
    }

    private List<VmCandidateSnapshot> updateCandidateCpuByIndex(
            List<VmCandidateSnapshot> candidates,
            int targetCandidateIndex,
            double newAvailableCpuPercent) {

        List<VmCandidateSnapshot> modifiedCandidates =
                new ArrayList<VmCandidateSnapshot>();

        boolean candidateUpdated =
                false;

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate == null) {

                continue;
            }

            if(candidate.getCandidateIndex()
                    == targetCandidateIndex) {

                modifiedCandidates.add(
                        candidate.withAvailableCpuPercent(
                                newAvailableCpuPercent));

                candidateUpdated =
                        true;
            }
            else {

                modifiedCandidates.add(
                        candidate);
            }
        }

        if(!candidateUpdated) {

            throw new IllegalStateException(
                    "The target CPU candidate could not be found. "
                    + "Candidate index: "
                    + targetCandidateIndex);
        }

        return modifiedCandidates;
    }
    
    
    private VmCandidateSnapshot findBestCpuCandidate(
            List<VmCandidateSnapshot> candidates) {

        VmCandidateSnapshot bestCandidate =
                null;

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate == null) {

                continue;
            }

            if(bestCandidate == null) {

                bestCandidate =
                        candidate;

                continue;
            }

            double candidateCpu =
                    candidate
                    .getAvailableCpuPercent();

            double bestCpu =
                    bestCandidate
                    .getAvailableCpuPercent();

            if(candidateCpu > bestCpu) {

                bestCandidate =
                        candidate;
            }
            else if(candidateCpu == bestCpu
                    && candidate.getCandidateIndex()
                    < bestCandidate.getCandidateIndex()) {

                bestCandidate =
                        candidate;
            }
        }

        if(bestCandidate == null) {

            throw new IllegalStateException(
                    "No VM candidate is available.");
        }

        return bestCandidate;
    }

    private void validateInputs(
            ReplayState baseState,
            List<VmCandidateSnapshot> candidates,
            SchedulerDecision originalDecision,
            List<FeatureChange> featureChanges,
            DecisionChangeType requestedChangeType) {

        if(baseState == null) {

            throw new IllegalArgumentException(
                    "Base ReplayState cannot be null.");
        }

        if(candidates == null
                || candidates.isEmpty()) {

            throw new IllegalArgumentException(
                    "VM candidate list cannot be null or empty.");
        }

        if(originalDecision == null) {

            throw new IllegalArgumentException(
                    "Original scheduler decision cannot be null.");
        }

        if(requestedChangeType == null) {

            throw new IllegalArgumentException(
                    "Requested decision-change type cannot be null.");
        }

        if(featureChanges == null
                || featureChanges.isEmpty()) {

            throw new IllegalArgumentException(
                    "Feature-change list cannot be null or empty.");
        }

        Set<String> encounteredFeatureNames =
                new LinkedHashSet<String>();

        for(FeatureChange featureChange
                : featureChanges) {

            if(featureChange == null) {

                throw new IllegalArgumentException(
                        "Feature-change list cannot contain null.");
            }

            if(!featureChange.isMutable()) {

                throw new IllegalArgumentException(
                        "Every feature change must be mutable. "
                        + "Feature: "
                        + featureChange.getFeatureName());
            }

            if(!featureChange.isActionable()) {

                throw new IllegalArgumentException(
                        "Every feature change must be actionable. "
                        + "Feature: "
                        + featureChange.getFeatureName());
            }

            if(!featureChange.hasChanged()) {

                throw new IllegalArgumentException(
                        "Every feature change must modify its "
                        + "original value. Feature: "
                        + featureChange.getFeatureName());
            }

            if(!featureChange.isValidChange()) {

                throw new IllegalArgumentException(
                        "Every feature change must remain within "
                        + "its allowed range. Feature: "
                        + featureChange.getFeatureName());
            }

            if(!encounteredFeatureNames.add(
                    featureChange.getFeatureName())) {

                throw new IllegalArgumentException(
                        "A counterfactual candidate cannot contain "
                        + "multiple changes for the same feature: "
                        + featureChange.getFeatureName());
            }
        }
    }
}
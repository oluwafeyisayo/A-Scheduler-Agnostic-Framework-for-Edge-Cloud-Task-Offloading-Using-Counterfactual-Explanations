package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.boun.edgecloudsim.network.DestinationNetworkState;

import edu.boun.edgecloudsim.research.counterfactual.features
        .CounterfactualFeatureNames;

import edu.boun.edgecloudsim.research.counterfactual.units
        .EngineeringUnits;

/**
 * Represents a reconstructed scheduler-decision state that can be
 * modified for counterfactual evaluation.
 *
 * DecisionState remains the immutable record of the original decision.
 * ReplayState represents a potentially modified environment derived
 * from that decision.
 *
 * Standard engineering units:
 *
 * simulationTimeSeconds        = seconds
 * taskLengthMi                 = million instructions
 * uploadSizeKb                 = kilobytes
 * availableBandwidthMbps       = megabits per second
 * networkLatencyMs             = milliseconds
 * CPU values                   = percentage
 * selectedVmMips               = million instructions per second
 */
public final class ReplayState {

    private static final double EPSILON =
            0.000001;

    private final DecisionState originalDecision;

    private final double simulationTimeSeconds;
    private final int taskId;

    private final String originalPolicy;
    private final String originalOutcome;
    private final String originalDestination;
    private final String attemptedDestination;

    private final int originalSelectedVmId;
    private final int roundRobinStartingCandidateIndex;
    private final int randomSampledCandidateIndex;

    private final double requiredCpuPercent;
    private final double selectedAvailableCpuPercent;
    private final double bestAvailableCpuPercent;

    /*
     * Compute requirements and selected-VM capacity.
     */
    private final int requiredCores;
    private final double selectedVmMips;
    private final int selectedVmCoreCount;

    /*
     * Legacy attempted-path values retained for existing feasibility
     * rules and search code.
     */
    private final double networkLatencyMs;
    private final double availableBandwidthMbps;

    /*
     * Destination-specific network state.
     */
    private final double edgeNetworkLatencyMs;
    private final double edgeAvailableBandwidthMbps;
    private final double edgeUploadDelayMs;
    private final boolean edgeNetworkFeasible;

    private final double cloudNetworkLatencyMs;
    private final double cloudAvailableBandwidthMbps;
    private final double cloudUploadDelayMs;
    private final boolean cloudNetworkFeasible;

    private final double taskLengthMi;
    private final double uploadSizeKb;

    private final int candidateVmCount;
    private final int feasibleVmCount;
    

    private final List<FeatureChange> appliedChanges;

    private ReplayState(
            Builder builder) {

        if(builder.originalDecision == null) {

            throw new IllegalArgumentException(
                    "Original decision cannot be null.");
        }

        this.originalDecision =
                builder.originalDecision;

        this.simulationTimeSeconds =
                builder.simulationTimeSeconds;

        this.taskId =
                builder.taskId;

        this.originalPolicy =
                safeText(
                        builder.originalPolicy);

        this.originalOutcome =
                safeText(
                        builder.originalOutcome);

        this.originalDestination =
                safeText(
                        builder.originalDestination);

        this.attemptedDestination =
                safeText(
                        builder.attemptedDestination);

        this.originalSelectedVmId =
                builder.originalSelectedVmId;

        this.roundRobinStartingCandidateIndex =
                builder.roundRobinStartingCandidateIndex;

        this.randomSampledCandidateIndex =
                builder.randomSampledCandidateIndex;

        this.requiredCpuPercent =
                builder.requiredCpuPercent;

        this.selectedAvailableCpuPercent =
                builder.selectedAvailableCpuPercent;

        this.bestAvailableCpuPercent =
                builder.bestAvailableCpuPercent;

        this.requiredCores =
                builder.requiredCores;

        this.selectedVmMips =
                builder.selectedVmMips;

        this.selectedVmCoreCount =
                builder.selectedVmCoreCount;

        this.networkLatencyMs =
                builder.networkLatencyMs;

        this.availableBandwidthMbps =
                builder.availableBandwidthMbps;

        this.edgeNetworkLatencyMs =
                builder.edgeNetworkLatencyMs;

        this.edgeAvailableBandwidthMbps =
                builder.edgeAvailableBandwidthMbps;

        this.edgeUploadDelayMs =
                builder.edgeUploadDelayMs;

        this.edgeNetworkFeasible =
                builder.edgeNetworkFeasible;

        this.cloudNetworkLatencyMs =
                builder.cloudNetworkLatencyMs;

        this.cloudAvailableBandwidthMbps =
                builder.cloudAvailableBandwidthMbps;

        this.cloudUploadDelayMs =
                builder.cloudUploadDelayMs;

        this.cloudNetworkFeasible =
                builder.cloudNetworkFeasible;

        this.taskLengthMi =
                builder.taskLengthMi;

        this.uploadSizeKb =
                builder.uploadSizeKb;

        this.candidateVmCount =
                builder.candidateVmCount;

        this.feasibleVmCount =
                builder.feasibleVmCount;

        this.appliedChanges =
                Collections.unmodifiableList(
                        new ArrayList<FeatureChange>(
                                builder.appliedChanges));
    }

    /**
     * Creates a ReplayState builder from an original DecisionState.
     */
    public static Builder from(
            DecisionState decisionState) {

        return new Builder(
                decisionState);
    }

    /**
     * Creates a ReplayState builder by copying an existing ReplayState.
     */
    public static Builder from(
            ReplayState replayState) {

        return new Builder(
                replayState);
    }

    private static String safeText(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return "UNKNOWN";
        }

        return value.trim();
    }

    private static boolean isFinite(
            double value) {

        return !Double.isNaN(value)
                && !Double.isInfinite(value);
    }

    public DecisionState getOriginalDecision() {

        return originalDecision;
    }

    public double getSimulationTimeSeconds() {

        return simulationTimeSeconds;
    }

    /**
     * Backward-compatible getter.
     */
    public double getSimulationTime() {

        return getSimulationTimeSeconds();
    }

    public int getTaskId() {

        return taskId;
    }

    public String getOriginalPolicy() {

        return originalPolicy;
    }

    public String getOriginalOutcome() {

        return originalOutcome;
    }

    public String getOriginalDestination() {

        return originalDestination;
    }

    public String getAttemptedDestination() {

        return attemptedDestination;
    }

    public int getOriginalSelectedVmId() {

        return originalSelectedVmId;
    }

    public int getRoundRobinStartingCandidateIndex() {

        return roundRobinStartingCandidateIndex;
    }

    public int getRandomSampledCandidateIndex() {

        return randomSampledCandidateIndex;
    }

    public double getRequiredCpuPercent() {

        return requiredCpuPercent;
    }

    public double getSelectedAvailableCpuPercent() {

        return selectedAvailableCpuPercent;
    }

    public double getBestAvailableCpuPercent() {

        return bestAvailableCpuPercent;
    }

    public int getRequiredCores() {

        return requiredCores;
    }

    public double getSelectedVmMips() {

        return selectedVmMips;
    }

    public int getSelectedVmCoreCount() {

        return selectedVmCoreCount;
    }

    public double getNetworkLatencyMs() {

        return networkLatencyMs;
    }
    
    /**
     * Returns true when the reconstructed task-level network path is
     * valid and produces a finite delay estimate.
     */
    public boolean isNetworkPathFeasible() {

        if(!hasValidAvailableBandwidth()
                || !hasValidNetworkLatency()
                || !hasValidUploadSize()) {

            return false;
        }

        double transmissionDelayMs =
                estimateUploadTransmissionTimeMs();

        double totalDelayMs =
                estimateTotalNetworkDelayMs();

        return isFinite(transmissionDelayMs)
                && transmissionDelayMs >= 0.0
                && isFinite(totalDelayMs)
                && totalDelayMs >= 0.0;
    }

    /**
     * Backward-compatible getter.
     */
    @Deprecated
    public double getNetworkLatency() {

        return getNetworkLatencyMs();
    }

    public double getAvailableBandwidthMbps() {

        return availableBandwidthMbps;
    }

    public double getEdgeNetworkLatencyMs() {

        return edgeNetworkLatencyMs;
    }

    public double getEdgeAvailableBandwidthMbps() {

        return edgeAvailableBandwidthMbps;
    }

    public double getEdgeUploadDelayMs() {

        return edgeUploadDelayMs;
    }

    public boolean isEdgeNetworkFeasible() {

        return edgeNetworkFeasible;
    }

    public double getCloudNetworkLatencyMs() {

        return cloudNetworkLatencyMs;
    }

    public double getCloudAvailableBandwidthMbps() {

        return cloudAvailableBandwidthMbps;
    }

    public double getCloudUploadDelayMs() {

        return cloudUploadDelayMs;
    }

    public boolean isCloudNetworkFeasible() {

        return cloudNetworkFeasible;
    }

    public double getNetworkLatencyMsForDestination(
            String destination) {

        return "CLOUD".equalsIgnoreCase(
                destination)
                ? cloudNetworkLatencyMs
                : edgeNetworkLatencyMs;
    }

    public double getAvailableBandwidthMbpsForDestination(
            String destination) {

        return "CLOUD".equalsIgnoreCase(
                destination)
                ? cloudAvailableBandwidthMbps
                : edgeAvailableBandwidthMbps;
    }

    public double getUploadDelayMsForDestination(
            String destination) {

        return "CLOUD".equalsIgnoreCase(
                destination)
                ? cloudUploadDelayMs
                : edgeUploadDelayMs;
    }

    public boolean isNetworkFeasibleForDestination(
            String destination) {

        return "CLOUD".equalsIgnoreCase(
                destination)
                ? cloudNetworkFeasible
                : edgeNetworkFeasible;
    }

    /**
     * Backward-compatible getter.
     */
    @Deprecated
    public double getAvailableBandwidth() {

        return getAvailableBandwidthMbps();
    }

    public double getTaskLengthMi() {

        return taskLengthMi;
    }

    /**
     * Backward-compatible getter.
     */
    @Deprecated
    public double getTaskLength() {

        return getTaskLengthMi();
    }

    public double getUploadSizeKb() {

        return uploadSizeKb;
    }

    /**
     * Backward-compatible getter.
     */
    @Deprecated
    public double getUploadSize() {

        return getUploadSizeKb();
    }

    public int getCandidateVmCount() {

        return candidateVmCount;
    }

    public int getFeasibleVmCount() {

        return feasibleVmCount;
    }

    public List<FeatureChange> getAppliedChanges() {

        return appliedChanges;
    }

    /**
     * Evaluates selected-resource CPU feasibility.
     */
    public boolean isCpuFeasible() {

        return selectedAvailableCpuPercent >= 0.0
                && requiredCpuPercent
                <= selectedAvailableCpuPercent
                + EPSILON;
    }

    public double getCpuFeasibilityMargin() {

        return selectedAvailableCpuPercent
                - requiredCpuPercent;
    }

    /**
     * Evaluates selected-resource core feasibility.
     */
    public boolean isSelectedResourceCoreFeasible() {

        return requiredCores >= 0
                && selectedVmCoreCount >= 0
                && requiredCores
                <= selectedVmCoreCount;
    }

    /**
     * Returns true when the selected VM exposes usable processing
     * capacity.
     */
    public boolean hasValidSelectedVmMips() {

        return isFinite(
                selectedVmMips)
                && selectedVmMips > 0.0;
    }

    /**
     * Returns true when the originally selected VM satisfies all
     * currently modeled compute constraints.
     */
    public boolean isSelectedResourceComputeFeasible() {

        return isCpuFeasible()
                && isSelectedResourceCoreFeasible()
                && hasValidSelectedVmMips();
    }

    public boolean isCandidateSetCpuFeasible() {

        return candidateVmCount > 0
                && feasibleVmCount > 0
                && bestAvailableCpuPercent >= 0.0
                && bestAvailableCpuPercent
                + EPSILON
                >= requiredCpuPercent;
    }

    public double getCandidateSetCpuFeasibilityMargin() {

        return bestAvailableCpuPercent
                - requiredCpuPercent;
    }

    public boolean hasValidNetworkLatency() {

        return isFinite(
                networkLatencyMs)
                && networkLatencyMs >= 0.0;
    }

    public boolean hasValidAvailableBandwidth() {

        return isFinite(
                availableBandwidthMbps)
                && availableBandwidthMbps > 0.0;
    }

    public boolean hasValidTaskLength() {

        return isFinite(
                taskLengthMi)
                && taskLengthMi > 0.0;
    }

    public boolean hasValidUploadSize() {

        return isFinite(
                uploadSizeKb)
                && uploadSizeKb > 0.0;
    }

    /**
     * Estimates selected-VM processing time in milliseconds.
     *
     * taskLengthMi   = million instructions
     * selectedVmMips = million instructions per second
     */
    public double estimateSelectedVmProcessingTimeMs() {

        if(!hasValidSelectedVmMips()) {

            return Double.POSITIVE_INFINITY;
        }

        return taskLengthMi
                / selectedVmMips
                * 1000.0;
    }

    /**
     * Estimates upload transmission time in milliseconds.
     *
     * uploadSizeKb            = kilobytes
     * availableBandwidthMbps  = megabits per second
     */
    public double estimateUploadTransmissionTimeMs() {

        return EngineeringUnits
                .calculateTransmissionTimeMs(
                        uploadSizeKb,
                        availableBandwidthMbps);
    }

    /**
     * Estimates total communication delay in milliseconds.
     */
    public double estimateTotalNetworkDelayMs() {

        double transmissionTimeMs =
                estimateUploadTransmissionTimeMs();

        if(Double.isInfinite(
                transmissionTimeMs)) {

            return Double.POSITIVE_INFINITY;
        }

        return networkLatencyMs
                + transmissionTimeMs;
    }

    /**
     * Backward-compatible method.
     */
    @Deprecated
    public double estimateUploadTransmissionTime() {

        return estimateUploadTransmissionTimeMs();
    }

    /**
     * Backward-compatible method.
     */
    @Deprecated
    public double estimateTotalNetworkDelay() {

        return estimateTotalNetworkDelayMs();
    }

    public boolean hasAppliedChanges() {

        for(FeatureChange change
                : appliedChanges) {

            if(change.hasChanged()) {

                return true;
            }
        }

        return false;
    }

    public int getChangedFeatureCount() {

        int count =
                0;

        for(FeatureChange change
                : appliedChanges) {

            if(change.hasChanged()) {

                count++;
            }
        }

        return count;
    }

    public boolean hasValidAppliedChanges() {

        for(FeatureChange change
                : appliedChanges) {

            if(!change.isValidChange()) {

                return false;
            }
        }

        return true;
    }

    public double calculateNormalizedDistance() {

        double distance =
                0.0;

        for(FeatureChange change
                : appliedChanges) {

            distance +=
                    change.getNormalizedChange();
        }

        return distance;
    }

    public FeatureChange findAppliedChange(
            String featureName) {

        if(featureName == null) {

            return null;
        }

        String normalizedName =
                Builder.normalizeFeatureName(
                        featureName);

        for(FeatureChange change
                : appliedChanges) {

            String changeName =
                    Builder.normalizeFeatureName(
                            change.getFeatureName());

            if(changeName.equals(
                    normalizedName)) {

                return change;
            }
        }

        return null;
    }

    @Override
    public String toString() {

        return "ReplayState{"
                + "simulationTimeSeconds="
                + simulationTimeSeconds

                + ", taskId="
                + taskId

                + ", originalPolicy='"
                + originalPolicy
                + '\''

                + ", originalOutcome='"
                + originalOutcome
                + '\''

                + ", originalDestination='"
                + originalDestination
                + '\''

                + ", originalSelectedVmId="
                + originalSelectedVmId

                + ", requiredCpuPercent="
                + requiredCpuPercent

                + ", selectedAvailableCpuPercent="
                + selectedAvailableCpuPercent

                + ", bestAvailableCpuPercent="
                + bestAvailableCpuPercent

                + ", requiredCores="
                + requiredCores

                + ", selectedVmCoreCount="
                + selectedVmCoreCount

                + ", selectedVmMips="
                + selectedVmMips

                + ", selectedResourceCoreFeasible="
                + isSelectedResourceCoreFeasible()

                + ", selectedVmMipsValid="
                + hasValidSelectedVmMips()

                + ", selectedResourceComputeFeasible="
                + isSelectedResourceComputeFeasible()

                + ", selectedVmProcessingTimeMs="
                + estimateSelectedVmProcessingTimeMs()

                + ", networkLatencyMs="
                + networkLatencyMs

                + ", availableBandwidthMbps="
                + availableBandwidthMbps

                + ", taskLengthMi="
                + taskLengthMi

                + ", uploadSizeKb="
                + uploadSizeKb

                + ", candidateVmCount="
                + candidateVmCount

                + ", feasibleVmCount="
                + feasibleVmCount

                + ", cpuFeasible="
                + isCpuFeasible()

                + ", candidateSetCpuFeasible="
                + isCandidateSetCpuFeasible()

                + ", cpuFeasibilityMargin="
                + getCpuFeasibilityMargin()

                + ", uploadTransmissionTimeMs="
                + estimateUploadTransmissionTimeMs()

                + ", totalNetworkDelayMs="
                + estimateTotalNetworkDelayMs()

                + ", changedFeatureCount="
                + getChangedFeatureCount()

                + ", normalizedDistance="
                + calculateNormalizedDistance()
                
                + ", networkPathFeasible="
                + isNetworkPathFeasible()

                + '}';
    }

    public static final class Builder {

        private final DecisionState originalDecision;

        private double simulationTimeSeconds;
        private int taskId;

        private String originalPolicy;
        private String originalOutcome;
        private String originalDestination;
        private String attemptedDestination;

        private int originalSelectedVmId;
        private int roundRobinStartingCandidateIndex;
        private int randomSampledCandidateIndex;

        private double requiredCpuPercent;
        private double selectedAvailableCpuPercent;
        private double bestAvailableCpuPercent;

        private int requiredCores;
        private double selectedVmMips;
        private int selectedVmCoreCount;

        private double networkLatencyMs;
        private double availableBandwidthMbps;

        private double edgeNetworkLatencyMs;
        private double edgeAvailableBandwidthMbps;
        private double edgeUploadDelayMs;
        private boolean edgeNetworkFeasible;

        private double cloudNetworkLatencyMs;
        private double cloudAvailableBandwidthMbps;
        private double cloudUploadDelayMs;
        private boolean cloudNetworkFeasible;

        private double taskLengthMi;
        private double uploadSizeKb;

        private int candidateVmCount;
        private int feasibleVmCount;

        private final List<FeatureChange> appliedChanges =
                new ArrayList<FeatureChange>();

        private Builder(
                DecisionState originalDecision) {

            if(originalDecision == null) {

                throw new IllegalArgumentException(
                        "Original decision cannot be null.");
            }

            this.originalDecision =
                    originalDecision;

            this.simulationTimeSeconds =
                    originalDecision
                    .getSimulationTimeSeconds();

            this.taskId =
                    originalDecision
                    .getTaskId();

            this.originalPolicy =
                    originalDecision
                    .getOrchestratorPolicy();

            this.originalOutcome =
                    originalDecision
                    .getDecisionOutcome();

            this.originalDestination =
                    originalDecision
                    .getSelectedDestination();

            this.attemptedDestination =
                    originalDecision
                    .getAttemptedDestination();

            this.originalSelectedVmId =
                    originalDecision
                    .getSelectedVmId();

            this.roundRobinStartingCandidateIndex =
                    originalDecision
                    .getRoundRobinStartingCandidateIndex();

            this.randomSampledCandidateIndex =
                    originalDecision
                    .getRandomSampledCandidateIndex();

            this.requiredCpuPercent =
                    originalDecision
                    .getRequiredCpuPercent();

            this.selectedAvailableCpuPercent =
                    originalDecision
                    .getSelectedAvailableCpuPercent();

            this.bestAvailableCpuPercent =
                    originalDecision
                    .getBestAvailableCpuPercent();

            this.requiredCores =
                    originalDecision
                    .getRequiredCores();

            this.selectedVmMips =
                    originalDecision
                    .getSelectedVmMips();

            this.selectedVmCoreCount =
                    originalDecision
                    .getSelectedVmCoreCount();

            this.networkLatencyMs =
                    originalDecision
                    .getNetworkLatencyMs();

            this.availableBandwidthMbps =
                    originalDecision
                    .getAvailableBandwidthMbps();

            DestinationNetworkState edgeState =
                    originalDecision
                    .getEdgeNetworkState();

            DestinationNetworkState cloudState =
                    originalDecision
                    .getCloudNetworkState();

            this.edgeNetworkLatencyMs =
                    edgeState
                    .getUploadDelayMilliseconds();

            this.edgeAvailableBandwidthMbps =
                    edgeState
                    .getAvailableBandwidthMbps();

            this.edgeUploadDelayMs =
                    edgeState
                    .getUploadDelayMilliseconds();

            this.edgeNetworkFeasible =
                    edgeState
                    .isNetworkFeasible();

            this.cloudNetworkLatencyMs =
                    cloudState
                    .getUploadDelayMilliseconds();

            this.cloudAvailableBandwidthMbps =
                    cloudState
                    .getAvailableBandwidthMbps();

            this.cloudUploadDelayMs =
                    cloudState
                    .getUploadDelayMilliseconds();

            this.cloudNetworkFeasible =
                    cloudState
                    .isNetworkFeasible();

            this.taskLengthMi =
                    originalDecision
                    .getTaskLengthMi();

            this.uploadSizeKb =
                    originalDecision
                    .getUploadSizeKb();

            this.candidateVmCount =
                    originalDecision
                    .getCandidateVmCount();

            this.feasibleVmCount =
                    originalDecision
                    .getFeasibleVmCount();
        }

        private Builder(
                ReplayState replayState) {

            if(replayState == null) {

                throw new IllegalArgumentException(
                        "Replay state cannot be null.");
            }

            this.originalDecision =
                    replayState.originalDecision;

            this.simulationTimeSeconds =
                    replayState.simulationTimeSeconds;

            this.taskId =
                    replayState.taskId;

            this.originalPolicy =
                    replayState.originalPolicy;

            this.originalOutcome =
                    replayState.originalOutcome;

            this.originalDestination =
                    replayState.originalDestination;

            this.attemptedDestination =
                    replayState.attemptedDestination;

            this.originalSelectedVmId =
                    replayState.originalSelectedVmId;

            this.roundRobinStartingCandidateIndex =
                    replayState.roundRobinStartingCandidateIndex;

            this.randomSampledCandidateIndex =
                    replayState.randomSampledCandidateIndex;

            this.requiredCpuPercent =
                    replayState.requiredCpuPercent;

            this.selectedAvailableCpuPercent =
                    replayState.selectedAvailableCpuPercent;

            this.bestAvailableCpuPercent =
                    replayState.bestAvailableCpuPercent;

            this.requiredCores =
                    replayState.requiredCores;

            this.selectedVmMips =
                    replayState.selectedVmMips;

            this.selectedVmCoreCount =
                    replayState.selectedVmCoreCount;

            this.networkLatencyMs =
                    replayState.networkLatencyMs;

            this.availableBandwidthMbps =
                    replayState.availableBandwidthMbps;

            this.edgeNetworkLatencyMs =
                    replayState.edgeNetworkLatencyMs;

            this.edgeAvailableBandwidthMbps =
                    replayState.edgeAvailableBandwidthMbps;

            this.edgeUploadDelayMs =
                    replayState.edgeUploadDelayMs;

            this.edgeNetworkFeasible =
                    replayState.edgeNetworkFeasible;

            this.cloudNetworkLatencyMs =
                    replayState.cloudNetworkLatencyMs;

            this.cloudAvailableBandwidthMbps =
                    replayState.cloudAvailableBandwidthMbps;

            this.cloudUploadDelayMs =
                    replayState.cloudUploadDelayMs;

            this.cloudNetworkFeasible =
                    replayState.cloudNetworkFeasible;

            this.taskLengthMi =
                    replayState.taskLengthMi;

            this.uploadSizeKb =
                    replayState.uploadSizeKb;

            this.candidateVmCount =
                    replayState.candidateVmCount;

            this.feasibleVmCount =
                    replayState.feasibleVmCount;

            this.appliedChanges.addAll(
                    replayState.appliedChanges);
        }

        public Builder withRequiredCpuPercent(
                double value) {

            this.requiredCpuPercent =
                    value;

            return this;
        }

        public Builder withSelectedAvailableCpuPercent(
                double value) {

            this.selectedAvailableCpuPercent =
                    value;

            return this;
        }

        public Builder withBestAvailableCpuPercent(
                double value) {

            this.bestAvailableCpuPercent =
                    value;

            return this;
        }

        public Builder withRequiredCores(
                int value) {

            this.requiredCores =
                    value;

            return this;
        }

        public Builder withSelectedVmMips(
                double value) {

            this.selectedVmMips =
                    value;

            return this;
        }

        public Builder withSelectedVmCoreCount(
                int value) {

            this.selectedVmCoreCount =
                    value;

            return this;
        }

        public Builder withNetworkLatencyMs(
                double value) {

            this.networkLatencyMs =
                    value;

            if("CLOUD".equalsIgnoreCase(
                    attemptedDestination)) {

                this.cloudNetworkLatencyMs =
                        value;
            }
            else {

                this.edgeNetworkLatencyMs =
                        value;
            }

            return this;
        }

        public Builder withAvailableBandwidthMbps(
                double value) {

            this.availableBandwidthMbps =
                    value;

            if("CLOUD".equalsIgnoreCase(
                    attemptedDestination)) {

                this.cloudAvailableBandwidthMbps =
                        value;
            }
            else {

                this.edgeAvailableBandwidthMbps =
                        value;
            }

            return this;
        }

        public Builder withEdgeNetworkLatencyMs(
                double value) {

            this.edgeNetworkLatencyMs =
                    value;

            if(!"CLOUD".equalsIgnoreCase(
                    attemptedDestination)) {

                this.networkLatencyMs =
                        value;
            }

            return this;
        }

        public Builder withEdgeAvailableBandwidthMbps(
                double value) {

            this.edgeAvailableBandwidthMbps =
                    value;

            if(!"CLOUD".equalsIgnoreCase(
                    attemptedDestination)) {

                this.availableBandwidthMbps =
                        value;
            }

            return this;
        }

        public Builder withEdgeUploadDelayMs(
                double value) {

            this.edgeUploadDelayMs =
                    value;

            return this;
        }

        public Builder withEdgeNetworkFeasible(
                boolean value) {

            this.edgeNetworkFeasible =
                    value;

            return this;
        }

        public Builder withCloudNetworkLatencyMs(
                double value) {

            this.cloudNetworkLatencyMs =
                    value;

            if("CLOUD".equalsIgnoreCase(
                    attemptedDestination)) {

                this.networkLatencyMs =
                        value;
            }

            return this;
        }

        public Builder withCloudAvailableBandwidthMbps(
                double value) {

            this.cloudAvailableBandwidthMbps =
                    value;

            if("CLOUD".equalsIgnoreCase(
                    attemptedDestination)) {

                this.availableBandwidthMbps =
                        value;
            }

            return this;
        }

        public Builder withCloudUploadDelayMs(
                double value) {

            this.cloudUploadDelayMs =
                    value;

            return this;
        }

        public Builder withCloudNetworkFeasible(
                boolean value) {

            this.cloudNetworkFeasible =
                    value;

            return this;
        }

        public Builder withTaskLengthMi(
                double value) {

            this.taskLengthMi =
                    value;

            return this;
        }

        public Builder withUploadSizeKb(
                double value) {

            this.uploadSizeKb =
                    value;

            return this;
        }

        @Deprecated
        public Builder withNetworkLatency(
                double value) {

            return withNetworkLatencyMs(
                    value);
        }

        @Deprecated
        public Builder withAvailableBandwidth(
                double value) {

            return withAvailableBandwidthMbps(
                    value);
        }

        @Deprecated
        public Builder withTaskLength(
                double value) {

            return withTaskLengthMi(
                    value);
        }

        @Deprecated
        public Builder withUploadSize(
                double value) {

            return withUploadSizeKb(
                    value);
        }

        public Builder withCandidateVmCount(
                int value) {

            this.candidateVmCount =
                    value;

            return this;
        }

        public Builder withFeasibleVmCount(
                int value) {

            this.feasibleVmCount =
                    value;

            return this;
        }

        public Builder withOriginalPolicy(
                String value) {

            if(value == null
                    || value.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Original policy cannot be null or empty.");
            }

            this.originalPolicy =
                    value.trim();

            return this;
        }

        public Builder withAttemptedDestination(
                String value) {

            if(value == null
                    || value.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Attempted destination cannot be empty.");
            }

            this.attemptedDestination =
                    value.trim();

            return this;
        }

        public Builder withRoundRobinStartingCandidateIndex(
                int value) {

            this.roundRobinStartingCandidateIndex =
                    value;

            return this;
        }

        public Builder withRandomSampledCandidateIndex(
                int value) {

            this.randomSampledCandidateIndex =
                    value;

            return this;
        }

        public Builder applyFeatureChange(
                FeatureChange featureChange) {

            if(featureChange == null) {

                throw new IllegalArgumentException(
                        "Feature change cannot be null.");
            }

            if(!featureChange.isValidChange()) {

                throw new IllegalArgumentException(
                        "Invalid feature change: "
                        + featureChange.getFeatureName());
            }

            String featureName =
                    normalizeFeatureName(
                            featureChange.getFeatureName());

            double newValue =
                    featureChange.getCounterfactualValue();

            if(CounterfactualFeatureNames
                    .REQUIRED_CPU_PERCENT
                    .equals(featureName)) {

                this.requiredCpuPercent =
                        newValue;
            }
            else if(CounterfactualFeatureNames
                    .REQUIRED_CORES
                    .equals(featureName)
                    || "required_core_count".equals(featureName)
                    || "required_core".equals(featureName)) {

                this.requiredCores =
                        toPositiveInteger(
                                newValue,
                                featureName);
            }
            else if(CounterfactualFeatureNames
                    .SELECTED_AVAILABLE_CPU_PERCENT
                    .equals(featureName)) {

                this.selectedAvailableCpuPercent =
                        newValue;
            }
            else if(CounterfactualFeatureNames
                    .BEST_AVAILABLE_CPU_PERCENT
                    .equals(featureName)) {

                this.bestAvailableCpuPercent =
                        newValue;
            }
            else if("edge_network_latency_ms".equals(
                    featureName)) {

                withEdgeNetworkLatencyMs(
                        newValue);
            }
            else if("cloud_network_latency_ms".equals(
                    featureName)) {

                withCloudNetworkLatencyMs(
                        newValue);
            }
            else if("edge_available_bandwidth_mbps".equals(
                    featureName)) {

                withEdgeAvailableBandwidthMbps(
                        newValue);
            }
            else if("cloud_available_bandwidth_mbps".equals(
                    featureName)) {

                withCloudAvailableBandwidthMbps(
                        newValue);
            }
            else if("edge_upload_delay_ms".equals(
                    featureName)) {

                withEdgeUploadDelayMs(
                        newValue);
            }
            else if("cloud_upload_delay_ms".equals(
                    featureName)) {

                withCloudUploadDelayMs(
                        newValue);
            }
            else if(CounterfactualFeatureNames
                    .NETWORK_LATENCY_MS
                    .equals(featureName)
                    || "network_latency".equals(featureName)
                    || "latency".equals(featureName)) {

                withNetworkLatencyMs(
                        newValue);
            }
            else if(CounterfactualFeatureNames
                    .AVAILABLE_BANDWIDTH_MBPS
                    .equals(featureName)
                    || "available_bandwidth".equals(featureName)
                    || "bandwidth".equals(featureName)) {

                withAvailableBandwidthMbps(
                        newValue);
            }
            else if(CounterfactualFeatureNames
                    .TASK_LENGTH_MI
                    .equals(featureName)
                    || "task_length".equals(featureName)) {

                this.taskLengthMi =
                        newValue;
            }
            else if(CounterfactualFeatureNames
                    .UPLOAD_SIZE_KB
                    .equals(featureName)
                    || "upload_size".equals(featureName)
                    || "input_size".equals(featureName)) {

                this.uploadSizeKb =
                        newValue;
            }
            else if("candidate_vm_count".equals(featureName)) {

                this.candidateVmCount =
                        toNonNegativeInteger(
                                newValue,
                                featureName);
            }
            else if("feasible_vm_count".equals(featureName)) {

                this.feasibleVmCount =
                        toNonNegativeInteger(
                                newValue,
                                featureName);
            }
            else {

                throw new IllegalArgumentException(
                        "Unsupported replay feature: "
                        + featureChange.getFeatureName());
            }

            this.appliedChanges.add(
                    featureChange);

            return this;
        }
        public Builder applyFeatureChanges(
                List<FeatureChange> featureChanges) {

            if(featureChanges == null) {

                return this;
            }

            for(FeatureChange featureChange
                    : featureChanges) {

                applyFeatureChange(
                        featureChange);
            }

            return this;
        }

        private static String normalizeFeatureName(
                String featureName) {

            if(featureName == null) {

                throw new IllegalArgumentException(
                        "Feature name cannot be null.");
            }

            return featureName
                    .trim()
                    .toLowerCase()
                    .replace(' ', '_')
                    .replace('-', '_');
        }
        
        private static int toPositiveInteger(
                double value,
                String featureName) {

            if(!isFinite(value)) {

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

        private static int toNonNegativeInteger(
                double value,
                String featureName) {

            if(!isFinite(
                    value)) {

                throw new IllegalArgumentException(
                        featureName
                        + " must be finite.");
            }

            if(value < 0.0) {

                throw new IllegalArgumentException(
                        featureName
                        + " cannot be negative.");
            }

            if(value != Math.floor(
                    value)) {

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

        public ReplayState build() {

            validate();

            return new ReplayState(
                    this);
        }

        private void validate() {

            validatePercentage(
                    requiredCpuPercent,
                    "Required CPU percent");

            validateOptionalPercentage(
                    selectedAvailableCpuPercent,
                    "Selected available CPU percent");

            validateOptionalPercentage(
                    bestAvailableCpuPercent,
                    "Best available CPU percent");

            validatePositiveInteger(
                    requiredCores,
                    "Required core count");

            validateOptionalNonNegativeFinite(
                    selectedVmMips,
                    "Selected VM MIPS");

            validateOptionalInteger(
                    selectedVmCoreCount,
                    "Selected VM core count");

            validateNonNegativeFinite(
                    networkLatencyMs,
                    "Network latency in milliseconds");

            validatePositiveFinite(
                    availableBandwidthMbps,
                    "Available bandwidth in Mbps");

            validateOptionalNonNegativeFinite(
                    edgeNetworkLatencyMs,
                    "EDGE network latency in milliseconds");

            validateOptionalPositiveFinite(
                    edgeAvailableBandwidthMbps,
                    "EDGE available bandwidth in Mbps");

            validateOptionalNonNegativeFinite(
                    edgeUploadDelayMs,
                    "EDGE upload delay in milliseconds");

            validateOptionalNonNegativeFinite(
                    cloudNetworkLatencyMs,
                    "CLOUD network latency in milliseconds");

            validateOptionalPositiveFinite(
                    cloudAvailableBandwidthMbps,
                    "CLOUD available bandwidth in Mbps");

            validateOptionalNonNegativeFinite(
                    cloudUploadDelayMs,
                    "CLOUD upload delay in milliseconds");

            validatePositiveFinite(
                    taskLengthMi,
                    "Task length in MI");

            validatePositiveFinite(
                    uploadSizeKb,
                    "Upload size in KB");

            if(candidateVmCount < 0) {

                throw new IllegalStateException(
                        "Candidate VM count cannot be negative.");
            }

            if(feasibleVmCount < 0) {

                throw new IllegalStateException(
                        "Feasible VM count cannot be negative.");
            }

            if(feasibleVmCount > candidateVmCount) {

                throw new IllegalStateException(
                        "Feasible VM count cannot exceed "
                        + "candidate VM count.");
            }

            validateOptionalInteger(
                    roundRobinStartingCandidateIndex,
                    "Round-robin starting candidate index");

            validateOptionalInteger(
                    randomSampledCandidateIndex,
                    "Random sampled candidate index");

            if(attemptedDestination == null
                    || attemptedDestination.trim().isEmpty()) {

                throw new IllegalStateException(
                        "Attempted destination cannot be empty.");
            }

            if(originalPolicy == null
                    || originalPolicy.trim().isEmpty()) {

                throw new IllegalStateException(
                        "Original policy cannot be null or empty.");
            }

            for(FeatureChange featureChange
                    : appliedChanges) {

                if(featureChange == null
                        || !featureChange.isValidChange()) {

                    throw new IllegalStateException(
                            "ReplayState contains an invalid "
                            + "feature change.");
                }
            }
        }

        private static void validatePercentage(
                double value,
                String fieldName) {

            if(!isFinite(value)
                    || value < 0.0
                    || value > 100.0) {

                throw new IllegalStateException(
                        fieldName
                        + " must be between 0 and 100.");
            }
        }

        private static void validateOptionalPercentage(
                double value,
                String fieldName) {

            if(value == -1.0) {
                return;
            }

            validatePercentage(
                    value,
                    fieldName);
        }

        private static void validatePositiveInteger(
                int value,
                String fieldName) {

            if(value < 1) {

                throw new IllegalStateException(
                        fieldName
                        + " must be at least 1.");
            }
        }

        private static void validateNonNegativeInteger(
                int value,
                String fieldName) {

            if(value < 0) {

                throw new IllegalStateException(
                        fieldName
                        + " cannot be negative.");
            }
        }

        private static void validateOptionalInteger(
                int value,
                String fieldName) {

            if(value < -1) {

                throw new IllegalStateException(
                        fieldName
                        + " must be -1 or non-negative.");
            }
        }

        private static void validateOptionalNonNegativeFinite(
                double value,
                String fieldName) {

            if(value == -1.0) {
                return;
            }

            if(!isFinite(value)
                    || value < 0.0) {

                throw new IllegalStateException(
                        fieldName
                        + " must be -1 or finite and non-negative.");
            }
        }

        private static void validateOptionalPositiveFinite(
                double value,
                String fieldName) {

            if(value == -1.0) {
                return;
            }

            validatePositiveFinite(
                    value,
                    fieldName);
        }

        private static void validateNonNegativeFinite(
                double value,
                String fieldName) {

            if(!isFinite(value)
                    || value < 0.0) {

                throw new IllegalStateException(
                        fieldName
                        + " must be finite and non-negative.");
            }
        }

        private static void validatePositiveFinite(
                double value,
                String fieldName) {

            if(!isFinite(value)
                    || value <= 0.0) {

                throw new IllegalStateException(
                        fieldName
                        + " must be finite and greater than zero.");
            }
        }
    }
}
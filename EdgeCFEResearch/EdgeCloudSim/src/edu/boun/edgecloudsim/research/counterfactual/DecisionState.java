package edu.boun.edgecloudsim.research.counterfactual;

import edu.boun.edgecloudsim.network.DestinationNetworkState;
import edu.boun.edgecloudsim.network.NetworkStateSnapshot;

/**
 * Immutable research representation of one logged task-offloading
 * decision.
 */
public final class DecisionState {

    private final double simulationTimeSeconds;
    private final int taskId;
    private final int mobileDeviceId;
    private final String applicationType;
    private final String orchestratorPolicy;
    private final String decisionOutcome;

    private final String selectedDestination;
    private final String attemptedDestination;
    private final int selectedVmId;
    private final int selectedHostId;
    private final int selectedDatacenterId;

    private final double taskLengthMi;
    private final int requiredCores;
    private final double uploadSizeKb;
    private final double downloadSizeKb;
    private final double requiredCpuPercent;

    private final DestinationNetworkState edgeNetworkState;
    private final DestinationNetworkState cloudNetworkState;

    private final double availableCpuPercent;
    private final double selectedVmMips;
    private final int selectedVmCoreCount;

    private final int candidateVmCount;
    private final int feasibleVmCount;
    private final double selectedAvailableCpuPercent;
    private final double bestAvailableCpuPercent;
    private final String selectionReason;
    private final int roundRobinStartingCandidateIndex;
    private final int randomSampledCandidateIndex;

    /**
     * Preferred constructor with both destination network paths.
     */
    public DecisionState(
            double simulationTimeSeconds,
            int taskId,
            int mobileDeviceId,
            String applicationType,
            String orchestratorPolicy,
            String decisionOutcome,
            String selectedDestination,
            String attemptedDestination,
            int selectedVmId,
            int selectedHostId,
            int selectedDatacenterId,
            double taskLengthMi,
            int requiredCores,
            double uploadSizeKb,
            double downloadSizeKb,
            double requiredCpuPercent,
            DestinationNetworkState edgeNetworkState,
            DestinationNetworkState cloudNetworkState,
            double availableCpuPercent,
            double selectedVmMips,
            int selectedVmCoreCount,
            int candidateVmCount,
            int feasibleVmCount,
            double selectedAvailableCpuPercent,
            double bestAvailableCpuPercent,
            String selectionReason,
            int roundRobinStartingCandidateIndex,
            int randomSampledCandidateIndex) {

        validateNonNegativeFinite(
                simulationTimeSeconds,
                "simulationTimeSeconds");
        validateIdentifier(taskId, "taskId");
        validateNonNegativeFinite(taskLengthMi, "taskLengthMi");
        validateNonNegativeFinite(uploadSizeKb, "uploadSizeKb");
        validateNonNegativeFinite(downloadSizeKb, "downloadSizeKb");
        validatePercentage(requiredCpuPercent, "requiredCpuPercent");
        validateOptionalPercentage(
                availableCpuPercent,
                "availableCpuPercent");
        validateOptionalPercentage(
                selectedAvailableCpuPercent,
                "selectedAvailableCpuPercent");
        validateOptionalPercentage(
                bestAvailableCpuPercent,
                "bestAvailableCpuPercent");
        validateOptionalNonNegativeFinite(
                selectedVmMips,
                "selectedVmMips");
        validateOptionalInteger(
                selectedVmCoreCount,
                "selectedVmCoreCount");
        validateNonNegativeInteger(requiredCores, "requiredCores");
        validateNonNegativeInteger(
                candidateVmCount,
                "candidateVmCount");
        validateNonNegativeInteger(
                feasibleVmCount,
                "feasibleVmCount");
        validateOptionalInteger(
                roundRobinStartingCandidateIndex,
                "roundRobinStartingCandidateIndex");
        validateOptionalInteger(
                randomSampledCandidateIndex,
                "randomSampledCandidateIndex");

        if(edgeNetworkState == null || cloudNetworkState == null) {
            throw new IllegalArgumentException(
                    "EDGE and CLOUD network states are required.");
        }

        if(feasibleVmCount > candidateVmCount) {
            throw new IllegalArgumentException(
                    "feasibleVmCount cannot exceed candidateVmCount.");
        }

        boolean selectedDecision =
                "SELECTED".equalsIgnoreCase(
                        safeText(decisionOutcome));

        if(selectedDecision) {
            validatePercentage(
                    availableCpuPercent,
                    "availableCpuPercent");
            validatePercentage(
                    selectedAvailableCpuPercent,
                    "selectedAvailableCpuPercent");
            validatePercentage(
                    bestAvailableCpuPercent,
                    "bestAvailableCpuPercent");
            validateNonNegativeFinite(
                    selectedVmMips,
                    "selectedVmMips");
            validateNonNegativeInteger(
                    selectedVmCoreCount,
                    "selectedVmCoreCount");

            if(selectedVmId < 0) {
                throw new IllegalArgumentException(
                        "A SELECTED decision requires selectedVmId.");
            }
        }

        this.simulationTimeSeconds = simulationTimeSeconds;
        this.taskId = taskId;
        this.mobileDeviceId = mobileDeviceId;
        this.applicationType = safeText(applicationType);
        this.orchestratorPolicy = safeText(orchestratorPolicy);
        this.decisionOutcome = safeText(decisionOutcome);
        this.selectedDestination = safeText(selectedDestination);
        this.attemptedDestination = safeText(attemptedDestination);
        this.selectedVmId = selectedVmId;
        this.selectedHostId = selectedHostId;
        this.selectedDatacenterId = selectedDatacenterId;
        this.taskLengthMi = taskLengthMi;
        this.requiredCores = requiredCores;
        this.uploadSizeKb = uploadSizeKb;
        this.downloadSizeKb = downloadSizeKb;
        this.requiredCpuPercent = requiredCpuPercent;
        this.edgeNetworkState = edgeNetworkState;
        this.cloudNetworkState = cloudNetworkState;
        this.availableCpuPercent = availableCpuPercent;
        this.selectedVmMips = selectedVmMips;
        this.selectedVmCoreCount = selectedVmCoreCount;
        this.candidateVmCount = candidateVmCount;
        this.feasibleVmCount = feasibleVmCount;
        this.selectedAvailableCpuPercent =
                selectedAvailableCpuPercent;
        this.bestAvailableCpuPercent =
                bestAvailableCpuPercent;
        this.selectionReason = safeText(selectionReason);
        this.roundRobinStartingCandidateIndex =
                roundRobinStartingCandidateIndex;
        this.randomSampledCandidateIndex =
                randomSampledCandidateIndex;
    }

    /**
     * Backward-compatible constructor for one observed network path.
     */
    public DecisionState(
            double simulationTimeSeconds,
            int taskId,
            int mobileDeviceId,
            String applicationType,
            String orchestratorPolicy,
            String decisionOutcome,
            String selectedDestination,
            String attemptedDestination,
            int selectedVmId,
            int selectedHostId,
            int selectedDatacenterId,
            double taskLengthMi,
            int requiredCores,
            double uploadSizeKb,
            double downloadSizeKb,
            double requiredCpuPercent,
            int servingWlanId,
            double availableBandwidthMbps,
            double networkLatencyMs,
            double uploadDelayMs,
            double availableCpuPercent,
            double selectedVmMips,
            int selectedVmCoreCount,
            int candidateVmCount,
            int feasibleVmCount,
            double selectedAvailableCpuPercent,
            double bestAvailableCpuPercent,
            String selectionReason,
            int roundRobinStartingCandidateIndex,
            int randomSampledCandidateIndex) {

        this(
                simulationTimeSeconds,
                taskId,
                mobileDeviceId,
                applicationType,
                orchestratorPolicy,
                decisionOutcome,
                selectedDestination,
                attemptedDestination,
                selectedVmId,
                selectedHostId,
                selectedDatacenterId,
                taskLengthMi,
                requiredCores,
                uploadSizeKb,
                downloadSizeKb,
                requiredCpuPercent,
                createLegacySnapshot(
                        attemptedDestination,
                        servingWlanId,
                        availableBandwidthMbps,
                        networkLatencyMs,
                        uploadDelayMs)
                        .getEdgeNetworkState(),
                createLegacySnapshot(
                        attemptedDestination,
                        servingWlanId,
                        availableBandwidthMbps,
                        networkLatencyMs,
                        uploadDelayMs)
                        .getCloudNetworkState(),
                availableCpuPercent,
                selectedVmMips,
                selectedVmCoreCount,
                candidateVmCount,
                feasibleVmCount,
                selectedAvailableCpuPercent,
                bestAvailableCpuPercent,
                selectionReason,
                roundRobinStartingCandidateIndex,
                randomSampledCandidateIndex);
    }

    private static NetworkStateSnapshot createLegacySnapshot(
            String attemptedDestination,
            int servingWlanId,
            double availableBandwidthMbps,
            double networkLatencyMs,
            double uploadDelayMs) {

        return new NetworkStateSnapshot(
                attemptedDestination,
                servingWlanId,
                -1,
                availableBandwidthMbps,
                availableBandwidthMbps,
                networkLatencyMs < 0.0
                        ? -1.0
                        : networkLatencyMs / 1000.0,
                uploadDelayMs < 0.0
                        ? -1.0
                        : uploadDelayMs / 1000.0,
                availableBandwidthMbps > 0.0
                        && networkLatencyMs >= 0.0
                        && uploadDelayMs >= 0.0,
                "LEGACY_NETWORK_STATE");
    }

    private DestinationNetworkState attemptedNetworkState() {
        return "CLOUD".equalsIgnoreCase(attemptedDestination)
                ? cloudNetworkState
                : edgeNetworkState;
    }

    private static String safeText(String value) {
        return value == null || value.trim().isEmpty()
                ? "UNKNOWN"
                : value.trim();
    }

    private static void validateIdentifier(
            int value,
            String fieldName) {
        if(value < 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be non-negative.");
        }
    }

    private static void validateNonNegativeInteger(
            int value,
            String fieldName) {
        if(value < 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be non-negative.");
        }
    }

    private static void validateOptionalInteger(
            int value,
            String fieldName) {
        if(value < -1) {
            throw new IllegalArgumentException(
                    fieldName + " must be -1 or non-negative.");
        }
    }

    private static void validateNonNegativeFinite(
            double value,
            String fieldName) {
        if(Double.isNaN(value)
                || Double.isInfinite(value)
                || value < 0.0) {
            throw new IllegalArgumentException(
                    fieldName + " must be finite and non-negative.");
        }
    }

    private static void validateOptionalNonNegativeFinite(
            double value,
            String fieldName) {
        if(value == -1.0) {
            return;
        }
        validateNonNegativeFinite(value, fieldName);
    }

    private static void validatePercentage(
            double value,
            String fieldName) {
        if(Double.isNaN(value)
                || Double.isInfinite(value)
                || value < 0.0
                || value > 100.0) {
            throw new IllegalArgumentException(
                    fieldName + " must be between 0 and 100.");
        }
    }

    private static void validateOptionalPercentage(
            double value,
            String fieldName) {
        if(value == -1.0) {
            return;
        }
        validatePercentage(value, fieldName);
    }

    public double getSimulationTimeSeconds() {
        return simulationTimeSeconds;
    }

    public double getSimulationTime() {
        return simulationTimeSeconds;
    }

    public int getTaskId() { return taskId; }
    public int getMobileDeviceId() { return mobileDeviceId; }
    public String getApplicationType() { return applicationType; }
    public String getOrchestratorPolicy() {
        return orchestratorPolicy;
    }
    public String getDecisionOutcome() { return decisionOutcome; }
    public String getSelectedDestination() {
        return selectedDestination;
    }
    public String getAttemptedDestination() {
        return attemptedDestination;
    }
    public int getSelectedVmId() { return selectedVmId; }
    public int getSelectedHostId() { return selectedHostId; }
    public int getSelectedDatacenterId() {
        return selectedDatacenterId;
    }
    public double getTaskLengthMi() { return taskLengthMi; }
    @Deprecated
    public double getTaskLength() { return taskLengthMi; }
    public int getRequiredCores() { return requiredCores; }
    public double getUploadSizeKb() { return uploadSizeKb; }
    @Deprecated
    public double getUploadSize() { return uploadSizeKb; }
    public double getDownloadSizeKb() { return downloadSizeKb; }
    @Deprecated
    public double getDownloadSize() { return downloadSizeKb; }
    public double getRequiredCpuPercent() {
        return requiredCpuPercent;
    }

    public DestinationNetworkState getEdgeNetworkState() {
        return edgeNetworkState;
    }

    public DestinationNetworkState getCloudNetworkState() {
        return cloudNetworkState;
    }

    public DestinationNetworkState getNetworkStateForDestination(
            String destination) {
        return "CLOUD".equalsIgnoreCase(destination)
                ? cloudNetworkState
                : edgeNetworkState;
    }

    public int getServingWlanId() {
        return attemptedNetworkState().getServingWlanId();
    }

    public double getAvailableBandwidthMbps() {
        return attemptedNetworkState().getAvailableBandwidthMbps();
    }

    @Deprecated
    public double getAvailableBandwidth() {
        return getAvailableBandwidthMbps();
    }

    public double getNetworkLatencyMs() {
        return attemptedNetworkState()
                .getUploadDelayMilliseconds();
    }

    @Deprecated
    public double getNetworkLatency() {
        return getNetworkLatencyMs();
    }

    public double getUploadDelayMs() {
        return attemptedNetworkState()
                .getUploadDelayMilliseconds();
    }

    @Deprecated
    public double getUploadDelay() {
        return getUploadDelayMs();
    }

    public double getAvailableCpuPercent() {
        return availableCpuPercent;
    }
    public double getSelectedVmMips() { return selectedVmMips; }
    public int getSelectedVmCoreCount() {
        return selectedVmCoreCount;
    }
    public int getCandidateVmCount() { return candidateVmCount; }
    public int getFeasibleVmCount() { return feasibleVmCount; }
    public double getSelectedAvailableCpuPercent() {
        return selectedAvailableCpuPercent;
    }
    public double getBestAvailableCpuPercent() {
        return bestAvailableCpuPercent;
    }
    public String getSelectionReason() { return selectionReason; }
    public int getRoundRobinStartingCandidateIndex() {
        return roundRobinStartingCandidateIndex;
    }
    public int getRandomSampledCandidateIndex() {
        return randomSampledCandidateIndex;
    }

    public boolean isSelectedDecision() {
        return "SELECTED".equalsIgnoreCase(decisionOutcome)
                && selectedVmId >= 0;
    }

    public boolean isCpuFeasible() {
        return isSelectedDecision()
                && requiredCpuPercent
                <= selectedAvailableCpuPercent;
    }

    public boolean hasAvailableBandwidth() {
        return getAvailableBandwidthMbps() > 0.0;
    }

    public boolean hasFeasibleCandidates() {
        return feasibleVmCount > 0;
    }

    public boolean selectedVmHasBestAvailableCpu() {
        return isSelectedDecision()
                && selectedAvailableCpuPercent >= 0.0
                && bestAvailableCpuPercent >= 0.0
                && Math.abs(
                        selectedAvailableCpuPercent
                        - bestAvailableCpuPercent)
                        <= 0.0001;
    }

    public boolean isEdgeDecision() {
        return "EDGE".equalsIgnoreCase(selectedDestination);
    }

    public boolean isCloudDecision() {
        return "CLOUD".equalsIgnoreCase(selectedDestination);
    }

    @Override
    public String toString() {
        return "DecisionState{"
                + "taskId="
                + taskId
                + ", policy='"
                + orchestratorPolicy
                + '\''
                + ", outcome='"
                + decisionOutcome
                + '\''
                + ", destination='"
                + selectedDestination
                + '\''
                + ", attemptedDestination='"
                + attemptedDestination
                + '\''
                + ", edgeBandwidthMbps="
                + edgeNetworkState.getAvailableBandwidthMbps()
                + ", cloudBandwidthMbps="
                + cloudNetworkState.getAvailableBandwidthMbps()
                + '}';
    }
}
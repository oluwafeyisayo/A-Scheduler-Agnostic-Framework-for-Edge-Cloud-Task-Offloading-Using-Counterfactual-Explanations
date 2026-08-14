package edu.boun.edgecloudsim.research.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.boun.edgecloudsim.network.DestinationNetworkState;
import edu.boun.edgecloudsim.network.NetworkStateSnapshot;

/**
 * Immutable provenance record for one task-offloading decision.
 */
public final class DecisionRecord {

    private final double simulationTime;
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
    private final int selectedCandidateIndex;

    private final double taskLengthMi;
    private final int requiredCores;
    private final double uploadSizeKb;
    private final double downloadSizeKb;
    private final double requiredCpuPercent;

    private final DestinationNetworkState edgeNetworkState;
    private final DestinationNetworkState cloudNetworkState;

    private final double selectedVmMips;
    private final int selectedVmCoreCount;
    private final double selectedAvailableCpuPercent;

    private final int candidateVmCount;
    private final int feasibleVmCount;
    private final double bestAvailableCpuPercent;
    private final String selectionReason;

    private final int roundRobinStartingCandidateIndex;
    private final int randomSampledCandidateIndex;

    private final List<CandidateVmRecord> candidateVmRecords;

    /**
     * Preferred constructor. Stores both EDGE and CLOUD network paths.
     */
    public DecisionRecord(
            double simulationTime,
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
            int selectedCandidateIndex,
            double taskLengthMi,
            int requiredCores,
            double uploadSizeKb,
            double downloadSizeKb,
            NetworkStateSnapshot networkStateSnapshot,
            double requiredCpuPercent,
            double selectedVmMips,
            int selectedVmCoreCount,
            int candidateVmCount,
            int feasibleVmCount,
            double selectedAvailableCpuPercent,
            double bestAvailableCpuPercent,
            String selectionReason,
            int roundRobinStartingCandidateIndex,
            int randomSampledCandidateIndex,
            List<CandidateVmRecord> candidateVmRecords) {

        if(networkStateSnapshot == null) {
            throw new IllegalArgumentException(
                    "NetworkStateSnapshot cannot be null.");
        }

        this.simulationTime =
                validateNonnegativeFinite(simulationTime, "simulationTime");
        this.taskId =
                validateNonnegativeInteger(taskId, "taskId");
        this.mobileDeviceId =
                validateNonnegativeInteger(mobileDeviceId, "mobileDeviceId");
        this.applicationType =
                safeText(applicationType);
        this.orchestratorPolicy =
                safeText(orchestratorPolicy);
        this.decisionOutcome =
                safeText(decisionOutcome);

        this.selectedDestination =
                normalizeDestination(selectedDestination);
        this.attemptedDestination =
                normalizeDestination(attemptedDestination);
        this.selectedVmId =
                validateIntegerOrSentinel(selectedVmId, "selectedVmId");
        this.selectedHostId =
                validateIntegerOrSentinel(selectedHostId, "selectedHostId");
        this.selectedDatacenterId =
                validateIntegerOrSentinel(
                        selectedDatacenterId,
                        "selectedDatacenterId");
        this.selectedCandidateIndex =
                validateIntegerOrSentinel(
                        selectedCandidateIndex,
                        "selectedCandidateIndex");

        this.taskLengthMi =
                validateNonnegativeFinite(taskLengthMi, "taskLengthMi");
        this.requiredCores =
                validatePositiveInteger(requiredCores, "requiredCores");
        this.uploadSizeKb =
                validateNonnegativeFinite(uploadSizeKb, "uploadSizeKb");
        this.downloadSizeKb =
                validateNonnegativeFinite(downloadSizeKb, "downloadSizeKb");
        this.requiredCpuPercent =
                validatePercentage(requiredCpuPercent, "requiredCpuPercent");

        this.edgeNetworkState =
                networkStateSnapshot.getEdgeNetworkState();
        this.cloudNetworkState =
                networkStateSnapshot.getCloudNetworkState();

        this.selectedVmMips =
                validateNonnegativeOrSentinel(
                        selectedVmMips,
                        "selectedVmMips");
        this.selectedVmCoreCount =
                validateIntegerOrSentinel(
                        selectedVmCoreCount,
                        "selectedVmCoreCount");
        this.selectedAvailableCpuPercent =
                validatePercentageOrSentinel(
                        selectedAvailableCpuPercent,
                        "selectedAvailableCpuPercent");

        this.candidateVmCount =
                validateNonnegativeInteger(
                        candidateVmCount,
                        "candidateVmCount");
        this.feasibleVmCount =
                validateNonnegativeInteger(
                        feasibleVmCount,
                        "feasibleVmCount");

        if(this.feasibleVmCount > this.candidateVmCount) {
            throw new IllegalArgumentException(
                    "feasibleVmCount cannot exceed candidateVmCount.");
        }

        this.bestAvailableCpuPercent =
                validatePercentageOrSentinel(
                        bestAvailableCpuPercent,
                        "bestAvailableCpuPercent");
        this.selectionReason =
                safeText(selectionReason);

        this.roundRobinStartingCandidateIndex =
                validateIntegerOrSentinel(
                        roundRobinStartingCandidateIndex,
                        "roundRobinStartingCandidateIndex");
        this.randomSampledCandidateIndex =
                validateIntegerOrSentinel(
                        randomSampledCandidateIndex,
                        "randomSampledCandidateIndex");

        this.candidateVmRecords =
                createImmutableCandidateList(candidateVmRecords);

        validateCandidateIndexes();
        validateCandidateCounts();
        validatePolicyState();
        validateDecisionSelectionConsistency();
        validateSelectedCandidate();
    }

    /**
     * Backward-compatible constructor for callers that still provide one
     * observed network path. The unobserved tier is marked unavailable.
     */
    public DecisionRecord(
            double simulationTime,
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
            int selectedCandidateIndex,
            double taskLengthMi,
            int requiredCores,
            double uploadSizeKb,
            double downloadSizeKb,
            int servingWlanId,
            double configuredBandwidthMbps,
            double availableBandwidthMbps,
            double networkLatencyMs,
            double propagationDelayMs,
            double uploadDelayMs,
            double networkUploadDelayMs,
            int connectedDeviceCount,
            boolean networkFeasible,
            String networkInfeasibilityReason,
            double requiredCpuPercent,
            double selectedVmMips,
            int selectedVmCoreCount,
            int candidateVmCount,
            int feasibleVmCount,
            double selectedAvailableCpuPercent,
            double bestAvailableCpuPercent,
            String selectionReason,
            int roundRobinStartingCandidateIndex,
            int randomSampledCandidateIndex,
            List<CandidateVmRecord> candidateVmRecords) {

        this(
                simulationTime,
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
                selectedCandidateIndex,
                taskLengthMi,
                requiredCores,
                uploadSizeKb,
                downloadSizeKb,
                new NetworkStateSnapshot(
                        attemptedDestination,
                        servingWlanId,
                        connectedDeviceCount,
                        configuredBandwidthMbps,
                        availableBandwidthMbps,
                        propagationDelayMs < 0.0
                                ? -1.0
                                : propagationDelayMs / 1000.0,
                        networkUploadDelayMs < 0.0
                                ? -1.0
                                : networkUploadDelayMs / 1000.0,
                        networkFeasible,
                        networkInfeasibilityReason),
                requiredCpuPercent,
                selectedVmMips,
                selectedVmCoreCount,
                candidateVmCount,
                feasibleVmCount,
                selectedAvailableCpuPercent,
                bestAvailableCpuPercent,
                selectionReason,
                roundRobinStartingCandidateIndex,
                randomSampledCandidateIndex,
                candidateVmRecords);
    }

    private static List<CandidateVmRecord> createImmutableCandidateList(
            List<CandidateVmRecord> candidates) {

        if(candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<CandidateVmRecord> copy =
                new ArrayList<CandidateVmRecord>(candidates.size());

        for(CandidateVmRecord candidate : candidates) {
            if(candidate == null) {
                throw new IllegalArgumentException(
                        "candidateVmRecords cannot contain null.");
            }
            copy.add(candidate);
        }

        return Collections.unmodifiableList(copy);
    }

    private void validateCandidateIndexes() {
        Set<Integer> observed = new HashSet<Integer>();

        for(CandidateVmRecord candidate : candidateVmRecords) {
            if(!observed.add(
                    Integer.valueOf(candidate.getCandidateIndex()))) {
                throw new IllegalArgumentException(
                        "Duplicate candidateIndex "
                        + candidate.getCandidateIndex()
                        + " for task "
                        + taskId
                        + ".");
            }
        }
    }

    private void validateCandidateCounts() {
        if(candidateVmRecords.isEmpty()) {
            if(candidateVmCount != 0 || feasibleVmCount != 0) {
                throw new IllegalArgumentException(
                        "An empty candidate list requires zero counts.");
            }
            return;
        }

        if(candidateVmRecords.size() != candidateVmCount) {
            throw new IllegalArgumentException(
                    "candidateVmCount does not match candidate records.");
        }

        int actualFeasibleCount = 0;
        for(CandidateVmRecord candidate : candidateVmRecords) {
            if(candidate.isFeasible()) {
                actualFeasibleCount++;
            }
        }

        if(actualFeasibleCount != feasibleVmCount) {
            throw new IllegalArgumentException(
                    "feasibleVmCount does not match candidate records.");
        }
    }

    private void validatePolicyState() {
        if(candidateVmCount == 0) {
            if(roundRobinStartingCandidateIndex != -1
                    || randomSampledCandidateIndex != -1) {
                throw new IllegalArgumentException(
                        "Policy state must be -1 when no candidates "
                        + "were evaluated.");
            }
            return;
        }

        String reason = normalizeText(selectionReason);
        boolean roundRobinExecuted = reason.contains("ROUND_ROBIN");
        boolean randomExecuted = reason.contains("RANDOM");

        if(roundRobinExecuted && roundRobinStartingCandidateIndex < 0) {
            throw new IllegalArgumentException(
                    "ROUND_ROBIN execution requires its starting index.");
        }

        if(randomExecuted && randomSampledCandidateIndex < 0) {
            throw new IllegalArgumentException(
                    "RANDOM execution requires its sampled index.");
        }

        if(!roundRobinExecuted && roundRobinStartingCandidateIndex != -1) {
            throw new IllegalArgumentException(
                    "ROUND_ROBIN index must be -1 when its branch "
                    + "did not execute.");
        }

        if(!randomExecuted && randomSampledCandidateIndex != -1) {
            throw new IllegalArgumentException(
                    "RANDOM index must be -1 when its branch "
                    + "did not execute.");
        }

        if(roundRobinStartingCandidateIndex >= 0
                && findCandidateByIndex(
                        roundRobinStartingCandidateIndex) == null) {
            throw new IllegalArgumentException(
                    "ROUND_ROBIN starting candidate is absent.");
        }

        if(randomSampledCandidateIndex >= 0
                && findCandidateByIndex(
                        randomSampledCandidateIndex) == null) {
            throw new IllegalArgumentException(
                    "RANDOM sampled candidate is absent.");
        }
    }

    private void validateDecisionSelectionConsistency() {
        int populated = 0;

        if(selectedCandidateIndex >= 0) populated++;
        if(selectedVmId >= 0) populated++;
        if(selectedHostId >= 0) populated++;
        if(selectedDatacenterId >= 0) populated++;
        if(!isAbsentDestination(selectedDestination)) populated++;

        boolean noSelection = populated == 0;
        boolean completeSelection = populated == 5;

        if(!noSelection && !completeSelection) {
            throw new IllegalArgumentException(
                    "Selected-resource fields are incomplete.");
        }

        if("SELECTED".equalsIgnoreCase(decisionOutcome)
                && !completeSelection) {
            throw new IllegalArgumentException(
                    "A SELECTED decision requires a complete resource.");
        }
    }

    private void validateSelectedCandidate() {
        if(selectedCandidateIndex < 0) {
            return;
        }

        CandidateVmRecord candidate =
                findCandidateByIndex(selectedCandidateIndex);

        if(candidate == null
                || candidate.getVmId() != selectedVmId
                || candidate.getHostId() != selectedHostId
                || candidate.getDatacenterId()
                        != selectedDatacenterId
                || !normalizeDestination(candidate.getDestination())
                        .equals(selectedDestination)) {
            throw new IllegalArgumentException(
                    "Selected candidate does not match the decision.");
        }
    }

    private CandidateVmRecord findCandidateByIndex(int candidateIndex) {
        for(CandidateVmRecord candidate : candidateVmRecords) {
            if(candidate.getCandidateIndex() == candidateIndex) {
                return candidate;
            }
        }
        return null;
    }

    private DestinationNetworkState observedNetworkState() {
        if("CLOUD".equals(attemptedDestination)) {
            return cloudNetworkState;
        }
        return edgeNetworkState;
    }

    private static boolean isAbsentDestination(String destination) {
        String value = normalizeDestination(destination);
        return "NONE".equals(value)
                || "UNKNOWN".equals(value)
                || "UNSELECTED".equals(value)
                || "UNDETERMINED".equals(value)
                || "NO_DESTINATION".equals(value);
    }

    private static String normalizeDestination(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? "NONE" : normalized;
    }

    private static String normalizeText(String value) {
        return value == null
                ? ""
                : value.trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');
    }

    private static String safeText(String value) {
        return value == null || value.trim().isEmpty()
                ? "UNKNOWN"
                : value.trim();
    }

    private static double validateNonnegativeFinite(
            double value,
            String fieldName) {
        validateFinite(value, fieldName);
        if(value < 0.0) {
            throw new IllegalArgumentException(
                    fieldName + " must be nonnegative.");
        }
        return value;
    }

    private static double validateNonnegativeOrSentinel(
            double value,
            String fieldName) {
        validateFinite(value, fieldName);
        if(value < 0.0 && value != -1.0) {
            throw new IllegalArgumentException(
                    fieldName + " must be -1 or nonnegative.");
        }
        return value;
    }

    private static double validatePercentage(
            double value,
            String fieldName) {
        validateFinite(value, fieldName);
        if(value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException(
                    fieldName + " must be between 0 and 100.");
        }
        return value;
    }

    private static double validatePercentageOrSentinel(
            double value,
            String fieldName) {
        return value == -1.0
                ? value
                : validatePercentage(value, fieldName);
    }

    private static void validateFinite(
            double value,
            String fieldName) {
        if(Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(
                    fieldName + " must be finite.");
        }
    }

    private static int validatePositiveInteger(
            int value,
            String fieldName) {
        if(value < 1) {
            throw new IllegalArgumentException(
                    fieldName + " must be at least 1.");
        }
        return value;
    }

    private static int validateNonnegativeInteger(
            int value,
            String fieldName) {
        if(value < 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be nonnegative.");
        }
        return value;
    }

    private static int validateIntegerOrSentinel(
            int value,
            String fieldName) {
        if(value < -1) {
            throw new IllegalArgumentException(
                    fieldName + " must be -1 or nonnegative.");
        }
        return value;
    }

    public double getSimulationTime() { return simulationTime; }
    public int getTaskId() { return taskId; }
    public int getMobileDeviceId() { return mobileDeviceId; }
    public String getApplicationType() { return applicationType; }
    public String getOrchestratorPolicy() { return orchestratorPolicy; }
    public String getDecisionOutcome() { return decisionOutcome; }
    public String getSelectedDestination() { return selectedDestination; }
    public String getAttemptedDestination() { return attemptedDestination; }
    public int getSelectedVmId() { return selectedVmId; }
    public int getSelectedHostId() { return selectedHostId; }
    public int getSelectedDatacenterId() {
        return selectedDatacenterId;
    }
    public int getSelectedCandidateIndex() {
        return selectedCandidateIndex;
    }
    public double getTaskLengthMi() { return taskLengthMi; }
    public int getRequiredCores() { return requiredCores; }
    public double getUploadSizeKb() { return uploadSizeKb; }
    public double getDownloadSizeKb() { return downloadSizeKb; }
    public double getRequiredCpuPercent() { return requiredCpuPercent; }

    public DestinationNetworkState getEdgeNetworkState() {
        return edgeNetworkState;
    }

    public DestinationNetworkState getCloudNetworkState() {
        return cloudNetworkState;
    }

    public int getServingWlanId() {
        return observedNetworkState().getServingWlanId();
    }

    public double getConfiguredBandwidthMbps() {
        return observedNetworkState().getConfiguredBandwidthMbps();
    }

    public double getAvailableBandwidthMbps() {
        return observedNetworkState().getAvailableBandwidthMbps();
    }

    public double getNetworkLatencyMs() {
        return observedNetworkState().getUploadDelayMilliseconds();
    }

    public double getPropagationDelayMs() {
        return observedNetworkState().getPropagationDelayMilliseconds();
    }

    public double getNetworkUploadDelayMs() {
        return observedNetworkState().getUploadDelayMilliseconds();
    }

    public int getConnectedDeviceCount() {
        return observedNetworkState().getConnectedDeviceCount();
    }

    public boolean isNetworkFeasible() {
        return observedNetworkState().isNetworkFeasible();
    }

    public String getNetworkInfeasibilityReason() {
        return observedNetworkState().getInfeasibilityReason();
    }

    public double getSelectedVmMips() { return selectedVmMips; }
    public int getSelectedVmCoreCount() {
        return selectedVmCoreCount;
    }
    public double getSelectedAvailableCpuPercent() {
        return selectedAvailableCpuPercent;
    }
    public int getCandidateVmCount() { return candidateVmCount; }
    public int getFeasibleVmCount() { return feasibleVmCount; }
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
    public List<CandidateVmRecord> getCandidateVmRecords() {
        return candidateVmRecords;
    }
}
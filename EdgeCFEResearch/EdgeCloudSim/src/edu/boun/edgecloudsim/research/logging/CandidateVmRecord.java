package edu.boun.edgecloudsim.research.logging;

/**
 * Immutable snapshot of one VM considered by the scheduler for a
 * task-offloading decision.
 *
 * Engineering units:
 *
 * - VM processing capacity: MIPS
 * - available CPU: percentage
 *
 * A candidate can remain in the recorded candidate set even when it is
 * infeasible. This is required to reproduce the complete scheduler
 * decision context and diagnose why alternative targets were rejected.
 */
public final class CandidateVmRecord {

    private final int candidateIndex;
    private final int vmId;
    private final int hostId;
    private final int datacenterId;
    private final String destination;

    private final double vmMips;
    private final int vmCoreCount;
    private final double availableCpuPercent;

    private final boolean feasible;
    private final String infeasibilityReason;

    public CandidateVmRecord(
            int candidateIndex,
            int vmId,
            int hostId,
            int datacenterId,
            String destination,
            double vmMips,
            int vmCoreCount,
            double availableCpuPercent,
            boolean feasible,
            String infeasibilityReason) {

        this.candidateIndex =
                validateNonnegativeInteger(
                        candidateIndex,
                        "candidateIndex");

        this.vmId =
                validateNonnegativeInteger(
                        vmId,
                        "vmId");

        this.hostId =
                validateNonnegativeInteger(
                        hostId,
                        "hostId");

        this.datacenterId =
                validateNonnegativeInteger(
                        datacenterId,
                        "datacenterId");

        this.destination =
                safeText(destination);

        this.vmMips =
                validateNonnegativeFinite(
                        vmMips,
                        "vmMips");

        this.vmCoreCount =
                validateNonnegativeInteger(
                        vmCoreCount,
                        "vmCoreCount");

        this.availableCpuPercent =
                validatePercentage(
                        availableCpuPercent,
                        "availableCpuPercent");

        this.feasible =
                feasible;

        this.infeasibilityReason =
                normalizeFeasibilityReason(
                        feasible,
                        infeasibilityReason);
    }

    private static String normalizeFeasibilityReason(
            boolean feasible,
            String infeasibilityReason) {

        if (feasible) {
            return "NONE";
        }

        return safeText(infeasibilityReason);
    }

    private static String safeText(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "UNKNOWN";
        }

        return value.trim();
    }

    private static int validateNonnegativeInteger(
            int value,
            String fieldName) {

        if (value < 0) {

            throw new IllegalArgumentException(
                    fieldName
                    + " must be nonnegative.");
        }

        return value;
    }

    private static double validateNonnegativeFinite(
            double value,
            String fieldName) {

        validateFinite(value, fieldName);

        if (value < 0.0) {

            throw new IllegalArgumentException(
                    fieldName
                    + " must be nonnegative.");
        }

        return value;
    }

    private static double validatePercentage(
            double value,
            String fieldName) {

        validateFinite(value, fieldName);

        if (value < 0.0 || value > 100.0) {

            throw new IllegalArgumentException(
                    fieldName
                    + " must be between 0 and 100.");
        }

        return value;
    }

    private static void validateFinite(
            double value,
            String fieldName) {

        if (Double.isNaN(value)
                || Double.isInfinite(value)) {

            throw new IllegalArgumentException(
                    fieldName
                    + " must be finite.");
        }
    }

    public int getCandidateIndex() {
        return candidateIndex;
    }

    public int getVmId() {
        return vmId;
    }

    public int getHostId() {
        return hostId;
    }

    public int getDatacenterId() {
        return datacenterId;
    }

    public String getDestination() {
        return destination;
    }

    public double getVmMips() {
        return vmMips;
    }

    public int getVmCoreCount() {
        return vmCoreCount;
    }

    public double getAvailableCpuPercent() {
        return availableCpuPercent;
    }

    public boolean isFeasible() {
        return feasible;
    }

    public String getInfeasibilityReason() {
        return infeasibilityReason;
    }
}
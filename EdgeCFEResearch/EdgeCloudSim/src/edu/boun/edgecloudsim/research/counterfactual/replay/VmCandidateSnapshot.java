package edu.boun.edgecloudsim.research.counterfactual.replay;

/**
 * Scheduler-independent snapshot of one candidate VM.
 *
 * The snapshot captures the state visible to the scheduler at the
 * moment an offloading decision was made.
 */
public final class VmCandidateSnapshot {

    private static final double EPSILON = 0.000001;

    private final int candidateIndex;
    private final int vmId;
    private final int hostId;
    private final int datacenterId;
    private final String destination;
    private final double requiredCpuPercent;
    private final double availableCpuPercent;
    private final int requiredCoreCount;
    private final double vmMips;
    private final int vmCoreCount;
    private final boolean cpuFeasible;
    private final boolean operationallyAvailable;
    private final int evaluationOrder;

    public VmCandidateSnapshot(
            int candidateIndex,
            int vmId,
            int hostId,
            int datacenterId,
            String destination,
            double requiredCpuPercent,
            double availableCpuPercent,
            int requiredCoreCount,
            double vmMips,
            int vmCoreCount,
            boolean cpuFeasible,
            boolean operationallyAvailable,
            int evaluationOrder) {

        if(candidateIndex < 0) {
            throw new IllegalArgumentException(
                    "Candidate index cannot be negative.");
        }

        if(vmId < 0) {
            throw new IllegalArgumentException(
                    "VM ID cannot be negative.");
        }

        if(Double.isNaN(requiredCpuPercent)
                || Double.isInfinite(requiredCpuPercent)
                || requiredCpuPercent < 0.0
                || requiredCpuPercent > 100.0) {
            throw new IllegalArgumentException(
                    "Required CPU must be between 0 and 100.");
        }

        if(Double.isNaN(availableCpuPercent)
                || Double.isInfinite(availableCpuPercent)
                || availableCpuPercent < 0.0
                || availableCpuPercent > 100.0) {
            throw new IllegalArgumentException(
                    "Available CPU must be between 0 and 100.");
        }

        if(requiredCoreCount < 0) {
            throw new IllegalArgumentException(
                    "Required core count cannot be negative.");
        }

        if(vmMips < 0.0
                || Double.isNaN(vmMips)
                || Double.isInfinite(vmMips)) {
            throw new IllegalArgumentException(
                    "VM MIPS must be finite and non-negative.");
        }

        if(vmCoreCount < 0) {
            throw new IllegalArgumentException(
                    "VM core count cannot be negative.");
        }

        this.candidateIndex = candidateIndex;
        this.vmId = vmId;
        this.hostId = hostId;
        this.datacenterId = datacenterId;
        this.destination = destination == null
                || destination.trim().isEmpty()
                ? "UNKNOWN"
                : destination.trim();
        this.requiredCpuPercent = requiredCpuPercent;
        this.availableCpuPercent = availableCpuPercent;
        this.requiredCoreCount = requiredCoreCount;
        this.vmMips = vmMips;
        this.vmCoreCount = vmCoreCount;

        /*
         * Recalculate CPU feasibility from the actual values. The supplied
         * boolean is retained for constructor compatibility but cannot
         * override the reconstructed state values.
         */
        this.cpuFeasible = cpuFeasible
                && availableCpuPercent + EPSILON >= requiredCpuPercent;

        this.operationallyAvailable = operationallyAvailable;
        this.evaluationOrder = evaluationOrder;
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

    public double getRequiredCpuPercent() {
        return requiredCpuPercent;
    }

    public double getAvailableCpuPercent() {
        return availableCpuPercent;
    }

    public int getRequiredCoreCount() {
        return requiredCoreCount;
    }

    public double getVmMips() {
        return vmMips;
    }

    public int getVmCoreCount() {
        return vmCoreCount;
    }

    public boolean isCpuFeasible() {
        return cpuFeasible
                && availableCpuPercent + EPSILON >= requiredCpuPercent;
    }

    public boolean isOperationallyAvailable() {
        return operationallyAvailable;
    }

    public int getEvaluationOrder() {
        return evaluationOrder;
    }

    /**
     * Returns true when the candidate satisfies compute feasibility and
     * the externally reconstructed task-level network path is valid.
     */
    public boolean isComputeNetworkFeasible(
            boolean networkPathFeasible) {

        return isComputeFeasible()
                && networkPathFeasible;
    }

    /**
     * Returns true when the VM provides at least the number of cores
     * required by the task.
     */
    public boolean isCoreFeasible() {
        return vmCoreCount >= requiredCoreCount;
    }

    /**
     * Returns true when the VM has positive processing capacity.
     */
    public boolean hasValidProcessingCapacity() {
        return vmMips > 0.0
                && !Double.isNaN(vmMips)
                && !Double.isInfinite(vmMips);
    }

    /**
     * Returns true when the candidate satisfies all currently modeled
     * compute constraints.
     */
    public boolean isComputeFeasible() {
        return operationallyAvailable
                && isCpuFeasible()
                && isCoreFeasible()
                && hasValidProcessingCapacity();
    }

    /**
     * Generic feasibility check used by replay strategies.
     */
    public boolean isFeasible() {
        return isComputeFeasible();
    }

    public double getCpuFeasibilityMargin() {
        return availableCpuPercent - requiredCpuPercent;
    }

    public int getCoreFeasibilityMargin() {
        return vmCoreCount - requiredCoreCount;
    }

    /**
     * Estimates task processing time in milliseconds.
     */
    public double estimateProcessingTimeMs(
            double taskLengthMi) {

        if(Double.isNaN(taskLengthMi)
                || Double.isInfinite(taskLengthMi)
                || taskLengthMi < 0.0) {
            throw new IllegalArgumentException(
                    "Task length must be finite and non-negative.");
        }

        if(!hasValidProcessingCapacity()) {
            return Double.POSITIVE_INFINITY;
        }

        return taskLengthMi / vmMips * 1000.0;
    }

    /**
     * Creates a copy with an updated available CPU percentage.
     */
    public VmCandidateSnapshot withAvailableCpuPercent(
            double newAvailableCpuPercent) {

        boolean newCpuFeasible = newAvailableCpuPercent
                + EPSILON >= requiredCpuPercent;

        return new VmCandidateSnapshot(
                candidateIndex,
                vmId,
                hostId,
                datacenterId,
                destination,
                requiredCpuPercent,
                newAvailableCpuPercent,
                requiredCoreCount,
                vmMips,
                vmCoreCount,
                newCpuFeasible,
                operationallyAvailable,
                evaluationOrder);
    }
    
    /**
     * Creates a copy with an updated task CPU requirement.
     *
     * CPU feasibility is recalculated using the candidate's current
     * available CPU capacity.
     */
    public VmCandidateSnapshot withRequiredCpuPercent(
            double newRequiredCpuPercent) {

        if(Double.isNaN(newRequiredCpuPercent)
                || Double.isInfinite(newRequiredCpuPercent)
                || newRequiredCpuPercent < 0.0
                || newRequiredCpuPercent > 100.0) {

            throw new IllegalArgumentException(
                    "Required CPU must be between 0 and 100.");
        }

        boolean newCpuFeasible =
                availableCpuPercent
                + EPSILON
                >= newRequiredCpuPercent;

        return new VmCandidateSnapshot(
                candidateIndex,
                vmId,
                hostId,
                datacenterId,
                destination,
                newRequiredCpuPercent,
                availableCpuPercent,
                requiredCoreCount,
                vmMips,
                vmCoreCount,
                newCpuFeasible,
                operationallyAvailable,
                evaluationOrder);
    }
    
    /**
     * Creates a copy with an updated task core requirement.
     */
    public VmCandidateSnapshot withRequiredCoreCount(
            int newRequiredCoreCount) {

        if(newRequiredCoreCount < 1) {

            throw new IllegalArgumentException(
                    "Required core count must be at least 1.");
        }

        boolean recalculatedCpuFeasible =
                availableCpuPercent
                + EPSILON
                >= requiredCpuPercent;

        return new VmCandidateSnapshot(
                candidateIndex,
                vmId,
                hostId,
                datacenterId,
                destination,
                requiredCpuPercent,
                availableCpuPercent,
                newRequiredCoreCount,
                vmMips,
                vmCoreCount,
                recalculatedCpuFeasible,
                operationallyAvailable,
                evaluationOrder);
    }

    @Override
    public String toString() {
        return "VmCandidateSnapshot{"
                + "candidateIndex=" + candidateIndex
                + ", vmId=" + vmId
                + ", hostId=" + hostId
                + ", datacenterId=" + datacenterId
                + ", destination='" + destination + '\''
                + ", requiredCpuPercent=" + requiredCpuPercent
                + ", availableCpuPercent=" + availableCpuPercent
                + ", requiredCoreCount=" + requiredCoreCount
                + ", vmMips=" + vmMips
                + ", vmCoreCount=" + vmCoreCount
                + ", cpuFeasible=" + isCpuFeasible()
                + ", coreFeasible=" + isCoreFeasible()
                + ", processingCapacityValid="
                + hasValidProcessingCapacity()
                + ", computeFeasible=" + isComputeFeasible()
                + ", operationallyAvailable="
                + operationallyAvailable
                + ", evaluationOrder=" + evaluationOrder
                + '}';
    }
}
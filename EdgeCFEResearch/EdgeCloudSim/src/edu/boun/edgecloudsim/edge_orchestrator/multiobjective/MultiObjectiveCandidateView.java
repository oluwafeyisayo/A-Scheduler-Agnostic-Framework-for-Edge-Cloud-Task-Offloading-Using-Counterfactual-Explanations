package edu.boun.edgecloudsim.edge_orchestrator.multiobjective;

/**
 * Scheduler-independent input consumed by the shared multi-objective
 * scoring routine.
 *
 * Both the live scheduler and deterministic replay convert their own
 * candidate representations into this immutable view.
 */
public final class MultiObjectiveCandidateView {

    private final int candidateIndex;
    private final int evaluationOrder;

    private final double vmMips;
    private final double availableCpuPercent;

    private final boolean feasible;

    public MultiObjectiveCandidateView(
            int candidateIndex,
            int evaluationOrder,
            double vmMips,
            double availableCpuPercent,
            boolean feasible) {

        if(candidateIndex < 0) {

            throw new IllegalArgumentException(
                    "candidateIndex cannot be negative.");
        }

        if(evaluationOrder < 0) {

            throw new IllegalArgumentException(
                    "evaluationOrder cannot be negative.");
        }

        if(Double.isNaN(vmMips)
                || Double.isInfinite(vmMips)
                || vmMips < 0.0) {

            throw new IllegalArgumentException(
                    "vmMips must be finite and nonnegative.");
        }

        if(Double.isNaN(availableCpuPercent)
                || Double.isInfinite(availableCpuPercent)
                || availableCpuPercent < 0.0
                || availableCpuPercent > 100.0) {

            throw new IllegalArgumentException(
                    "availableCpuPercent must be between 0 and 100.");
        }

        this.candidateIndex =
                candidateIndex;

        this.evaluationOrder =
                evaluationOrder;

        this.vmMips =
                vmMips;

        this.availableCpuPercent =
                availableCpuPercent;

        this.feasible =
                feasible;
    }

    public int getCandidateIndex() {
        return candidateIndex;
    }

    public int getEvaluationOrder() {
        return evaluationOrder;
    }

    public double getVmMips() {
        return vmMips;
    }

    public double getAvailableCpuPercent() {
        return availableCpuPercent;
    }

    public boolean isFeasible() {
        return feasible;
    }
}
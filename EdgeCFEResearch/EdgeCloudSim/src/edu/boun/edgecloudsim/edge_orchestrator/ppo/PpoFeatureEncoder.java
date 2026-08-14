package edu.boun.edgecloudsim.edge_orchestrator.ppo;

import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.logging
        .CandidateVmRecord;

/**
 * Shared feature encoder for live scheduling and deterministic replay.
 *
 * Feature order:
 * 0 task length
 * 1 upload size
 * 2 required CPU
 * 3 available CPU
 * 4 VM MIPS
 * 5 VM cores
 * 6 destination upload delay
 * 7 destination bandwidth
 * 8 EDGE indicator
 * 9 CLOUD indicator
 */
public final class PpoFeatureEncoder {

    private static final double MAX_TASK_LENGTH_MI =
            10000.0;

    private static final double MAX_UPLOAD_SIZE_KB =
            10000.0;

    private static final double MAX_VM_MIPS =
            20000.0;

    private static final double MAX_VM_CORES =
            16.0;

    private static final double MAX_DELAY_MS =
            2000.0;

    private static final double MAX_BANDWIDTH_MBPS =
            1000.0;

    public PpoFeatureVector encodeLive(
            double taskLengthMi,
            double uploadSizeKb,
            double requiredCpuPercent,
            double uploadDelayMs,
            double availableBandwidthMbps,
            CandidateVmRecord candidate) {

        if(candidate == null) {

            throw new IllegalArgumentException(
                    "CandidateVmRecord cannot be null.");
        }

        return encode(
                taskLengthMi,
                uploadSizeKb,
                requiredCpuPercent,
                candidate.getAvailableCpuPercent(),
                candidate.getVmMips(),
                candidate.getVmCoreCount(),
                uploadDelayMs,
                availableBandwidthMbps,
                candidate.getDestination());
    }

    public PpoFeatureVector encodeReplay(
            ReplayState replayState,
            VmCandidateSnapshot candidate) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        if(candidate == null) {

            throw new IllegalArgumentException(
                    "VmCandidateSnapshot cannot be null.");
        }

        String destination =
                candidate.getDestination();

        return encode(
                replayState.getTaskLengthMi(),
                replayState.getUploadSizeKb(),
                replayState.getRequiredCpuPercent(),
                candidate.getAvailableCpuPercent(),
                candidate.getVmMips(),
                candidate.getVmCoreCount(),
                replayState
                .getUploadDelayMsForDestination(
                        destination),
                replayState
                .getAvailableBandwidthMbpsForDestination(
                        destination),
                destination);
    }

    private PpoFeatureVector encode(
            double taskLengthMi,
            double uploadSizeKb,
            double requiredCpuPercent,
            double availableCpuPercent,
            double vmMips,
            int vmCoreCount,
            double uploadDelayMs,
            double availableBandwidthMbps,
            String destination) {

        double edgeIndicator =
                "EDGE".equalsIgnoreCase(
                        destination)
                ? 1.0
                : 0.0;

        double cloudIndicator =
                "CLOUD".equalsIgnoreCase(
                        destination)
                ? 1.0
                : 0.0;

        return new PpoFeatureVector(
                new double[] {
                        normalizeNonnegative(
                                taskLengthMi,
                                MAX_TASK_LENGTH_MI),
                        normalizeNonnegative(
                                uploadSizeKb,
                                MAX_UPLOAD_SIZE_KB),
                        normalizePercentage(
                                requiredCpuPercent),
                        normalizePercentage(
                                availableCpuPercent),
                        normalizeNonnegative(
                                vmMips,
                                MAX_VM_MIPS),
                        normalizeNonnegative(
                                vmCoreCount,
                                MAX_VM_CORES),
                        normalizeOptionalNonnegative(
                                uploadDelayMs,
                                MAX_DELAY_MS),
                        normalizeOptionalNonnegative(
                                availableBandwidthMbps,
                                MAX_BANDWIDTH_MBPS),
                        edgeIndicator,
                        cloudIndicator
                });
    }

    private static double normalizePercentage(
            double value) {

        return clamp(
                value / 100.0);
    }

    private static double normalizeNonnegative(
            double value,
            double scale) {

        if(Double.isNaN(value)
                || Double.isInfinite(value)
                || value < 0.0) {

            throw new IllegalArgumentException(
                    "Required PPO feature must be finite "
                    + "and nonnegative.");
        }

        return clamp(
                value / scale);
    }

    private static double normalizeOptionalNonnegative(
            double value,
            double scale) {

        if(Double.isNaN(value)
                || Double.isInfinite(value)
                || value < 0.0) {

            return 0.0;
        }

        return clamp(
                value / scale);
    }

    private static double clamp(
            double value) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value));
    }
}
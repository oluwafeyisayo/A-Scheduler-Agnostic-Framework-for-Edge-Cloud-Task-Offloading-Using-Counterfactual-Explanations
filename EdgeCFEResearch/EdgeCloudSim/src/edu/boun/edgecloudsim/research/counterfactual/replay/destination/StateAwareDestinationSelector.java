package edu.boun.edgecloudsim.research.counterfactual.replay.destination;

import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.units
        .EngineeringUnits;

/**
 * Selects EDGE, CLOUD, or NONE from a modified replay state.
 *
 * The selector is scheduler-independent. It first evaluates
 * destination-level network feasibility, then determines whether each
 * tier contains at least one compute-feasible candidate. If both tiers
 * are operationally feasible, it selects the tier with the lower
 * estimated service cost:
 *
 * destination service cost =
 *     destination network delay
 *     + best feasible candidate processing time.
 */
public final class StateAwareDestinationSelector {

    private static final double EPSILON =
            0.000001;

    public DestinationSelectionResult selectDestination(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidates) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        if(candidates == null) {

            throw new IllegalArgumentException(
                    "Candidate snapshot list cannot be null.");
        }

        DestinationEvaluation edge =
                evaluateDestination(
                        DestinationSelectionResult.EDGE,
                        replayState,
                        candidates);

        DestinationEvaluation cloud =
                evaluateDestination(
                        DestinationSelectionResult.CLOUD,
                        replayState,
                        candidates);

        boolean edgeOperational =
                edge.networkFeasible
                && edge.computeFeasibleCount > 0;

        boolean cloudOperational =
                cloud.networkFeasible
                && cloud.computeFeasibleCount > 0;

        String selectedDestination;
        String reason;

        if(edgeOperational
                && !cloudOperational) {

            selectedDestination =
                    DestinationSelectionResult.EDGE;

            reason =
                    "EDGE was selected because it is the only "
                    + "operationally feasible destination.";
        }
        else if(!edgeOperational
                && cloudOperational) {

            selectedDestination =
                    DestinationSelectionResult.CLOUD;

            reason =
                    "CLOUD was selected because it is the only "
                    + "operationally feasible destination.";
        }
        else if(!edgeOperational
                && !cloudOperational) {

            selectedDestination =
                    DestinationSelectionResult.NONE;

            reason =
                    buildNoDestinationReason(
                            edge,
                            cloud);
        }
        else if(edge.serviceCostMs
                + EPSILON
                < cloud.serviceCostMs) {

            selectedDestination =
                    DestinationSelectionResult.EDGE;

            reason =
                    "Both destinations are operationally feasible; "
                    + "EDGE has the lower estimated service cost.";
        }
        else if(cloud.serviceCostMs
                + EPSILON
                < edge.serviceCostMs) {

            selectedDestination =
                    DestinationSelectionResult.CLOUD;

            reason =
                    "Both destinations are operationally feasible; "
                    + "CLOUD has the lower estimated service cost.";
        }
        else {

            /*
             * Deterministic tie-breaking preserves the originally
             * attempted destination when possible. This avoids
             * manufacturing a destination change from numerically equal
             * service costs.
             */
            String attemptedDestination =
                    normalizeDestination(
                            replayState
                            .getAttemptedDestination());

            if(DestinationSelectionResult.CLOUD
                    .equals(attemptedDestination)) {

                selectedDestination =
                        DestinationSelectionResult.CLOUD;
            }
            else {

                selectedDestination =
                        DestinationSelectionResult.EDGE;
            }

            reason =
                    "Both destinations are operationally feasible "
                    + "with equal estimated service cost; the "
                    + "originally attempted destination was retained.";
        }

        return new DestinationSelectionResult(
                selectedDestination,
                edge.networkFeasible,
                cloud.networkFeasible,
                edge.computeFeasibleCount > 0,
                cloud.computeFeasibleCount > 0,
                edge.candidateCount,
                cloud.candidateCount,
                edge.computeFeasibleCount,
                cloud.computeFeasibleCount,
                edge.serviceCostMs,
                cloud.serviceCostMs,
                reason);
    }

    private DestinationEvaluation evaluateDestination(
            String destination,
            ReplayState replayState,
            List<VmCandidateSnapshot> candidates) {

        int candidateCount =
                0;

        int computeFeasibleCount =
                0;

        double bestProcessingTimeMs =
                Double.POSITIVE_INFINITY;

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate == null
                    || !destination.equalsIgnoreCase(
                            candidate.getDestination())) {

                continue;
            }

            candidateCount++;

            if(!candidate.isComputeFeasible()) {

                continue;
            }

            computeFeasibleCount++;

            double processingTimeMs =
                    candidate.estimateProcessingTimeMs(
                            replayState.getTaskLengthMi());

            if(processingTimeMs
                    < bestProcessingTimeMs) {

                bestProcessingTimeMs =
                        processingTimeMs;
            }
        }

        boolean networkFeasible =
                replayState
                .isNetworkFeasibleForDestination(
                        destination)
                && hasValidNetworkMeasurements(
                        replayState,
                        destination);

        double networkDelayMs =
                estimateNetworkDelayMs(
                        replayState,
                        destination);

        double serviceCostMs =
                networkFeasible
                && computeFeasibleCount > 0
                && isFiniteNonnegative(
                        networkDelayMs)
                && isFiniteNonnegative(
                        bestProcessingTimeMs)
                ? networkDelayMs
                  + bestProcessingTimeMs
                : Double.POSITIVE_INFINITY;

        return new DestinationEvaluation(
                destination,
                candidateCount,
                computeFeasibleCount,
                networkFeasible,
                networkDelayMs,
                bestProcessingTimeMs,
                serviceCostMs);
    }

    private boolean hasValidNetworkMeasurements(
            ReplayState replayState,
            String destination) {

        double bandwidthMbps =
                replayState
                .getAvailableBandwidthMbpsForDestination(
                        destination);

        double latencyMs =
                replayState
                .getNetworkLatencyMsForDestination(
                        destination);

        return isFinitePositive(
                bandwidthMbps)
                && isFiniteNonnegative(
                        latencyMs)
                && replayState.getUploadSizeKb() > 0.0;
    }

    private double estimateNetworkDelayMs(
            ReplayState replayState,
            String destination) {

        double loggedUploadDelayMs =
                replayState
                .getUploadDelayMsForDestination(
                        destination);

        if(isFiniteNonnegative(
                loggedUploadDelayMs)) {

            return loggedUploadDelayMs;
        }

        double transmissionTimeMs =
                EngineeringUnits
                .calculateTransmissionTimeMs(
                        replayState.getUploadSizeKb(),
                        replayState
                        .getAvailableBandwidthMbpsForDestination(
                                destination));

        if(!isFiniteNonnegative(
                transmissionTimeMs)) {

            return Double.POSITIVE_INFINITY;
        }

        double latencyMs =
                replayState
                .getNetworkLatencyMsForDestination(
                        destination);

        if(!isFiniteNonnegative(
                latencyMs)) {

            return Double.POSITIVE_INFINITY;
        }

        return latencyMs
                + transmissionTimeMs;
    }

    private String buildNoDestinationReason(
            DestinationEvaluation edge,
            DestinationEvaluation cloud) {

        return "No operationally feasible destination exists. "
                + "EDGE(networkFeasible="
                + edge.networkFeasible
                + ", computeFeasibleCandidates="
                + edge.computeFeasibleCount
                + "); CLOUD(networkFeasible="
                + cloud.networkFeasible
                + ", computeFeasibleCandidates="
                + cloud.computeFeasibleCount
                + ").";
    }

    private static String normalizeDestination(
            String value) {

        if(value == null) {

            return DestinationSelectionResult.NONE;
        }

        return value.trim()
                .toUpperCase();
    }

    private static boolean isFinitePositive(
            double value) {

        return !Double.isNaN(value)
                && !Double.isInfinite(value)
                && value > 0.0;
    }

    private static boolean isFiniteNonnegative(
            double value) {

        return !Double.isNaN(value)
                && !Double.isInfinite(value)
                && value >= 0.0;
    }

    private static final class DestinationEvaluation {

        private final String destination;
        private final int candidateCount;
        private final int computeFeasibleCount;
        private final boolean networkFeasible;
        private final double networkDelayMs;
        private final double bestProcessingTimeMs;
        private final double serviceCostMs;

        private DestinationEvaluation(
                String destination,
                int candidateCount,
                int computeFeasibleCount,
                boolean networkFeasible,
                double networkDelayMs,
                double bestProcessingTimeMs,
                double serviceCostMs) {

            this.destination =
                    destination;

            this.candidateCount =
                    candidateCount;

            this.computeFeasibleCount =
                    computeFeasibleCount;

            this.networkFeasible =
                    networkFeasible;

            this.networkDelayMs =
                    networkDelayMs;

            this.bestProcessingTimeMs =
                    bestProcessingTimeMs;

            this.serviceCostMs =
                    serviceCostMs;
        }

        @Override
        public String toString() {

            return "DestinationEvaluation{"
                    + "destination='"
                    + destination
                    + '\''
                    + ", candidateCount="
                    + candidateCount
                    + ", computeFeasibleCount="
                    + computeFeasibleCount
                    + ", networkFeasible="
                    + networkFeasible
                    + ", networkDelayMs="
                    + networkDelayMs
                    + ", bestProcessingTimeMs="
                    + bestProcessingTimeMs
                    + ", serviceCostMs="
                    + serviceCostMs
                    + '}';
        }
    }
}
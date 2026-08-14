package edu.boun.edgecloudsim.research.counterfactual.replay.destination;

/**
 * Immutable result produced by the state-aware destination selector.
 */
public final class DestinationSelectionResult {

    public static final String EDGE =
            "EDGE";

    public static final String CLOUD =
            "CLOUD";

    public static final String NONE =
            "NONE";

    private final String selectedDestination;

    private final boolean edgeNetworkFeasible;
    private final boolean cloudNetworkFeasible;

    private final boolean edgeComputeFeasible;
    private final boolean cloudComputeFeasible;

    private final int edgeCandidateCount;
    private final int cloudCandidateCount;

    private final int edgeComputeFeasibleCandidateCount;
    private final int cloudComputeFeasibleCandidateCount;

    private final double edgeServiceCostMs;
    private final double cloudServiceCostMs;

    private final String reason;

    public DestinationSelectionResult(
            String selectedDestination,
            boolean edgeNetworkFeasible,
            boolean cloudNetworkFeasible,
            boolean edgeComputeFeasible,
            boolean cloudComputeFeasible,
            int edgeCandidateCount,
            int cloudCandidateCount,
            int edgeComputeFeasibleCandidateCount,
            int cloudComputeFeasibleCandidateCount,
            double edgeServiceCostMs,
            double cloudServiceCostMs,
            String reason) {

        this.selectedDestination =
                normalizeDestination(
                        selectedDestination);

        this.edgeNetworkFeasible =
                edgeNetworkFeasible;

        this.cloudNetworkFeasible =
                cloudNetworkFeasible;

        this.edgeComputeFeasible =
                edgeComputeFeasible;

        this.cloudComputeFeasible =
                cloudComputeFeasible;

        this.edgeCandidateCount =
                validateNonnegative(
                        edgeCandidateCount,
                        "edgeCandidateCount");

        this.cloudCandidateCount =
                validateNonnegative(
                        cloudCandidateCount,
                        "cloudCandidateCount");

        this.edgeComputeFeasibleCandidateCount =
                validateNonnegative(
                        edgeComputeFeasibleCandidateCount,
                        "edgeComputeFeasibleCandidateCount");

        this.cloudComputeFeasibleCandidateCount =
                validateNonnegative(
                        cloudComputeFeasibleCandidateCount,
                        "cloudComputeFeasibleCandidateCount");

        this.edgeServiceCostMs =
                edgeServiceCostMs;

        this.cloudServiceCostMs =
                cloudServiceCostMs;

        this.reason =
                normalizeReason(
                        reason);
    }

    private static int validateNonnegative(
            int value,
            String fieldName) {

        if(value < 0) {

            throw new IllegalArgumentException(
                    fieldName
                    + " cannot be negative.");
        }

        return value;
    }

    private static String normalizeDestination(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return NONE;
        }

        String normalized =
                value.trim()
                .toUpperCase();

        if(!EDGE.equals(normalized)
                && !CLOUD.equals(normalized)
                && !NONE.equals(normalized)) {

            throw new IllegalArgumentException(
                    "Unsupported destination: "
                    + value);
        }

        return normalized;
    }

    private static String normalizeReason(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return "NO_REASON";
        }

        return value.trim();
    }

    public String getSelectedDestination() {

        return selectedDestination;
    }

    public boolean isEdgeSelected() {

        return EDGE.equals(
                selectedDestination);
    }

    public boolean isCloudSelected() {

        return CLOUD.equals(
                selectedDestination);
    }

    public boolean isNoDestinationSelected() {

        return NONE.equals(
                selectedDestination);
    }

    public boolean isEdgeNetworkFeasible() {

        return edgeNetworkFeasible;
    }

    public boolean isCloudNetworkFeasible() {

        return cloudNetworkFeasible;
    }

    public boolean isEdgeComputeFeasible() {

        return edgeComputeFeasible;
    }

    public boolean isCloudComputeFeasible() {

        return cloudComputeFeasible;
    }

    public boolean isEdgeOperationallyFeasible() {

        return edgeNetworkFeasible
                && edgeComputeFeasible;
    }

    public boolean isCloudOperationallyFeasible() {

        return cloudNetworkFeasible
                && cloudComputeFeasible;
    }

    public int getEdgeCandidateCount() {

        return edgeCandidateCount;
    }

    public int getCloudCandidateCount() {

        return cloudCandidateCount;
    }

    public int getEdgeComputeFeasibleCandidateCount() {

        return edgeComputeFeasibleCandidateCount;
    }

    public int getCloudComputeFeasibleCandidateCount() {

        return cloudComputeFeasibleCandidateCount;
    }

    public double getEdgeServiceCostMs() {

        return edgeServiceCostMs;
    }

    public double getCloudServiceCostMs() {

        return cloudServiceCostMs;
    }

    public String getReason() {

        return reason;
    }

    @Override
    public String toString() {

        return "DestinationSelectionResult{"
                + "selectedDestination='"
                + selectedDestination
                + '\''
                + ", edgeNetworkFeasible="
                + edgeNetworkFeasible
                + ", cloudNetworkFeasible="
                + cloudNetworkFeasible
                + ", edgeComputeFeasible="
                + edgeComputeFeasible
                + ", cloudComputeFeasible="
                + cloudComputeFeasible
                + ", edgeCandidateCount="
                + edgeCandidateCount
                + ", cloudCandidateCount="
                + cloudCandidateCount
                + ", edgeComputeFeasibleCandidateCount="
                + edgeComputeFeasibleCandidateCount
                + ", cloudComputeFeasibleCandidateCount="
                + cloudComputeFeasibleCandidateCount
                + ", edgeServiceCostMs="
                + edgeServiceCostMs
                + ", cloudServiceCostMs="
                + cloudServiceCostMs
                + ", reason='"
                + reason
                + '\''
                + '}';
    }
}
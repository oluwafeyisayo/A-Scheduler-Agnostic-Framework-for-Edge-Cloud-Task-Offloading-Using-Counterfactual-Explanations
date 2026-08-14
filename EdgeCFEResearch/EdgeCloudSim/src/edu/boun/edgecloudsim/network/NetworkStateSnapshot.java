package edu.boun.edgecloudsim.network;

public final class NetworkStateSnapshot {

    private final DestinationNetworkState edgeNetworkState;
    private final DestinationNetworkState cloudNetworkState;
    private final String observedDestinationType;

    public NetworkStateSnapshot(
            String destinationType,
            int servingWlanId,
            int connectedDeviceCount,
            double configuredBandwidthMbps,
            double availableBandwidthMbps,
            double propagationDelaySeconds,
            double uploadDelaySeconds,
            boolean networkFeasible,
            String infeasibilityReason) {

        String normalized = normalizeDestination(destinationType);

        DestinationNetworkState observed =
                new DestinationNetworkState(
                        normalized,
                        servingWlanId,
                        connectedDeviceCount,
                        configuredBandwidthMbps,
                        availableBandwidthMbps,
                        propagationDelaySeconds,
                        uploadDelaySeconds,
                        networkFeasible,
                        infeasibilityReason);

        this.edgeNetworkState =
                "EDGE".equals(normalized)
                ? observed
                : DestinationNetworkState.unavailable(
                        "EDGE",
                        servingWlanId,
                        "EDGE_NETWORK_STATE_NOT_CAPTURED");

        this.cloudNetworkState =
                "CLOUD".equals(normalized)
                ? observed
                : DestinationNetworkState.unavailable(
                        "CLOUD",
                        servingWlanId,
                        "CLOUD_NETWORK_STATE_NOT_CAPTURED");

        this.observedDestinationType =
                normalized;
    }

    public NetworkStateSnapshot(
            DestinationNetworkState edgeNetworkState,
            DestinationNetworkState cloudNetworkState,
            String observedDestinationType) {

        if(edgeNetworkState == null || cloudNetworkState == null) {
            throw new IllegalArgumentException(
                    "EDGE and CLOUD network states cannot be null.");
        }

        this.edgeNetworkState = edgeNetworkState;
        this.cloudNetworkState = cloudNetworkState;
        this.observedDestinationType =
                normalizeDestination(observedDestinationType);
    }

    public static NetworkStateSnapshot combine(
            NetworkStateSnapshot edgeSnapshot,
            NetworkStateSnapshot cloudSnapshot,
            String observedDestinationType,
            int servingWlanId) {

        DestinationNetworkState edge =
                edgeSnapshot == null
                ? DestinationNetworkState.unavailable(
                        "EDGE",
                        servingWlanId,
                        "EDGE_NETWORK_STATE_UNAVAILABLE")
                : edgeSnapshot.getForDestination("EDGE");

        DestinationNetworkState cloud =
                cloudSnapshot == null
                ? DestinationNetworkState.unavailable(
                        "CLOUD",
                        servingWlanId,
                        "CLOUD_NETWORK_STATE_UNAVAILABLE")
                : cloudSnapshot.getForDestination("CLOUD");

        return new NetworkStateSnapshot(
                edge,
                cloud,
                observedDestinationType);
    }

    private static String normalizeDestination(
            String value) {

        if(value == null || value.trim().isEmpty()) {
            return "UNKNOWN";
        }

        String normalized = value.trim().toUpperCase();

        if("GENERIC_EDGE_DEVICE".equals(normalized)) {
            return "EDGE";
        }

        return normalized;
    }

    public DestinationNetworkState getEdgeNetworkState() {
        return edgeNetworkState;
    }

    public DestinationNetworkState getCloudNetworkState() {
        return cloudNetworkState;
    }

    public DestinationNetworkState getForDestination(
            String destinationType) {

        String normalized =
                normalizeDestination(destinationType);

        if("EDGE".equals(normalized)) {
            return edgeNetworkState;
        }

        if("CLOUD".equals(normalized)) {
            return cloudNetworkState;
        }

        return DestinationNetworkState.unavailable(
                normalized,
                getObservedState().getServingWlanId(),
                "UNKNOWN_DESTINATION");
    }

    public String getObservedDestinationType() {
        return observedDestinationType;
    }

    private DestinationNetworkState getObservedState() {

        if("EDGE".equals(observedDestinationType)) {
            return edgeNetworkState;
        }

        if("CLOUD".equals(observedDestinationType)) {
            return cloudNetworkState;
        }

        if(edgeNetworkState.hasUsableMeasurements()) {
            return edgeNetworkState;
        }

        return cloudNetworkState;
    }

    public String getDestinationType() {
        return getObservedState().getDestinationType();
    }

    public int getServingWlanId() {
        return getObservedState().getServingWlanId();
    }

    public int getConnectedDeviceCount() {
        return getObservedState().getConnectedDeviceCount();
    }

    public double getConfiguredBandwidthMbps() {
        return getObservedState().getConfiguredBandwidthMbps();
    }

    public double getAvailableBandwidthMbps() {
        return getObservedState().getAvailableBandwidthMbps();
    }

    public double getPropagationDelaySeconds() {
        return getObservedState().getPropagationDelaySeconds();
    }

    public double getPropagationDelayMilliseconds() {
        return getObservedState().getPropagationDelayMilliseconds();
    }

    public double getUploadDelaySeconds() {
        return getObservedState().getUploadDelaySeconds();
    }

    public double getUploadDelayMilliseconds() {
        return getObservedState().getUploadDelayMilliseconds();
    }

    public boolean isNetworkFeasible() {
        return getObservedState().isNetworkFeasible();
    }

    public String getInfeasibilityReason() {
        return getObservedState().getInfeasibilityReason();
    }
}
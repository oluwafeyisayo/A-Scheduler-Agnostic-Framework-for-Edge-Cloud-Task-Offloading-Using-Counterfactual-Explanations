package edu.boun.edgecloudsim.network;

public final class DestinationNetworkState {

    private final String destinationType;
    private final int servingWlanId;
    private final int connectedDeviceCount;
    private final double configuredBandwidthMbps;
    private final double availableBandwidthMbps;
    private final double propagationDelaySeconds;
    private final double uploadDelaySeconds;
    private final boolean networkFeasible;
    private final String infeasibilityReason;

    public DestinationNetworkState(
            String destinationType,
            int servingWlanId,
            int connectedDeviceCount,
            double configuredBandwidthMbps,
            double availableBandwidthMbps,
            double propagationDelaySeconds,
            double uploadDelaySeconds,
            boolean networkFeasible,
            String infeasibilityReason) {

        this.destinationType = normalize(destinationType, "UNKNOWN");
        this.servingWlanId = servingWlanId;
        this.connectedDeviceCount = connectedDeviceCount;
        this.configuredBandwidthMbps = configuredBandwidthMbps;
        this.availableBandwidthMbps = availableBandwidthMbps;
        this.propagationDelaySeconds = propagationDelaySeconds;
        this.uploadDelaySeconds = uploadDelaySeconds;
        this.networkFeasible = networkFeasible;
        this.infeasibilityReason =
                normalize(infeasibilityReason, "UNKNOWN");
    }

    public static DestinationNetworkState unavailable(
            String destinationType,
            int servingWlanId,
            String reason) {

        return new DestinationNetworkState(
                destinationType,
                servingWlanId,
                -1,
                -1.0,
                -1.0,
                -1.0,
                -1.0,
                false,
                reason);
    }

    private static String normalize(
            String value,
            String fallback) {

        if(value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim().toUpperCase();
    }

    public String getDestinationType() {
        return destinationType;
    }

    public int getServingWlanId() {
        return servingWlanId;
    }

    public int getConnectedDeviceCount() {
        return connectedDeviceCount;
    }

    public double getConfiguredBandwidthMbps() {
        return configuredBandwidthMbps;
    }

    public double getAvailableBandwidthMbps() {
        return availableBandwidthMbps;
    }

    public double getPropagationDelaySeconds() {
        return propagationDelaySeconds;
    }

    public double getPropagationDelayMilliseconds() {
        return propagationDelaySeconds < 0.0
                ? -1.0
                : propagationDelaySeconds * 1000.0;
    }

    public double getUploadDelaySeconds() {
        return uploadDelaySeconds;
    }

    public double getUploadDelayMilliseconds() {
        return uploadDelaySeconds < 0.0
                ? -1.0
                : uploadDelaySeconds * 1000.0;
    }

    public boolean isNetworkFeasible() {
        return networkFeasible;
    }

    public String getInfeasibilityReason() {
        return infeasibilityReason;
    }

    public boolean hasUsableMeasurements() {
        return configuredBandwidthMbps >= 0.0
                && availableBandwidthMbps >= 0.0
                && propagationDelaySeconds >= 0.0
                && uploadDelaySeconds >= 0.0;
    }
}
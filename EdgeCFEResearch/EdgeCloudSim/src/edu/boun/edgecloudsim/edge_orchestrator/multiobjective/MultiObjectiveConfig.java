package edu.boun.edgecloudsim.edge_orchestrator.multiobjective;

/**
 * Immutable configuration for the lightweight multi-objective scheduler.
 */
public final class MultiObjectiveConfig {

    public static final String DEFAULT_CONFIGURATION_ID =
            "MO_LATENCY_050_ENERGY_030_LOAD_020";

    private final double latencyWeight;
    private final double energyWeight;
    private final double loadWeight;

    private final double transmissionPowerWatts;
    private final double computePowerWatts;

    private final String configurationId;

    public MultiObjectiveConfig(
            double latencyWeight,
            double energyWeight,
            double loadWeight,
            double transmissionPowerWatts,
            double computePowerWatts,
            String configurationId) {

        validateWeight(
                latencyWeight,
                "latencyWeight");

        validateWeight(
                energyWeight,
                "energyWeight");

        validateWeight(
                loadWeight,
                "loadWeight");

        double weightSum =
                latencyWeight
                + energyWeight
                + loadWeight;

        if(Math.abs(weightSum - 1.0) > 0.000001) {

            throw new IllegalArgumentException(
                    "Multi-objective weights must sum to 1.0.");
        }

        if(!isFinitePositive(
                transmissionPowerWatts)) {

            throw new IllegalArgumentException(
                    "Transmission power must be finite and positive.");
        }

        if(!isFinitePositive(
                computePowerWatts)) {

            throw new IllegalArgumentException(
                    "Compute power must be finite and positive.");
        }

        this.latencyWeight =
                latencyWeight;

        this.energyWeight =
                energyWeight;

        this.loadWeight =
                loadWeight;

        this.transmissionPowerWatts =
                transmissionPowerWatts;

        this.computePowerWatts =
                computePowerWatts;

        this.configurationId =
                configurationId == null
                || configurationId.trim().isEmpty()
                ? DEFAULT_CONFIGURATION_ID
                : configurationId.trim();
    }

    public static MultiObjectiveConfig createDefault() {

        return new MultiObjectiveConfig(
                0.50,
                0.30,
                0.20,
                1.30,
                2.00,
                DEFAULT_CONFIGURATION_ID);
    }

    private static void validateWeight(
            double value,
            String fieldName) {

        if(Double.isNaN(value)
                || Double.isInfinite(value)
                || value < 0.0
                || value > 1.0) {

            throw new IllegalArgumentException(
                    fieldName
                    + " must be between 0 and 1.");
        }
    }

    private static boolean isFinitePositive(
            double value) {

        return !Double.isNaN(value)
                && !Double.isInfinite(value)
                && value > 0.0;
    }

    public double getLatencyWeight() {
        return latencyWeight;
    }

    public double getEnergyWeight() {
        return energyWeight;
    }

    public double getLoadWeight() {
        return loadWeight;
    }

    public double getTransmissionPowerWatts() {
        return transmissionPowerWatts;
    }

    public double getComputePowerWatts() {
        return computePowerWatts;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    @Override
    public String toString() {

        return "MultiObjectiveConfig{"
                + "latencyWeight="
                + latencyWeight
                + ", energyWeight="
                + energyWeight
                + ", loadWeight="
                + loadWeight
                + ", transmissionPowerWatts="
                + transmissionPowerWatts
                + ", computePowerWatts="
                + computePowerWatts
                + ", configurationId='"
                + configurationId
                + '\''
                + '}';
    }
}
package edu.boun.edgecloudsim.research.counterfactual.replay.state;

import edu.boun.edgecloudsim.edge_orchestrator.multiobjective
        .MultiObjectiveConfig;

/**
 * Immutable policy state used to replay the deterministic
 * MULTI_OBJECTIVE scheduler.
 *
 * The current implementation uses one fixed, versioned configuration.
 * Keeping the weights in policy state makes the replay contract explicit
 * and allows additional configurations to be added later.
 */
public final class MultiObjectivePolicyState
        implements SchedulerPolicyState {

    private final String configurationId;

    private final double latencyWeight;
    private final double energyWeight;
    private final double loadWeight;

    private final double transmissionPowerWatts;
    private final double computePowerWatts;

    public MultiObjectivePolicyState(
            String configurationId,
            double latencyWeight,
            double energyWeight,
            double loadWeight,
            double transmissionPowerWatts,
            double computePowerWatts) {

        MultiObjectiveConfig validatedConfig =
                new MultiObjectiveConfig(
                        latencyWeight,
                        energyWeight,
                        loadWeight,
                        transmissionPowerWatts,
                        computePowerWatts,
                        configurationId);

        this.configurationId =
                validatedConfig.getConfigurationId();

        this.latencyWeight =
                validatedConfig.getLatencyWeight();

        this.energyWeight =
                validatedConfig.getEnergyWeight();

        this.loadWeight =
                validatedConfig.getLoadWeight();

        this.transmissionPowerWatts =
                validatedConfig
                .getTransmissionPowerWatts();

        this.computePowerWatts =
                validatedConfig
                .getComputePowerWatts();
    }

    public static MultiObjectivePolicyState
            createDefault() {

        MultiObjectiveConfig config =
                MultiObjectiveConfig.createDefault();

        return new MultiObjectivePolicyState(
                config.getConfigurationId(),
                config.getLatencyWeight(),
                config.getEnergyWeight(),
                config.getLoadWeight(),
                config.getTransmissionPowerWatts(),
                config.getComputePowerWatts());
    }

    @Override
    public String getPolicyName() {

        return "MULTI_OBJECTIVE";
    }

    public String getConfigurationId() {
        return configurationId;
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

    public MultiObjectiveConfig toConfig() {

        return new MultiObjectiveConfig(
                latencyWeight,
                energyWeight,
                loadWeight,
                transmissionPowerWatts,
                computePowerWatts,
                configurationId);
    }

    @Override
    public String toString() {

        return "MultiObjectivePolicyState{"
                + "configurationId='"
                + configurationId
                + '\''
                + ", latencyWeight="
                + latencyWeight
                + ", energyWeight="
                + energyWeight
                + ", loadWeight="
                + loadWeight
                + ", transmissionPowerWatts="
                + transmissionPowerWatts
                + ", computePowerWatts="
                + computePowerWatts
                + '}';
    }
}
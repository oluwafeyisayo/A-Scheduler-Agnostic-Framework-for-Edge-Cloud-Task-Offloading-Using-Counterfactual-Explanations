package edu.boun.edgecloudsim.network;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.utils.Location;

public abstract class NetworkModel {

    protected int numberOfMobileDevices;
    protected String simScenario;

    public NetworkModel(
            int numberOfMobileDevices,
            String simScenario) {

        this.numberOfMobileDevices =
                numberOfMobileDevices;

        this.simScenario =
                simScenario;
    }

    public abstract void initialize();

    public abstract double getUploadDelay(
            int sourceDeviceId,
            int destDeviceId,
            Task task);

    public abstract double getDownloadDelay(
            int sourceDeviceId,
            int destDeviceId,
            Task task);

    public abstract void uploadStarted(
            Location accessPointLocation,
            int destDeviceId);

    public abstract void uploadFinished(
            Location accessPointLocation,
            int destDeviceId);

    public abstract void downloadStarted(
            Location accessPointLocation,
            int sourceDeviceId);

    public abstract void downloadFinished(
            Location accessPointLocation,
            int sourceDeviceId);

    public NetworkStateSnapshot getNetworkState(
            int sourceDeviceId,
            int destDeviceId,
            Task task) {

        String destinationType =
                destDeviceId == SimSettings.CLOUD_DATACENTER_ID
                ? "CLOUD"
                : destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID
                ? "EDGE"
                : "UNKNOWN";

        int servingWlanId =
                task == null
                || task.getSubmittedLocation() == null
                ? -1
                : task.getSubmittedLocation()
                .getServingWlanId();

        return new NetworkStateSnapshot(
                destinationType,
                servingWlanId,
                -1,
                -1.0,
                -1.0,
                -1.0,
                -1.0,
                false,
                "NETWORK_STATE_NOT_SUPPORTED");
    }

    public NetworkStateSnapshot getNetworkStates(
            int sourceDeviceId,
            Task task,
            String observedDestinationType) {

        int servingWlanId =
                task == null
                || task.getSubmittedLocation() == null
                ? -1
                : task.getSubmittedLocation()
                .getServingWlanId();

        NetworkStateSnapshot edge =
                safeGetNetworkState(
                        sourceDeviceId,
                        SimSettings.GENERIC_EDGE_DEVICE_ID,
                        task,
                        "EDGE",
                        servingWlanId);

        NetworkStateSnapshot cloud =
                safeGetNetworkState(
                        sourceDeviceId,
                        SimSettings.CLOUD_DATACENTER_ID,
                        task,
                        "CLOUD",
                        servingWlanId);

        return NetworkStateSnapshot.combine(
                edge,
                cloud,
                observedDestinationType,
                servingWlanId);
    }

    private NetworkStateSnapshot safeGetNetworkState(
            int sourceDeviceId,
            int destinationId,
            Task task,
            String destinationType,
            int servingWlanId) {

        try {

            NetworkStateSnapshot snapshot =
                    getNetworkState(
                            sourceDeviceId,
                            destinationId,
                            task);

            if(snapshot != null) {
                return snapshot;
            }
        }
        catch(RuntimeException exception) {

            return new NetworkStateSnapshot(
                    destinationType,
                    servingWlanId,
                    -1,
                    -1.0,
                    -1.0,
                    -1.0,
                    -1.0,
                    false,
                    destinationType
                    + "_NETWORK_STATE_ERROR");
        }

        return new NetworkStateSnapshot(
                destinationType,
                servingWlanId,
                -1,
                -1.0,
                -1.0,
                -1.0,
                -1.0,
                false,
                destinationType
                + "_NETWORK_STATE_UNAVAILABLE");
    }
}
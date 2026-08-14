/*
 * Title:        EdgeCloudSim - Mobile Device Manager
 *
 * Description:
 * DefaultMobileDeviceManager submits tasks through the Edge Orchestrator.
 * This research version also records scheduler provenance and network state.
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.edge_client;

import java.util.Collections;
import java.util.List;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.edge_orchestrator.BasicEdgeOrchestrator;
import edu.boun.edgecloudsim.edge_orchestrator.SchedulerDecision;
import edu.boun.edgecloudsim.network.DestinationNetworkState;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.NetworkStateSnapshot;
import edu.boun.edgecloudsim.research.logging.CandidateVmRecord;
import edu.boun.edgecloudsim.research.logging.DecisionLogger;
import edu.boun.edgecloudsim.research.logging.DecisionRecord;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;

public class DefaultMobileDeviceManager extends MobileDeviceManager {

    private static final int BASE = 100000;

    private static final int REQUEST_RECEIVED_BY_CLOUD =
            BASE + 1;

    private static final int REQUEST_RECIVED_BY_EDGE_DEVICE =
            BASE + 2;

    private static final int RESPONSE_RECEIVED_BY_MOBILE_DEVICE =
            BASE + 3;

    private int taskIdCounter = 0;

    public DefaultMobileDeviceManager() throws Exception {
    }

    @Override
    public void initialize() {
        // DecisionLogger is initialized by MainApp.
    }

    @Override
    public UtilizationModel getCpuUtilizationModel() {
        return new CpuUtilizationModel_Custom();
    }

    @Override
    protected void submitCloudlets() {
        // Tasks are submitted individually.
    }

    @Override
    protected void processCloudletReturn(SimEvent ev) {

        NetworkModel networkModel =
                SimManager.getInstance()
                        .getNetworkModel();

        Task task =
                (Task) ev.getData();

        SimLogger.getInstance()
                .taskExecuted(
                        task.getCloudletId());

        if (task.getAssociatedDatacenterId()
                == SimSettings.CLOUD_DATACENTER_ID) {

            double wanDelay =
                    networkModel.getDownloadDelay(
                            SimSettings.CLOUD_DATACENTER_ID,
                            task.getMobileDeviceId(),
                            task);

            if (wanDelay > 0) {

                Location currentLocation =
                        SimManager.getInstance()
                                .getMobilityModel()
                                .getLocation(
                                        task.getMobileDeviceId(),
                                        CloudSim.clock()
                                        + wanDelay);

                if (task.getSubmittedLocation()
                        .getServingWlanId()
                        == currentLocation
                        .getServingWlanId()) {

                    networkModel.downloadStarted(
                            task.getSubmittedLocation(),
                            SimSettings.CLOUD_DATACENTER_ID);

                    SimLogger.getInstance()
                            .setDownloadDelay(
                                    task.getCloudletId(),
                                    wanDelay,
                                    NETWORK_DELAY_TYPES.WAN_DELAY);

                    schedule(
                            getId(),
                            wanDelay,
                            RESPONSE_RECEIVED_BY_MOBILE_DEVICE,
                            task);
                }
                else {

                    SimLogger.getInstance()
                            .failedDueToMobility(
                                    task.getCloudletId(),
                                    CloudSim.clock());
                }
            }
            else {

                SimLogger.getInstance()
                        .failedDueToBandwidth(
                                task.getCloudletId(),
                                CloudSim.clock(),
                                NETWORK_DELAY_TYPES.WAN_DELAY);
            }
        }
        else {

            double wlanDelay =
                    networkModel.getDownloadDelay(
                            task.getAssociatedHostId(),
                            task.getMobileDeviceId(),
                            task);

            if (wlanDelay > 0) {

                Location currentLocation =
                        SimManager.getInstance()
                                .getMobilityModel()
                                .getLocation(
                                        task.getMobileDeviceId(),
                                        CloudSim.clock()
                                        + wlanDelay);

                if (task.getSubmittedLocation()
                        .getServingWlanId()
                        == currentLocation
                        .getServingWlanId()) {

                    networkModel.downloadStarted(
                            currentLocation,
                            SimSettings.GENERIC_EDGE_DEVICE_ID);

                    SimLogger.getInstance()
                            .setDownloadDelay(
                                    task.getCloudletId(),
                                    wlanDelay,
                                    NETWORK_DELAY_TYPES.WLAN_DELAY);

                    schedule(
                            getId(),
                            wlanDelay,
                            RESPONSE_RECEIVED_BY_MOBILE_DEVICE,
                            task);
                }
                else {

                    SimLogger.getInstance()
                            .failedDueToMobility(
                                    task.getCloudletId(),
                                    CloudSim.clock());
                }
            }
            else {

                SimLogger.getInstance()
                        .failedDueToBandwidth(
                                task.getCloudletId(),
                                CloudSim.clock(),
                                NETWORK_DELAY_TYPES.WLAN_DELAY);
            }
        }
    }

    @Override
    protected void processOtherEvent(SimEvent ev) {

        if (ev == null) {

            SimLogger.printLine(
                    getName()
                    + ".processOtherEvent(): "
                    + "Event is null. Terminating simulation.");

            System.exit(0);
            return;
        }

        NetworkModel networkModel =
                SimManager.getInstance()
                        .getNetworkModel();

        switch (ev.getTag()) {

            case REQUEST_RECEIVED_BY_CLOUD: {

                Task task =
                        (Task) ev.getData();

                networkModel.uploadFinished(
                        task.getSubmittedLocation(),
                        SimSettings.CLOUD_DATACENTER_ID);

                submitTaskToVm(
                        task,
                        0,
                        SimSettings.CLOUD_DATACENTER_ID);

                break;
            }

            case REQUEST_RECIVED_BY_EDGE_DEVICE: {

                Task task =
                        (Task) ev.getData();

                networkModel.uploadFinished(
                        task.getSubmittedLocation(),
                        SimSettings.GENERIC_EDGE_DEVICE_ID);

                submitTaskToVm(
                        task,
                        0,
                        SimSettings.GENERIC_EDGE_DEVICE_ID);

                break;
            }

            case RESPONSE_RECEIVED_BY_MOBILE_DEVICE: {

                Task task =
                        (Task) ev.getData();

                if (task.getAssociatedDatacenterId()
                        == SimSettings.CLOUD_DATACENTER_ID) {

                    networkModel.downloadFinished(
                            task.getSubmittedLocation(),
                            SimSettings.CLOUD_DATACENTER_ID);
                }
                else if (task.getAssociatedDatacenterId()
                        != SimSettings.MOBILE_DATACENTER_ID) {

                    networkModel.downloadFinished(
                            task.getSubmittedLocation(),
                            SimSettings.GENERIC_EDGE_DEVICE_ID);
                }

                SimLogger.getInstance()
                        .taskEnded(
                                task.getCloudletId(),
                                CloudSim.clock());

                break;
            }

            default:

                SimLogger.printLine(
                        getName()
                        + ".processOtherEvent(): "
                        + "Unknown event. Terminating simulation.");

                System.exit(0);
                break;
        }
    }

    public void submitTask(TaskProperty edgeTask) {

        NetworkModel networkModel =
                SimManager.getInstance()
                        .getNetworkModel();

        Task task =
                createTask(edgeTask);

        Location currentLocation =
                SimManager.getInstance()
                        .getMobilityModel()
                        .getLocation(
                                task.getMobileDeviceId(),
                                CloudSim.clock());

        task.setSubmittedLocation(
                currentLocation);

        SimLogger.getInstance()
                .addLog(
                        task.getCloudletId(),
                        task.getTaskType(),
                        (int) task.getCloudletLength(),
                        (int) task.getCloudletFileSize(),
                        (int) task.getCloudletOutputSize());

        int nextHopId =
                SimManager.getInstance()
                        .getEdgeOrchestrator()
                        .getDeviceToOffload(task);

        String attemptedDestination =
                nextHopId
                == SimSettings.CLOUD_DATACENTER_ID
                ? "CLOUD"
                : "EDGE";

        NetworkStateSnapshot networkState =
                captureNetworkStates(
                        networkModel,
                        task,
                        currentLocation,
                        attemptedDestination);

        task.setResearchNetworkState(
                networkState);

        DestinationNetworkState attemptedNetworkState =
                networkState.getForDestination(
                        attemptedDestination);

        double uploadDelaySeconds =
                attemptedNetworkState
                .getUploadDelaySeconds();

        task.setResearchUploadDelay(
                uploadDelaySeconds);

        if (nextHopId
                == SimSettings.CLOUD_DATACENTER_ID) {

            if (uploadDelaySeconds > 0
                    && attemptedNetworkState
                    .isNetworkFeasible()) {

                networkModel.uploadStarted(
                        currentLocation,
                        nextHopId);

                SimLogger.getInstance()
                        .taskStarted(
                                task.getCloudletId(),
                                CloudSim.clock());

                SimLogger.getInstance()
                        .setUploadDelay(
                                task.getCloudletId(),
                                uploadDelaySeconds,
                                NETWORK_DELAY_TYPES.WAN_DELAY);

                schedule(
                        getId(),
                        uploadDelaySeconds,
                        REQUEST_RECEIVED_BY_CLOUD,
                        task);
            }
            else {

                logNetworkRejectedDecision(
                        task,
                        "CLOUD",
                        SimSettings.CLOUD_DATACENTER_ID,
                        networkState);

                SimLogger.getInstance()
                        .rejectedDueToBandwidth(
                                task.getCloudletId(),
                                CloudSim.clock(),
                                SimSettings.VM_TYPES.CLOUD_VM.ordinal(),
                                NETWORK_DELAY_TYPES.WAN_DELAY);
            }
        }
        else if (nextHopId
                == SimSettings.GENERIC_EDGE_DEVICE_ID) {

            if (uploadDelaySeconds > 0
                    && attemptedNetworkState
                    .isNetworkFeasible()) {

                networkModel.uploadStarted(
                        currentLocation,
                        nextHopId);

                schedule(
                        getId(),
                        uploadDelaySeconds,
                        REQUEST_RECIVED_BY_EDGE_DEVICE,
                        task);

                SimLogger.getInstance()
                        .taskStarted(
                                task.getCloudletId(),
                                CloudSim.clock());

                SimLogger.getInstance()
                        .setUploadDelay(
                                task.getCloudletId(),
                                uploadDelaySeconds,
                                NETWORK_DELAY_TYPES.WLAN_DELAY);
            }
            else {

                logNetworkRejectedDecision(
                        task,
                        "EDGE",
                        SimSettings.GENERIC_EDGE_DEVICE_ID,
                        networkState);

                SimLogger.getInstance()
                        .rejectedDueToBandwidth(
                                task.getCloudletId(),
                                CloudSim.clock(),
                                SimSettings.VM_TYPES.EDGE_VM.ordinal(),
                                NETWORK_DELAY_TYPES.WLAN_DELAY);
            }
        }
        else {

            SimLogger.printLine(
                    "Unknown nextHopId: "
                    + nextHopId
                    + ". Terminating simulation.");

            System.exit(0);
        }
    }

    private NetworkStateSnapshot captureNetworkStates(
            NetworkModel networkModel,
            Task task,
            Location currentLocation,
            String attemptedDestination) {

        try {

            NetworkStateSnapshot networkStates =
                    networkModel.getNetworkStates(
                            task.getMobileDeviceId(),
                            task,
                            attemptedDestination);

            if(networkStates == null) {

                throw new IllegalStateException(
                        "Network model returned null state.");
            }

            return networkStates;
        }
        catch(Exception exception) {

            System.err.println(
                    "Tier-specific network-state capture failed "
                    + "for task "
                    + task.getCloudletId()
                    + ": "
                    + exception.getMessage());

            DestinationNetworkState edgeState =
                    DestinationNetworkState.unavailable(
                            "EDGE",
                            currentLocation.getServingWlanId(),
                            "EDGE_NETWORK_STATE_ERROR");

            DestinationNetworkState cloudState =
                    DestinationNetworkState.unavailable(
                            "CLOUD",
                            currentLocation.getServingWlanId(),
                            "CLOUD_NETWORK_STATE_ERROR");

            return new NetworkStateSnapshot(
                    edgeState,
                    cloudState,
                    attemptedDestination);
        }
    }

    private void submitTaskToVm(
            Task task,
            double delay,
            int datacenterId) {
    	
    	SimSettings.VM_TYPES targetVmType =
    	        datacenterId == SimSettings.CLOUD_DATACENTER_ID
    	        ? SimSettings.VM_TYPES.CLOUD_VM
    	        : SimSettings.VM_TYPES.EDGE_VM;

    	int vmType =
    	        targetVmType.ordinal();

    	double requiredCpuPercent =
    	        predictRequiredCpuPercent(
    	                task,
    	                targetVmType);

        Vm selectedVM =
                SimManager.getInstance()
                        .getEdgeOrchestrator()
                        .getVmToOffload(
                                task,
                                datacenterId);

        SchedulerDecision schedulerDecision =
                getLastSchedulerDecision();

        int candidateVmCount = 0;
        int feasibleVmCount = 0;
        int selectedCandidateIndex = -1;
        int roundRobinStartingCandidateIndex = -1;
        int randomSampledCandidateIndex = -1;

        double selectedAvailableCpu = -1.0;
        double bestAvailableCpu = -1.0;

        String selectionReason =
                "SCHEDULER_DECISION_UNAVAILABLE";

        List<CandidateVmRecord> candidateVmRecords =
                Collections.emptyList();

        if (schedulerDecision != null) {

            candidateVmCount =
                    schedulerDecision
                            .getCandidateVmCount();

            feasibleVmCount =
                    schedulerDecision
                            .getFeasibleVmCount();

            selectedAvailableCpu =
                    schedulerDecision
                            .getSelectedAvailableCpu();

            bestAvailableCpu =
                    schedulerDecision
                            .getBestAvailableCpu();

            selectionReason =
                    schedulerDecision
                            .getSelectionReason();

            selectedCandidateIndex =
                    schedulerDecision
                            .getSelectedCandidateIndex();

            roundRobinStartingCandidateIndex =
                    schedulerDecision
                            .getRoundRobinStartingCandidateIndex();

            randomSampledCandidateIndex =
                    schedulerDecision
                            .getRandomSampledCandidateIndex();

            candidateVmRecords =
                    schedulerDecision
                            .getCandidateVmRecords();

            /*
             * Normalize scheduler-specific provenance before creating
             * DecisionRecord. A policy branch can execute and still
             * return NO_FEASIBLE_VM. In that case the branch-specific
             * cursor/sample remains valid even though the generic
             * selection reason does not name the policy.
             */
            if(roundRobinStartingCandidateIndex >= 0) {

                randomSampledCandidateIndex =
                        -1;

                if(selectionReason == null
                        || !selectionReason
                        .toUpperCase()
                        .contains(
                                "ROUND_ROBIN")) {

                    selectionReason =
                            "ROUND_ROBIN_"
                            + normalizeSelectionReason(
                                    selectionReason);
                }
            }
            else if(randomSampledCandidateIndex >= 0) {

                roundRobinStartingCandidateIndex =
                        -1;

                if(selectionReason == null
                        || !selectionReason
                        .toUpperCase()
                        .contains(
                                "RANDOM")) {

                    selectionReason =
                            "RANDOM_"
                            + normalizeSelectionReason(
                                    selectionReason);
                }
            }
            else {

                /*
                 * LEAST_LOADED and cloud least-loaded selection have no
                 * scheduler cursor or random sample.
                 */
                roundRobinStartingCandidateIndex =
                        -1;

                randomSampledCandidateIndex =
                        -1;
            }
        }

        NetworkStateSnapshot networkState =
                getTaskNetworkState(task);

        if (selectedVM != null) {    

            double currentCpuUtilization =
                    selectedVM
                            .getCloudletScheduler()
                            .getTotalUtilizationOfCpu(
                                    CloudSim.clock());

            double measuredSelectedAvailableCpu =
                    100.0
                    - currentCpuUtilization;

            if (selectedAvailableCpu < 0.0) {

                selectedAvailableCpu =
                        measuredSelectedAvailableCpu;
            }

            if (datacenterId
                    == SimSettings.CLOUD_DATACENTER_ID) {

                task.setAssociatedDatacenterId(
                        SimSettings.CLOUD_DATACENTER_ID);
            }
            else {

                task.setAssociatedDatacenterId(
                        selectedVM.getHost()
                                .getDatacenter()
                                .getId());
            }

            task.setAssociatedHostId(
                    selectedVM.getHost()
                            .getId());

            task.setAssociatedVmId(
                    selectedVM.getId());

            String selectedDestination =
                    determineSelectedDestination(
                            selectedVM);

            DecisionRecord decisionRecord =
                    createDecisionRecord(
                            task,
                            "SELECTED",
                            selectedDestination,
                            attemptedDestinationForDatacenter(
                                    datacenterId),
                            selectedVM.getId(),
                            selectedVM.getHost().getId(),
                            selectedVM.getHost()
                                    .getDatacenter()
                                    .getId(),
                            selectedCandidateIndex,
                            networkState,
                            requiredCpuPercent,
                            selectedVM.getMips(),
                            selectedVM.getNumberOfPes(),
                            candidateVmCount,
                            feasibleVmCount,
                            selectedAvailableCpu,
                            bestAvailableCpu,
                            selectionReason,
                            roundRobinStartingCandidateIndex,
                            randomSampledCandidateIndex,
                            candidateVmRecords);

            DecisionLogger.getInstance()
                    .logDecision(
                            decisionRecord);

            getCloudletList()
                    .add(task);

            bindCloudletToVm(
                    task.getCloudletId(),
                    selectedVM.getId());

            schedule(
                    getVmsToDatacentersMap()
                            .get(task.getVmId()),
                    delay,
                    CloudSimTags.CLOUDLET_SUBMIT,
                    task);

            SimLogger.getInstance()
                    .taskAssigned(
                            task.getCloudletId(),
                            selectedVM.getHost()
                                    .getDatacenter()
                                    .getId(),
                            selectedVM.getHost()
                                    .getId(),
                            selectedVM.getId(),
                            vmType);
        }
        else {

            String attemptedDestination =
                    datacenterId
                    == SimSettings.CLOUD_DATACENTER_ID
                    ? "CLOUD"
                    : "EDGE";

            DecisionRecord rejectedRecord =
                    createDecisionRecord(
                            task,
                            "NO_FEASIBLE_VM",
                            "NONE",
                            attemptedDestination,
                            -1,
                            -1,
                            -1,
                            -1,
                            networkState,
                            requiredCpuPercent,
                            -1.0,
                            -1,
                            candidateVmCount,
                            feasibleVmCount,
                            -1.0,
                            bestAvailableCpu,
                            selectionReason,
                            roundRobinStartingCandidateIndex,
                            randomSampledCandidateIndex,
                            candidateVmRecords);

            DecisionLogger.getInstance()
                    .logDecision(
                            rejectedRecord);

            SimLogger.getInstance()
                    .rejectedDueToVMCapacity(
                            task.getCloudletId(),
                            CloudSim.clock(),
                            vmType);
        }
    }

    private SchedulerDecision getLastSchedulerDecision() {

        if (!(SimManager.getInstance()
                .getEdgeOrchestrator()
                instanceof BasicEdgeOrchestrator)) {

            return null;
        }

        BasicEdgeOrchestrator basicOrchestrator =
                (BasicEdgeOrchestrator)
                SimManager.getInstance()
                        .getEdgeOrchestrator();

        return basicOrchestrator
                .getLastSchedulerDecision();
    }

    private NetworkStateSnapshot getTaskNetworkState(
            Task task) {

        NetworkStateSnapshot networkState =
                task.getResearchNetworkState();

        if (networkState != null) {
            return networkState;
        }

        return new NetworkStateSnapshot(
                "UNKNOWN",
                task.getSubmittedLocation()
                        .getServingWlanId(),
                -1,
                -1.0,
                -1.0,
                -1.0,
                task.getResearchUploadDelay(),
                task.getResearchUploadDelay() > 0,
                "NETWORK_STATE_UNAVAILABLE");
    }

    private double predictRequiredCpuPercent(
            Task task,
            Vm selectedVM) {

        if (!(task.getUtilizationModelCpu()
                instanceof CpuUtilizationModel_Custom)) {

            return -1.0;
        }

        CpuUtilizationModel_Custom cpuModel =
                (CpuUtilizationModel_Custom)
                task.getUtilizationModelCpu();

        if (selectedVM
                instanceof edu.boun.edgecloudsim.edge_server.EdgeVM) {

            edu.boun.edgecloudsim.edge_server.EdgeVM edgeVM =
                    (edu.boun.edgecloudsim.edge_server.EdgeVM)
                    selectedVM;

            return cpuModel.predictUtilization(
                    edgeVM.getVmType());
        }

        if (selectedVM
                instanceof edu.boun.edgecloudsim.cloud_server.CloudVM) {

            edu.boun.edgecloudsim.cloud_server.CloudVM cloudVM =
                    (edu.boun.edgecloudsim.cloud_server.CloudVM)
                    selectedVM;

            return cpuModel.predictUtilization(
                    cloudVM.getVmType());
        }

        return -1.0;
    }
    
    private double predictRequiredCpuPercent(
            Task task,
            SimSettings.VM_TYPES vmType) {

        if (!(task.getUtilizationModelCpu()
                instanceof CpuUtilizationModel_Custom)) {

            return -1.0;
        }

        CpuUtilizationModel_Custom cpuModel =
                (CpuUtilizationModel_Custom)
                task.getUtilizationModelCpu();

        return cpuModel.predictUtilization(vmType);
    }

    private void logNetworkRejectedDecision(
            Task task,
            String destination,
            int datacenterId,
            NetworkStateSnapshot networkState) {
    	
    	SimSettings.VM_TYPES targetVmType =
    	        datacenterId == SimSettings.CLOUD_DATACENTER_ID
    	        ? SimSettings.VM_TYPES.CLOUD_VM
    	        : SimSettings.VM_TYPES.EDGE_VM;

    	double requiredCpuPercent =
    	        predictRequiredCpuPercent(
    	                task,
    	                targetVmType);
    	
    	int vmType =
    	        datacenterId
    	        == SimSettings.CLOUD_DATACENTER_ID
    	        ? SimSettings.VM_TYPES.CLOUD_VM.ordinal()
    	        : SimSettings.VM_TYPES.EDGE_VM.ordinal();

        DecisionRecord rejectedRecord =
                createDecisionRecord(
                        task,
                        "UPLOAD_BANDWIDTH_REJECTED",
                        "NONE",
                        destination,
                        -1,
                        -1,
                        -1,
                        -1,
                        networkState,
                        requiredCpuPercent,
                        -1.0,
                        -1,
                        0,
                        0,
                        -1.0,
                        -1.0,
                        "NOT_SCHEDULED",
                        -1,
                        -1,
                        Collections
                                .<CandidateVmRecord>emptyList());

        DecisionLogger.getInstance()
                .logDecision(
                        rejectedRecord);
    }

    private String normalizeSelectionReason(
            String selectionReason) {

        if(selectionReason == null
                || selectionReason.trim().isEmpty()) {

            return "UNKNOWN";
        }

        return selectionReason
                .trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');
    }

    /**
     * Determines the destination tier from the VM actually selected
     * by the live scheduler.
     */
    private String determineSelectedDestination(
            Vm selectedVm) {

        if(selectedVm == null) {

            return "NONE";
        }

        if(selectedVm
                instanceof edu.boun.edgecloudsim.cloud_server.CloudVM) {

            return "CLOUD";
        }

        if(selectedVm
                instanceof edu.boun.edgecloudsim.edge_server.EdgeVM) {

            return "EDGE";
        }

        return "UNKNOWN";
    }

    private String attemptedDestinationForDatacenter(
            int datacenterId) {

        return datacenterId
                == SimSettings.CLOUD_DATACENTER_ID
                ? "CLOUD"
                : "EDGE";
    }

    /*
     * This method centralizes DecisionRecord construction so that
     * all decision outcomes use the same field order.
     */
    private DecisionRecord createDecisionRecord(
            Task task,
            String decisionOutcome,
            String destination,
            String attemptedDestination,
            int selectedVmId,
            int selectedHostId,
            int selectedDatacenterId,
            int selectedCandidateIndex,
            NetworkStateSnapshot networkState,
            double requiredCpuPercent,
            double selectedVmMips,
            int selectedVmCoreCount,
            int candidateVmCount,
            int feasibleVmCount,
            double selectedAvailableCpuPercent,
            double bestAvailableCpuPercent,
            String selectionReason,
            int roundRobinStartingCandidateIndex,
            int randomSampledCandidateIndex,
            List<CandidateVmRecord> candidateRecords) {

        double uploadDelayMs =
                networkState
                        .getUploadDelayMilliseconds();

        return new DecisionRecord(
                CloudSim.clock(),
                task.getCloudletId(),
                task.getMobileDeviceId(),
                String.valueOf(
                        task.getTaskType()),
                SimManager.getInstance()
                        .getEdgeOrchestrator()
                        .getPolicy(),
                decisionOutcome,

                destination,
                attemptedDestination,
                selectedVmId,
                selectedHostId,
                selectedDatacenterId,
                selectedCandidateIndex,

                task.getCloudletLength(),
                task.getNumberOfPes(),
                task.getCloudletFileSize(),
                task.getCloudletOutputSize(),

                networkState,

                requiredCpuPercent,

                selectedVmMips,
                selectedVmCoreCount,

                candidateVmCount,
                feasibleVmCount,
                selectedAvailableCpuPercent,
                bestAvailableCpuPercent,
                selectionReason,

                roundRobinStartingCandidateIndex,
                randomSampledCandidateIndex,

                candidateRecords);
    }

    private Task createTask(
            TaskProperty edgeTask) {

        UtilizationModel utilizationModel =
                new UtilizationModelFull();

        UtilizationModel utilizationModelCPU =
                getCpuUtilizationModel();

        Task task =
                new Task(
                        edgeTask.getMobileDeviceId(),
                        ++taskIdCounter,
                        edgeTask.getLength(),
                        edgeTask.getPesNumber(),
                        edgeTask.getInputFileSize(),
                        edgeTask.getOutputFileSize(),
                        utilizationModelCPU,
                        utilizationModel,
                        utilizationModel);

        task.setUserId(
                this.getId());

        task.setTaskType(
                edgeTask.getTaskType());

        if (utilizationModelCPU
                instanceof CpuUtilizationModel_Custom) {

            ((CpuUtilizationModel_Custom)
                    utilizationModelCPU)
                    .setTask(task);
        }

        return task;
    }
}
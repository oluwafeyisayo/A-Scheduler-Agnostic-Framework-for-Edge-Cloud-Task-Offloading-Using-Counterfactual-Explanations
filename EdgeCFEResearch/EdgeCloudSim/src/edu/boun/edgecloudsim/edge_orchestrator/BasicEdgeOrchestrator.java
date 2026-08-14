/*
 * Title:        EdgeCloudSim - Basic Edge Orchestrator implementation
 *
 * Description:
 * BasicEdgeOrchestrator implements basic VM-selection algorithms and
 * records the complete candidate VM state for scheduler replay and
 * counterfactual explanation.
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.edge_orchestrator;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.research.logging.CandidateVmRecord;
import edu.boun.edgecloudsim.edge_orchestrator.multiobjective.MultiObjectiveConfig;
import edu.boun.edgecloudsim.edge_orchestrator.multiobjective.MultiObjectiveScorer;
import edu.boun.edgecloudsim.edge_orchestrator.multiobjective.MultiObjectiveSelectionResult;
import edu.boun.edgecloudsim.edge_orchestrator.ppo.PpoConfig;
import edu.boun.edgecloudsim.edge_orchestrator.ppo.PpoScheduler;
import edu.boun.edgecloudsim.edge_orchestrator.ppo.PpoSelectionResult;
import edu.boun.edgecloudsim.network.DestinationNetworkState;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.NetworkStateSnapshot;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimUtils;

public class BasicEdgeOrchestrator extends EdgeOrchestrator {

    private int numberOfHost;
    private int lastSelectedHostIndex;
    private int[] lastSelectedVmIndexes;
    private SchedulerDecision lastSchedulerDecision;

    private final MultiObjectiveScorer multiObjectiveScorer =
            new MultiObjectiveScorer(
                    MultiObjectiveConfig.createDefault());

    /*
     * Shared deterministic PPO actor used by the live scheduler.
     *
     * PPO_BOOTSTRAP_V1 validates the integration path. It will later be
     * replaced by weights exported from the offline PPO trainer.
     */
    private final PpoScheduler ppoScheduler =
            new PpoScheduler(
                    PpoConfig.createBootstrap());

    private String lastDestinationSelectionReason =
            "NOT_SELECTED";

    private double lastEdgeDestinationCostMs =
            Double.POSITIVE_INFINITY;

    private double lastCloudDestinationCostMs =
            Double.POSITIVE_INFINITY;

    public BasicEdgeOrchestrator(String policy, String simScenario) {
        super(policy, simScenario);
    }

    @Override
    public void initialize() {
        numberOfHost = SimSettings.getInstance().getNumOfEdgeHosts();
        lastSelectedHostIndex = -1;
        lastSelectedVmIndexes = new int[numberOfHost];

        for (int i = 0; i < numberOfHost; i++) {
            lastSelectedVmIndexes[i] = -1;
        }
    }

    @Override
    public int getDeviceToOffload(
            Task task) {

        if(task == null) {

            throw new IllegalArgumentException(
                    "Task cannot be null.");
        }

        /*
         * SINGLE_TIER preserves the original EdgeCloudSim behavior.
         */
        if(simScenario.equals(
                "SINGLE_TIER")) {

            lastDestinationSelectionReason =
                    "SINGLE_TIER_EDGE";

            lastEdgeDestinationCostMs =
                    0.0;

            lastCloudDestinationCostMs =
                    Double.POSITIVE_INFINITY;

            return SimSettings.GENERIC_EDGE_DEVICE_ID;
        }

        List<CandidateVmRecord> candidates =
                collectCrossTierCandidates(
                        task);

        NetworkStateSnapshot networkStates =
                captureLiveNetworkStates(
                        task);

        LiveDestinationEvaluation edge =
                evaluateLiveDestination(
                        "EDGE",
                        task,
                        candidates,
                        networkStates
                        .getEdgeNetworkState());

        LiveDestinationEvaluation cloud =
                evaluateLiveDestination(
                        "CLOUD",
                        task,
                        candidates,
                        networkStates
                        .getCloudNetworkState());

        lastEdgeDestinationCostMs =
                edge.serviceCostMs;

        lastCloudDestinationCostMs =
                cloud.serviceCostMs;

        boolean edgeOperational =
                edge.isOperationallyFeasible();

        boolean cloudOperational =
                cloud.isOperationallyFeasible();

        if(edgeOperational
                && !cloudOperational) {

            lastDestinationSelectionReason =
                    "STATE_AWARE_EDGE_ONLY_FEASIBLE";

            return SimSettings.GENERIC_EDGE_DEVICE_ID;
        }

        if(!edgeOperational
                && cloudOperational) {

            lastDestinationSelectionReason =
                    "STATE_AWARE_CLOUD_ONLY_FEASIBLE";

            return SimSettings.CLOUD_DATACENTER_ID;
        }

        if(edgeOperational
                && cloudOperational) {

            if(edge.serviceCostMs
                    <= cloud.serviceCostMs) {

                lastDestinationSelectionReason =
                        edge.serviceCostMs
                        < cloud.serviceCostMs
                        ? "STATE_AWARE_EDGE_LOWER_SERVICE_COST"
                        : "STATE_AWARE_EQUAL_COST_EDGE_TIE_BREAK";

                return SimSettings.GENERIC_EDGE_DEVICE_ID;
            }

            lastDestinationSelectionReason =
                    "STATE_AWARE_CLOUD_LOWER_SERVICE_COST";

            return SimSettings.CLOUD_DATACENTER_ID;
        }

        /*
         * EdgeCloudSim requires a concrete next-hop identifier even when
         * neither tier is operationally feasible. Use a deterministic
         * attempted tier so the failure and its provenance can still be
         * logged. Prefer a network-feasible tier; otherwise prefer EDGE.
         */
        if(edge.networkFeasible
                && !cloud.networkFeasible) {

            lastDestinationSelectionReason =
                    "STATE_AWARE_NO_COMPUTE_FEASIBLE_VM_EDGE_ATTEMPT";

            return SimSettings.GENERIC_EDGE_DEVICE_ID;
        }

        if(!edge.networkFeasible
                && cloud.networkFeasible) {

            lastDestinationSelectionReason =
                    "STATE_AWARE_NO_COMPUTE_FEASIBLE_VM_CLOUD_ATTEMPT";

            return SimSettings.CLOUD_DATACENTER_ID;
        }

        lastDestinationSelectionReason =
                "STATE_AWARE_NO_OPERATIONAL_DESTINATION_EDGE_FALLBACK";

        return SimSettings.GENERIC_EDGE_DEVICE_ID;
    }


    @Override
    public Vm getVmToOffload(
            Task task,
            int deviceId) {

        Vm selectedVm;

        lastSchedulerDecision =
                null;

        /*
         * Preserve the existing live scheduler behavior.
         */
        if(deviceId
                == SimSettings.CLOUD_DATACENTER_ID) {

            selectedVm =
                    selectCloudVm(
                            task);
        }
        else if(simScenario.equals(
                "TWO_TIER_WITH_EO")) {

            selectedVm =
                    selectVmOnLoadBalancer(
                            task);
        }
        else {

            selectedVm =
                    selectVmOnHost(
                            task);
        }

        /*
         * The tier-specific selection method creates the original
         * scheduling result. Replace only its candidate evidence with
         * the complete EDGE+CLOUD search space.
         */
        SchedulerDecision tierDecision =
                lastSchedulerDecision;

        List<CandidateVmRecord> crossTierCandidates =
                collectCrossTierCandidates(
                        task);

        int selectedCandidateIndex =
                findSelectedCandidateIndex(
                        selectedVm,
                        crossTierCandidates);

        int feasibleCandidateCount =
                countFeasibleCandidates(
                        crossTierCandidates);

        double bestAvailableCpu =
                findBestAvailableCpu(
                        crossTierCandidates);

        double selectedAvailableCpu =
                selectedVm == null
                ? -1.0
                : getAvailableCpu(
                        selectedVm);

        String tierSelectionReason =
                tierDecision == null
                ? "UNKNOWN"
                : tierDecision
                .getSelectionReason();

        String selectionReason =
                lastDestinationSelectionReason
                + "|"
                + tierSelectionReason;

        int roundRobinStartingCandidateIndex =
                tierDecision == null
                ? -1
                : remapCandidateIndexToCrossTierSet(
                        tierDecision
                        .getRoundRobinStartingCandidateIndex(),
                        tierDecision,
                        crossTierCandidates);

        /*
         * ROUND_ROBIN uses least-loaded selection in the live CLOUD
         * branch, so the tier decision does not naturally contain an
         * EDGE cursor. Capture the latent EDGE cursor without advancing
         * it. This makes a later CLOUD-to-EDGE counterfactual replay
         * deterministic and prevents valid CLOUD decisions from being
         * treated as missing provenance.
         */
        if(policy.equalsIgnoreCase(
                "ROUND_ROBIN")
                && roundRobinStartingCandidateIndex < 0
                && !crossTierCandidates.isEmpty()) {

            int firstHostPosition =
                    numberOfHost <= 0
                    ? 0
                    : Math.floorMod(
                            lastSelectedHostIndex + 1,
                            numberOfHost);

            roundRobinStartingCandidateIndex =
                    findFirstRoundRobinCandidateIndex(
                            firstHostPosition,
                            crossTierCandidates);
        }

        int randomSampledCandidateIndex =
                tierDecision == null
                ? -1
                : remapCandidateIndexToCrossTierSet(
                        tierDecision
                        .getRandomSampledCandidateIndex(),
                        tierDecision,
                        crossTierCandidates);

        lastSchedulerDecision =
                new SchedulerDecision(
                        selectedVm,
                        crossTierCandidates.size(),
                        feasibleCandidateCount,
                        selectedAvailableCpu,
                        bestAvailableCpu,
                        selectionReason,
                        selectedCandidateIndex,
                        roundRobinStartingCandidateIndex,
                        randomSampledCandidateIndex,
                        crossTierCandidates);

        return selectedVm;
    }

    private CloudVM selectCloudVm(
            Task task) {

        String normalizedPolicy =
                normalizePolicyName(
                        policy);

        CloudVM selectedVm =
                null;

        List<CandidateVmRecord> candidateRecords =
                new ArrayList<CandidateVmRecord>();

        List<CloudVM> candidateVms =
                new ArrayList<CloudVM>();

        double highestAvailableCapacity =
                -1.0;

        double selectedAvailableCapacity =
                -1.0;

        int feasibleVmCount =
                0;

        int randomSampledCandidateIndex =
                -1;

        List<Host> hostList =
                SimManager.getInstance()
                .getCloudServerManager()
                .getDatacenter()
                .getHostList();

        for(int hostIndex = 0;
                hostIndex < hostList.size();
                hostIndex++) {

            List<CloudVM> vmArray =
                    SimManager.getInstance()
                    .getCloudServerManager()
                    .getVmList(
                            hostIndex);

            if(vmArray == null
                    || vmArray.isEmpty()) {

                continue;
            }

            for(CloudVM candidateVm
                    : vmArray) {

                double requiredCpu =
                        getRequiredCpu(
                                task,
                                candidateVm);

                double availableCpu =
                        getAvailableCpu(
                                candidateVm);

                boolean feasible =
                        isVmFeasible(
                                requiredCpu,
                                availableCpu);

                int candidateIndex =
                        candidateRecords.size();

                candidateRecords.add(
                        createCandidateRecord(
                                candidateIndex,
                                candidateVm,
                                "CLOUD",
                                availableCpu,
                                feasible,
                                determineCpuInfeasibilityReason(
                                        requiredCpu,
                                        availableCpu)));

                candidateVms.add(
                        candidateVm);

                if(feasible) {

                    feasibleVmCount++;

                    highestAvailableCapacity =
                            Math.max(
                                    highestAvailableCapacity,
                                    availableCpu);
                }
            }
        }

        String selectionReason;

        /*
         * RANDOM samples from the complete candidate set, not from the
         * feasible subset. The sampled index is therefore preserved even
         * when the sampled VM is infeasible and no VM is selected.
         */
        if(normalizedPolicy.equals(
                "RANDOM")) {

            if(candidateRecords.isEmpty()) {

                selectionReason =
                        "NO_CANDIDATE_VM";
            }
            else {

                int sampledPosition =
                        SimUtils.getRandomNumber(
                                0,
                                candidateRecords.size() - 1);

                CandidateVmRecord sampledRecord =
                        candidateRecords.get(
                                sampledPosition);

                randomSampledCandidateIndex =
                        sampledRecord.getCandidateIndex();

                if(sampledRecord.isFeasible()) {

                    selectedVm =
                            findCloudVmByCandidateRecord(
                                    sampledRecord,
                                    candidateVms);

                    if(selectedVm != null) {

                        selectedAvailableCapacity =
                                getAvailableCpu(
                                        selectedVm);

                        selectionReason =
                                "RANDOM_SAMPLED_FEASIBLE_VM";
                    }
                    else {

                        selectionReason =
                                "RANDOM_SAMPLED_VM_NOT_FOUND";
                    }
                }
                else {

                    selectionReason =
                            "RANDOM_SAMPLED_INFEASIBLE_VM";
                }
            }
        }
        else if(normalizedPolicy.equals(
                "MULTI_OBJECTIVE")) {

            MultiObjectiveSelectionResult result =
                    selectMultiObjectiveCandidate(
                            task,
                            candidateRecords,
                            "CLOUD");

            CandidateVmRecord selectedRecord =
                    findCandidateRecordByIndex(
                            candidateRecords,
                            result.getSelectedCandidateIndex());

            selectedVm =
                    findCloudVmByCandidateRecord(
                            selectedRecord,
                            candidateVms);

            if(selectedVm != null) {

                selectedAvailableCapacity =
                        getAvailableCpu(
                                selectedVm);
            }

            selectionReason =
                    buildMultiObjectiveSelectionReason(
                            result);
        }
        else if(normalizedPolicy.equals(
                "PPO")) {

            PpoSelectionResult result =
                    selectPpoCandidate(
                            task,
                            candidateRecords,
                            "CLOUD");

            CandidateVmRecord selectedRecord =
                    findCandidateRecordByIndex(
                            candidateRecords,
                            result.getSelectedCandidateIndex());

            selectedVm =
                    findCloudVmByCandidateRecord(
                            selectedRecord,
                            candidateVms);

            if(selectedVm != null) {

                selectedAvailableCapacity =
                        getAvailableCpu(
                                selectedVm);
            }

            selectionReason =
                    buildPpoSelectionReason(
                            result);
        }
        else {

            /*
             * Preserve the existing cloud least-loaded behavior for the
             * remaining policies.
             */
            for(int index = 0;
                    index < candidateRecords.size();
                    index++) {

                CandidateVmRecord candidateRecord =
                        candidateRecords.get(
                                index);

                if(!candidateRecord.isFeasible()) {

                    continue;
                }

                double availableCpu =
                        candidateRecord
                        .getAvailableCpuPercent();

                if(selectedVm == null
                        || availableCpu
                        > selectedAvailableCapacity) {

                    selectedVm =
                            candidateVms.get(
                                    index);

                    selectedAvailableCapacity =
                            availableCpu;
                }
            }

            selectionReason =
                    selectedVm == null
                    ? "NO_FEASIBLE_CLOUD_VM"
                    : "CLOUD_MAX_AVAILABLE_CPU";
        }

        int selectedCandidateIndex =
                findSelectedCandidateIndex(
                        selectedVm,
                        candidateRecords);

        lastSchedulerDecision =
                new SchedulerDecision(
                        selectedVm,
                        candidateRecords.size(),
                        feasibleVmCount,
                        selectedAvailableCapacity,
                        highestAvailableCapacity,
                        selectionReason,
                        selectedCandidateIndex,
                        -1,
                        randomSampledCandidateIndex,
                        candidateRecords);

        return selectedVm;
    }

    public EdgeVM selectVmOnHost(Task task) {

        String normalizedPolicy =
                normalizePolicyName(
                        policy);

        EdgeVM selectedVm = null;

        Location deviceLocation = SimManager.getInstance()
                .getMobilityModel()
                .getLocation(task.getMobileDeviceId(), CloudSim.clock());

        int relatedHostId = deviceLocation.getServingWlanId();

        List<EdgeVM> vmArray = SimManager.getInstance()
                .getEdgeServerManager()
                .getVmList(relatedHostId);

        List<CandidateVmRecord> candidateRecords = new ArrayList<CandidateVmRecord>();
        List<EdgeVM> feasibleVmList = new ArrayList<EdgeVM>();

        int feasibleVmCount = 0;
        double bestAvailableCapacity = -1.0;
        double selectedAvailableCapacity = -1.0;

        if (vmArray != null) {
            for (EdgeVM candidateVm : vmArray) {
                double requiredCpu = getRequiredCpu(task, candidateVm);
                double availableCpu = getAvailableCpu(candidateVm);
                boolean feasible = isVmFeasible(requiredCpu, availableCpu);
                int candidateIndex = candidateRecords.size();

                candidateRecords.add(createCandidateRecord(
                        candidateIndex,
                        candidateVm,
                        "EDGE",
                        availableCpu,
                        feasible,
                        determineCpuInfeasibilityReason(requiredCpu, availableCpu)));

                if (feasible) {
                    feasibleVmCount++;
                    feasibleVmList.add(candidateVm);
                    bestAvailableCapacity = Math.max(bestAvailableCapacity, availableCpu);
                }
            }
        }

        String selectionReason =
                "UNSUPPORTED_POLICY:"
                + normalizedPolicy;

        if("MULTI_OBJECTIVE".equals(
                normalizedPolicy)) {

            System.out.println(
                    "[MULTI_OBJECTIVE Live Path] "
                    + "method=selectVmOnHost"
                    + ", taskId="
                    + task.getCloudletId()
                    + ", candidateCount="
                    + candidateRecords.size());
        }

        int roundRobinStartingCandidateIndex =
                -1;

        int randomSampledCandidateIndex =
                -1;

        if (normalizedPolicy.equals("RANDOM")) {

            if (!candidateRecords.isEmpty()) {

                int sampledPosition =
                        SimUtils.getRandomNumber(
                                0,
                                candidateRecords.size() - 1);

                CandidateVmRecord sampledRecord =
                        candidateRecords.get(
                                sampledPosition);

                randomSampledCandidateIndex =
                        sampledRecord.getCandidateIndex();

                if (sampledRecord.isFeasible()) {

                    selectedVm =
                            findVmByCandidateRecord(
                                    sampledRecord,
                                    vmArray);

                    if (selectedVm != null) {

                        selectedAvailableCapacity =
                                getAvailableCpu(
                                        selectedVm);

                        selectionReason =
                                "RANDOM_SAMPLED_FEASIBLE_VM";
                    }
                    else {

                        selectionReason =
                                "RANDOM_SAMPLED_VM_NOT_FOUND";
                    }
                }
                else {

                    selectionReason =
                            "RANDOM_SAMPLED_INFEASIBLE_VM";
                }
            }
            else {

                selectionReason =
                        "NO_CANDIDATE_VM";
            }
        }
        else if(normalizedPolicy.equals(
                "MULTI_OBJECTIVE")) {

            MultiObjectiveSelectionResult result =
                    selectMultiObjectiveCandidate(
                            task,
                            candidateRecords,
                            "EDGE");

            CandidateVmRecord selectedRecord =
                    findCandidateRecordByIndex(
                            candidateRecords,
                            result.getSelectedCandidateIndex());

            selectedVm =
                    findVmByCandidateRecord(
                            selectedRecord,
                            vmArray);

            if(selectedVm != null) {

                selectedAvailableCapacity =
                        getAvailableCpu(
                                selectedVm);
            }

            selectionReason =
                    buildMultiObjectiveSelectionReason(
                            result);
        }
        else if(normalizedPolicy.equals(
                "PPO")) {

            PpoSelectionResult result =
                    selectPpoCandidate(
                            task,
                            candidateRecords,
                            "EDGE");

            CandidateVmRecord selectedRecord =
                    findCandidateRecordByIndex(
                            candidateRecords,
                            result.getSelectedCandidateIndex());

            selectedVm =
                    findVmByCandidateRecord(
                            selectedRecord,
                            vmArray);

            if(selectedVm != null) {

                selectedAvailableCapacity =
                        getAvailableCpu(
                                selectedVm);
            }

            selectionReason =
                    buildPpoSelectionReason(
                            result);
        }
        else if (normalizedPolicy.equals("LEAST_LOADED")) {
            for (EdgeVM candidateVm : feasibleVmList) {
                double availableCpu = getAvailableCpu(candidateVm);

                if (selectedVm == null || availableCpu > selectedAvailableCapacity) {
                    selectedVm = candidateVm;
                    selectedAvailableCapacity = availableCpu;
                }
            }

            selectionReason = selectedVm == null
                    ? "NO_FEASIBLE_VM"
                    : "MAX_AVAILABLE_CPU";
        }
        else if (normalizedPolicy.equals("BEST_FIT")) {
            double selectedVmCapacity = Double.MAX_VALUE;

            for (EdgeVM candidateVm : feasibleVmList) {
                double availableCpu = getAvailableCpu(candidateVm);

                if (availableCpu < selectedVmCapacity) {
                    selectedVm = candidateVm;
                    selectedVmCapacity = availableCpu;
                    selectedAvailableCapacity = availableCpu;
                }
            }

            selectionReason = selectedVm == null
                    ? "NO_FEASIBLE_VM"
                    : "BEST_FIT_AVAILABLE_CPU";
        }
        else if (normalizedPolicy.equals("FIRST_FIT")) {
            if (!feasibleVmList.isEmpty()) {
                selectedVm = feasibleVmList.get(0);
                selectedAvailableCapacity = getAvailableCpu(selectedVm);
                selectionReason = "FIRST_FEASIBLE_VM";
            }
            else {
                selectionReason = "NO_FEASIBLE_VM";
            }
        }
        else if (normalizedPolicy.equals("ROUND_ROBIN")) {

            if (vmArray != null
                    && !vmArray.isEmpty()) {

                int firstVmPosition =
                        (lastSelectedVmIndexes[relatedHostId] + 1)
                        % vmArray.size();

                EdgeVM firstCandidate =
                        vmArray.get(
                                firstVmPosition);

                roundRobinStartingCandidateIndex =
                        findSelectedCandidateIndex(
                                firstCandidate,
                                candidateRecords);

                int tries =
                        0;

                while (tries < vmArray.size()) {

                    lastSelectedVmIndexes[relatedHostId] =
                            (lastSelectedVmIndexes[relatedHostId] + 1)
                            % vmArray.size();

                    EdgeVM candidateVm =
                            vmArray.get(
                                    lastSelectedVmIndexes[
                                            relatedHostId]);

                    double requiredCpu =
                            getRequiredCpu(
                                    task,
                                    candidateVm);

                    double availableCpu =
                            getAvailableCpu(
                                    candidateVm);

                    if (isVmFeasible(
                            requiredCpu,
                            availableCpu)) {

                        selectedVm =
                                candidateVm;

                        selectedAvailableCapacity =
                                availableCpu;

                        break;
                    }

                    tries++;
                }
            }

            selectionReason =
                    selectedVm == null
                    ? "NO_FEASIBLE_VM"
                    : "ROUND_ROBIN_FEASIBLE_VM";
        }

        int selectedCandidateIndex = findSelectedCandidateIndex(
                selectedVm,
                candidateRecords);

        lastSchedulerDecision = new SchedulerDecision(
                selectedVm,
                candidateRecords.size(),
                feasibleVmCount,
                selectedAvailableCapacity,
                bestAvailableCapacity,
                selectionReason,
                selectedCandidateIndex,
                roundRobinStartingCandidateIndex,
                randomSampledCandidateIndex,
                candidateRecords);

        return selectedVm;
    }

    public EdgeVM selectVmOnLoadBalancer(Task task) {

        String normalizedPolicy =
                normalizePolicyName(
                        policy);

        EdgeVM selectedVm = null;

        List<CandidateVmRecord> candidateRecords = new ArrayList<CandidateVmRecord>();
        List<EdgeVM> feasibleVmList = new ArrayList<EdgeVM>();

        int feasibleVmCount = 0;
        double bestAvailableCapacity = -1.0;
        double selectedAvailableCapacity = -1.0;

        for (int hostIndex = 0; hostIndex < numberOfHost; hostIndex++) {
            List<EdgeVM> vmArray = SimManager.getInstance()
                    .getEdgeServerManager()
                    .getVmList(hostIndex);

            if (vmArray == null || vmArray.isEmpty()) {
                continue;
            }

            for (EdgeVM candidateVm : vmArray) {
                double requiredCpu = getRequiredCpu(task, candidateVm);
                double availableCpu = getAvailableCpu(candidateVm);
                boolean feasible = isVmFeasible(requiredCpu, availableCpu);
                int candidateIndex = candidateRecords.size();

                candidateRecords.add(createCandidateRecord(
                        candidateIndex,
                        candidateVm,
                        "EDGE",
                        availableCpu,
                        feasible,
                        determineCpuInfeasibilityReason(requiredCpu, availableCpu)));

                if (feasible) {
                    feasibleVmCount++;
                    feasibleVmList.add(candidateVm);
                    bestAvailableCapacity = Math.max(bestAvailableCapacity, availableCpu);
                }
            }
        }

        String selectionReason =
                "UNSUPPORTED_POLICY:"
                + normalizedPolicy;

        if("MULTI_OBJECTIVE".equals(
                normalizedPolicy)) {

            System.out.println(
                    "[MULTI_OBJECTIVE Live Path] "
                    + "method=selectVmOnLoadBalancer"
                    + ", taskId="
                    + task.getCloudletId()
                    + ", candidateCount="
                    + candidateRecords.size());
        }

        int roundRobinStartingCandidateIndex =
                -1;

        int randomSampledCandidateIndex =
                -1;

        if (normalizedPolicy.equals("RANDOM")) {

            if (!candidateRecords.isEmpty()) {

                int sampledPosition =
                        SimUtils.getRandomNumber(
                                0,
                                candidateRecords.size() - 1);

                CandidateVmRecord sampledRecord =
                        candidateRecords.get(
                                sampledPosition);

                randomSampledCandidateIndex =
                        sampledRecord.getCandidateIndex();

                if (sampledRecord.isFeasible()) {

                    selectedVm =
                            findVmByCandidateRecord(
                                    sampledRecord,
                                    sampledRecord.getDestination()
                                    .equalsIgnoreCase("EDGE")
                                    ? getAllEdgeVms()
                                    : new ArrayList<EdgeVM>());

                    if (selectedVm != null) {

                        selectedAvailableCapacity =
                                getAvailableCpu(
                                        selectedVm);

                        selectionReason =
                                "RANDOM_SAMPLED_FEASIBLE_VM";
                    }
                    else {

                        selectionReason =
                                "RANDOM_SAMPLED_VM_NOT_FOUND";
                    }
                }
                else {

                    selectionReason =
                            "RANDOM_SAMPLED_INFEASIBLE_VM";
                }
            }
            else {

                selectionReason =
                        "NO_CANDIDATE_VM";
            }
        }
        else if(normalizedPolicy.equals(
                "MULTI_OBJECTIVE")) {

            MultiObjectiveSelectionResult result =
                    selectMultiObjectiveCandidate(
                            task,
                            candidateRecords,
                            "EDGE");

            CandidateVmRecord selectedRecord =
                    findCandidateRecordByIndex(
                            candidateRecords,
                            result.getSelectedCandidateIndex());

            selectedVm =
                    findVmByCandidateRecord(
                            selectedRecord,
                            getAllEdgeVms());

            if(selectedVm != null) {

                selectedAvailableCapacity =
                        getAvailableCpu(
                                selectedVm);
            }

            selectionReason =
                    buildMultiObjectiveSelectionReason(
                            result);
        }
        else if(normalizedPolicy.equals(
                "PPO")) {

            PpoSelectionResult result =
                    selectPpoCandidate(
                            task,
                            candidateRecords,
                            "EDGE");

            CandidateVmRecord selectedRecord =
                    findCandidateRecordByIndex(
                            candidateRecords,
                            result.getSelectedCandidateIndex());

            selectedVm =
                    findVmByCandidateRecord(
                            selectedRecord,
                            getAllEdgeVms());

            if(selectedVm != null) {

                selectedAvailableCapacity =
                        getAvailableCpu(
                                selectedVm);
            }

            selectionReason =
                    buildPpoSelectionReason(
                            result);
        }
        else if (normalizedPolicy.equals("LEAST_LOADED")) {
            for (EdgeVM candidateVm : feasibleVmList) {
                double availableCpu = getAvailableCpu(candidateVm);

                if (selectedVm == null || availableCpu > selectedAvailableCapacity) {
                    selectedVm = candidateVm;
                    selectedAvailableCapacity = availableCpu;
                }
            }

            selectionReason = selectedVm == null
                    ? "NO_FEASIBLE_VM"
                    : "MAX_AVAILABLE_CPU";
        }
        else if (normalizedPolicy.equals("BEST_FIT")) {
            double selectedVmCapacity = Double.MAX_VALUE;

            for (EdgeVM candidateVm : feasibleVmList) {
                double availableCpu = getAvailableCpu(candidateVm);

                if (availableCpu < selectedVmCapacity) {
                    selectedVm = candidateVm;
                    selectedVmCapacity = availableCpu;
                    selectedAvailableCapacity = availableCpu;
                }
            }

            selectionReason = selectedVm == null
                    ? "NO_FEASIBLE_VM"
                    : "BEST_FIT_AVAILABLE_CPU";
        }
        else if (normalizedPolicy.equals("FIRST_FIT")) {
            if (!feasibleVmList.isEmpty()) {
                selectedVm = feasibleVmList.get(0);
                selectedAvailableCapacity = getAvailableCpu(selectedVm);
                selectionReason = "FIRST_FEASIBLE_VM";
            }
            else {
                selectionReason = "NO_FEASIBLE_VM";
            }
        }
        else if (normalizedPolicy.equals("ROUND_ROBIN")) {

            int firstHostPosition =
                    (lastSelectedHostIndex + 1)
                    % numberOfHost;

            roundRobinStartingCandidateIndex =
                    findFirstRoundRobinCandidateIndex(
                            firstHostPosition,
                            candidateRecords);

            int hostCheckCounter =
                    0;

            while (selectedVm == null
                    && hostCheckCounter < numberOfHost) {

                lastSelectedHostIndex =
                        (lastSelectedHostIndex + 1)
                        % numberOfHost;

                List<EdgeVM> vmArray =
                        SimManager.getInstance()
                        .getEdgeServerManager()
                        .getVmList(
                                lastSelectedHostIndex);

                if (vmArray == null
                        || vmArray.isEmpty()) {

                    hostCheckCounter++;
                    continue;
                }

                int tries =
                        0;

                while (tries < vmArray.size()) {

                    lastSelectedVmIndexes[
                            lastSelectedHostIndex] =
                            (lastSelectedVmIndexes[
                                    lastSelectedHostIndex] + 1)
                            % vmArray.size();

                    EdgeVM candidateVm =
                            vmArray.get(
                                    lastSelectedVmIndexes[
                                            lastSelectedHostIndex]);

                    double requiredCpu =
                            getRequiredCpu(
                                    task,
                                    candidateVm);

                    double availableCpu =
                            getAvailableCpu(
                                    candidateVm);

                    if (isVmFeasible(
                            requiredCpu,
                            availableCpu)) {

                        selectedVm =
                                candidateVm;

                        selectedAvailableCapacity =
                                availableCpu;

                        break;
                    }

                    tries++;
                }

                hostCheckCounter++;
            }

            selectionReason =
                    selectedVm == null
                    ? "NO_FEASIBLE_VM"
                    : "ROUND_ROBIN_FEASIBLE_VM";
        }

        int selectedCandidateIndex = findSelectedCandidateIndex(
                selectedVm,
                candidateRecords);

        lastSchedulerDecision = new SchedulerDecision(
                selectedVm,
                candidateRecords.size(),
                feasibleVmCount,
                selectedAvailableCapacity,
                bestAvailableCapacity,
                selectionReason,
                selectedCandidateIndex,
                roundRobinStartingCandidateIndex,
                randomSampledCandidateIndex,
                candidateRecords);

        return selectedVm;
    }

    private NetworkStateSnapshot captureLiveNetworkStates(
            Task task) {

        NetworkModel networkModel =
                SimManager.getInstance()
                .getNetworkModel();

        if(networkModel == null) {

            int servingWlanId =
                    SimManager.getInstance()
                    .getMobilityModel()
                    .getLocation(
                            task.getMobileDeviceId(),
                            CloudSim.clock())
                    .getServingWlanId();

            return new NetworkStateSnapshot(
                    DestinationNetworkState.unavailable(
                            "EDGE",
                            servingWlanId,
                            "NETWORK_MODEL_UNAVAILABLE"),
                    DestinationNetworkState.unavailable(
                            "CLOUD",
                            servingWlanId,
                            "NETWORK_MODEL_UNAVAILABLE"),
                    "EDGE");
        }

        return networkModel.getNetworkStates(
                task.getMobileDeviceId(),
                task,
                "EDGE");
    }

    private LiveDestinationEvaluation evaluateLiveDestination(
            String destination,
            Task task,
            List<CandidateVmRecord> candidates,
            DestinationNetworkState networkState) {

        int destinationCandidateCount =
                0;

        int computeFeasibleCandidateCount =
                0;

        double bestProcessingTimeMs =
                Double.POSITIVE_INFINITY;

        if(candidates != null) {

            for(CandidateVmRecord candidate
                    : candidates) {

                if(candidate == null
                        || !destination.equalsIgnoreCase(
                                candidate.getDestination())) {

                    continue;
                }

                destinationCandidateCount++;

                if(!candidate.isFeasible()
                        || candidate.getVmMips() <= 0.0) {

                    continue;
                }

                computeFeasibleCandidateCount++;

                double processingTimeMs =
                        (
                                task.getCloudletLength()
                                / candidate.getVmMips()
                        )
                        * 1000.0;

                if(processingTimeMs
                        < bestProcessingTimeMs) {

                    bestProcessingTimeMs =
                            processingTimeMs;
                }
            }
        }

        boolean networkFeasible =
                networkState != null
                && networkState.isNetworkFeasible()
                && isFinitePositive(
                        networkState
                        .getAvailableBandwidthMbps())
                && isFiniteNonnegative(
                        networkState
                        .getUploadDelayMilliseconds());

        double networkDelayMs =
                networkFeasible
                ? networkState
                .getUploadDelayMilliseconds()
                : Double.POSITIVE_INFINITY;

        double serviceCostMs =
                networkFeasible
                && computeFeasibleCandidateCount > 0
                && isFiniteNonnegative(
                        bestProcessingTimeMs)
                ? networkDelayMs
                  + bestProcessingTimeMs
                : Double.POSITIVE_INFINITY;

        return new LiveDestinationEvaluation(
                destination,
                destinationCandidateCount,
                computeFeasibleCandidateCount,
                networkFeasible,
                networkDelayMs,
                bestProcessingTimeMs,
                serviceCostMs);
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

    public String getLastDestinationSelectionReason() {

        return lastDestinationSelectionReason;
    }

    public double getLastEdgeDestinationCostMs() {

        return lastEdgeDestinationCostMs;
    }

    public double getLastCloudDestinationCostMs() {

        return lastCloudDestinationCostMs;
    }

    private static final class LiveDestinationEvaluation {

        private final String destination;
        private final int candidateCount;
        private final int computeFeasibleCandidateCount;
        private final boolean networkFeasible;
        private final double networkDelayMs;
        private final double bestProcessingTimeMs;
        private final double serviceCostMs;

        private LiveDestinationEvaluation(
                String destination,
                int candidateCount,
                int computeFeasibleCandidateCount,
                boolean networkFeasible,
                double networkDelayMs,
                double bestProcessingTimeMs,
                double serviceCostMs) {

            this.destination =
                    destination;

            this.candidateCount =
                    candidateCount;

            this.computeFeasibleCandidateCount =
                    computeFeasibleCandidateCount;

            this.networkFeasible =
                    networkFeasible;

            this.networkDelayMs =
                    networkDelayMs;

            this.bestProcessingTimeMs =
                    bestProcessingTimeMs;

            this.serviceCostMs =
                    serviceCostMs;
        }

        private boolean isOperationallyFeasible() {

            return networkFeasible
                    && computeFeasibleCandidateCount > 0
                    && !Double.isInfinite(
                            serviceCostMs);
        }

        @Override
        public String toString() {

            return "LiveDestinationEvaluation{"
                    + "destination='"
                    + destination
                    + '\''
                    + ", candidateCount="
                    + candidateCount
                    + ", computeFeasibleCandidateCount="
                    + computeFeasibleCandidateCount
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

    private static String normalizePolicyName(
            String value) {

        if(value == null) {

            return "";
        }

        return value
                .trim()
                .toUpperCase();
    }

    private MultiObjectiveSelectionResult
            selectMultiObjectiveCandidate(
                    Task task,
                    List<CandidateVmRecord> candidates,
                    String destination) {

        NetworkStateSnapshot networkStates =
                captureLiveNetworkStates(
                        task);

        DestinationNetworkState destinationState =
                "CLOUD".equalsIgnoreCase(
                        destination)
                ? networkStates.getCloudNetworkState()
                : networkStates.getEdgeNetworkState();

        double uploadDelayMs =
                destinationState == null
                || !destinationState.isNetworkFeasible()
                ? Double.POSITIVE_INFINITY
                : destinationState
                .getUploadDelayMilliseconds();

        return multiObjectiveScorer.select(
                candidates,
                task.getCloudletLength(),
                uploadDelayMs);
    }

    private String buildMultiObjectiveSelectionReason(
            MultiObjectiveSelectionResult result) {

        if(result == null
                || !result.hasSelection()) {

            return "MULTI_OBJECTIVE_NO_FEASIBLE_VM";
        }

        return "MULTI_OBJECTIVE_MIN_SCORE"
                + "|CONFIG="
                + MultiObjectiveConfig
                .DEFAULT_CONFIGURATION_ID
                + "|SCORE="
                + result.getSelectedScore()
                .getTotalScore()
                + "|LATENCY_MS="
                + result.getSelectedScore()
                .getTotalLatencyMs()
                + "|ENERGY_J="
                + result.getSelectedScore()
                .getTotalEnergyJoules()
                + "|LOAD="
                + result.getSelectedScore()
                .getLoadPenalty();
    }

    /**
     * Runs deterministic PPO actor inference over the candidates of one
     * destination tier.
     */
    private PpoSelectionResult selectPpoCandidate(
            Task task,
            List<CandidateVmRecord> candidates,
            String destination) {

        NetworkStateSnapshot networkStates =
                captureLiveNetworkStates(
                        task);

        DestinationNetworkState destinationState =
                "CLOUD".equalsIgnoreCase(
                        destination)
                ? networkStates.getCloudNetworkState()
                : networkStates.getEdgeNetworkState();

        double requiredCpuPercent =
                0.0;

        if(candidates != null
                && !candidates.isEmpty()) {

            CandidateVmRecord firstCandidate =
                    candidates.get(0);

            Vm candidateVm =
                    findVmForCandidateRecord(
                            firstCandidate);

            if(candidateVm instanceof EdgeVM) {

                requiredCpuPercent =
                        getRequiredCpu(
                                task,
                                (EdgeVM)candidateVm);
            }
            else if(candidateVm instanceof CloudVM) {

                requiredCpuPercent =
                        getRequiredCpu(
                                task,
                                (CloudVM)candidateVm);
            }
        }

        return ppoScheduler.selectLive(
                task.getCloudletLength(),
                task.getCloudletFileSize(),
                requiredCpuPercent,
                destinationState,
                candidates);
    }

    private String buildPpoSelectionReason(
            PpoSelectionResult result) {

        if(result == null
                || !result.hasSelection()) {

            return "PPO_NO_FEASIBLE_VM"
                    + "|MODEL="
                    + ppoScheduler
                    .getConfig()
                    .getModelId()
                    + "|SCHEMA="
                    + ppoScheduler
                    .getConfig()
                    .getFeatureSchemaId();
        }

        return "PPO_DETERMINISTIC_ARGMAX"
                + "|MODEL="
                + result.getModelId()
                + "|SCHEMA="
                + result.getFeatureSchemaId()
                + "|CANDIDATE_INDEX="
                + result.getSelectedCandidateIndex()
                + "|LOGIT="
                + result.getSelectedLogit()
                + "|PROBABILITY="
                + result.getSelectedProbability()
                + "|TRAINING_STATUS=BOOTSTRAP_NOT_TRAINED";
    }

    /**
     * Resolves a live VM from its immutable candidate provenance record.
     */
    private Vm findVmForCandidateRecord(
            CandidateVmRecord candidateRecord) {

        if(candidateRecord == null) {

            return null;
        }

        if("CLOUD".equalsIgnoreCase(
                candidateRecord.getDestination())) {

            List<Host> cloudHosts =
                    SimManager.getInstance()
                    .getCloudServerManager()
                    .getDatacenter()
                    .getHostList();

            for(int hostIndex = 0;
                    hostIndex < cloudHosts.size();
                    hostIndex++) {

                List<CloudVM> cloudVms =
                        SimManager.getInstance()
                        .getCloudServerManager()
                        .getVmList(
                                hostIndex);

                CloudVM found =
                        findCloudVmByCandidateRecord(
                                candidateRecord,
                                cloudVms);

                if(found != null) {

                    return found;
                }
            }

            return null;
        }

        return findVmByCandidateRecord(
                candidateRecord,
                getAllEdgeVms());
    }

    private CandidateVmRecord createCandidateRecord(
            int candidateIndex,
            Vm candidateVm,
            String destination,
            double availableCpu,
            boolean feasible,
            String infeasibilityReason) {

        return new CandidateVmRecord(
                candidateIndex,
                candidateVm.getId(),
                candidateVm.getHost().getId(),
                candidateVm.getHost().getDatacenter().getId(),
                destination,
                candidateVm.getMips(),
                candidateVm.getNumberOfPes(),
                availableCpu,
                feasible,
                infeasibilityReason);
    }
    
    /**
     * Captures the complete EDGE and CLOUD VM state available when the
     * live scheduling decision is made.
     *
     * Candidate indexes are unique and continuous across destination
     * tiers.
     */
    private List<CandidateVmRecord>
            collectCrossTierCandidates(
                    Task task) {

        List<CandidateVmRecord> candidateRecords =
                new ArrayList<CandidateVmRecord>();

        /*
         * EDGE candidates.
         */
        for(int hostIndex = 0;
                hostIndex < numberOfHost;
                hostIndex++) {

            List<EdgeVM> edgeVms =
                    SimManager.getInstance()
                    .getEdgeServerManager()
                    .getVmList(
                            hostIndex);

            if(edgeVms == null
                    || edgeVms.isEmpty()) {

                continue;
            }

            for(EdgeVM candidateVm
                    : edgeVms) {

                double requiredCpu =
                        getRequiredCpu(
                                task,
                                candidateVm);

                double availableCpu =
                        getAvailableCpu(
                                candidateVm);

                boolean feasible =
                        isVmFeasible(
                                requiredCpu,
                                availableCpu);

                int candidateIndex =
                        candidateRecords.size();

                candidateRecords.add(
                        createCandidateRecord(
                                candidateIndex,
                                candidateVm,
                                "EDGE",
                                availableCpu,
                                feasible,
                                determineCpuInfeasibilityReason(
                                        requiredCpu,
                                        availableCpu)));
            }
        }

        /*
         * CLOUD candidates.
         */
        List<Host> cloudHosts =
                SimManager.getInstance()
                .getCloudServerManager()
                .getDatacenter()
                .getHostList();

        if(cloudHosts != null) {

            for(int hostIndex = 0;
                    hostIndex < cloudHosts.size();
                    hostIndex++) {

                List<CloudVM> cloudVms =
                        SimManager.getInstance()
                        .getCloudServerManager()
                        .getVmList(
                                hostIndex);

                if(cloudVms == null
                        || cloudVms.isEmpty()) {

                    continue;
                }

                for(CloudVM candidateVm
                        : cloudVms) {

                    double requiredCpu =
                            getRequiredCpu(
                                    task,
                                    candidateVm);

                    double availableCpu =
                            getAvailableCpu(
                                    candidateVm);

                    boolean feasible =
                            isVmFeasible(
                                    requiredCpu,
                                    availableCpu);

                    int candidateIndex =
                            candidateRecords.size();

                    candidateRecords.add(
                            createCandidateRecord(
                                    candidateIndex,
                                    candidateVm,
                                    "CLOUD",
                                    availableCpu,
                                    feasible,
                                    determineCpuInfeasibilityReason(
                                            requiredCpu,
                                            availableCpu)));
                }
            }
        }

        return candidateRecords;
    }

    private int findSelectedCandidateIndex(
            Vm selectedVm,
            List<CandidateVmRecord> candidateRecords) {

        if (selectedVm == null || candidateRecords == null) {
            return -1;
        }

        for (CandidateVmRecord candidate : candidateRecords) {
            if (candidate.getVmId() == selectedVm.getId()
                    && candidate.getHostId() == selectedVm.getHost().getId()
                    && candidate.getDatacenterId()
                    == selectedVm.getHost().getDatacenter().getId()) {
                return candidate.getCandidateIndex();
            }
        }

        return -1;
    }

    private CloudVM findCloudVmByCandidateRecord(
            CandidateVmRecord candidateRecord,
            List<CloudVM> candidates) {

        if(candidateRecord == null
                || candidates == null) {

            return null;
        }

        for(CloudVM candidateVm
                : candidates) {

            if(candidateVm != null
                    && candidateVm.getId()
                    == candidateRecord.getVmId()
                    && candidateVm.getHost().getId()
                    == candidateRecord.getHostId()
                    && candidateVm.getHost()
                    .getDatacenter()
                    .getId()
                    == candidateRecord.getDatacenterId()) {

                return candidateVm;
            }
        }

        return null;
    }

    private EdgeVM findVmByCandidateRecord(
            CandidateVmRecord candidateRecord,
            List<EdgeVM> candidates) {

        if(candidateRecord == null
                || candidates == null) {

            return null;
        }

        for(EdgeVM candidateVm
                : candidates) {

            if(candidateVm != null
                    && candidateVm.getId()
                    == candidateRecord.getVmId()
                    && candidateVm.getHost().getId()
                    == candidateRecord.getHostId()
                    && candidateVm.getHost()
                    .getDatacenter()
                    .getId()
                    == candidateRecord.getDatacenterId()) {

                return candidateVm;
            }
        }

        return null;
    }

    private List<EdgeVM> getAllEdgeVms() {

        List<EdgeVM> edgeVms =
                new ArrayList<EdgeVM>();

        for(int hostIndex = 0;
                hostIndex < numberOfHost;
                hostIndex++) {

            List<EdgeVM> hostVms =
                    SimManager.getInstance()
                    .getEdgeServerManager()
                    .getVmList(
                            hostIndex);

            if(hostVms != null) {

                edgeVms.addAll(
                        hostVms);
            }
        }

        return edgeVms;
    }

    private int findFirstRoundRobinCandidateIndex(
            int startingHostIndex,
            List<CandidateVmRecord> candidateRecords) {

        for(int hostOffset = 0;
                hostOffset < numberOfHost;
                hostOffset++) {

            int hostIndex =
                    (startingHostIndex + hostOffset)
                    % numberOfHost;

            List<EdgeVM> vmArray =
                    SimManager.getInstance()
                    .getEdgeServerManager()
                    .getVmList(
                            hostIndex);

            if(vmArray == null
                    || vmArray.isEmpty()) {

                continue;
            }

            int firstVmPosition =
                    (lastSelectedVmIndexes[hostIndex] + 1)
                    % vmArray.size();

            EdgeVM firstCandidate =
                    vmArray.get(
                            firstVmPosition);

            return findSelectedCandidateIndex(
                    firstCandidate,
                    candidateRecords);
        }

        return -1;
    }

    private int remapCandidateIndexToCrossTierSet(
            int tierCandidateIndex,
            SchedulerDecision tierDecision,
            List<CandidateVmRecord> crossTierCandidates) {

        if(tierCandidateIndex < 0
                || tierDecision == null
                || crossTierCandidates == null) {

            return -1;
        }

        CandidateVmRecord tierCandidate =
                findCandidateRecordByIndex(
                        tierDecision.getCandidateVmRecords(),
                        tierCandidateIndex);

        if(tierCandidate == null) {

            return -1;
        }

        for(CandidateVmRecord candidate
                : crossTierCandidates) {

            if(candidate.getVmId()
                    == tierCandidate.getVmId()
                    && candidate.getHostId()
                    == tierCandidate.getHostId()
                    && candidate.getDatacenterId()
                    == tierCandidate.getDatacenterId()
                    && candidate.getDestination()
                    .equalsIgnoreCase(
                            tierCandidate.getDestination())) {

                return candidate.getCandidateIndex();
            }
        }

        return -1;
    }

    private CandidateVmRecord findCandidateRecordByIndex(
            List<CandidateVmRecord> candidates,
            int candidateIndex) {

        if(candidates == null) {

            return null;
        }

        for(CandidateVmRecord candidate
                : candidates) {

            if(candidate != null
                    && candidate.getCandidateIndex()
                    == candidateIndex) {

                return candidate;
            }
        }

        return null;
    }

    private String determineCpuInfeasibilityReason(
            double requiredCpu,
            double availableCpu) {

        if (requiredCpu > availableCpu) {
            return "INSUFFICIENT_CPU";
        }

        return "NONE";
    }

    private double getRequiredCpu(Task task, EdgeVM vm) {
        return ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                .predictUtilization(vm.getVmType());
    }

    private double getRequiredCpu(Task task, CloudVM vm) {
        return ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                .predictUtilization(vm.getVmType());
    }

    private double getAvailableCpu(Vm vm) {
        return 100.0
                - vm.getCloudletScheduler()
                .getTotalUtilizationOfCpu(CloudSim.clock());
    }

    private boolean isVmFeasible(double requiredCpu, double availableCpu) {
        return requiredCpu <= availableCpu;
    }

    @Override
    public void processEvent(SimEvent event) {
        // No orchestrator-specific events.
    }

    @Override
    public void shutdownEntity() {
        // No orchestrator-specific shutdown action.
    }

    @Override
    public void startEntity() {
        // No orchestrator-specific startup action.
    }

    public SchedulerDecision getLastSchedulerDecision() {
        return lastSchedulerDecision;
    }
    
    private int countFeasibleCandidates(
            List<CandidateVmRecord> candidates) {

        int count =
                0;

        if(candidates == null) {

            return count;
        }

        for(CandidateVmRecord candidate
                : candidates) {

            if(candidate != null
                    && candidate.isFeasible()) {

                count++;
            }
        }

        return count;
    }

    private double findBestAvailableCpu(
            List<CandidateVmRecord> candidates) {

        double bestAvailableCpu =
                -1.0;

        if(candidates == null) {

            return bestAvailableCpu;
        }

        for(CandidateVmRecord candidate
                : candidates) {

            if(candidate == null) {

                continue;
            }

            bestAvailableCpu =
                    Math.max(
                            bestAvailableCpu,
                            candidate
                            .getAvailableCpuPercent());
        }

        return bestAvailableCpu;
    }
}
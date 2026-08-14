package edu.boun.edgecloudsim.edge_orchestrator.ppo;

import java.util.ArrayList;
import java.util.List;

import edu.boun.edgecloudsim.network.DestinationNetworkState;
import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.logging
        .CandidateVmRecord;

/**
 * Shared PPO policy service for live scheduling and replay.
 */
public final class PpoScheduler {

    private final PpoConfig config;
    private final PpoFeatureEncoder featureEncoder;
    private final PpoActorNetwork actorNetwork;

    public PpoScheduler(
            PpoConfig config) {

        if(config == null) {

            throw new IllegalArgumentException(
                    "PpoConfig cannot be null.");
        }

        this.config =
                config;

        this.featureEncoder =
                new PpoFeatureEncoder();

        this.actorNetwork =
                new PpoActorNetwork(
                        config);
    }

    public PpoSelectionResult selectLive(
            double taskLengthMi,
            double uploadSizeKb,
            double requiredCpuPercent,
            DestinationNetworkState networkState,
            List<CandidateVmRecord> candidates) {

        List<Integer> candidateIndexes =
                new ArrayList<Integer>();

        List<PpoFeatureVector> featureVectors =
                new ArrayList<PpoFeatureVector>();

        List<Boolean> feasibleMask =
                new ArrayList<Boolean>();

        if(candidates == null) {

            candidates =
                    new ArrayList<CandidateVmRecord>();
        }

        for(CandidateVmRecord candidate
                : candidates) {

            if(candidate == null) {

                continue;
            }

            double uploadDelayMs =
                    networkState == null
                    ? -1.0
                    : networkState
                    .getUploadDelayMilliseconds();

            double bandwidthMbps =
                    networkState == null
                    ? -1.0
                    : networkState
                    .getAvailableBandwidthMbps();

            boolean networkFeasible =
                    networkState != null
                    && networkState.isNetworkFeasible();

            candidateIndexes.add(
                    candidate.getCandidateIndex());

            featureVectors.add(
                    featureEncoder.encodeLive(
                            taskLengthMi,
                            uploadSizeKb,
                            requiredCpuPercent,
                            uploadDelayMs,
                            bandwidthMbps,
                            candidate));

            feasibleMask.add(
                    Boolean.valueOf(
                            candidate.isFeasible()
                            && networkFeasible));
        }

        return actorNetwork.select(
                candidateIndexes,
                featureVectors,
                feasibleMask);
    }

    public PpoSelectionResult selectReplay(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidates) {

        List<Integer> candidateIndexes =
                new ArrayList<Integer>();

        List<PpoFeatureVector> featureVectors =
                new ArrayList<PpoFeatureVector>();

        List<Boolean> feasibleMask =
                new ArrayList<Boolean>();

        if(candidates == null) {

            candidates =
                    new ArrayList<VmCandidateSnapshot>();
        }

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate == null) {

                continue;
            }

            candidateIndexes.add(
                    candidate.getCandidateIndex());

            featureVectors.add(
                    featureEncoder.encodeReplay(
                            replayState,
                            candidate));

            feasibleMask.add(
                    Boolean.valueOf(
                            candidate.isFeasible()
                            && replayState
                            .isNetworkFeasibleForDestination(
                                    candidate
                                    .getDestination())));
        }

        return actorNetwork.select(
                candidateIndexes,
                featureVectors,
                feasibleMask);
    }

    public PpoConfig getConfig() {
        return config;
    }
}
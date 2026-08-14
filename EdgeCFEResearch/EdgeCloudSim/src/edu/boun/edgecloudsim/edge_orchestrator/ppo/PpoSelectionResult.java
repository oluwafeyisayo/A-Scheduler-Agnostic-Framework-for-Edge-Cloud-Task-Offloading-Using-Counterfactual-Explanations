package edu.boun.edgecloudsim.edge_orchestrator.ppo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic PPO actor-inference result.
 */
public final class PpoSelectionResult {

    private final int selectedCandidateIndex;
    private final double selectedLogit;
    private final double selectedProbability;
    private final String modelId;
    private final String featureSchemaId;
    private final String reason;
    private final List<PpoCandidateScore> candidateScores;

    public PpoSelectionResult(
            int selectedCandidateIndex,
            double selectedLogit,
            double selectedProbability,
            String modelId,
            String featureSchemaId,
            String reason,
            List<PpoCandidateScore> candidateScores) {

        this.selectedCandidateIndex =
                selectedCandidateIndex;

        this.selectedLogit =
                selectedLogit;

        this.selectedProbability =
                selectedProbability;

        this.modelId =
                modelId;

        this.featureSchemaId =
                featureSchemaId;

        this.reason =
                reason == null
                ? "UNKNOWN"
                : reason;

        this.candidateScores =
                candidateScores == null
                ? Collections
                .<PpoCandidateScore>emptyList()
                : Collections.unmodifiableList(
                        new ArrayList<PpoCandidateScore>(
                                candidateScores));
    }

    public boolean hasSelection() {
        return selectedCandidateIndex >= 0;
    }

    public int getSelectedCandidateIndex() {
        return selectedCandidateIndex;
    }

    public double getSelectedLogit() {
        return selectedLogit;
    }

    public double getSelectedProbability() {
        return selectedProbability;
    }

    public String getModelId() {
        return modelId;
    }

    public String getFeatureSchemaId() {
        return featureSchemaId;
    }

    public String getReason() {
        return reason;
    }

    public List<PpoCandidateScore> getCandidateScores() {
        return candidateScores;
    }
}
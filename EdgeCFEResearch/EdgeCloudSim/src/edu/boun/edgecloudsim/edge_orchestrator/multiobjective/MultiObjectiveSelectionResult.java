package edu.boun.edgecloudsim.edge_orchestrator.multiobjective;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of one multi-objective VM-selection pass.
 */
public final class MultiObjectiveSelectionResult {

    private final int selectedCandidateIndex;
    private final MultiObjectiveScore selectedScore;
    private final List<MultiObjectiveScore> candidateScores;
    private final String reason;

    public MultiObjectiveSelectionResult(
            int selectedCandidateIndex,
            MultiObjectiveScore selectedScore,
            List<MultiObjectiveScore> candidateScores,
            String reason) {

        this.selectedCandidateIndex =
                selectedCandidateIndex;

        this.selectedScore =
                selectedScore;

        this.candidateScores =
                candidateScores == null
                ? Collections.<MultiObjectiveScore>emptyList()
                : Collections.unmodifiableList(
                        new ArrayList<MultiObjectiveScore>(
                                candidateScores));

        this.reason =
                reason == null
                || reason.trim().isEmpty()
                ? "NO_REASON"
                : reason.trim();
    }

    public boolean hasSelection() {

        return selectedCandidateIndex >= 0
                && selectedScore != null;
    }

    public int getSelectedCandidateIndex() {
        return selectedCandidateIndex;
    }

    public MultiObjectiveScore getSelectedScore() {
        return selectedScore;
    }

    public List<MultiObjectiveScore> getCandidateScores() {
        return candidateScores;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {

        return "MultiObjectiveSelectionResult{"
                + "selectedCandidateIndex="
                + selectedCandidateIndex
                + ", selectedScore="
                + selectedScore
                + ", scoredCandidateCount="
                + candidateScores.size()
                + ", reason='"
                + reason
                + '\''
                + '}';
    }
}
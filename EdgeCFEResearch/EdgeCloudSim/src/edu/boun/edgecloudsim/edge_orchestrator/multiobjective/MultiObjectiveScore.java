package edu.boun.edgecloudsim.edge_orchestrator.multiobjective;

/**
 * Immutable score assigned to one feasible VM candidate.
 */
public final class MultiObjectiveScore {

    private final int candidateIndex;

    private final double processingTimeMs;
    private final double totalLatencyMs;
    private final double totalEnergyJoules;
    private final double loadPenalty;

    private final double normalizedLatency;
    private final double normalizedEnergy;
    private final double normalizedLoad;

    private final double totalScore;

    public MultiObjectiveScore(
            int candidateIndex,
            double processingTimeMs,
            double totalLatencyMs,
            double totalEnergyJoules,
            double loadPenalty,
            double normalizedLatency,
            double normalizedEnergy,
            double normalizedLoad,
            double totalScore) {

        this.candidateIndex =
                candidateIndex;

        this.processingTimeMs =
                processingTimeMs;

        this.totalLatencyMs =
                totalLatencyMs;

        this.totalEnergyJoules =
                totalEnergyJoules;

        this.loadPenalty =
                loadPenalty;

        this.normalizedLatency =
                normalizedLatency;

        this.normalizedEnergy =
                normalizedEnergy;

        this.normalizedLoad =
                normalizedLoad;

        this.totalScore =
                totalScore;
    }

    public int getCandidateIndex() {
        return candidateIndex;
    }

    public double getProcessingTimeMs() {
        return processingTimeMs;
    }

    public double getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public double getTotalEnergyJoules() {
        return totalEnergyJoules;
    }

    public double getLoadPenalty() {
        return loadPenalty;
    }

    public double getNormalizedLatency() {
        return normalizedLatency;
    }

    public double getNormalizedEnergy() {
        return normalizedEnergy;
    }

    public double getNormalizedLoad() {
        return normalizedLoad;
    }

    public double getTotalScore() {
        return totalScore;
    }

    @Override
    public String toString() {

        return "MultiObjectiveScore{"
                + "candidateIndex="
                + candidateIndex
                + ", processingTimeMs="
                + processingTimeMs
                + ", totalLatencyMs="
                + totalLatencyMs
                + ", totalEnergyJoules="
                + totalEnergyJoules
                + ", loadPenalty="
                + loadPenalty
                + ", normalizedLatency="
                + normalizedLatency
                + ", normalizedEnergy="
                + normalizedEnergy
                + ", normalizedLoad="
                + normalizedLoad
                + ", totalScore="
                + totalScore
                + '}';
    }
}
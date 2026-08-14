package edu.boun.edgecloudsim.edge_orchestrator.ppo;

/**
 * One actor-network score for a candidate action.
 */
public final class PpoCandidateScore {

    private final int candidateIndex;
    private final double logit;
    private final double probability;
    private final boolean masked;

    public PpoCandidateScore(
            int candidateIndex,
            double logit,
            double probability,
            boolean masked) {

        this.candidateIndex =
                candidateIndex;

        this.logit =
                logit;

        this.probability =
                probability;

        this.masked =
                masked;
    }

    public int getCandidateIndex() {
        return candidateIndex;
    }

    public double getLogit() {
        return logit;
    }

    public double getProbability() {
        return probability;
    }

    public boolean isMasked() {
        return masked;
    }

    @Override
    public String toString() {

        return "PpoCandidateScore{"
                + "candidateIndex="
                + candidateIndex
                + ", logit="
                + logit
                + ", probability="
                + probability
                + ", masked="
                + masked
                + '}';
    }
}
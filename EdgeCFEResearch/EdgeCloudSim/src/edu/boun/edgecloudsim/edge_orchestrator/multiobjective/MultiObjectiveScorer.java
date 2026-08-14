package edu.boun.edgecloudsim.edge_orchestrator.multiobjective;

import java.util.ArrayList;
import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.logging
        .CandidateVmRecord;

/**
 * Shared deterministic weighted-sum scorer used by both the live
 * scheduler and deterministic replay.
 *
 * Lower score is better.
 */
public final class MultiObjectiveScorer {

    private static final double EPSILON =
            0.000001;

    private final MultiObjectiveConfig config;

    public MultiObjectiveScorer(
            MultiObjectiveConfig config) {

        if(config == null) {

            throw new IllegalArgumentException(
                    "MultiObjectiveConfig cannot be null.");
        }

        this.config =
                config;
    }

    /**
     * Live-scheduler adapter.
     */
    public MultiObjectiveSelectionResult select(
            List<CandidateVmRecord> candidates,
            double taskLengthMi,
            double uploadDelayMs) {

        if(candidates == null) {

            throw new IllegalArgumentException(
                    "Candidate list cannot be null.");
        }

        List<MultiObjectiveCandidateView> views =
                new ArrayList<MultiObjectiveCandidateView>();

        for(CandidateVmRecord candidate
                : candidates) {

            if(candidate == null) {

                continue;
            }

            views.add(
                    new MultiObjectiveCandidateView(
                            candidate.getCandidateIndex(),
                            candidate.getCandidateIndex(),
                            candidate.getVmMips(),
                            candidate.getAvailableCpuPercent(),
                            candidate.isFeasible()));
        }

        return selectViews(
                views,
                taskLengthMi,
                uploadDelayMs);
    }

    /**
     * Replay adapter. This method deliberately delegates to the exact
     * same scoring routine used by the live scheduler.
     */
    public MultiObjectiveSelectionResult selectSnapshots(
            List<VmCandidateSnapshot> candidates,
            double taskLengthMi,
            double uploadDelayMs,
            boolean networkFeasible) {

        if(candidates == null) {

            throw new IllegalArgumentException(
                    "Candidate list cannot be null.");
        }

        if(!networkFeasible) {

            return noSelection(
                    "NETWORK_PATH_INFEASIBLE");
        }

        List<MultiObjectiveCandidateView> views =
                new ArrayList<MultiObjectiveCandidateView>();

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate == null) {

                continue;
            }

            views.add(
                    new MultiObjectiveCandidateView(
                            candidate.getCandidateIndex(),
                            candidate.getEvaluationOrder(),
                            candidate.getVmMips(),
                            candidate.getAvailableCpuPercent(),
                            candidate.isFeasible()));
        }

        return selectViews(
                views,
                taskLengthMi,
                uploadDelayMs);
    }

    /**
     * The one authoritative scoring implementation.
     */
    public MultiObjectiveSelectionResult selectViews(
            List<MultiObjectiveCandidateView> candidates,
            double taskLengthMi,
            double uploadDelayMs) {

        if(candidates == null) {

            throw new IllegalArgumentException(
                    "Candidate list cannot be null.");
        }

        if(!isFinitePositive(
                taskLengthMi)) {

            throw new IllegalArgumentException(
                    "Task length must be finite and positive.");
        }

        if(!isFiniteNonnegative(
                uploadDelayMs)) {

            return noSelection(
                    "NETWORK_DELAY_UNAVAILABLE");
        }

        List<RawScore> rawScores =
                new ArrayList<RawScore>();

        for(MultiObjectiveCandidateView candidate
                : candidates) {

            if(candidate == null
                    || !candidate.isFeasible()
                    || candidate.getVmMips() <= 0.0) {

                continue;
            }

            double processingTimeMs =
                    taskLengthMi
                    / candidate.getVmMips()
                    * 1000.0;

            double totalLatencyMs =
                    uploadDelayMs
                    + processingTimeMs;

            double transmissionEnergy =
                    uploadDelayMs
                    / 1000.0
                    * config.getTransmissionPowerWatts();

            double computeEnergy =
                    processingTimeMs
                    / 1000.0
                    * config.getComputePowerWatts();

            double totalEnergy =
                    transmissionEnergy
                    + computeEnergy;

            double loadPenalty =
                    1.0
                    - candidate
                    .getAvailableCpuPercent()
                    / 100.0;

            rawScores.add(
                    new RawScore(
                            candidate,
                            processingTimeMs,
                            totalLatencyMs,
                            totalEnergy,
                            loadPenalty));
        }

        if(rawScores.isEmpty()) {

            return noSelection(
                    "NO_FEASIBLE_VM");
        }

        ScoreRanges ranges =
                new ScoreRanges();

        for(RawScore raw
                : rawScores) {

            ranges.include(
                    raw);
        }

        List<MultiObjectiveScore> finalScores =
                new ArrayList<MultiObjectiveScore>();

        RawScore selectedRaw =
                null;

        MultiObjectiveScore selectedScore =
                null;

        for(RawScore raw
                : rawScores) {

            double normalizedLatency =
                    normalize(
                            raw.totalLatencyMs,
                            ranges.minimumLatency,
                            ranges.maximumLatency);

            double normalizedEnergy =
                    normalize(
                            raw.totalEnergyJoules,
                            ranges.minimumEnergy,
                            ranges.maximumEnergy);

            double normalizedLoad =
                    normalize(
                            raw.loadPenalty,
                            ranges.minimumLoad,
                            ranges.maximumLoad);

            double totalScore =
                    config.getLatencyWeight()
                    * normalizedLatency
                    + config.getEnergyWeight()
                    * normalizedEnergy
                    + config.getLoadWeight()
                    * normalizedLoad;

            MultiObjectiveScore score =
                    new MultiObjectiveScore(
                            raw.candidate
                            .getCandidateIndex(),
                            raw.processingTimeMs,
                            raw.totalLatencyMs,
                            raw.totalEnergyJoules,
                            raw.loadPenalty,
                            normalizedLatency,
                            normalizedEnergy,
                            normalizedLoad,
                            totalScore);

            finalScores.add(
                    score);

            if(selectedScore == null
                    || isBetter(
                            raw,
                            score,
                            selectedRaw,
                            selectedScore)) {

                selectedRaw =
                        raw;

                selectedScore =
                        score;
            }
        }

        return new MultiObjectiveSelectionResult(
                selectedScore.getCandidateIndex(),
                selectedScore,
                finalScores,
                "MINIMUM_WEIGHTED_MULTI_OBJECTIVE_SCORE");
    }

    private boolean isBetter(
            RawScore candidateRaw,
            MultiObjectiveScore candidateScore,
            RawScore selectedRaw,
            MultiObjectiveScore selectedScore) {

        if(candidateScore.getTotalScore()
                < selectedScore.getTotalScore()
                - EPSILON) {

            return true;
        }

        if(Math.abs(
                candidateScore.getTotalScore()
                - selectedScore.getTotalScore())
                > EPSILON) {

            return false;
        }

        if(candidateRaw.candidate
                .getAvailableCpuPercent()
                > selectedRaw.candidate
                .getAvailableCpuPercent()
                + EPSILON) {

            return true;
        }

        if(Math.abs(
                candidateRaw.candidate
                .getAvailableCpuPercent()
                - selectedRaw.candidate
                .getAvailableCpuPercent())
                > EPSILON) {

            return false;
        }

        if(candidateRaw.candidate
                .getEvaluationOrder()
                != selectedRaw.candidate
                .getEvaluationOrder()) {

            return candidateRaw.candidate
                    .getEvaluationOrder()
                    < selectedRaw.candidate
                    .getEvaluationOrder();
        }

        return candidateRaw.candidate
                .getCandidateIndex()
                < selectedRaw.candidate
                .getCandidateIndex();
    }

    private MultiObjectiveSelectionResult noSelection(
            String reason) {

        return new MultiObjectiveSelectionResult(
                -1,
                null,
                new ArrayList<MultiObjectiveScore>(),
                reason);
    }

    private static double normalize(
            double value,
            double minimum,
            double maximum) {

        double range =
                maximum
                - minimum;

        if(Math.abs(range)
                <= EPSILON) {

            return 0.0;
        }

        return (
                value
                - minimum
        )
        / range;
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

    private static final class RawScore {

        private final MultiObjectiveCandidateView candidate;
        private final double processingTimeMs;
        private final double totalLatencyMs;
        private final double totalEnergyJoules;
        private final double loadPenalty;

        private RawScore(
                MultiObjectiveCandidateView candidate,
                double processingTimeMs,
                double totalLatencyMs,
                double totalEnergyJoules,
                double loadPenalty) {

            this.candidate =
                    candidate;

            this.processingTimeMs =
                    processingTimeMs;

            this.totalLatencyMs =
                    totalLatencyMs;

            this.totalEnergyJoules =
                    totalEnergyJoules;

            this.loadPenalty =
                    loadPenalty;
        }
    }

    private static final class ScoreRanges {

        private double minimumLatency =
                Double.POSITIVE_INFINITY;

        private double maximumLatency =
                Double.NEGATIVE_INFINITY;

        private double minimumEnergy =
                Double.POSITIVE_INFINITY;

        private double maximumEnergy =
                Double.NEGATIVE_INFINITY;

        private double minimumLoad =
                Double.POSITIVE_INFINITY;

        private double maximumLoad =
                Double.NEGATIVE_INFINITY;

        private void include(
                RawScore score) {

            minimumLatency =
                    Math.min(
                            minimumLatency,
                            score.totalLatencyMs);

            maximumLatency =
                    Math.max(
                            maximumLatency,
                            score.totalLatencyMs);

            minimumEnergy =
                    Math.min(
                            minimumEnergy,
                            score.totalEnergyJoules);

            maximumEnergy =
                    Math.max(
                            maximumEnergy,
                            score.totalEnergyJoules);

            minimumLoad =
                    Math.min(
                            minimumLoad,
                            score.loadPenalty);

            maximumLoad =
                    Math.max(
                            maximumLoad,
                            score.loadPenalty);
        }
    }
}
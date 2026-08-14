package edu.boun.edgecloudsim.edge_orchestrator.ppo;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight feed-forward actor used for PPO policy inference.
 *
 * Architecture:
 * input -> tanh hidden layer -> scalar candidate logit
 *
 * Candidate logits are converted to a masked softmax distribution.
 */
public final class PpoActorNetwork {

    private static final double EPSILON =
            0.000000001;

    private final PpoConfig config;

    public PpoActorNetwork(
            PpoConfig config) {

        if(config == null) {

            throw new IllegalArgumentException(
                    "PpoConfig cannot be null.");
        }

        this.config =
                config;
    }

    public double computeLogit(
            PpoFeatureVector vector) {

        if(vector == null) {

            throw new IllegalArgumentException(
                    "PpoFeatureVector cannot be null.");
        }

        double[] input =
                vector.toArray();

        if(input.length
                != config.getInputSize()) {

            throw new IllegalArgumentException(
                    "PPO actor input size does not match model.");
        }

        double[][] hiddenWeights =
                config.getHiddenWeights();

        double[] hiddenBias =
                config.getHiddenBias();

        double[] outputWeights =
                config.getOutputWeights();

        double logit =
                config.getOutputBias();

        for(int hidden = 0;
                hidden < config.getHiddenSize();
                hidden++) {

            double activation =
                    hiddenBias[hidden];

            for(int feature = 0;
                    feature < input.length;
                    feature++) {

                activation +=
                        hiddenWeights[hidden][feature]
                        * input[feature];
            }

            double hiddenOutput =
                    Math.tanh(
                            activation);

            logit +=
                    outputWeights[hidden]
                    * hiddenOutput;
        }

        return logit;
    }

    public PpoSelectionResult select(
            List<Integer> candidateIndexes,
            List<PpoFeatureVector> featureVectors,
            List<Boolean> feasibleMask) {

        if(candidateIndexes == null
                || featureVectors == null
                || feasibleMask == null
                || candidateIndexes.size()
                != featureVectors.size()
                || candidateIndexes.size()
                != feasibleMask.size()) {

            throw new IllegalArgumentException(
                    "PPO candidate inputs must have equal lengths.");
        }

        if(candidateIndexes.isEmpty()) {

            return noSelection(
                    "NO_CANDIDATE_VM");
        }

        List<Double> logits =
                new ArrayList<Double>();

        double maximumFeasibleLogit =
                Double.NEGATIVE_INFINITY;

        for(int index = 0;
                index < candidateIndexes.size();
                index++) {

            double logit =
                    computeLogit(
                            featureVectors.get(
                                    index));

            logits.add(
                    Double.valueOf(
                            logit));

            if(Boolean.TRUE.equals(
                    feasibleMask.get(
                            index))) {

                maximumFeasibleLogit =
                        Math.max(
                                maximumFeasibleLogit,
                                logit);
            }
        }

        if(!Double.isFinite(
                maximumFeasibleLogit)) {

            return noSelection(
                    "NO_FEASIBLE_VM");
        }

        double denominator =
                0.0;

        for(int index = 0;
                index < logits.size();
                index++) {

            if(Boolean.TRUE.equals(
                    feasibleMask.get(
                            index))) {

                denominator +=
                        Math.exp(
                                logits.get(index)
                                - maximumFeasibleLogit);
            }
        }

        int selectedPosition =
                -1;

        double selectedProbability =
                -1.0;

        double selectedLogit =
                Double.NEGATIVE_INFINITY;

        List<PpoCandidateScore> scores =
                new ArrayList<PpoCandidateScore>();

        for(int index = 0;
                index < logits.size();
                index++) {

            boolean feasible =
                    Boolean.TRUE.equals(
                            feasibleMask.get(
                                    index));

            double logit =
                    logits.get(index);

            double probability =
                    feasible
                    ? Math.exp(
                            logit
                            - maximumFeasibleLogit)
                            / Math.max(
                                    denominator,
                                    EPSILON)
                    : 0.0;

            scores.add(
                    new PpoCandidateScore(
                            candidateIndexes.get(index),
                            logit,
                            probability,
                            !feasible));

            if(feasible
                    && (
                            selectedPosition < 0
                            || probability
                            > selectedProbability
                            + EPSILON
                            || (
                                    Math.abs(
                                            probability
                                            - selectedProbability)
                                    <= EPSILON
                                    && candidateIndexes.get(index)
                                    < candidateIndexes.get(
                                            selectedPosition)
                            )
                    )) {

                selectedPosition =
                        index;

                selectedProbability =
                        probability;

                selectedLogit =
                        logit;
            }
        }

        if(selectedPosition < 0) {

            return noSelection(
                    "NO_FEASIBLE_VM");
        }

        return new PpoSelectionResult(
                candidateIndexes.get(
                        selectedPosition),
                selectedLogit,
                selectedProbability,
                config.getModelId(),
                config.getFeatureSchemaId(),
                "PPO_DETERMINISTIC_ARGMAX",
                scores);
    }

    private PpoSelectionResult noSelection(
            String reason) {

        return new PpoSelectionResult(
                -1,
                Double.NEGATIVE_INFINITY,
                0.0,
                config.getModelId(),
                config.getFeatureSchemaId(),
                reason,
                new ArrayList<PpoCandidateScore>());
    }
}
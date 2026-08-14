package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerDecision;

import edu.boun.edgecloudsim.research.counterfactual.verification
        .CounterfactualVerificationResult;

/**
 * Represents one candidate evaluated by Replay-Guided Feasible
 * Counterfactual Search.
 *
 * A candidate contains:
 *
 * 1. The feature changes proposed by the search
 * 2. The resulting replay state
 * 3. Its feasibility result
 * 4. Its scheduler replay decision
 * 5. Its verification result
 * 6. Its semantic classification
 * 7. Its objective-function score
 */
public final class CounterfactualCandidate
        implements Comparable<CounterfactualCandidate> {

    private final int taskId;
    private final int candidateId;

    private final List<FeatureChange>
            featureChanges;

    private final ReplayState
            replayState;

    private final FeasibilityResult
            feasibilityResult;

    private final SchedulerDecision
            schedulerDecision;

    private final CounterfactualVerificationResult
            verificationResult;

    private final CounterfactualClassificationResult
            classificationResult;

    private final CounterfactualScore
            score;

    private final boolean evaluated;
    private final boolean accepted;

    private final String reason;

    private CounterfactualCandidate(
            Builder builder) {

        this.taskId =
                builder.taskId;

        this.candidateId =
                builder.candidateId;

        this.featureChanges =
                Collections.unmodifiableList(
                        new ArrayList<FeatureChange>(
                                builder.featureChanges));

        this.replayState =
                builder.replayState;

        this.feasibilityResult =
                builder.feasibilityResult;

        this.schedulerDecision =
                builder.schedulerDecision;

        this.verificationResult =
                builder.verificationResult;

        this.classificationResult =
                builder.classificationResult;

        this.score =
                builder.score;

        this.evaluated =
                builder.evaluated;

        this.accepted =
                builder.accepted;

        this.reason =
                normalizeReason(
                        builder.reason);
    }

    private static String normalizeReason(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return "No candidate explanation was provided.";
        }

        return value.trim();
    }

    public int getTaskId() {

        return taskId;
    }

    public int getCandidateId() {

        return candidateId;
    }

    public List<FeatureChange> getFeatureChanges() {

        return featureChanges;
    }

    public ReplayState getReplayState() {

        return replayState;
    }

    public FeasibilityResult getFeasibilityResult() {

        return feasibilityResult;
    }

    public SchedulerDecision getSchedulerDecision() {

        return schedulerDecision;
    }

    public CounterfactualVerificationResult
            getVerificationResult() {

        return verificationResult;
    }

    public CounterfactualClassificationResult
            getClassificationResult() {

        return classificationResult;
    }

    /**
     * Returns the semantic classification of the candidate.
     *
     * Unevaluated candidates without a classification are reported as
     * INVALID because they are not yet valid counterfactual results.
     */
    public CounterfactualClassification
            getClassification() {

        if(classificationResult == null) {

            return CounterfactualClassification.INVALID;
        }

        return classificationResult
                .getClassification();
    }

    public CounterfactualScore getScore() {

        return score;
    }

    public boolean isEvaluated() {

        return evaluated;
    }

    public boolean isAccepted() {

        return accepted;
    }

    public String getReason() {

        return reason;
    }

    public int getChangedFeatureCount() {

        return featureChanges.size();
    }

    public boolean hasScore() {

        return score != null;
    }

    public boolean hasClassification() {

        return classificationResult != null;
    }

    /**
     * Lower-scoring candidates are ordered first.
     *
     * Unevaluated or unscored candidates are placed after candidates
     * that have valid scores.
     */
    @Override
    public int compareTo(
            CounterfactualCandidate other) {

        if(other == null) {

            return -1;
        }

        if(this.score == null
                && other.score == null) {

            return Integer.compare(
                    this.candidateId,
                    other.candidateId);
        }

        if(this.score == null) {

            return 1;
        }

        if(other.score == null) {

            return -1;
        }

        int scoreComparison =
                this.score.compareTo(
                        other.score);

        if(scoreComparison != 0) {

            return scoreComparison;
        }

        /*
         * Prefer the candidate changing fewer features when objective
         * scores are identical.
         */
        int sparsityComparison =
                Integer.compare(
                        this.getChangedFeatureCount(),
                        other.getChangedFeatureCount());

        if(sparsityComparison != 0) {

            return sparsityComparison;
        }

        /*
         * Stable final tie-breaker.
         */
        return Integer.compare(
                this.candidateId,
                other.candidateId);
    }

    @Override
    public String toString() {

        return "CounterfactualCandidate{"
                + "taskId="
                + taskId

                + ", candidateId="
                + candidateId

                + ", changedFeatureCount="
                + featureChanges.size()

                + ", evaluated="
                + evaluated

                + ", accepted="
                + accepted

                + ", classification="
                + (classificationResult == null
                        ? "NOT_CLASSIFIED"
                        : classificationResult
                                .getClassification())

                + ", score="
                + score

                + ", reason='"
                + reason
                + '\''

                + '}';
    }

    public static final class Builder {

        private final int taskId;
        private final int candidateId;

        private final List<FeatureChange>
                featureChanges =
                new ArrayList<FeatureChange>();

        private ReplayState
                replayState;

        private FeasibilityResult
                feasibilityResult;

        private SchedulerDecision
                schedulerDecision;

        private CounterfactualVerificationResult
                verificationResult;

        private CounterfactualClassificationResult
                classificationResult;

        private CounterfactualScore
                score;

        private boolean evaluated;
        private boolean accepted;

        private String reason;

        public Builder(
                int taskId,
                int candidateId) {

            if(taskId < 0) {

                throw new IllegalArgumentException(
                        "Task ID cannot be negative.");
            }

            if(candidateId < 0) {

                throw new IllegalArgumentException(
                        "Candidate ID cannot be negative.");
            }

            this.taskId =
                    taskId;

            this.candidateId =
                    candidateId;
        }

        public Builder addFeatureChange(
                FeatureChange featureChange) {

            if(featureChange == null) {

                throw new IllegalArgumentException(
                        "Feature change cannot be null.");
            }

            featureChanges.add(
                    featureChange);

            return this;
        }

        public Builder addFeatureChanges(
                List<FeatureChange> changes) {

            if(changes == null) {

                throw new IllegalArgumentException(
                        "Feature-change list cannot be null.");
            }

            for(FeatureChange change
                    : changes) {

                addFeatureChange(
                        change);
            }

            return this;
        }

        public Builder withReplayState(
                ReplayState replayState) {

            this.replayState =
                    replayState;

            return this;
        }

        public Builder withFeasibilityResult(
                FeasibilityResult result) {

            this.feasibilityResult =
                    result;

            return this;
        }

        public Builder withSchedulerDecision(
                SchedulerDecision decision) {

            this.schedulerDecision =
                    decision;

            return this;
        }

        public Builder withVerificationResult(
                CounterfactualVerificationResult result) {

            this.verificationResult =
                    result;

            return this;
        }

        public Builder withClassificationResult(
                CounterfactualClassificationResult result) {

            this.classificationResult =
                    result;

            return this;
        }

        public Builder withScore(
                CounterfactualScore score) {

            this.score =
                    score;

            return this;
        }

        public Builder withEvaluated(
                boolean evaluated) {

            this.evaluated =
                    evaluated;

            return this;
        }

        public Builder withAccepted(
                boolean accepted) {

            this.accepted =
                    accepted;

            return this;
        }

        public Builder withReason(
                String reason) {

            this.reason =
                    reason;

            return this;
        }

        public CounterfactualCandidate build() {

            validate();

            return new CounterfactualCandidate(
                    this);
        }

        private void validate() {

            if(featureChanges.isEmpty()) {

                throw new IllegalStateException(
                        "A counterfactual candidate must contain "
                        + "at least one feature change.");
            }

            if(replayState == null) {

                throw new IllegalStateException(
                        "A counterfactual candidate must contain "
                        + "a ReplayState.");
            }

            if(evaluated) {

                if(feasibilityResult == null) {

                    throw new IllegalStateException(
                            "An evaluated candidate must contain "
                            + "a feasibility result.");
                }

                if(schedulerDecision == null) {

                    throw new IllegalStateException(
                            "An evaluated candidate must contain "
                            + "a scheduler decision.");
                }

                if(verificationResult == null) {

                    throw new IllegalStateException(
                            "An evaluated candidate must contain "
                            + "a verification result.");
                }

                if(classificationResult == null) {

                    throw new IllegalStateException(
                            "An evaluated candidate must contain "
                            + "a classification result.");
                }

                if(score == null) {

                    throw new IllegalStateException(
                            "An evaluated candidate must contain "
                            + "an objective score.");
                }
            }

            if(accepted) {

                if(!evaluated) {

                    throw new IllegalStateException(
                            "An accepted candidate must first "
                            + "be evaluated.");
                }

                if(classificationResult == null
                        || !classificationResult
                                .isAcceptedCounterfactual()) {

                    throw new IllegalStateException(
                            "An accepted candidate must have the "
                            + "FEASIBLE_EFFECTIVE classification.");
                }

                if(verificationResult == null
                        || !verificationResult
                                .isAccepted()) {

                    throw new IllegalStateException(
                            "An accepted candidate must contain "
                            + "an accepted verification result.");
                }

                if(feasibilityResult == null
                        || !feasibilityResult
                                .isOperationallyFeasible()) {

                    throw new IllegalStateException(
                            "An accepted candidate must be "
                            + "operationally feasible.");
                }
            }

            if(!accepted
                    && classificationResult != null
                    && classificationResult
                            .isAcceptedCounterfactual()) {

                throw new IllegalStateException(
                        "A FEASIBLE_EFFECTIVE candidate must be "
                        + "marked as accepted.");
            }
        }
    }
}
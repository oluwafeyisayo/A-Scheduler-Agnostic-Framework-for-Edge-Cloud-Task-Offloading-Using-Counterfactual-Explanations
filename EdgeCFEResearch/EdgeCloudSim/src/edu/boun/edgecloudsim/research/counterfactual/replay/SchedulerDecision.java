package edu.boun.edgecloudsim.research.counterfactual.replay;

/**
 * Generic result returned by any scheduler replay strategy.
 */
public final class SchedulerDecision {

    private final long taskId;
    private final String schedulerPolicy;
    private final SchedulerReplayStatus replayStatus;

    private final String decisionOutcome;
    private final String selectedDestination;

    private final int selectedVmId;
    private final int selectedHostId;
    private final int selectedDatacenterId;
    private final int selectedCandidateIndex;

    private final int roundRobinStartingCandidateIndex;
    private final int randomSampledCandidateIndex;

    private final String reason;

    private SchedulerDecision(
            Builder builder) {

        this.taskId =
                builder.taskId;

        this.schedulerPolicy =
                safeText(
                        builder.schedulerPolicy,
                        "UNKNOWN");

        this.replayStatus =
                builder.replayStatus == null
                ? SchedulerReplayStatus.ERROR
                : builder.replayStatus;

        this.decisionOutcome =
                safeText(
                        builder.decisionOutcome,
                        "UNDETERMINED");

        this.selectedDestination =
                safeText(
                        builder.selectedDestination,
                        "UNDETERMINED");

        this.selectedVmId =
                builder.selectedVmId;

        this.selectedHostId =
                builder.selectedHostId;

        this.selectedDatacenterId =
                builder.selectedDatacenterId;

        this.selectedCandidateIndex =
                builder.selectedCandidateIndex;

        this.roundRobinStartingCandidateIndex =
                builder.roundRobinStartingCandidateIndex;

        this.randomSampledCandidateIndex =
                builder.randomSampledCandidateIndex;

        this.reason =
                safeText(
                        builder.reason,
                        "NO_REASON");
    }

    private static String safeText(
            String value,
            String defaultValue) {

        if(value == null
                || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value.trim();
    }

    public long getTaskId() {
        return taskId;
    }

    public String getSchedulerPolicy() {
        return schedulerPolicy;
    }

    public SchedulerReplayStatus getReplayStatus() {
        return replayStatus;
    }

    public String getDecisionOutcome() {
        return decisionOutcome;
    }

    public String getSelectedDestination() {
        return selectedDestination;
    }

    public int getSelectedVmId() {
        return selectedVmId;
    }

    public int getSelectedHostId() {
        return selectedHostId;
    }

    public int getSelectedDatacenterId() {
        return selectedDatacenterId;
    }

    public int getSelectedCandidateIndex() {
        return selectedCandidateIndex;
    }

    public int getRoundRobinStartingCandidateIndex() {
        return roundRobinStartingCandidateIndex;
    }

    public int getRandomSampledCandidateIndex() {
        return randomSampledCandidateIndex;
    }

    public String getReason() {
        return reason;
    }

    public boolean wasSuccessfullyReplayed() {
        return replayStatus
                == SchedulerReplayStatus.REPLAYED;
    }

    public boolean isSelected() {
        return "SELECTED".equalsIgnoreCase(
                decisionOutcome);
    }

    @Override
    public String toString() {

        return "SchedulerDecision{"
                + "taskId="
                + taskId
                + ", schedulerPolicy='"
                + schedulerPolicy
                + '\''
                + ", replayStatus="
                + replayStatus
                + ", decisionOutcome='"
                + decisionOutcome
                + '\''
                + ", selectedDestination='"
                + selectedDestination
                + '\''
                + ", selectedVmId="
                + selectedVmId
                + ", selectedHostId="
                + selectedHostId
                + ", selectedDatacenterId="
                + selectedDatacenterId
                + ", selectedCandidateIndex="
                + selectedCandidateIndex
                + ", roundRobinStartingCandidateIndex="
                + roundRobinStartingCandidateIndex
                + ", randomSampledCandidateIndex="
                + randomSampledCandidateIndex
                + ", reason='"
                + reason
                + '\''
                + '}';
    }

    public static final class Builder {

        private final long taskId;
        private final String schedulerPolicy;

        private SchedulerReplayStatus replayStatus =
                SchedulerReplayStatus.INDETERMINATE;

        private String decisionOutcome =
                "UNDETERMINED";

        private String selectedDestination =
                "UNDETERMINED";

        private int selectedVmId = -1;
        private int selectedHostId = -1;
        private int selectedDatacenterId = -1;
        private int selectedCandidateIndex = -1;

        private int roundRobinStartingCandidateIndex = -1;
        private int randomSampledCandidateIndex = -1;

        private String reason =
                "NO_REASON";

        public Builder(
                long taskId,
                String schedulerPolicy) {

            this.taskId =
                    taskId;

            this.schedulerPolicy =
                    schedulerPolicy;
        }

        public Builder withReplayStatus(
                SchedulerReplayStatus value) {
            this.replayStatus = value;
            return this;
        }

        public Builder withDecisionOutcome(
                String value) {
            this.decisionOutcome = value;
            return this;
        }

        public Builder withSelectedDestination(
                String value) {
            this.selectedDestination = value;
            return this;
        }

        public Builder withSelectedVmId(
                int value) {
            this.selectedVmId = value;
            return this;
        }

        public Builder withSelectedHostId(
                int value) {
            this.selectedHostId = value;
            return this;
        }

        public Builder withSelectedDatacenterId(
                int value) {
            this.selectedDatacenterId = value;
            return this;
        }

        public Builder withSelectedCandidateIndex(
                int value) {
            this.selectedCandidateIndex = value;
            return this;
        }

        public Builder withRoundRobinStartingCandidateIndex(
                int value) {
            this.roundRobinStartingCandidateIndex = value;
            return this;
        }

        public Builder withRandomSampledCandidateIndex(
                int value) {
            this.randomSampledCandidateIndex = value;
            return this;
        }

        public Builder withReason(
                String value) {
            this.reason = value;
            return this;
        }

        public SchedulerDecision build() {

            if("NO_FEASIBLE_VM".equalsIgnoreCase(
                    decisionOutcome)) {

                selectedDestination =
                        "NONE";

                selectedVmId =
                        -1;

                selectedHostId =
                        -1;

                selectedDatacenterId =
                        -1;

                selectedCandidateIndex =
                        -1;
            }

            return new SchedulerDecision(
                    this);
        }
    }
}
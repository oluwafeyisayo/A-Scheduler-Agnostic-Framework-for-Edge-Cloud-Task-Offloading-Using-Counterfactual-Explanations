package edu.boun.edgecloudsim.research.counterfactual;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.boun.edgecloudsim.research.counterfactual.comparison
        .DecisionChangeType;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

import edu.boun.edgecloudsim.research.counterfactual.replay.engine
        .SchedulerReplayEngine;

/**
 * Runs the same decision batch independently across:
 *
 * 1. all supported counterfactual decision-change objectives; and
 * 2. all supplied scheduler replay policies.
 *
 * Experimental structure:
 *
 * DecisionChangeType
 *     -> Scheduler policy
 *         -> Decision batch
 */
public final class MultiSchedulerCounterfactualExperimentRunner {

    private final FeasibleCounterfactualSearchConfig
            configuration;

    private final SchedulerReplayEngine
            schedulerReplayEngine;

    private final File
            outputDirectory;

    private final boolean
            debugEnabled;

    private final Map<Integer, List<VmCandidateSnapshot>>
            candidateSnapshotsByTaskId;

    /**
     * Creates a multi-objective, multi-scheduler experiment runner.
     *
     * Every value returned by DecisionChangeType.values() is evaluated.
     */
    public MultiSchedulerCounterfactualExperimentRunner(
            FeasibleCounterfactualSearchConfig configuration,
            SchedulerReplayEngine schedulerReplayEngine,
            File outputDirectory,
            boolean debugEnabled,
            Map<Integer, List<VmCandidateSnapshot>>
                    candidateSnapshotsByTaskId) {

        if(configuration == null) {

            throw new IllegalArgumentException(
                    "Search configuration cannot be null.");
        }

        if(schedulerReplayEngine == null) {

            throw new IllegalArgumentException(
                    "Scheduler replay engine cannot be null.");
        }

        if(outputDirectory == null) {

            throw new IllegalArgumentException(
                    "Output directory cannot be null.");
        }

        if(candidateSnapshotsByTaskId == null) {

            throw new IllegalArgumentException(
                    "Candidate snapshot map cannot be null.");
        }

        this.configuration =
                configuration;

        this.schedulerReplayEngine =
                schedulerReplayEngine;

        this.outputDirectory =
                outputDirectory;

        this.debugEnabled =
                debugEnabled;

        this.candidateSnapshotsByTaskId =
                candidateSnapshotsByTaskId;
    }

    /**
     * Convenience constructor with debugging disabled.
     */
    public MultiSchedulerCounterfactualExperimentRunner(
            FeasibleCounterfactualSearchConfig configuration,
            SchedulerReplayEngine schedulerReplayEngine,
            File outputDirectory,
            Map<Integer, List<VmCandidateSnapshot>>
                    candidateSnapshotsByTaskId) {

        this(
                configuration,
                schedulerReplayEngine,
                outputDirectory,
                false,
                candidateSnapshotsByTaskId);
    }

    /**
     * Runs all supported counterfactual objectives for all supplied
     * scheduler policies.
     *
     * Summary keys use:
     *
     * REQUESTED_CHANGE_TYPE::SCHEDULER_POLICY
     */
    public Map<String, CounterfactualExperimentSummary> run(
            List<DecisionState> decisionStates,
            List<SchedulerReplayContextFactory> contextFactories)
            throws IOException {

        validateRunArguments(
                decisionStates,
                contextFactories);

        createDirectoryIfNecessary(
                outputDirectory);

        Map<String, CounterfactualExperimentSummary> summaries =
                new LinkedHashMap<String,
                        CounterfactualExperimentSummary>();

        /*
         * Outer loop: requested counterfactual objective.
         */
        for(DecisionChangeType requestedChangeType
                : DecisionChangeType.values()) {

            List<DecisionState> objectiveDecisions =
                    selectDecisionsForObjective(
                            decisionStates,
                            requestedChangeType);

            /*
             * Selected decisions are suitable for execution-target and
             * destination changes.
             *
             * Unsuccessful decisions are suitable for feasibility
             * restoration.
             */
            if(objectiveDecisions.isEmpty()) {

                printSkippedObjective(
                        requestedChangeType);

                continue;
            }

            File objectiveOutputDirectory =
                    new File(
                            outputDirectory,
                            requestedChangeType.name());

            createDirectoryIfNecessary(
                    objectiveOutputDirectory);

            printObjectiveStart(
                    requestedChangeType,
                    objectiveOutputDirectory,
                    objectiveDecisions.size());

            /*
             * Inner loop: scheduler replay policy.
             */
            for(SchedulerReplayContextFactory contextFactory
                    : contextFactories) {

                if(contextFactory == null) {

                    continue;
                }

                runSchedulerExperiment(
                        objectiveDecisions,
                        contextFactory,
                        requestedChangeType,
                        objectiveOutputDirectory,
                        summaries);
            }
        }

        printOverallCompletion(
                summaries);

        return summaries;
    }

    /**
     * Selects decisions compatible with the requested objective.
     *
     * EXECUTION_TARGET_CHANGE and DESTINATION_CHANGE use decisions that
     * were originally selected successfully.
     *
     * FEASIBILITY_RESTORATION uses decisions that were originally
     * unsuccessful or unselected.
     */
    private List<DecisionState> selectDecisionsForObjective(
            List<DecisionState> decisionStates,
            DecisionChangeType requestedChangeType) {

        List<DecisionState> objectiveDecisions =
                new ArrayList<DecisionState>();

        for(DecisionState decisionState
                : decisionStates) {

            if(decisionState == null) {

                continue;
            }

            boolean originallySelected =
                    isOriginallySelected(
                            decisionState);

            if(requestedChangeType
                    == DecisionChangeType
                    .FEASIBILITY_RESTORATION) {

                if(!originallySelected) {

                    objectiveDecisions.add(
                            decisionState);
                }
            }
            else {

                if(originallySelected) {

                    objectiveDecisions.add(
                            decisionState);
                }
            }
        }

        return objectiveDecisions;
    }

    /**
     * Determines whether the logged decision outcome was SELECTED.
     *
     * String.valueOf supports either a String-returning getter or an
     * enum-returning getter.
     */
    private boolean isOriginallySelected(
            DecisionState decisionState) {

        if(decisionState == null
                || decisionState.getDecisionOutcome() == null) {

            return false;
        }

        String decisionOutcome =
                String.valueOf(
                        decisionState
                        .getDecisionOutcome());

        return "SELECTED".equalsIgnoreCase(
                decisionOutcome.trim());
    }

    /**
     * Runs one requested counterfactual objective for one scheduler.
     */
    private void runSchedulerExperiment(
            List<DecisionState> decisionStates,
            SchedulerReplayContextFactory contextFactory,
            DecisionChangeType requestedChangeType,
            File objectiveOutputDirectory,
            Map<String, CounterfactualExperimentSummary> summaries)
            throws IOException {

        String schedulerPolicy =
                contextFactory
                .getSchedulerPolicy();

        if(schedulerPolicy == null
                || schedulerPolicy.trim().isEmpty()) {

            throw new IllegalStateException(
                    "Scheduler context factory returned "
                    + "an empty policy name.");
        }

        File schedulerOutputFile =
                new File(
                        objectiveOutputDirectory,
                        "counterfactual_results_"
                        + schedulerPolicy
                        + ".csv");

        CounterfactualSearchExecutor searchExecutor =
                new PerDecisionCounterfactualSearchExecutor(
                        configuration,
                        schedulerReplayEngine,
                        contextFactory,
                        requestedChangeType,
                        candidateSnapshotsByTaskId);

        try(CounterfactualResultLogger resultLogger =
                new CounterfactualResultLogger(
                        schedulerOutputFile)) {

            CounterfactualExperimentRunner experimentRunner =
                    new CounterfactualExperimentRunner(
                            searchExecutor,
                            resultLogger,
                            schedulerPolicy,
                            debugEnabled);

            CounterfactualExperimentSummary summary =
                    experimentRunner.run(
                            decisionStates);

            String summaryKey =
                    createSummaryKey(
                            requestedChangeType,
                            schedulerPolicy);

            summaries.put(
                    summaryKey,
                    summary);

            printSchedulerCompletion(
                    requestedChangeType,
                    schedulerPolicy,
                    schedulerOutputFile,
                    summary);
        }
    }

    /**
     * Creates a stable summary-map key.
     */
    private String createSummaryKey(
            DecisionChangeType requestedChangeType,
            String schedulerPolicy) {

        return requestedChangeType.name()
                + "::"
                + schedulerPolicy;
    }

    /**
     * Validates arguments supplied to run().
     */
    private void validateRunArguments(
            List<DecisionState> decisionStates,
            List<SchedulerReplayContextFactory> contextFactories) {

        if(decisionStates == null) {

            throw new IllegalArgumentException(
                    "Decision list cannot be null.");
        }

        if(decisionStates.isEmpty()) {

            throw new IllegalArgumentException(
                    "Decision list cannot be empty.");
        }

        if(contextFactories == null
                || contextFactories.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one scheduler context factory "
                    + "is required.");
        }
    }

    /**
     * Creates an output directory when it does not already exist.
     */
    private void createDirectoryIfNecessary(
            File directory)
            throws IOException {

        if(directory.exists()) {

            if(!directory.isDirectory()) {

                throw new IOException(
                        "Output path is not a directory: "
                        + directory.getAbsolutePath());
            }

            return;
        }

        if(!directory.mkdirs()) {

            throw new IOException(
                    "Unable to create output directory: "
                    + directory.getAbsolutePath());
        }
    }

    private void printObjectiveStart(
            DecisionChangeType requestedChangeType,
            File objectiveOutputDirectory,
            int decisionCount) {

        if(!debugEnabled) {

            return;
        }

        System.out.println();
        System.out.println(
                "================================================");

        System.out.println(
                "Starting counterfactual objective: "
                + requestedChangeType);

        System.out.println(
                "Compatible decisions: "
                + decisionCount);

        System.out.println(
                "Objective output directory: "
                + objectiveOutputDirectory
                .getAbsolutePath());

        System.out.println(
                "================================================");
    }

    private void printSkippedObjective(
            DecisionChangeType requestedChangeType) {

        if(!debugEnabled) {

            return;
        }

        System.out.println();
        System.out.println(
                "Skipping counterfactual objective "
                + requestedChangeType
                + ": no compatible decisions were supplied.");

        if(requestedChangeType
                == DecisionChangeType
                .FEASIBILITY_RESTORATION) {

            System.out.println(
                    "Feasibility restoration requires originally "
                    + "unsuccessful or unselected decisions.");
        }
    }

    private void printSchedulerCompletion(
            DecisionChangeType requestedChangeType,
            String schedulerPolicy,
            File schedulerOutputFile,
            CounterfactualExperimentSummary summary) {

        if(!debugEnabled) {

            return;
        }

        System.out.println();

        System.out.println(
                "Completed counterfactual experiment:");

        System.out.println(
                "Requested change type: "
                + requestedChangeType);

        System.out.println(
                "Scheduler: "
                + schedulerPolicy);

        System.out.println(
                "Output: "
                + schedulerOutputFile
                .getAbsolutePath());

        System.out.println(
                "Summary: "
                + summary);
    }

    private void printOverallCompletion(
            Map<String, CounterfactualExperimentSummary> summaries) {

        if(!debugEnabled) {

            return;
        }

        System.out.println();
        System.out.println(
                "================================================");

        System.out.println(
                "All compatible multi-objective, multi-scheduler "
                + "experiments completed.");

        System.out.println(
                "Completed experiment combinations: "
                + summaries.size());

        System.out.println(
                "================================================");
    }
}
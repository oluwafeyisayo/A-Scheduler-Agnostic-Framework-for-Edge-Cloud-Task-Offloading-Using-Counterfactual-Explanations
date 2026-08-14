package edu.boun.edgecloudsim.research.counterfactual.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualCandidate;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualCandidateGenerator;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualClassificationResult;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualClassifier;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualExperimentRunner;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualExperimentSummary;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualObjectiveFunction;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualResultLogger;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualResultRecord;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualResultRecordMapper;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualScore;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualSearchExecutor;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualSearchMode;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualSearchResult;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualStateSynchronizer;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualStateSynchronizer
        .SynchronizedCounterfactualState;

import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualValidator;

import edu.boun.edgecloudsim.research.counterfactual
        .DecisionReplayEngine;

import edu.boun.edgecloudsim.research.counterfactual
        .DecisionState;

import edu.boun.edgecloudsim.research.counterfactual
        .FeatureCategory;

import edu.boun.edgecloudsim.research.counterfactual
        .FeatureChange;

import edu.boun.edgecloudsim.research.counterfactual
        .FeasibilityResult;

import edu.boun.edgecloudsim.research.counterfactual
        .FeasibleCounterfactualSearchConfig;

import edu.boun.edgecloudsim.research.counterfactual
        .MultiSchedulerCounterfactualExperimentRunner;

import edu.boun.edgecloudsim.research.counterfactual
        .PerDecisionCounterfactualSearchExecutor;

import edu.boun.edgecloudsim.research.counterfactual
        .ReplayGuidedFeasibleCounterfactualSearch;

import edu.boun.edgecloudsim.research.counterfactual
        .ReplayState;

import edu.boun.edgecloudsim.research.counterfactual
        .SchedulerReplayContextFactory;

import edu.boun.edgecloudsim.research.counterfactual.comparison
        .DecisionChangeType;

import edu.boun.edgecloudsim.research.counterfactual.comparison
        .DecisionComparator;

import edu.boun.edgecloudsim.research.counterfactual.comparison
        .DecisionComparisonResult;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .SchedulerDecision;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

import edu.boun.edgecloudsim.research.counterfactual.replay.context
        .SchedulerReplayContext;

import edu.boun.edgecloudsim.research.counterfactual.replay.context.factory
        .LeastLoadedReplayContextFactory;

import edu.boun.edgecloudsim.research.counterfactual.replay.context.factory
        .MultiObjectiveReplayContextFactory;

import edu.boun.edgecloudsim.research.counterfactual.replay.context.factory
        .RandomReplayContextFactory;

import edu.boun.edgecloudsim.research.counterfactual.replay.context.factory
        .RoundRobinReplayContextFactory;

import edu.boun.edgecloudsim.research.counterfactual.replay.engine
        .SchedulerReplayEngine;

import edu.boun.edgecloudsim.research.counterfactual.replay.io
        .CandidateSnapshotCsvLoader;

import edu.boun.edgecloudsim.research.counterfactual.replay.state
        .LeastLoadedPolicyState;

import edu.boun.edgecloudsim.research.counterfactual.replay.strategy
        .LeastLoadedReplayStrategy;

import edu.boun.edgecloudsim.research.counterfactual.replay.strategy
        .MultiObjectiveReplayStrategy;

import edu.boun.edgecloudsim.research.counterfactual.replay.strategy
        .RandomReplayStrategy;

import edu.boun.edgecloudsim.research.counterfactual.replay.strategy
        .RoundRobinReplayStrategy;

import edu.boun.edgecloudsim.research.counterfactual.verification
        .CounterfactualVerificationResult;

import edu.boun.edgecloudsim.research.counterfactual.verification
        .CounterfactualVerifier;

import edu.boun.edgecloudsim.research.counterfactual.replay.context.factory
.PpoReplayContextFactory;

import edu.boun.edgecloudsim.research.counterfactual.replay.strategy
.PpoReplayStrategy;

import edu.boun.edgecloudsim.research.counterfactual.evaluation.config
        .EvaluationExperimentConfig;

import edu.boun.edgecloudsim.research.counterfactual.evaluation.directory
        .EvaluationDirectoryManager;

import edu.boun.edgecloudsim.research.counterfactual.evaluation.reporting
        .EvaluationReportWriter;


public class EvaluationSchedulerReplayEngineTest {

    /*
     * Stage A functional-comparison sample caps.
     *
     * Execution-target and destination-change experiments use selected
     * decisions. Feasibility-restoration experiments use genuine
     * NO_FEASIBLE_VM decisions.
     */
    private static final int
            MAX_SELECTED_DECISIONS_PER_OBJECTIVE = 100;

    private static final int
            MAX_RESTORATION_DECISIONS_PER_OBJECTIVE = 100;

    private static final String SCENARIO =
            "TWO_TIER_WITH_EO";

    public static void main(String[] args) {

        int iterationNumber =
                parsePositiveIntegerArgument(
                        args,
                        0,
                        "iteration");

        int deviceCount =
                parsePositiveIntegerArgument(
                        args,
                        1,
                        "device count");

        String simulationConfigPath =
                requireArgument(
                        args,
                        2,
                        "simulation configuration file");

        try {

            EvaluationExperimentConfig evaluationConfig =
                    EvaluationExperimentConfig
                    .createStageB(
                            new File(
                                    simulationConfigPath),
                            deviceCount);

            EvaluationDirectoryManager directoryManager =
                    new EvaluationDirectoryManager(
                            evaluationConfig);

            String iterationFolder =
                    directoryManager
                    .getIterationDirectory(
                            deviceCount,
                            iterationNumber)
                    .getPath();

            String decisionCsvFilePath =
                    directoryManager
                    .getDecisionCsvFile(
                            deviceCount,
                            iterationNumber,
                            "LEAST_LOADED")
                    .getPath();

            String candidateCsvFilePath =
                    directoryManager
                    .getCandidateCsvFile(
                            deviceCount,
                            iterationNumber,
                            "LEAST_LOADED")
                    .getPath();

            DecisionReplayEngine decisionReplayEngine =
                    new DecisionReplayEngine();

            /*
             * This currently loads selected decisions.
             *
             * Therefore, execution-target and destination-change
             * experiments will run. Feasibility restoration will be
             * skipped unless unsuccessful decisions are added to this
             * collection.
             */
        	List<DecisionState> allDecisions =
        	        decisionReplayEngine
        	        .loadDecisions(
        	                decisionCsvFilePath);

        	List<DecisionState> selectedDecisions =
        	        new ArrayList<DecisionState>();

        	List<DecisionState> unsuccessfulDecisions =
        	        new ArrayList<DecisionState>();

        	for(DecisionState decision
        	        : allDecisions) {

        	    if(decision == null) {

        	        continue;
        	    }

        	    if(decision.isSelectedDecision()) {

        	        selectedDecisions.add(
        	                decision);
        	    }
        	    else if(isEligibleForFeasibilityRestoration(
        	            decision)) {

        	        unsuccessfulDecisions.add(
        	                decision);
        	    }
        	}

        	System.out.println();
        	System.out.println(
        	        "Decision dataset summary:");

        	System.out.println(
        	        "All decisions: "
        	        + allDecisions.size());

        	System.out.println(
        	        "Selected decisions: "
        	        + selectedDecisions.size());

        	System.out.println(
        	        "Unsuccessful decisions: "
        	        + unsuccessfulDecisions.size());

        	if(selectedDecisions.isEmpty()) {

        	    System.out.println(
        	            "No selected decisions were found.");

        	    return;
        	}

        	/*
        	 * Retain the first successful decision for the existing replay,
        	 * synchronization, and classifier demonstrations.
        	 */
        	DecisionState originalDecision =
        	        selectedDecisions.get(0);

            double requiredCpu =
                    originalDecision
                    .getRequiredCpuPercent();

            CandidateSnapshotCsvLoader candidateLoader =
                    new CandidateSnapshotCsvLoader();

            Map<Integer, List<VmCandidateSnapshot>>
                    candidatesByTaskId =
                    candidateLoader.load(
                            candidateCsvFilePath,
                            decisionCsvFilePath);

            System.out.println(
                    "Tasks with candidate snapshots: "
                    + candidatesByTaskId.size());

            List<VmCandidateSnapshot> originalCandidates =
                    candidatesByTaskId.get(
                            originalDecision.getTaskId());

            if(originalCandidates == null
                    || originalCandidates.isEmpty()) {

                throw new IllegalStateException(
                        "No candidate snapshots found for task "
                        + originalDecision.getTaskId());
            }

            ReplayState originalReplayState =
                    ReplayState
                    .from(originalDecision)

                    .withSelectedAvailableCpuPercent(
                            100.0)

                    .withBestAvailableCpuPercent(
                            100.0)

                    .withCandidateVmCount(
                            originalCandidates.size())

                    .withFeasibleVmCount(
                            countCpuFeasibleCandidates(
                                    originalCandidates,
                                    requiredCpu))

                    .build();

            SchedulerReplayContext originalContext =
                    new SchedulerReplayContext(
                            originalReplayState,
                            originalCandidates,
                            new LeastLoadedPolicyState());
            System.out.println();
            System.out.println(
                    "========== Destination Coverage ==========");

            System.out.println(
                    "EDGE candidates : "
                    + originalContext
                    .getCandidateCountForDestination(
                            "EDGE"));

            System.out.println(
                    "CLOUD candidates: "
                    + originalContext
                    .getCandidateCountForDestination(
                            "CLOUD"));

            System.out.println(
                    "MOBILE candidates: "
                    + originalContext
                    .getCandidateCountForDestination(
                            "MOBILE"));

            System.out.println(
                    "Available destinations: "
                    + originalContext
                    .getAvailableDestinations());

            System.out.println(
                    "Multiple destinations: "
                    + originalContext
                    .hasMultipleDestinations());

            System.out.println(
                    "=========================================");
            
            SchedulerReplayEngine schedulerReplayEngine =
                    new SchedulerReplayEngine()

                    .register(
                            new LeastLoadedReplayStrategy())

                    .register(
                            new RoundRobinReplayStrategy())

                    .register(
                            new RandomReplayStrategy())

                    .register(
                            new MultiObjectiveReplayStrategy())

                    .register(
                            new PpoReplayStrategy());

            System.out.println(
                    "Registered policies: "
                    + schedulerReplayEngine
                    .getRegisteredPolicies());

            FeasibleCounterfactualSearchConfig searchConfig =
                    FeasibleCounterfactualSearchConfig
                    .createDefault(
                            CounterfactualSearchMode
                            .ADAPTIVE_BEST_FIRST);

            File reportDirectory =
                    directoryManager
                    .getReportDirectory(
                            deviceCount,
                            iterationNumber);

            EvaluationReportWriter reportWriter =
                    new EvaluationReportWriter(
                            evaluationConfig,
                            directoryManager,
                            deviceCount,
                            iterationNumber,
                            searchConfig);

            CounterfactualCandidateGenerator candidateGenerator =
                    new CounterfactualCandidateGenerator(
                            searchConfig,
                            true);

            List<FeatureChange> generatedChanges =
                    candidateGenerator
                    .generateSingleFeatureChanges(
                            originalReplayState);

            System.out.println();
            System.out.println(
                    "RG-FCS candidate generation:");

            System.out.println(
                    "Generated feature changes: "
                    + generatedChanges.size());

            int displayedChangeCount =
                    Math.min(
                            10,
                            generatedChanges.size());

            for(int index = 0;
                    index < displayedChangeCount;
                    index++) {

                System.out.println(
                        "Generated change "
                        + (index + 1)
                        + ": "
                        + generatedChanges.get(index));
            }

            System.out.println();
            System.out.println(
                    "FCS configuration:");

            System.out.println(
                    searchConfig);

            for(FeasibleCounterfactualSearchConfig
                    .FeatureSearchSpec specification
                    : searchConfig
                    .getFeatureSearchSpecifications()) {

                System.out.println(
                        specification);
            }

            SchedulerDecision originalReplayDecision =
                    schedulerReplayEngine.replayBaseline(
                            originalContext);

            if(originalReplayDecision == null) {

                throw new IllegalStateException(
                        "Original scheduler replay returned null.");
            }

            CounterfactualStateSynchronizer synchronizer =
                    new CounterfactualStateSynchronizer();

            ReplayGuidedFeasibleCounterfactualSearch rgFcsSearch =
                    new ReplayGuidedFeasibleCounterfactualSearch(
                            searchConfig,
                            schedulerReplayEngine,
                            new LeastLoadedReplayContextFactory());

            /*
             * Run the direct RG-FCS demonstration for every objective.
             */
            Map<DecisionChangeType, CounterfactualSearchResult>
                    automatedSearchResults =
                    new LinkedHashMap<DecisionChangeType,
                            CounterfactualSearchResult>();

            for(DecisionChangeType requestedChangeType
                    : DecisionChangeType.values()) {

                System.out.println();
                System.out.println(
                        "==============================================");

                System.out.println(
                        "Requested counterfactual type: "
                        + requestedChangeType);

                CounterfactualSearchResult result =
                        rgFcsSearch.search(
                                originalReplayState,
                                originalCandidates,
                                originalReplayDecision,
                                requestedChangeType);

                automatedSearchResults.put(
                        requestedChangeType,
                        result);

                System.out.println(
                        result);
            }

            /*
             * Preserve one result for the existing single-result logging
             * demonstration.
             */
            CounterfactualSearchResult automatedSearchResult =
                    automatedSearchResults.get(
                            DecisionChangeType
                            .EXECUTION_TARGET_CHANGE);

            if(automatedSearchResult == null) {

                throw new IllegalStateException(
                        "No execution-target search result "
                        + "was generated.");
            }
            
            List<DecisionState> batchDecisions =
                    createMixedDecisionBatch(
                            selectedDecisions,
                            unsuccessfulDecisions,
                            candidatesByTaskId,
                            10,
                            10);

            System.out.println();
            System.out.println(
                    "Mixed counterfactual batch:");

            System.out.println(
                    "Total batch decisions: "
                    + batchDecisions.size());

            System.out.println(
                    "Selected batch decisions: "
                    + countSelectedDecisions(
                            batchDecisions));

            System.out.println(
                    "Unsuccessful batch decisions: "
                    + countUnsuccessfulDecisions(
                            batchDecisions));



            /*
             * Retained as a single-objective batch demonstration.
             */
            CounterfactualSearchExecutor searchExecutor =
                    new PerDecisionCounterfactualSearchExecutor(
                            searchConfig,
                            schedulerReplayEngine,
                            new LeastLoadedReplayContextFactory(),
                            DecisionChangeType
                            .EXECUTION_TARGET_CHANGE,
                            candidatesByTaskId);

            System.out.println();
            System.out.println(
                    "Automated RG-FCS search result:");

            System.out.println(
                    automatedSearchResult);

            File counterfactualOutputFile =
                    new File(
                            directoryManager
                            .getCounterfactualResultDirectory(
                                    deviceCount,
                                    iterationNumber),
                            "counterfactual_results_test_ITE"
                            + iterationNumber
                            + ".csv");

            File batchOutputFile =
                    new File(
                            directoryManager
                            .getCounterfactualResultDirectory(
                                    deviceCount,
                                    iterationNumber),
                            "counterfactual_batch_test_ITE"
                            + iterationNumber
                            + ".csv");

            try(CounterfactualResultLogger batchLogger =
                    new CounterfactualResultLogger(
                            batchOutputFile)) {

                CounterfactualExperimentRunner experimentRunner =
                        new CounterfactualExperimentRunner(
                                searchExecutor,
                                batchLogger,
                                "LEAST_LOADED",
                                true);

                CounterfactualExperimentSummary experimentSummary =
                        experimentRunner.run(
                                batchDecisions);

                System.out.println();
                System.out.println(
                        "RG-FCS batch experiment summary:");

                System.out.println(
                        experimentSummary);
            }

            CounterfactualResultRecord resultRecord =
                    CounterfactualResultRecordMapper.from(
                            automatedSearchResult,
                            originalReplayDecision
                            .getSchedulerPolicy());

            try(CounterfactualResultLogger resultLogger =
                    new CounterfactualResultLogger(
                            counterfactualOutputFile)) {

                resultLogger.log(
                        resultRecord);
            }

            System.out.println();
            System.out.println(
                    "Counterfactual result written to:");

            System.out.println(
                    counterfactualOutputFile
                    .getAbsolutePath());

            if(generatedChanges.isEmpty()) {

                throw new IllegalStateException(
                        "No candidate feature changes were generated.");
            }

            FeatureChange firstGeneratedChange =
                    generatedChanges.get(0);

            SynchronizedCounterfactualState synchronizedState =
                    synchronizer.synchronize(
                            originalReplayState,
                            originalCandidates,
                            originalReplayDecision,
                            firstGeneratedChange);

            System.out.println();
            System.out.println(
                    "RG-FCS synchronized state:");

            System.out.println(
                    "Applied change: "
                    + synchronizedState
                    .getFeatureChange());

            System.out.println(
                    "Replay selected CPU: "
                    + synchronizedState
                    .getReplayState()
                    .getSelectedAvailableCpuPercent());

            for(VmCandidateSnapshot candidate
                    : synchronizedState.getVmCandidates()) {

                System.out.println(
                        "Candidate "
                        + candidate.getCandidateIndex()
                        + " VM "
                        + candidate.getVmId()
                        + " available CPU = "
                        + candidate.getAvailableCpuPercent());
            }

            System.out.println();
            System.out.println(
                    "Original scheduler replay:");

            System.out.println(
                    originalReplayDecision);

            List<VmCandidateSnapshot> modifiedCandidates =
                    reduceCandidateCpu(
                            originalCandidates,
                            0,
                            28.0);

            FeatureChange cpuChange =
                    new FeatureChange(
                            FeasibleCounterfactualSearchConfig
                            .SELECTED_AVAILABLE_CPU_PERCENT,
                            FeatureCategory.COMPUTE,
                            100.0,
                            28.0,
                            0.0,
                            100.0,
                            true,
                            true);

            ReplayState modifiedReplayState =
                    ReplayState
                    .from(originalDecision)

                    .withBestAvailableCpuPercent(
                            80.0)

                    .withCandidateVmCount(
                            modifiedCandidates.size())

                    .withFeasibleVmCount(
                            countCpuFeasibleCandidates(
                                    modifiedCandidates,
                                    requiredCpu))

                    .withNetworkLatency(
                            1.0)

                    .withAvailableBandwidth(
                            20.0)

                    .withTaskLength(
                            5000.0)

                    .withUploadSize(
                            20.0)

                    .applyFeatureChange(
                            cpuChange)

                    .build();

            CounterfactualObjectiveFunction objectiveFunction =
                    new CounterfactualObjectiveFunction(
                            searchConfig);

            CounterfactualScore counterfactualScore =
                    objectiveFunction.score(
                            modifiedReplayState);

            System.out.println();
            System.out.println(
                    "Counterfactual objective score:");

            System.out.println(
                    counterfactualScore);

            SchedulerReplayContext modifiedContext =
                    new SchedulerReplayContext(
                            modifiedReplayState,
                            modifiedCandidates,
                            new LeastLoadedPolicyState());

            SchedulerDecision modifiedReplayDecision =
                    schedulerReplayEngine.replay(
                            modifiedContext);

            DecisionComparator comparator =
                    new DecisionComparator();

            DecisionComparisonResult comparison =
                    comparator.compare(
                            modifiedReplayState,
                            modifiedReplayDecision);

            CounterfactualValidator validator =
                    new CounterfactualValidator();

            FeasibilityResult feasibilityResult =
                    validator.validate(
                            modifiedReplayState,
                            modifiedCandidates);

            System.out.println();
            System.out.println(
                    "Counterfactual search mode: "
                    + searchConfig.getSearchMode());

            System.out.println(
                    "Feasibility:");

            System.out.println(
                    feasibilityResult);

            System.out.println(
                    "Selected resource feasible: "
                    + feasibilityResult
                    .isSelectedResourceFeasible());

            System.out.println(
                    "Candidate set feasible: "
                    + feasibilityResult
                    .isCandidateSetFeasible());

            System.out.println(
                    "Operationally feasible: "
                    + feasibilityResult
                    .isOperationallyFeasible());

            CounterfactualVerifier verifier =
                    new CounterfactualVerifier(
                            schedulerReplayEngine);

            /*
             * Store all verification results so they remain available
             * after the loop.
             */
            Map<DecisionChangeType,
                    CounterfactualVerificationResult>
                    verificationResults =
                    new LinkedHashMap<DecisionChangeType,
                            CounterfactualVerificationResult>();

            for(DecisionChangeType requestedObjective
                    : DecisionChangeType.values()) {

                CounterfactualVerificationResult result =
                        verifier.verify(
                                modifiedContext,
                                feasibilityResult,
                                requestedObjective);

                verificationResults.put(
                        requestedObjective,
                        result);

                System.out.println();
                System.out.println(
                        "Verification for "
                        + requestedObjective
                        + ":");

                System.out.println(
                        result);
            }

            /*
             * Preserve the execution-target result for the existing
             * classifier and candidate demonstration.
             */
            CounterfactualVerificationResult verificationResult =
                    verificationResults.get(
                            DecisionChangeType
                            .EXECUTION_TARGET_CHANGE);

            if(verificationResult == null) {

                throw new IllegalStateException(
                        "Execution-target verification result "
                        + "was not generated.");
            }

            List<FeatureChange> candidateChanges =
                    modifiedReplayState
                    .getAppliedChanges();

//            List<SchedulerReplayContextFactory> schedulerFactories =
//                    Arrays.<SchedulerReplayContextFactory>asList(
//                            new LeastLoadedReplayContextFactory(),
//                            new RoundRobinReplayContextFactory(),
//                            new RandomReplayContextFactory());
//
//            File multiSchedulerOutputDirectory =
//                    new File(
//                            "sim_results/ite1/"
//                            + "counterfactual_results/"
//                            + "multi_scheduler_10_decisions");
//
//            MultiSchedulerCounterfactualExperimentRunner
//                    multiSchedulerRunner =
//                    new MultiSchedulerCounterfactualExperimentRunner(
//                            searchConfig,
//                            schedulerReplayEngine,
//                            multiSchedulerOutputDirectory,
//                            true,
//                            candidatesByTaskId);
//
//            Map<String, CounterfactualExperimentSummary>
//                    schedulerSummaries =
//                    multiSchedulerRunner.run(
//                            batchDecisions,
//                            schedulerFactories);
            
            Map<String, CounterfactualExperimentSummary>
            schedulerSummaries =
            new LinkedHashMap<String,
                    CounterfactualExperimentSummary>();

	    runSchedulerDataset(
	            "LEAST_LOADED",
	            decisionReplayEngine,
	            schedulerReplayEngine,
	            searchConfig,
	            new LeastLoadedReplayContextFactory(),
	            directoryManager
                    .getSchedulerSpecificResultDirectory(
                            deviceCount,
                            iterationNumber),
	            schedulerSummaries,
	            iterationNumber,
	            deviceCount,
	            directoryManager,
	            reportWriter);
	
	    runSchedulerDataset(
	            "ROUND_ROBIN",
	            decisionReplayEngine,
	            schedulerReplayEngine,
	            searchConfig,
	            new RoundRobinReplayContextFactory(),
	            directoryManager
                    .getSchedulerSpecificResultDirectory(
                            deviceCount,
                            iterationNumber),
	            schedulerSummaries,
	            iterationNumber,
	            deviceCount,
	            directoryManager,
	            reportWriter);
	
	    runSchedulerDataset(
	            "RANDOM",
	            decisionReplayEngine,
	            schedulerReplayEngine,
	            searchConfig,
	            new RandomReplayContextFactory(),
	            directoryManager
                    .getSchedulerSpecificResultDirectory(
                            deviceCount,
                            iterationNumber),
	            schedulerSummaries,
	            iterationNumber,
	            deviceCount,
	            directoryManager,
	            reportWriter);
	
	    runSchedulerDataset(
	            "MULTI_OBJECTIVE",
	            decisionReplayEngine,
	            schedulerReplayEngine,
	            searchConfig,
	            new MultiObjectiveReplayContextFactory(),
	            directoryManager
                    .getSchedulerSpecificResultDirectory(
                            deviceCount,
                            iterationNumber),
	            schedulerSummaries,
	            iterationNumber,
	            deviceCount,
	            directoryManager,
	            reportWriter);
	    
	    runSchedulerDataset(
	            "PPO",
	            decisionReplayEngine,
	            schedulerReplayEngine,
	            searchConfig,
	            new PpoReplayContextFactory(),
	            directoryManager
                    .getSchedulerSpecificResultDirectory(
                            deviceCount,
                            iterationNumber),
	            schedulerSummaries,
	            iterationNumber,
	            deviceCount,
	            directoryManager,
	            reportWriter);
	
            reportWriter.close();

            System.out.println(
                    "Stage B reports written to: "
                    + reportDirectory.getAbsolutePath());

	            boolean candidateValid =
	                    modifiedReplayState != null
	                    && modifiedContext != null;
	
	            boolean replaySuccessful =
	                    verificationResult
	                    .isReplaySuccessful();
	
	            boolean targetAchieved =
	                    verificationResult
	                    .isTargetAchieved();
	
	            boolean operationallyFeasible =
	                    feasibilityResult != null
	                    && feasibilityResult
	                    .isOperationallyFeasible();
	
	            int candidateId =
	                    1;

            CounterfactualClassifier classifier =
                    new CounterfactualClassifier();
            
            

            CounterfactualClassificationResult
                    classificationResult =
                    classifier.classify(
                            originalDecision.getTaskId(),
                            candidateId,
                            candidateValid,
                            replaySuccessful,
                            targetAchieved,
                            operationallyFeasible);

            CounterfactualCandidate candidate =
                    new CounterfactualCandidate.Builder(
                            originalDecision.getTaskId(),
                            candidateId)

                    .addFeatureChanges(
                            candidateChanges)

                    .withReplayState(
                            modifiedReplayState)

                    .withFeasibilityResult(
                            feasibilityResult)

                    .withSchedulerDecision(
                            modifiedReplayDecision)

                    .withVerificationResult(
                            verificationResult)

                    .withClassificationResult(
                            classificationResult)

                    .withScore(
                            counterfactualScore)

                    .withEvaluated(
                            true)

                    .withAccepted(
                            classificationResult
                            .isAcceptedCounterfactual())

                    .withReason(
                            classificationResult
                            .getReason())

                    .build();

            System.out.println();
            System.out.println(
                    "Counterfactual candidate:");

            System.out.println(
                    candidate);

            System.out.println();
            System.out.println(
                    "Counterfactual verification:");

            System.out.println(
                    verificationResult);

            System.out.println(
                    "Candidate classification: "
                    + candidate.getClassification());

            System.out.println(
                    "Candidate accepted: "
                    + candidate.isAccepted());

            System.out.println();
            System.out.println(
                    "Observed change type: "
                    + comparison.getChangeType());

            System.out.println(
                    "Satisfies execution-target request: "
                    + comparison.satisfies(
                            DecisionChangeType
                            .EXECUTION_TARGET_CHANGE));

            System.out.println(
                    "Satisfies destination-change request: "
                    + comparison.satisfies(
                            DecisionChangeType
                            .DESTINATION_CHANGE));

            System.out.println();
            System.out.println(
                    "Modified scheduler replay:");

            System.out.println(
                    modifiedReplayDecision);

            System.out.println();
            System.out.println(
                    "Decision comparison:");

            System.out.println(
                    comparison);

            System.out.println();
            System.out.println(
                    "Replay successful: "
                    + modifiedReplayDecision
                    .wasSuccessfullyReplayed());

            System.out.println(
                    "Execution target changed: "
                    + comparison
                    .isExecutionTargetChanged());

            System.out.println(
                    "Offloading decision flipped: "
                    + comparison
                    .isOffloadingDecisionFlipped());

            System.out.println(
                    "Original replay VM: "
                    + originalReplayDecision
                    .getSelectedVmId());

            System.out.println(
                    "Modified replay VM: "
                    + modifiedReplayDecision
                    .getSelectedVmId());

            System.out.println();
            System.out.println(
                    "Multi-scheduler RG-FCS summaries:");

            for(Map.Entry<String,
                    CounterfactualExperimentSummary> entry
                    : schedulerSummaries.entrySet()) {

                System.out.println(
                        entry.getKey()
                        + " -> "
                        + entry.getValue());
            }
        }
        catch(IOException exception) {

            System.err.println(
                    "Unable to load or write experiment data: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
        catch(IllegalArgumentException exception) {

            System.err.println(
                    "Invalid replay input: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
        catch(IllegalStateException exception) {

            System.err.println(
                    "Replay experiment failed: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
    }
    
    private static void runSchedulerDataset(
            String schedulerPolicy,
            DecisionReplayEngine decisionReplayEngine,
            SchedulerReplayEngine schedulerReplayEngine,
            FeasibleCounterfactualSearchConfig searchConfig,
            SchedulerReplayContextFactory contextFactory,
            File outputRoot,
            Map<String, CounterfactualExperimentSummary> summaries,
            int iterationNumber,
            int deviceCount,
            EvaluationDirectoryManager directoryManager,
            EvaluationReportWriter reportWriter)
            throws IOException {

        String decisionPath =
                directoryManager
                .getDecisionCsvFile(
                        deviceCount,
                        iterationNumber,
                        schedulerPolicy)
                .getPath();

        String candidatePath =
                directoryManager
                .getCandidateCsvFile(
                        deviceCount,
                        iterationNumber,
                        schedulerPolicy)
                .getPath();

        List<DecisionState> decisions =
                decisionReplayEngine.loadDecisions(decisionPath);

        CandidateSnapshotCsvLoader candidateLoader =
                new CandidateSnapshotCsvLoader();

        Map<Integer, List<VmCandidateSnapshot>> candidatesByTaskId =
                candidateLoader.load(
                        candidatePath,
                        decisionPath);

        List<DecisionState> selectedDecisions =
                new ArrayList<DecisionState>();

        List<DecisionState> unsuccessfulDecisions =
                new ArrayList<DecisionState>();

        int missingCandidateSnapshotCount = 0;
        int invalidRoundRobinProvenanceCount = 0;

        for(DecisionState decision : decisions) {

            if(decision == null) {
                continue;
            }

            if(!hasCandidateSnapshots(
                    decision,
                    candidatesByTaskId)) {

                missingCandidateSnapshotCount++;
                continue;
            }

            if("ROUND_ROBIN".equalsIgnoreCase(
                    schedulerPolicy)
                    && "EDGE".equalsIgnoreCase(
                            decision.getAttemptedDestination())
                    && decision.getRoundRobinStartingCandidateIndex() < 0
                    && decision.isSelectedDecision()) {

                /*
                 * Only selected ROUND_ROBIN decisions require the
                 * exact logged cursor for deterministic replay.
                 *
                 * Original NO_FEASIBLE_VM decisions are now replayed
                 * using the deterministic fallback cursor inside
                 * RoundRobinReplayContextFactory.
                 */
                invalidRoundRobinProvenanceCount++;
                continue;
            }

            if(decision.isSelectedDecision()) {

                selectedDecisions.add(
                        decision);
            }
            else if(isEligibleForFeasibilityRestoration(
                    decision)) {

                unsuccessfulDecisions.add(
                        decision);
            }
        }

        List<DecisionState> selectedBatch =
                takeFirstDecisions(
                        selectedDecisions,
                        MAX_SELECTED_DECISIONS_PER_OBJECTIVE);

        List<DecisionState> restorationBatch =
                takeFirstDecisions(
                        unsuccessfulDecisions,
                        MAX_RESTORATION_DECISIONS_PER_OBJECTIVE);

        /*
         * Write one eligibility row per scheduler before executing the
         * objective-specific searches.
         */
        reportWriter.writeEligibility(
                schedulerPolicy,
                decisions.size(),
                selectedDecisions.size(),
                unsuccessfulDecisions.size(),
                selectedBatch.size(),
                restorationBatch.size(),
                missingCandidateSnapshotCount,
                invalidRoundRobinProvenanceCount);

System.out.println();
        System.out.println(
                schedulerPolicy + " restoration batch:");

        for(DecisionState decision : restorationBatch) {

            System.out.println(
                    "task="
                    + decision.getTaskId()
                    + ", outcome="
                    + decision.getDecisionOutcome()
                    + ", selectedVm="
                    + decision.getSelectedVmId()
                    + ", cursor="
                    + decision.getRoundRobinStartingCandidateIndex()
                    + ", attempted="
                    + decision.getAttemptedDestination());
        }

        System.out.println();
        System.out.println(
                "==============================================");
        System.out.println(
                "Scheduler dataset: "
                + schedulerPolicy);
        System.out.println(
                "Loaded decisions: "
                + decisions.size());
        System.out.println(
                "Eligible selected decisions: "
                + selectedDecisions.size());
        System.out.println(
                "Eligible unsuccessful decisions: "
                + unsuccessfulDecisions.size());
        System.out.println(
                "Selected experiment batch size: "
                + selectedBatch.size());
        System.out.println(
                "Restoration experiment batch size: "
                + restorationBatch.size());
        System.out.println(
                "Rows without candidate snapshots: "
                + missingCandidateSnapshotCount);

        if("ROUND_ROBIN".equalsIgnoreCase(schedulerPolicy)) {
            System.out.println(
                    "Rows excluded for missing ROUND_ROBIN cursor: "
                    + invalidRoundRobinProvenanceCount);
        }

        System.out.println(
                "==============================================");

        for(DecisionChangeType objective
                : DecisionChangeType.values()) {

            boolean restoration =
                    objective
                    == DecisionChangeType.FEASIBILITY_RESTORATION;

            List<DecisionState> compatibleDecisions =
                    restoration
                    ? restorationBatch
                    : selectedBatch;

            if(compatibleDecisions.isEmpty()) {

                System.out.println(
                        "[RG-FCS Objective Skipped] policy="
                        + schedulerPolicy
                        + ", objective="
                        + objective
                        + ", reason="
                        + (
                                restoration
                                ? "NO_ELIGIBLE_UNSUCCESSFUL_DECISIONS"
                                : "NO_ELIGIBLE_SELECTED_DECISIONS"
                        ));

                CounterfactualExperimentSummary zeroSummary =
                        createZeroExperimentSummary();

                summaries.put(
                        objective
                        + "::"
                        + schedulerPolicy,
                        zeroSummary);

                reportWriter.writeObjectiveSummary(
                        schedulerPolicy,
                        objective,
                        zeroSummary);

                reportWriter.writeObjectiveSeparation(
                        schedulerPolicy,
                        objective,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0);

                continue;
            }

            PerDecisionCounterfactualSearchExecutor executor =
                    new PerDecisionCounterfactualSearchExecutor(
                            searchConfig,
                            schedulerReplayEngine,
                            contextFactory,
                            objective,
                            candidatesByTaskId);

            File outputFile =
                    new File(
                            outputRoot,
                            objective
                            + "/counterfactual_results_"
                            + schedulerPolicy
                            + ".csv");

            try(CounterfactualResultLogger logger =
                    new CounterfactualResultLogger(
                            outputFile)) {

                CounterfactualExperimentRunner runner =
                        new CounterfactualExperimentRunner(
                                executor,
                                logger,
                                schedulerPolicy,
                                true);

                CounterfactualExperimentSummary summary =
                        runner.run(
                                compatibleDecisions);

                summaries.put(
                        objective
                        + "::"
                        + schedulerPolicy,
                        summary);

                reportWriter.writeObjectiveSummary(
                        schedulerPolicy,
                        objective,
                        summary);

                reportWriter.writeObjectiveSeparation(
                        schedulerPolicy,
                        objective,
                        executor);

                executor.printObjectiveSeparationSummary();
            }
        }
    }

    private static CounterfactualExperimentSummary
            createZeroExperimentSummary() {

        return new CounterfactualExperimentSummary(
                0,
                0,
                0,
                0,
                0,

                0L,
                0L,
                0L,

                0L,
                0L,
                0L,

                0L,
                0L,
                0L,

                0,
                0,
                0,
                0,

                0L);
    }

    private static int parsePositiveIntegerArgument(
            String[] args,
            int index,
            String argumentName) {

        String value =
                requireArgument(
                        args,
                        index,
                        argumentName);

        try {

            int parsedValue =
                    Integer.parseInt(
                            value);

            if(parsedValue < 1) {

                throw new IllegalArgumentException(
                        argumentName
                        + " must be positive.");
            }

            return parsedValue;
        }
        catch(NumberFormatException exception) {

            throw new IllegalArgumentException(
                    argumentName
                    + " must be an integer.",
                    exception);
        }
    }

    private static String requireArgument(
            String[] args,
            int index,
            String argumentName) {

        if(args == null
                || args.length <= index
                || args[index] == null
                || args[index].trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Missing required program argument: "
                    + argumentName
                    + ".");
        }

        return args[index].trim();
    }


    private static List<DecisionState> takeFirstDecisions(
            List<DecisionState> decisions,
            int maximumCount) {

        List<DecisionState> result =
                new ArrayList<DecisionState>();

        if(decisions == null
                || maximumCount <= 0) {

            return result;
        }

        for(DecisionState decision : decisions) {

            if(result.size() >= maximumCount) {
                break;
            }

            if(decision != null) {
                result.add(decision);
            }
        }

        return result;
    }

    private static int countCpuFeasibleCandidates(
            List<VmCandidateSnapshot> candidates,
            double requiredCpuPercent) {

        int feasibleCount =
                0;

        if(candidates == null) {

            return feasibleCount;
        }

        for(VmCandidateSnapshot candidate
                : candidates) {

            if(candidate == null) {

                continue;
            }

            if(candidate.isOperationallyAvailable()
                    && candidate.getAvailableCpuPercent()
                    >= requiredCpuPercent) {

                feasibleCount++;
            }
        }

        return feasibleCount;
    }

    private static List<VmCandidateSnapshot>
            reduceCandidateCpu(
                    List<VmCandidateSnapshot> originalCandidates,
                    int candidateIndex,
                    double newAvailableCpu) {

        List<VmCandidateSnapshot> modified =
                new ArrayList<VmCandidateSnapshot>();

        if(originalCandidates == null) {

            return modified;
        }

        for(VmCandidateSnapshot candidate
                : originalCandidates) {

            if(candidate == null) {

                continue;
            }

            if(candidate.getCandidateIndex()
                    == candidateIndex) {

                modified.add(
                        candidate
                        .withAvailableCpuPercent(
                                newAvailableCpu));
            }
            else {

                modified.add(
                        candidate);
            }
        }

        return modified;
    }
    
    private static List<DecisionState> createMixedDecisionBatch(
            List<DecisionState> selectedDecisions,
            List<DecisionState> unsuccessfulDecisions,
            Map<Integer, List<VmCandidateSnapshot>>
                    candidatesByTaskId,
            int maximumSelectedCount,
            int maximumUnsuccessfulCount) {

        List<DecisionState> batch =
                new ArrayList<DecisionState>();

        if(selectedDecisions != null) {

            for(DecisionState decision
                    : selectedDecisions) {

                if(batch.size()
                        >= maximumSelectedCount) {

                    break;
                }

                if(hasCandidateSnapshots(
                        decision,
                        candidatesByTaskId)) {

                    batch.add(
                            decision);
                }
            }
        }

        int unsuccessfulAdded =
                0;

        if(unsuccessfulDecisions != null) {

            for(DecisionState decision
                    : unsuccessfulDecisions) {

                if(unsuccessfulAdded
                        >= maximumUnsuccessfulCount) {

                    break;
                }

                if(isEligibleForFeasibilityRestoration(
                        decision)
                        && hasCandidateSnapshots(
                                decision,
                                candidatesByTaskId)) {

                    batch.add(
                            decision);

                    unsuccessfulAdded++;
                }
            }
        }

        return batch;
    }

    private static boolean hasCandidateSnapshots(
            DecisionState decision,
            Map<Integer, List<VmCandidateSnapshot>>
                    candidatesByTaskId) {

        if(decision == null
                || candidatesByTaskId == null) {

            return false;
        }

        List<VmCandidateSnapshot> candidates =
                candidatesByTaskId.get(
                        decision.getTaskId());

        return candidates != null
                && !candidates.isEmpty();
    }

    
    /**
     * Returns true only for a genuine scheduler-level failure that can
     * serve as the original state of a feasibility-restoration
     * counterfactual.
     *
     * A task that was assigned to a VM and later failed during upload,
     * download, or execution is not a feasibility-restoration case.
     */
    private static boolean isEligibleForFeasibilityRestoration(
            DecisionState decision) {

        if(decision == null
                || decision.isSelectedDecision()) {

            return false;
        }

        boolean noFeasibleVmOutcome =
                "NO_FEASIBLE_VM".equalsIgnoreCase(
                        decision.getDecisionOutcome());

        /*
         * DecisionState does not expose selectedCandidateIndex.
         * The decision log still exposes the scheduler outcome and the
         * selected VM. For a genuine NO_FEASIBLE_VM outcome, selectedVmId
         * must remain negative.
         */
        boolean noSelectedVm =
                decision.getSelectedVmId()
                < 0;

        boolean noSelectedDestination =
                decision.getSelectedDestination() == null
                || "NONE".equalsIgnoreCase(
                        decision.getSelectedDestination())
                || "UNKNOWN".equalsIgnoreCase(
                        decision.getSelectedDestination());

        return noFeasibleVmOutcome
                && noSelectedVm
                && noSelectedDestination;
    }

    private static int countSelectedDecisions(
            List<DecisionState> decisions) {

        int count =
                0;

        if(decisions == null) {

            return count;
        }

        for(DecisionState decision
                : decisions) {

            if(decision != null
                    && decision.isSelectedDecision()) {

                count++;
            }
        }

        return count;
    }

    private static int countUnsuccessfulDecisions(
            List<DecisionState> decisions) {

        int count =
                0;

        if(decisions == null) {

            return count;
        }

        for(DecisionState decision
                : decisions) {

            if(isEligibleForFeasibilityRestoration(
                    decision)) {

                count++;
            }
        }

        return count;
    }
}
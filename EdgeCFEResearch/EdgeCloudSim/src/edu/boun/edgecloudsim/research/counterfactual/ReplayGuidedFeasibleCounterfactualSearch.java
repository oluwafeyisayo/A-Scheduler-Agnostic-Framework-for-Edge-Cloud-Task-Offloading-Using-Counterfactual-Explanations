package edu.boun.edgecloudsim.research.counterfactual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;


import edu.boun.edgecloudsim.research.counterfactual
        .CounterfactualStateSynchronizer
        .SynchronizedCounterfactualState;

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

import edu.boun.edgecloudsim.research.counterfactual.replay.engine
        .SchedulerReplayEngine;

import edu.boun.edgecloudsim.research.counterfactual.verification
        .CounterfactualVerificationResult;

import edu.boun.edgecloudsim.research.counterfactual.verification
        .CounterfactualVerifier;

/**
 * Replay-Guided Feasible Counterfactual Search.
 *
 * Candidates are generated, synchronized, scored, ordered by objective
 * cost, validated, replayed, compared, classified, and verified against
 * a requested decision change.
 *
 * In FIXED_EXHAUSTIVE mode, the search evaluates candidates up to the
 * configured fixed evaluation limit.
 *
 * In ADAPTIVE_BEST_FIRST mode, the search begins with a task-specific
 * evaluation budget and may extend that budget using replay and
 * feasibility feedback.
 */
public final class ReplayGuidedFeasibleCounterfactualSearch {

    private final FeasibleCounterfactualSearchConfig
            config;

    private final CounterfactualCandidateGenerator
            candidateGenerator;

    private final CounterfactualStateSynchronizer
            stateSynchronizer;

    private final SchedulerReplayContextFactory
            contextFactory;

    private final CounterfactualObjectiveFunction
            objectiveFunction;

    private final CounterfactualValidator
            validator;

    private final SchedulerReplayEngine
            replayEngine;

    private final CounterfactualVerifier
            verifier;

    private final CounterfactualClassifier
            classifier;

    private final DecisionComparator
            decisionComparator;

    private final AdaptiveSearchBudgetCalculator
            adaptiveBudgetCalculator;

    private final AdaptiveSearchExtensionPolicy
            adaptiveExtensionPolicy;

    public ReplayGuidedFeasibleCounterfactualSearch(
            FeasibleCounterfactualSearchConfig config,
            SchedulerReplayEngine replayEngine,
            SchedulerReplayContextFactory contextFactory) {

        if(config == null) {

            throw new IllegalArgumentException(
                    "Search configuration cannot be null.");
        }

        if(replayEngine == null) {

            throw new IllegalArgumentException(
                    "Scheduler replay engine cannot be null.");
        }

        if(contextFactory == null) {

            throw new IllegalArgumentException(
                    "Scheduler replay context factory cannot be null.");
        }

        this.config =
                config;

        this.replayEngine =
                replayEngine;

        this.contextFactory =
                contextFactory;

        this.candidateGenerator =
                new CounterfactualCandidateGenerator(
                        config);

        this.stateSynchronizer =
                new CounterfactualStateSynchronizer();

        this.objectiveFunction =
                new CounterfactualObjectiveFunction(
                        config);

        this.validator =
                new CounterfactualValidator();

        this.verifier =
                new CounterfactualVerifier(
                        replayEngine);

        this.classifier =
                new CounterfactualClassifier();

        this.decisionComparator =
                new DecisionComparator();

        this.adaptiveBudgetCalculator =
                new AdaptiveSearchBudgetCalculator();

        this.adaptiveExtensionPolicy =
                new AdaptiveSearchExtensionPolicy();
    }

    public CounterfactualSearchResult search(
            ReplayState originalReplayState,
            List<VmCandidateSnapshot> originalCandidates,
            SchedulerDecision originalSchedulerDecision,
            DecisionChangeType requestedChangeType) {

        validateSearchInputs(
                originalReplayState,
                originalCandidates,
                originalSchedulerDecision,
                requestedChangeType);

        if(!originalSchedulerDecision
                .wasSuccessfullyReplayed()) {

            return createBaselineReplayFailureResult(
                    originalReplayState,
                    originalSchedulerDecision,
                    requestedChangeType);
        }

        /*
         * One result builder is retained throughout the search.
         * Every evaluated candidate contributes exactly one
         * classification result.
         */
        CounterfactualSearchResult.Builder resultBuilder =
                new CounterfactualSearchResult.Builder(
                        originalReplayState.getTaskId());

        /*
         * Generate complete single-feature and multi-feature counterfactual
         * change sets.
         *
         * Every inner list represents one complete candidate and may contain
         * between the configured minimum and maximum numbers of changed
         * features.
         */
        
        List<List<FeatureChange>> generatedChangeSets =
                candidateGenerator
                .generateCandidateChangeSets(
                        originalReplayState);

        /*
         * Score all generated candidates and insert them into the
         * best-first frontier.
         */
        PriorityQueue<SearchEntry> frontier =
                new PriorityQueue<SearchEntry>(
                        new SearchEntryComparator());

        int generatedCandidateCount =
                0;

        for(List<FeatureChange> featureChanges
                : generatedChangeSets) {

            if(generatedCandidateCount
                    >= config.getMaximumGeneratedCandidates()) {

                break;
            }

            /*
             * Defensive protection against malformed generator output.
             *
             * The generator should never return null or empty change sets,
             * but ignoring them here prevents an invalid candidate from
             * interrupting the complete search.
             */
            if(featureChanges == null
                    || featureChanges.isEmpty()) {

                continue;
            }
            
            resultBuilder.incrementGeneratedCardinalityCount(
                    featureChanges.size());

            SynchronizedCounterfactualState synchronizedState =
                    stateSynchronizer.synchronize(
                            originalReplayState,
                            originalCandidates,
                            originalSchedulerDecision,
                            featureChanges);

            CounterfactualScore score =
                    objectiveFunction.score(
                            synchronizedState
                            .getReplayState());

            frontier.add(
                    new SearchEntry(
                            generatedCandidateCount + 1,
                            synchronizedState,
                            score));

            generatedCandidateCount++;
        }
        
        if(!generatedChangeSets.isEmpty()) {

            int minimumGeneratedChangeCount =
                    Integer.MAX_VALUE;

            int maximumGeneratedChangeCount =
                    0;

            int multiFeatureCandidateCount =
                    0;

            for(List<FeatureChange> featureChanges
                    : generatedChangeSets) {

                if(featureChanges == null
                        || featureChanges.isEmpty()) {

                    continue;
                }

                int changedFeatureCount =
                        featureChanges.size();

                minimumGeneratedChangeCount =
                        Math.min(
                                minimumGeneratedChangeCount,
                                changedFeatureCount);

                maximumGeneratedChangeCount =
                        Math.max(
                                maximumGeneratedChangeCount,
                                changedFeatureCount);

                if(changedFeatureCount > 1) {

                    multiFeatureCandidateCount++;
                }
            }

            if(minimumGeneratedChangeCount
                    == Integer.MAX_VALUE) {

                minimumGeneratedChangeCount =
                        0;
            }

            System.out.println(
                    "[RG-FCS Candidate Sets] taskId="
                    + originalReplayState.getTaskId()
                    + ", generated="
                    + generatedCandidateCount
                    + ", minimumChangedFeatures="
                    + minimumGeneratedChangeCount
                    + ", maximumChangedFeatures="
                    + maximumGeneratedChangeCount
                    + ", multiFeatureCandidates="
                    + multiFeatureCandidateCount);
        }
        /*
         * Fixed mode uses the configured fixed evaluation limit.
         *
         * Adaptive mode derives a task-specific initial budget using
         * the number of generated candidates, searchable features,
         * and original scheduler VM candidates.
         */
        int currentEvaluationBudget =
                Math.min(
                        config.getMaximumEvaluatedCandidates(),
                        generatedCandidateCount);

        if(config.isAdaptiveBestFirstSearch()) {

            currentEvaluationBudget =
                    adaptiveBudgetCalculator
                    .calculateEvaluationBudget(
                            config,
                            generatedCandidateCount,
                            originalCandidates.size());
        }

        System.out.println(
                "[RG-FCS Search Budget] mode="
                + config.getSearchMode()
                + ", taskId="
                + originalReplayState.getTaskId()
                + ", generated="
                + generatedCandidateCount
                + ", originalVmCandidates="
                + originalCandidates.size()
                + ", evaluationBudget="
                + currentEvaluationBudget);

        CounterfactualCandidate bestCandidate =
                null;
        
        /*
         * Accepted candidates are retained locally for the separate bounded
         * neighborhood-refinement stage.
         *
         * This list is intentionally independent of the result builder because
         * the builder constructs output; it should not control search behavior.
         */
        List<CounterfactualCandidate>
                acceptedCandidatesForRefinement =
                new ArrayList<CounterfactualCandidate>();

        int evaluatedCandidateCount =
                0;

        /*
         * Classification counters used by the adaptive extension
         * policy.
         */
        int acceptedCandidateCount =
                0;

        int feasibleIneffectiveCount =
                0;

        int infeasibleEffectiveCount =
                0;

        int infeasibleIneffectiveCount =
                0;

        int invalidCount =
                0;

        /*
         * Number of adaptive budget extensions completed after the
         * initial budget.
         *
         * This must be declared outside the loop so that it persists
         * across evaluation stages.
         */
        int completedAdaptiveExtensionCount =
                0;
        
        /*
         * Cumulative counter values recorded at the beginning of the
         * current evaluation stage.
         *
         * Subtracting these values from the current cumulative counts gives
         * the classifications contributed only by the current stage.
         */
        int stageStartEvaluatedCandidateCount =
                0;

        int stageStartAcceptedCandidateCount =
                0;

        int stageStartFeasibleIneffectiveCount =
                0;

        int stageStartInfeasibleEffectiveCount =
                0;

        int stageStartInfeasibleIneffectiveCount =
                0;

        int stageStartInvalidCount =
                0;
        

        while(!frontier.isEmpty()) {

            /*
             * The current evaluation stage has been exhausted.
             */
            if(evaluatedCandidateCount
                    >= currentEvaluationBudget) {

                /*
                 * Derive evidence contributed only by the stage that has just
                 * completed.
                 */
                int stageEvaluatedCandidateCount =
                        evaluatedCandidateCount
                        - stageStartEvaluatedCandidateCount;

                int stageAcceptedCandidateCount =
                        acceptedCandidateCount
                        - stageStartAcceptedCandidateCount;

                int stageFeasibleIneffectiveCount =
                        feasibleIneffectiveCount
                        - stageStartFeasibleIneffectiveCount;

                int stageInfeasibleEffectiveCount =
                        infeasibleEffectiveCount
                        - stageStartInfeasibleEffectiveCount;

                int stageInfeasibleIneffectiveCount =
                        infeasibleIneffectiveCount
                        - stageStartInfeasibleIneffectiveCount;

                int stageInvalidCount =
                        invalidCount
                        - stageStartInvalidCount;

                boolean shouldExtend =
                        adaptiveExtensionPolicy
                        .shouldExtend(
                                config,
                                currentEvaluationBudget,
                                generatedCandidateCount,
                                evaluatedCandidateCount,
                                acceptedCandidateCount,
                                feasibleIneffectiveCount,
                                infeasibleEffectiveCount,
                                infeasibleIneffectiveCount,
                                invalidCount,
                                completedAdaptiveExtensionCount,
                                stageStartEvaluatedCandidateCount,
                                stageStartAcceptedCandidateCount,
                                stageStartFeasibleIneffectiveCount,
                                stageStartInfeasibleEffectiveCount,
                                stageStartInfeasibleIneffectiveCount,
                                stageStartInvalidCount);

                if(!shouldExtend) {

                    System.out.println(
                            "[RG-FCS Convergence Stop] mode="
                            + config.getSearchMode()
                            + ", taskId="
                            + originalReplayState.getTaskId()
                            + ", completedExtensions="
                            + completedAdaptiveExtensionCount
                            + ", evaluated="
                            + evaluatedCandidateCount
                            + ", currentBudget="
                            + currentEvaluationBudget
                            + ", stageEvaluated="
                            + stageEvaluatedCandidateCount
                            + ", stageAccepted="
                            + stageAcceptedCandidateCount
                            + ", stageFeasibleIneffective="
                            + stageFeasibleIneffectiveCount
                            + ", stageInfeasibleEffective="
                            + stageInfeasibleEffectiveCount
                            + ", stageInfeasibleIneffective="
                            + stageInfeasibleIneffectiveCount
                            + ", stageInvalid="
                            + stageInvalidCount);

                    break;
                }

                int previousEvaluationBudget =
                        currentEvaluationBudget;

                currentEvaluationBudget =
                        adaptiveExtensionPolicy
                        .calculateExtendedBudget(
                                config,
                                currentEvaluationBudget,
                                generatedCandidateCount);

                /*
                 * Defensive protection against a policy that returns the same
                 * or a smaller budget.
                 */
                if(currentEvaluationBudget
                        <= previousEvaluationBudget) {

                    System.out.println(
                            "[RG-FCS Convergence Stop] mode="
                            + config.getSearchMode()
                            + ", taskId="
                            + originalReplayState.getTaskId()
                            + ", reason=NON_INCREASING_BUDGET"
                            + ", evaluated="
                            + evaluatedCandidateCount
                            + ", currentBudget="
                            + previousEvaluationBudget);

                    break;
                }

                completedAdaptiveExtensionCount++;

                /*
                 * Record the cumulative state at the beginning of the newly
                 * granted stage.
                 */
                stageStartEvaluatedCandidateCount =
                        evaluatedCandidateCount;

                stageStartAcceptedCandidateCount =
                        acceptedCandidateCount;

                stageStartFeasibleIneffectiveCount =
                        feasibleIneffectiveCount;

                stageStartInfeasibleEffectiveCount =
                        infeasibleEffectiveCount;

                stageStartInfeasibleIneffectiveCount =
                        infeasibleIneffectiveCount;

                stageStartInvalidCount =
                        invalidCount;

                System.out.println(
                        "[RG-FCS Budget Extension] mode="
                        + config.getSearchMode()
                        + ", taskId="
                        + originalReplayState.getTaskId()
                        + ", extension="
                        + completedAdaptiveExtensionCount
                        + ", previousBudget="
                        + previousEvaluationBudget
                        + ", extendedBudget="
                        + currentEvaluationBudget
                        + ", stageEvaluated="
                        + stageEvaluatedCandidateCount
                        + ", stageAccepted="
                        + stageAcceptedCandidateCount
                        + ", stageFeasibleIneffective="
                        + stageFeasibleIneffectiveCount
                        + ", stageInfeasibleEffective="
                        + stageInfeasibleEffectiveCount
                        + ", stageInfeasibleIneffective="
                        + stageInfeasibleIneffectiveCount
                        + ", stageInvalid="
                        + stageInvalidCount
                        + ", cumulativeAccepted="
                        + acceptedCandidateCount
                        + ", cumulativeInfeasibleEffective="
                        + infeasibleEffectiveCount);

                continue;
            }

            SearchEntry entry =
                    frontier.poll();

            evaluatedCandidateCount++;
            
            int evaluatedChangedFeatureCount =
                    entry
                    .synchronizedState
                    .getReplayState()
                    .getAppliedChanges()
                    .size();

            resultBuilder.incrementEvaluatedCardinalityCount(
                    evaluatedChangedFeatureCount);

            ReplayState candidateReplayState =
                    entry
                    .synchronizedState
                    .getReplayState();

            List<VmCandidateSnapshot> candidateSnapshots =
                    entry
                    .synchronizedState
                    .getVmCandidates();

            SchedulerReplayContext candidateContext =
                    contextFactory.create(
                            candidateReplayState,
                            candidateSnapshots);

            /*
             * Step 1: Determine whether synchronization produced a
             * structurally valid candidate.
             */
            boolean candidateValid =
                    candidateReplayState != null
                    && candidateContext != null
                    && candidateSnapshots != null
                    && !candidateSnapshots.isEmpty();

            /*
             * Step 2: Evaluate operational feasibility independently
             * from scheduler replay.
             */
//            FeasibilityResult feasibilityResult =
//                    validator.validate(
//                            candidateReplayState);
            
            FeasibilityResult feasibilityResult =
                    validator.validate(
                            candidateReplayState,
                            candidateSnapshots);
            
            if(requestedChangeType
                    == DecisionChangeType.FEASIBILITY_RESTORATION
                    && evaluatedCandidateCount <= 3) {

                System.out.println(
                        "[Restoration Feasibility] taskId="
                        + candidateReplayState.getTaskId()
                        + ", candidateId="
                        + entry.candidateId
                        + ", requiredCpu="
                        + candidateReplayState.getRequiredCpuPercent()
                        + ", failedConstraints="
                        + feasibilityResult.getFailedEvaluations()
                        + ", operationallyFeasible="
                        + feasibilityResult.isOperationallyFeasible());
            }

            boolean operationallyFeasible =
                    feasibilityResult != null
                    && feasibilityResult
                    .isOperationallyFeasible();

            /*
             * Step 3: Replay the scheduler using the modified state.
             */
//            SchedulerDecision replayDecision =
//                    replayEngine.replay(
//                            candidateContext);
            
            SchedulerDecision replayDecision =
                    replayEngine.replay(
                            candidateContext);


            boolean replaySuccessful =
                    replayDecision != null
                    && replayDecision
                    .wasSuccessfullyReplayed();

            /*
             * Step 4: Compare the original scheduler decision with the
             * modified replay decision.
             *
             * Target achievement is determined independently of
             * operational feasibility.
             */
            DecisionComparisonResult comparisonResult =
                    null;

            boolean targetAchieved =
                    false;

            if(replaySuccessful) {

                comparisonResult =
                        decisionComparator.compare(
                                candidateReplayState,
                                replayDecision);

                targetAchieved =
                        comparisonResult != null
                        && comparisonResult.satisfies(
                                requestedChangeType);
            }

            /*
             * Step 5: Run the existing verifier for detailed
             * verification output and compatibility.
             */
            CounterfactualVerificationResult verificationResult =
                    verifier.verify(
                            candidateContext,
                            feasibilityResult,
                            requestedChangeType);

            /*
             * Step 6: Classify the candidate using independent
             * validity, replay, effectiveness, and feasibility
             * properties.
             */
            CounterfactualClassificationResult
                    classificationResult =
                    classifier.classify(
                            candidateReplayState
                            .getTaskId(),

                            entry.candidateId,

                            candidateValid,

                            replaySuccessful,

                            targetAchieved,

                            operationallyFeasible);

            /*
             * Record exactly one classification for every evaluated
             * candidate.
             */
            resultBuilder.addClassificationResult(
                    classificationResult);

            /*
             * Maintain local classification counts for adaptive
             * extension decisions.
             */
            switch(classificationResult.getClassification()) {

                case FEASIBLE_EFFECTIVE:

                    acceptedCandidateCount++;
                    break;

                case FEASIBLE_INEFFECTIVE:

                    feasibleIneffectiveCount++;
                    break;

                case INFEASIBLE_EFFECTIVE:

                    infeasibleEffectiveCount++;
                    break;

                case INFEASIBLE_INEFFECTIVE:

                    infeasibleIneffectiveCount++;
                    break;

                case INVALID:

                    invalidCount++;
                    break;

                default:

                    throw new IllegalStateException(
                            "Unsupported counterfactual "
                            + "classification: "
                            + classificationResult
                            .getClassification());
            }

            /*
             * Step 7: Build the evaluated candidate and attach its
             * classification.
             */
            CounterfactualCandidate candidate =
                    new CounterfactualCandidate.Builder(
                            candidateReplayState
                            .getTaskId(),
                            entry.candidateId)

                    .addFeatureChanges(
                            candidateReplayState
                            .getAppliedChanges())

                    .withReplayState(
                            candidateReplayState)

                    .withFeasibilityResult(
                            feasibilityResult)

                    .withSchedulerDecision(
                            replayDecision)

                    .withVerificationResult(
                            verificationResult)

                    .withClassificationResult(
                            classificationResult)

                    .withScore(
                            entry.score)

                    .withEvaluated(
                            true)

                    .withAccepted(
                            classificationResult
                            .isAcceptedCounterfactual())

                    .withReason(
                            classificationResult
                            .getReason())

                    .build();

            /*
             * Every FEASIBLE_EFFECTIVE candidate is an accepted
             * counterfactual.
             */
            if(candidate.isAccepted()) {

                resultBuilder.incrementAcceptedCardinalityCount(
                        candidate.getChangedFeatureCount());

                /*
                 * Preserve all accepted candidates in the result.
                 */
                resultBuilder.addAcceptedCandidate(
                        candidate);

                /*
                 * Retain the accepted candidate as a possible
                 * neighborhood-refinement seed.
                 */
                acceptedCandidatesForRefinement.add(
                        candidate);

                /*
                 * Preserve the accepted candidate with the lowest
                 * objective score.
                 */
                if(bestCandidate == null
                        || candidate
                        .getScore()
                        .getTotalScore()
                        < bestCandidate
                        .getScore()
                        .getTotalScore()) {

                    bestCandidate =
                            candidate;
                }

                if(config
                        .isStopAtFirstAcceptedCandidate()) {

                    break;
                }
            }
        }

        /*
         * Generate and evaluate a bounded local neighborhood around the
         * strongest accepted adaptive candidates.
         */
        NeighborhoodRefinementGenerator
                neighborhoodRefinementGenerator =
                new NeighborhoodRefinementGenerator(
                        config);

        List<List<FeatureChange>>
                neighborhoodChangeSets =
                neighborhoodRefinementGenerator
                .generateNeighborhoodChangeSets(
                        acceptedCandidatesForRefinement);

        int neighborhoodSingleFeatureCount =
                0;

        int neighborhoodMultiFeatureCount =
                0;

        for(List<FeatureChange> neighborhoodChanges
                : neighborhoodChangeSets) {

            if(neighborhoodChanges == null
                    || neighborhoodChanges.isEmpty()) {

                continue;
            }

            if(neighborhoodChanges.size() == 1) {

                neighborhoodSingleFeatureCount++;
            }
            else {

                neighborhoodMultiFeatureCount++;
            }
        }

        System.out.println(
                "[RG-FCS Neighborhood Generation] taskId="
                + originalReplayState.getTaskId()
                + ", enabled="
                + config.isNeighborhoodRefinementEnabled()
                + ", acceptedSeedPool="
                + acceptedCandidatesForRefinement.size()
                + ", maximumBoundarySeeds="
                + config.getMaximumBoundarySeeds()
                + ", samplesPerSeed="
                + config.getNeighborhoodSamplesPerSeed()
                + ", maximumNeighborhoodCandidates="
                + config.getMaximumNeighborhoodCandidates()
                + ", generatedNeighborhoodCandidates="
                + neighborhoodChangeSets.size()
                + ", singleFeature="
                + neighborhoodSingleFeatureCount
                + ", multiFeature="
                + neighborhoodMultiFeatureCount);

        int neighborhoodEvaluatedCandidateCount =
                0;

        int neighborhoodAcceptedCandidateCount =
                0;

        CounterfactualCandidate
                bestNeighborhoodCandidate =
                null;

        /*
         * Neighborhood candidates are evaluated through the same
         * synchronization, feasibility, replay, comparison,
         * verification, classification, and scoring pipeline as the
         * adaptive candidates. Their accounting remains separate.
         */
        for(List<FeatureChange> neighborhoodChanges
                : neighborhoodChangeSets) {

            if(neighborhoodChanges == null
                    || neighborhoodChanges.isEmpty()) {

                continue;
            }

            neighborhoodEvaluatedCandidateCount++;

            SynchronizedCounterfactualState synchronizedState =
                    stateSynchronizer.synchronize(
                            originalReplayState,
                            originalCandidates,
                            originalSchedulerDecision,
                            neighborhoodChanges);

            ReplayState neighborhoodReplayState =
                    synchronizedState.getReplayState();

            List<VmCandidateSnapshot>
                    neighborhoodCandidateSnapshots =
                    synchronizedState.getVmCandidates();

            CounterfactualScore neighborhoodScore =
                    objectiveFunction.score(
                            neighborhoodReplayState);

            SchedulerReplayContext neighborhoodContext =
                    contextFactory.create(
                            neighborhoodReplayState,
                            neighborhoodCandidateSnapshots);

            boolean neighborhoodCandidateValid =
                    neighborhoodReplayState != null
                    && neighborhoodContext != null
                    && neighborhoodCandidateSnapshots != null
                    && !neighborhoodCandidateSnapshots.isEmpty();

            FeasibilityResult neighborhoodFeasibilityResult =
                    validator.validate(
                            neighborhoodReplayState,
                            neighborhoodCandidateSnapshots);

            boolean neighborhoodOperationallyFeasible =
                    neighborhoodFeasibilityResult != null
                    && neighborhoodFeasibilityResult
                    .isOperationallyFeasible();

            SchedulerDecision neighborhoodReplayDecision =
                    replayEngine.replay(
                            neighborhoodContext);

            boolean neighborhoodReplaySuccessful =
                    neighborhoodReplayDecision != null
                    && neighborhoodReplayDecision
                    .wasSuccessfullyReplayed();

            boolean neighborhoodTargetAchieved =
                    false;

            if(neighborhoodReplaySuccessful) {

                DecisionComparisonResult
                        neighborhoodComparisonResult =
                        decisionComparator.compare(
                                neighborhoodReplayState,
                                neighborhoodReplayDecision);

                neighborhoodTargetAchieved =
                        neighborhoodComparisonResult != null
                        && neighborhoodComparisonResult.satisfies(
                                requestedChangeType);
            }

            CounterfactualVerificationResult
                    neighborhoodVerificationResult =
                    verifier.verify(
                            neighborhoodContext,
                            neighborhoodFeasibilityResult,
                            requestedChangeType);

            /*
             * Neighborhood IDs begin after the complete adaptive
             * generation range so IDs remain unique in one task search.
             */
            int neighborhoodCandidateId =
                    generatedCandidateCount
                    + neighborhoodEvaluatedCandidateCount;

            CounterfactualClassificationResult
                    neighborhoodClassificationResult =
                    classifier.classify(
                            neighborhoodReplayState.getTaskId(),
                            neighborhoodCandidateId,
                            neighborhoodCandidateValid,
                            neighborhoodReplaySuccessful,
                            neighborhoodTargetAchieved,
                            neighborhoodOperationallyFeasible);

            CounterfactualCandidate neighborhoodCandidate =
                    new CounterfactualCandidate.Builder(
                            neighborhoodReplayState.getTaskId(),
                            neighborhoodCandidateId)

                    .addFeatureChanges(
                            neighborhoodReplayState
                            .getAppliedChanges())

                    .withReplayState(
                            neighborhoodReplayState)

                    .withFeasibilityResult(
                            neighborhoodFeasibilityResult)

                    .withSchedulerDecision(
                            neighborhoodReplayDecision)

                    .withVerificationResult(
                            neighborhoodVerificationResult)

                    .withClassificationResult(
                            neighborhoodClassificationResult)

                    .withScore(
                            neighborhoodScore)

                    .withEvaluated(
                            true)

                    .withAccepted(
                            neighborhoodClassificationResult
                            .isAcceptedCounterfactual())

                    .withReason(
                            neighborhoodClassificationResult
                            .getReason())

                    .build();

            if(!neighborhoodCandidate.isAccepted()) {

                continue;
            }

            neighborhoodAcceptedCandidateCount++;

            if(bestNeighborhoodCandidate == null
                    || neighborhoodCandidate
                    .getScore()
                    .getTotalScore()
                    < bestNeighborhoodCandidate
                    .getScore()
                    .getTotalScore()) {

                bestNeighborhoodCandidate =
                        neighborhoodCandidate;
            }
        }

        CounterfactualCandidate finalBestCandidate =
                bestCandidate;

        boolean improvedByNeighborhoodRefinement =
                false;

        String bestCandidateSource =
                bestCandidate == null
                ? "NONE"
                : "ADAPTIVE";

        if(bestNeighborhoodCandidate != null
                && (
                        bestCandidate == null
                        || bestNeighborhoodCandidate
                        .getScore()
                        .getTotalScore()
                        < bestCandidate
                        .getScore()
                        .getTotalScore()
                )) {

            finalBestCandidate =
                    bestNeighborhoodCandidate;

            improvedByNeighborhoodRefinement =
                    true;

            bestCandidateSource =
                    "NEIGHBORHOOD";
        }

        System.out.println(
                "[RG-FCS Neighborhood Evaluation] taskId="
                + originalReplayState.getTaskId()
                + ", generated="
                + neighborhoodChangeSets.size()
                + ", evaluated="
                + neighborhoodEvaluatedCandidateCount
                + ", accepted="
                + neighborhoodAcceptedCandidateCount
                + ", improved="
                + improvedByNeighborhoodRefinement
                + ", bestSource="
                + bestCandidateSource);

        String searchReason;

        if(finalBestCandidate != null) {

            if(config.isAdaptiveBestFirstSearch()) {

                searchReason =
                        "The lowest-scoring feasible and effective "
                        + "counterfactual encountered within the "
                        + "adaptive replay budget was returned.";
            }
            else {

                searchReason =
                        "The lowest-scoring feasible and effective "
                        + "counterfactual was returned.";
            }
        }
        else if(config.isAdaptiveBestFirstSearch()
                && evaluatedCandidateCount
                < generatedCandidateCount) {

            searchReason =
                    "No feasible and effective counterfactual was "
                    + "found before the adaptive replay-extension "
                    + "policy terminated the search.";
        }
        else {

            searchReason =
                    "No feasible and effective counterfactual was "
                    + "found within the evaluated candidate set.";
        }
        
        if(improvedByNeighborhoodRefinement) {

            resultBuilder.withRefinedBestCandidate(
                    finalBestCandidate);
        }
        else {

            resultBuilder.withAcceptedCandidate(
                    finalBestCandidate);
        }

        CounterfactualSearchResult searchResult =
                resultBuilder

                .withGeneratedCandidateCount(
                        generatedCandidateCount)

                .withEvaluatedCandidateCount(
                        evaluatedCandidateCount)

                .withNeighborhoodRefinementStatistics(
                        neighborhoodChangeSets.size(),
                        neighborhoodEvaluatedCandidateCount,
                        neighborhoodAcceptedCandidateCount,
                        improvedByNeighborhoodRefinement,
                        bestCandidateSource)

                .withSearchSuccessful(
                        finalBestCandidate != null)

                .withReason(
                        searchReason)

                .build();

        System.out.println(
                "[RG-FCS Candidate Cardinality] taskId="
                + originalReplayState.getTaskId()
                + ", generated="
                + searchResult
                .getGeneratedCountsByChangedFeatureCount()
                + ", evaluated="
                + searchResult
                .getEvaluatedCountsByChangedFeatureCount()
                + ", accepted="
                + searchResult
                .getAcceptedCountsByChangedFeatureCount()
                + ", generatedMultiFeature="
                + searchResult
                .getGeneratedMultiFeatureCandidateCount()
                + ", evaluatedMultiFeature="
                + searchResult
                .getEvaluatedMultiFeatureCandidateCount()
                + ", acceptedMultiFeature="
                + searchResult
                .getAcceptedMultiFeatureCandidateCount()
                + ", bestChangedFeatureCount="
                + searchResult
                .getBestCandidateChangedFeatureCount());

        return searchResult;
    }


    private CounterfactualSearchResult
            createBaselineReplayFailureResult(
                    ReplayState originalReplayState,
                    SchedulerDecision originalSchedulerDecision,
                    DecisionChangeType requestedChangeType) {

        String reason =
                "RG-FCS search was not started because the original "
                + "scheduler replay did not reproduce a faithful "
                + "baseline. Replay status="
                + originalSchedulerDecision.getReplayStatus()
                + ", requestedChangeType="
                + requestedChangeType
                + ", replay reason="
                + originalSchedulerDecision.getReason();

        System.out.println(
                "[RG-FCS Baseline Rejected] taskId="
                + originalReplayState.getTaskId()
                + ", status="
                + originalSchedulerDecision.getReplayStatus()
                + ", requestedChangeType="
                + requestedChangeType
                + ", reason="
                + originalSchedulerDecision.getReason());

        return new CounterfactualSearchResult.Builder(
                originalReplayState.getTaskId())
                .withGeneratedCandidateCount(0)
                .withEvaluatedCandidateCount(0)
                .withAcceptedCandidate(null)
                .withSearchSuccessful(false)
                .withReason(reason)
                .build();
    }

    private void validateSearchInputs(
            ReplayState originalReplayState,
            List<VmCandidateSnapshot> originalCandidates,
            SchedulerDecision originalSchedulerDecision,
            DecisionChangeType requestedChangeType) {

        if(originalReplayState == null) {

            throw new IllegalArgumentException(
                    "Original replay state cannot be null.");
        }

        if(originalCandidates == null
                || originalCandidates.isEmpty()) {

            throw new IllegalArgumentException(
                    "Original VM candidates cannot be null or empty.");
        }

        if(originalSchedulerDecision == null) {

            throw new IllegalArgumentException(
                    "Original scheduler decision cannot be null.");
        }

        if(requestedChangeType == null) {

            throw new IllegalArgumentException(
                    "Requested decision change cannot be null.");
        }
    }

    private static final class SearchEntry {

        private final int candidateId;

        private final SynchronizedCounterfactualState
                synchronizedState;

        private final CounterfactualScore
                score;

        private SearchEntry(
                int candidateId,
                SynchronizedCounterfactualState synchronizedState,
                CounterfactualScore score) {

            this.candidateId =
                    candidateId;

            this.synchronizedState =
                    synchronizedState;

            this.score =
                    score;
        }
    }

    private static final class SearchEntryComparator
            implements Comparator<SearchEntry> {

        @Override
        public int compare(
                SearchEntry first,
                SearchEntry second) {

            int scoreComparison =
                    Double.compare(
                            first
                            .score
                            .getTotalScore(),

                            second
                            .score
                            .getTotalScore());

            if(scoreComparison != 0) {

                return scoreComparison;
            }

            /*
             * Deterministic tie-breaking.
             */
            return Integer.compare(
                    first.candidateId,
                    second.candidateId);
        }
    }
}
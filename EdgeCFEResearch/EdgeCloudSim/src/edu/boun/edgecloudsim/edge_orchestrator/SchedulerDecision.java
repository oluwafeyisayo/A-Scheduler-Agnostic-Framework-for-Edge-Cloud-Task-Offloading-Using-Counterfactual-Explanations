package edu.boun.edgecloudsim.edge_orchestrator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudbus.cloudsim.Vm;

import edu.boun.edgecloudsim.research.logging.CandidateVmRecord;

/**
 * Immutable result produced by the live EdgeCloudSim scheduler.
 *
 * In addition to the selected VM and candidate set, this result stores
 * the scheduler-specific state required for faithful replay.
 */
public final class SchedulerDecision {

    private final Vm selectedVm;

    private final int candidateVmCount;
    private final int feasibleVmCount;

    private final double selectedAvailableCpu;
    private final double bestAvailableCpu;

    private final String selectionReason;

    private final int selectedCandidateIndex;

    private final int roundRobinStartingCandidateIndex;
    private final int randomSampledCandidateIndex;

    private final List<CandidateVmRecord>
            candidateVmRecords;

    /**
     * Backward-compatible constructor.
     *
     * Use the full constructor in ROUND_ROBIN and RANDOM scheduler
     * branches so their exact internal state is preserved.
     */
    public SchedulerDecision(
            Vm selectedVm,
            int candidateVmCount,
            int feasibleVmCount,
            double selectedAvailableCpu,
            double bestAvailableCpu,
            String selectionReason,
            int selectedCandidateIndex,
            List<CandidateVmRecord> candidateVmRecords) {

        this(
                selectedVm,
                candidateVmCount,
                feasibleVmCount,
                selectedAvailableCpu,
                bestAvailableCpu,
                selectionReason,
                selectedCandidateIndex,
                -1,
                -1,
                candidateVmRecords);
    }

    public SchedulerDecision(
            Vm selectedVm,
            int candidateVmCount,
            int feasibleVmCount,
            double selectedAvailableCpu,
            double bestAvailableCpu,
            String selectionReason,
            int selectedCandidateIndex,
            int roundRobinStartingCandidateIndex,
            int randomSampledCandidateIndex,
            List<CandidateVmRecord> candidateVmRecords) {

        this.selectedVm =
                selectedVm;

        this.candidateVmRecords =
                createImmutableCandidateList(
                        candidateVmRecords);

        this.candidateVmCount =
                candidateVmCount;

        this.feasibleVmCount =
                feasibleVmCount;

        this.selectedAvailableCpu =
                selectedAvailableCpu;

        this.bestAvailableCpu =
                bestAvailableCpu;

        this.selectionReason =
                normalizeText(
                        selectionReason);

        this.selectedCandidateIndex =
                selectedCandidateIndex;

        this.roundRobinStartingCandidateIndex =
                roundRobinStartingCandidateIndex;

        this.randomSampledCandidateIndex =
                randomSampledCandidateIndex;

        validate();
    }

    private List<CandidateVmRecord>
            createImmutableCandidateList(
                    List<CandidateVmRecord> candidates) {

        if(candidates == null
                || candidates.isEmpty()) {

            return Collections.emptyList();
        }

        List<CandidateVmRecord> copy =
                new ArrayList<CandidateVmRecord>(
                        candidates.size());

        for(CandidateVmRecord candidate
                : candidates) {

            if(candidate == null) {

                throw new IllegalArgumentException(
                        "candidateVmRecords cannot contain null.");
            }

            copy.add(
                    candidate);
        }

        return Collections.unmodifiableList(
                copy);
    }

    private void validate() {

        if(candidateVmCount < 0) {

            throw new IllegalArgumentException(
                    "candidateVmCount cannot be negative.");
        }

        if(feasibleVmCount < 0) {

            throw new IllegalArgumentException(
                    "feasibleVmCount cannot be negative.");
        }

        if(feasibleVmCount
                > candidateVmCount) {

            throw new IllegalArgumentException(
                    "feasibleVmCount cannot exceed candidateVmCount.");
        }

        if(!candidateVmRecords.isEmpty()
                && candidateVmRecords.size()
                != candidateVmCount) {

            throw new IllegalArgumentException(
                    "candidateVmCount does not match "
                    + "candidateVmRecords size.");
        }

        validateOptionalCandidateIndex(
                selectedCandidateIndex,
                "selectedCandidateIndex");

        validateOptionalCandidateIndex(
                roundRobinStartingCandidateIndex,
                "roundRobinStartingCandidateIndex");

        validateOptionalCandidateIndex(
                randomSampledCandidateIndex,
                "randomSampledCandidateIndex");

        validateUniqueCandidateIndexes();

        if(roundRobinStartingCandidateIndex >= 0
                && findCandidateByIndex(
                        roundRobinStartingCandidateIndex) == null) {

            throw new IllegalArgumentException(
                    "roundRobinStartingCandidateIndex does not "
                    + "identify a candidate in the candidate set.");
        }

        if(randomSampledCandidateIndex >= 0
                && findCandidateByIndex(
                        randomSampledCandidateIndex) == null) {

            throw new IllegalArgumentException(
                    "randomSampledCandidateIndex does not identify "
                    + "a candidate in the candidate set.");
        }

        if(selectedVm == null) {

            if(selectedCandidateIndex != -1) {

                throw new IllegalArgumentException(
                        "selectedCandidateIndex must be -1 "
                        + "when no VM was selected.");
            }
        }
        else {

            if(selectedCandidateIndex < 0) {

                throw new IllegalArgumentException(
                        "A selected VM must have a nonnegative "
                        + "selectedCandidateIndex.");
            }

            CandidateVmRecord selectedCandidate =
                    findCandidateByIndex(
                            selectedCandidateIndex);

            if(selectedCandidate == null) {

                throw new IllegalArgumentException(
                        "selectedCandidateIndex does not identify "
                        + "a candidate in the candidate set.");
            }

            if(selectedCandidate.getVmId()
                    != selectedVm.getId()) {

                throw new IllegalArgumentException(
                        "Selected VM does not match the candidate "
                        + "identified by selectedCandidateIndex.");
            }
        }

        int actualFeasibleCount =
                0;

        for(CandidateVmRecord candidate
                : candidateVmRecords) {

            if(candidate.isFeasible()) {
                actualFeasibleCount++;
            }
        }

        if(!candidateVmRecords.isEmpty()
                && actualFeasibleCount
                != feasibleVmCount) {

            throw new IllegalArgumentException(
                    "feasibleVmCount does not match candidate records. "
                    + "Declared="
                    + feasibleVmCount
                    + ", actual="
                    + actualFeasibleCount
                    + ".");
        }
    }

    private void validateUniqueCandidateIndexes() {

        Set<Integer> indexes =
                new HashSet<Integer>();

        for(CandidateVmRecord candidate
                : candidateVmRecords) {

            Integer index =
                    Integer.valueOf(
                            candidate.getCandidateIndex());

            if(!indexes.add(index)) {

                throw new IllegalArgumentException(
                        "Duplicate candidate index "
                        + candidate.getCandidateIndex()
                        + " exists in the candidate set.");
            }
        }
    }

    private void validateOptionalCandidateIndex(
            int value,
            String fieldName) {

        if(value < -1) {

            throw new IllegalArgumentException(
                    fieldName
                    + " must be -1 or nonnegative.");
        }
    }

    private CandidateVmRecord findCandidateByIndex(
            int candidateIndex) {

        for(CandidateVmRecord candidate
                : candidateVmRecords) {

            if(candidate.getCandidateIndex()
                    == candidateIndex) {

                return candidate;
            }
        }

        return null;
    }

    private String normalizeText(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return "UNKNOWN";
        }

        return value.trim();
    }

    public Vm getSelectedVm() {
        return selectedVm;
    }

    public int getCandidateVmCount() {
        return candidateVmCount;
    }

    public int getFeasibleVmCount() {
        return feasibleVmCount;
    }

    public double getSelectedAvailableCpu() {
        return selectedAvailableCpu;
    }

    public double getBestAvailableCpu() {
        return bestAvailableCpu;
    }

    public String getSelectionReason() {
        return selectionReason;
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

    public List<CandidateVmRecord> getCandidateVmRecords() {
        return candidateVmRecords;
    }

    public int getCandidateCountForDestination(
            String destination) {

        if(destination == null
                || destination.trim().isEmpty()) {

            return 0;
        }

        int count =
                0;

        for(CandidateVmRecord candidate
                : candidateVmRecords) {

            if(destination.trim()
                    .equalsIgnoreCase(
                            candidate.getDestination())) {

                count++;
            }
        }

        return count;
    }

    public boolean hasMultipleCandidateDestinations() {

        String firstDestination =
                null;

        for(CandidateVmRecord candidate
                : candidateVmRecords) {

            String destination =
                    candidate.getDestination();

            if(firstDestination == null) {

                firstDestination =
                        destination;
            }
            else if(!firstDestination
                    .equalsIgnoreCase(
                            destination)) {

                return true;
            }
        }

        return false;
    }
}
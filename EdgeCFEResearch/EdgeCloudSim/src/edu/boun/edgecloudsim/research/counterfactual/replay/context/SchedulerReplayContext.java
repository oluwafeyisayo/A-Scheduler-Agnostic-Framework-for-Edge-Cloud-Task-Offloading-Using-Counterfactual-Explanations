package edu.boun.edgecloudsim.research.counterfactual.replay.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.replay.state
        .SchedulerPolicyState;

/**
 * Generic input supplied to any scheduler replay strategy.
 *
 * The context preserves the complete candidate search space supplied
 * for the task, including candidates from different destination tiers
 * when those candidates are available in the provenance data.
 */
public final class SchedulerReplayContext {

    private final ReplayState replayState;

    private final List<VmCandidateSnapshot>
            candidateSnapshots;

    private final SchedulerPolicyState policyState;

    public SchedulerReplayContext(
            ReplayState replayState,
            List<VmCandidateSnapshot> candidateSnapshots,
            SchedulerPolicyState policyState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        this.replayState =
                replayState;

        if(candidateSnapshots == null) {

            this.candidateSnapshots =
                    Collections.emptyList();
        }
        else {

            List<VmCandidateSnapshot> copiedCandidates =
                    new ArrayList<VmCandidateSnapshot>();

            for(VmCandidateSnapshot candidate
                    : candidateSnapshots) {

                if(candidate != null) {

                    copiedCandidates.add(
                            candidate);
                }
            }

            this.candidateSnapshots =
                    Collections.unmodifiableList(
                            copiedCandidates);
        }

        this.policyState =
                policyState;
    }

    public ReplayState getReplayState() {

        return replayState;
    }

    public List<VmCandidateSnapshot>
            getCandidateSnapshots() {

        return candidateSnapshots;
    }

    public SchedulerPolicyState getPolicyState() {

        return policyState;
    }

    public boolean hasCandidateSnapshots() {

        return !candidateSnapshots.isEmpty();
    }

    public boolean hasPolicyState() {

        return policyState != null;
    }

    /**
     * Finds a candidate by its globally unique task-level candidate
     * index.
     */
    public VmCandidateSnapshot findCandidateByIndex(
            int candidateIndex) {

        for(VmCandidateSnapshot candidate
                : candidateSnapshots) {

            if(candidate.getCandidateIndex()
                    == candidateIndex) {

                return candidate;
            }
        }

        return null;
    }

    /**
     * Returns all candidates belonging to one destination tier.
     */
    public List<VmCandidateSnapshot>
            getCandidatesForDestination(
                    String destination) {

        if(destination == null
                || destination.trim().isEmpty()) {

            return Collections.emptyList();
        }

        String normalizedDestination =
                normalizeDestination(
                        destination);

        List<VmCandidateSnapshot> matchingCandidates =
                new ArrayList<VmCandidateSnapshot>();

        for(VmCandidateSnapshot candidate
                : candidateSnapshots) {

            if(normalizedDestination.equals(
                    normalizeDestination(
                            candidate.getDestination()))) {

                matchingCandidates.add(
                        candidate);
            }
        }

        return Collections.unmodifiableList(
                matchingCandidates);
    }

    /**
     * Returns the number of candidates belonging to one destination.
     */
    public int getCandidateCountForDestination(
            String destination) {

        return getCandidatesForDestination(
                destination).size();
    }

    /**
     * Returns all destination labels represented in the candidate set.
     */
    public Set<String> getAvailableDestinations() {

        Set<String> destinations =
                new LinkedHashSet<String>();

        for(VmCandidateSnapshot candidate
                : candidateSnapshots) {

            String destination =
                    normalizeDestination(
                            candidate.getDestination());

            if(!isAbsentDestination(
                    destination)) {

                destinations.add(
                        destination);
            }
        }

        return Collections.unmodifiableSet(
                destinations);
    }

    /**
     * Returns true when candidates from more than one destination tier
     * are represented.
     */
    public boolean hasMultipleDestinations() {

        return getAvailableDestinations().size() > 1;
    }

    /**
     * Returns true when at least one operational candidate exists for
     * the supplied destination.
     */
    public boolean hasOperationalCandidateForDestination(
            String destination) {

        for(VmCandidateSnapshot candidate
                : getCandidatesForDestination(
                        destination)) {

            if(candidate.isOperationallyAvailable()) {

                return true;
            }
        }

        return false;
    }

    /**
     * Returns true when at least one compute-feasible candidate exists
     * for the supplied destination.
     */
    public boolean hasFeasibleCandidateForDestination(
            String destination) {

        for(VmCandidateSnapshot candidate
                : getCandidatesForDestination(
                        destination)) {

            if(candidate.isFeasible()) {

                return true;
            }
        }

        return false;
    }

    private static String normalizeDestination(
            String destination) {

        if(destination == null) {

            return "NONE";
        }

        String normalized =
                destination
                .trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');

        return normalized.isEmpty()
                ? "NONE"
                : normalized;
    }

    private static boolean isAbsentDestination(
            String destination) {

        return "NONE".equals(destination)
                || "UNKNOWN".equals(destination)
                || "UNSELECTED".equals(destination)
                || "UNDETERMINED".equals(destination)
                || "NO_DESTINATION".equals(destination);
    }

    @Override
    public String toString() {

        return "SchedulerReplayContext{"
                + "taskId="
                + replayState.getTaskId()
                + ", policy='"
                + replayState.getOriginalPolicy()
                + '\''
                + ", candidateCount="
                + candidateSnapshots.size()
                + ", destinations="
                + getAvailableDestinations()
                + ", multipleDestinations="
                + hasMultipleDestinations()
                + ", policyState="
                + policyState
                + '}';
    }
}
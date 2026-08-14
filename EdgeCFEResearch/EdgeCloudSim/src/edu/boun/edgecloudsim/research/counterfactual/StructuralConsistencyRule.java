package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Evaluates internal consistency among ReplayState values.
 *
 * Selected-resource fields may use -1 as a sentinel when the original
 * scheduler decision did not select a VM. Mandatory task-level values
 * must remain within their normal domains.
 */
public final class StructuralConsistencyRule
        implements FeasibilityRule {

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .STRUCTURAL_CONSISTENCY;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        boolean originalSelectionSuccessful =
                isSuccessfulOriginalSelection(
                        replayState);

        /*
         * Required CPU is a mandatory task-level value.
         */
        boolean requiredCpuValid =
                isPercentage(
                        replayState
                        .getRequiredCpuPercent());

        /*
         * The selected-resource CPU may be -1 when no VM was
         * originally selected.
         */
        boolean selectedCpuValid =
                originalSelectionSuccessful
                ? isPercentage(
                        replayState
                        .getSelectedAvailableCpuPercent())
                : isOptionalPercentage(
                        replayState
                        .getSelectedAvailableCpuPercent());

        /*
         * Best available CPU may also be absent in incomplete or
         * unsuccessful logged decisions.
         */
        boolean bestCpuValid =
                isOptionalPercentage(
                        replayState
                        .getBestAvailableCpuPercent());

        boolean validCpuRange =
                requiredCpuValid
                && selectedCpuValid
                && bestCpuValid;

        boolean validRequiredCores =
                replayState.getRequiredCores()
                >= 0;

        /*
         * Selected-resource compute fields may be -1 when no
         * resource was selected.
         */
        boolean validSelectedVmCoreCount =
                replayState
                .getSelectedVmCoreCount()
                >= -1;

        boolean validSelectedVmMips =
                isOptionalNonNegativeFinite(
                        replayState
                        .getSelectedVmMips());

        boolean validSelectedResourceState =
                originalSelectionSuccessful
                ? replayState.getSelectedVmCoreCount() >= 0
                        && isNonNegativeFinite(
                                replayState
                                .getSelectedVmMips())
                : validSelectedVmCoreCount
                        && validSelectedVmMips;

        boolean validCandidateCounts =
                replayState
                .getCandidateVmCount()
                >= 0

                && replayState
                .getFeasibleVmCount()
                >= 0

                && replayState
                .getFeasibleVmCount()
                <= replayState
                .getCandidateVmCount();

        boolean consistent =
                validCpuRange
                && validRequiredCores
                && validSelectedResourceState
                && validCandidateCounts;

        String message;

        if(consistent) {

            message =
                    "Replay-state values are structurally consistent.";
        }
        else {

            message =
                    buildFailureMessage(
                            requiredCpuValid,
                            selectedCpuValid,
                            bestCpuValid,
                            validRequiredCores,
                            validSelectedResourceState,
                            validCandidateCounts,
                            originalSelectionSuccessful,
                            replayState);
        }

        return new ConstraintEvaluation(
                getConstraint(),
                consistent,
                consistent ? 1.0 : 0.0,
                1.0,
                consistent ? 0.0 : -1.0,
                message);
    }

    private static boolean isSuccessfulOriginalSelection(
            ReplayState replayState) {

        return "SELECTED".equalsIgnoreCase(
                replayState.getOriginalOutcome())

                && replayState
                .getOriginalSelectedVmId()
                >= 0

                && !isNoDestination(
                        replayState
                        .getOriginalDestination());
    }

    private static boolean isNoDestination(
            String destination) {

        if(destination == null) {

            return true;
        }

        String normalized =
                destination
                .trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');

        return normalized.length() == 0
                || "NONE".equals(normalized)
                || "NO_DESTINATION".equals(normalized)
                || "UNSELECTED".equals(normalized)
                || "UNDETERMINED".equals(normalized)
                || "UNKNOWN".equals(normalized);
    }

    private static boolean isPercentage(
            double value) {

        return isFinite(value)
                && value >= 0.0
                && value <= 100.0;
    }

    /**
     * Accepts -1 as an explicit unavailable-value sentinel.
     */
    private static boolean isOptionalPercentage(
            double value) {

        return value == -1.0
                || isPercentage(value);
    }

    private static boolean isNonNegativeFinite(
            double value) {

        return isFinite(value)
                && value >= 0.0;
    }

    /**
     * Accepts -1 as an explicit unavailable-value sentinel.
     */
    private static boolean isOptionalNonNegativeFinite(
            double value) {

        return value == -1.0
                || isNonNegativeFinite(value);
    }

    private static boolean isFinite(
            double value) {

        return !Double.isNaN(value)
                && !Double.isInfinite(value);
    }

    private static String buildFailureMessage(
            boolean requiredCpuValid,
            boolean selectedCpuValid,
            boolean bestCpuValid,
            boolean requiredCoresValid,
            boolean selectedResourceStateValid,
            boolean candidateCountsValid,
            boolean originalSelectionSuccessful,
            ReplayState replayState) {

        StringBuilder message =
                new StringBuilder(
                        "Replay-state structural inconsistency:");

        if(!requiredCpuValid) {

            message.append(
                    " requiredCpuPercent=")
                    .append(
                            replayState
                            .getRequiredCpuPercent())
                    .append(
                            " is outside 0-100.");
        }

        if(!selectedCpuValid) {

            message.append(
                    " selectedAvailableCpuPercent=")
                    .append(
                            replayState
                            .getSelectedAvailableCpuPercent())
                    .append(
                            originalSelectionSuccessful
                            ? " must be within 0-100 because "
                                    + "the original decision selected a VM."
                            : " must be -1 or within 0-100.");
        }

        if(!bestCpuValid) {

            message.append(
                    " bestAvailableCpuPercent=")
                    .append(
                            replayState
                            .getBestAvailableCpuPercent())
                    .append(
                            " must be -1 or within 0-100.");
        }

        if(!requiredCoresValid) {

            message.append(
                    " requiredCores=")
                    .append(
                            replayState
                            .getRequiredCores())
                    .append(
                            " cannot be negative.");
        }

        if(!selectedResourceStateValid) {

            message.append(
                    " selected VM core-count or MIPS values are "
                    + "inconsistent with the original selection "
                    + "outcome.");
        }

        if(!candidateCountsValid) {

            message.append(
                    " candidateVmCount=")
                    .append(
                            replayState
                            .getCandidateVmCount())
                    .append(
                            ", feasibleVmCount=")
                    .append(
                            replayState
                            .getFeasibleVmCount())
                    .append(
                            " are invalid.");
        }

        return message.toString();
    }
}
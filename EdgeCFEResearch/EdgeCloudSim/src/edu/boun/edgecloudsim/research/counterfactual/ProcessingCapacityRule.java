package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Evaluates whether the originally selected VM exposes valid
 * processing capacity.
 *
 * This rule checks that VM MIPS is positive and finite. Whether the
 * processing time satisfies an application deadline will be evaluated
 * later by the end-to-end latency rule.
 */
public final class ProcessingCapacityRule
        implements FeasibilityRule {

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .SELECTED_RESOURCE_PROCESSING_CAPACITY;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null) {

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        double vmMips =
                replayState.getSelectedVmMips();

        double minimumRequiredMips =
                0.0;

        boolean satisfied =
                !Double.isNaN(vmMips)
                && !Double.isInfinite(vmMips)
                && vmMips > minimumRequiredMips;

        double margin =
                vmMips
                - minimumRequiredMips;

        String message;

        if(satisfied) {

            message =
                    "The referenced resource has valid processing "
                    + "capacity. VM MIPS="
                    + vmMips
                    + ", estimated processing time="
                    + replayState
                    .estimateSelectedVmProcessingTimeMs()
                    + " ms.";
        }
        else {

            message =
                    "The referenced resource does not expose valid "
                    + "processing capacity. VM MIPS="
                    + vmMips
                    + ".";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                satisfied,
                vmMips,
                minimumRequiredMips,
                margin,
                message);
    }
}
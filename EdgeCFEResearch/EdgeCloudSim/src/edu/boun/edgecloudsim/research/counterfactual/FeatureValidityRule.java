package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Evaluates whether every applied FeatureChange is mutable
 * and remains inside its allowed range.
 */
public final class FeatureValidityRule
        implements FeasibilityRule {

    @Override
    public FeasibilityConstraint getConstraint() {

        return FeasibilityConstraint
                .FEATURE_VALIDITY;
    }

    @Override
    public ConstraintEvaluation evaluate(
            ReplayState replayState) {

        if(replayState == null){

            throw new IllegalArgumentException(
                    "ReplayState cannot be null.");
        }

        boolean valid =
                replayState
                .hasValidAppliedChanges();

        int totalChanges =
                replayState
                .getChangedFeatureCount();

        int invalidChanges =
                0;

        for(FeatureChange featureChange
                : replayState.getAppliedChanges()){

            if(!featureChange.isValidChange()){
                invalidChanges++;
            }
        }

        String message;

        if(valid){

            message =
                    "All applied feature changes are valid.";
        }
        else {

            message =
                    invalidChanges
                    + " applied feature change(s) are invalid.";
        }

        return new ConstraintEvaluation(
                getConstraint(),
                valid,
                invalidChanges,
                0.0,
                -invalidChanges,
                message);
    }
}
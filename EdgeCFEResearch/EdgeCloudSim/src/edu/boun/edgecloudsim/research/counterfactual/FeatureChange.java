package edu.boun.edgecloudsim.research.counterfactual;

/**
 * Immutable representation of one feature modification in a
 * counterfactual state.
 */
public final class FeatureChange {

    private final String featureName;
    private final FeatureCategory category;

    private final double originalValue;
    private final double counterfactualValue;

    private final double minimumAllowedValue;
    private final double maximumAllowedValue;

    private final boolean mutable;
    private final boolean actionable;

    public FeatureChange(
            String featureName,
            FeatureCategory category,
            double originalValue,
            double counterfactualValue,
            double minimumAllowedValue,
            double maximumAllowedValue,
            boolean mutable,
            boolean actionable) {

        if(featureName == null
                || featureName.trim().isEmpty()){

            throw new IllegalArgumentException(
                    "Feature name cannot be empty.");
        }

        if(minimumAllowedValue
                > maximumAllowedValue){

            throw new IllegalArgumentException(
                    "Minimum value cannot exceed maximum value.");
        }

        this.featureName =
                featureName.trim();

        this.category =
                category == null
                ? FeatureCategory.UNKNOWN
                : category;

        this.originalValue =
                originalValue;

        this.counterfactualValue =
                counterfactualValue;

        this.minimumAllowedValue =
                minimumAllowedValue;

        this.maximumAllowedValue =
                maximumAllowedValue;

        this.mutable =
                mutable;

        this.actionable =
                actionable;
    }

    public String getFeatureName() {
        return featureName;
    }

    public FeatureCategory getCategory() {
        return category;
    }

    public double getOriginalValue() {
        return originalValue;
    }

    public double getCounterfactualValue() {
        return counterfactualValue;
    }

    public double getMinimumAllowedValue() {
        return minimumAllowedValue;
    }

    public double getMaximumAllowedValue() {
        return maximumAllowedValue;
    }

    public boolean isMutable() {
        return mutable;
    }

    public boolean isActionable() {
        return actionable;
    }

    /**
     * Returns the signed change.
     *
     * Positive:
     * counterfactual value increased.
     *
     * Negative:
     * counterfactual value decreased.
     */
    public double getSignedChange() {

        return counterfactualValue
                - originalValue;
    }

    /**
     * Returns the absolute amount of change.
     */
    public double getAbsoluteChange() {

        return Math.abs(
                getSignedChange());
    }

    /**
     * Returns true when the feature value actually changed.
     */
    public boolean hasChanged() {

        return getAbsoluteChange()
                > 0.0001;
    }

    /**
     * Determines whether the proposed value is within its allowed range.
     */
    public boolean isWithinAllowedRange() {

        return counterfactualValue
                >= minimumAllowedValue
                && counterfactualValue
                <= maximumAllowedValue;
    }

    /**
     * Returns a normalized feature change between approximately 0 and 1.
     *
     * A value of 0 indicates no change.
     * A value of 1 indicates a change equal to the full allowed range.
     */
    public double getNormalizedChange() {

        double range =
                maximumAllowedValue
                - minimumAllowedValue;

        if(range <= 0.0){
            return hasChanged()
                    ? 1.0
                    : 0.0;
        }

        return getAbsoluteChange()
                / range;
    }

    /**
     * A valid counterfactual change must modify a mutable feature
     * and remain inside its allowed range.
     */
    public boolean isValidChange() {

        if(!hasChanged()){
            return true;
        }

        return mutable
                && isWithinAllowedRange();
    }

    @Override
    public String toString() {

        return "FeatureChange{"
                + "featureName='"
                + featureName
                + '\''
                + ", category="
                + category
                + ", originalValue="
                + originalValue
                + ", counterfactualValue="
                + counterfactualValue
                + ", absoluteChange="
                + getAbsoluteChange()
                + ", normalizedChange="
                + getNormalizedChange()
                + ", mutable="
                + mutable
                + ", actionable="
                + actionable
                + ", withinRange="
                + isWithinAllowedRange()
                + '}';
    }
}
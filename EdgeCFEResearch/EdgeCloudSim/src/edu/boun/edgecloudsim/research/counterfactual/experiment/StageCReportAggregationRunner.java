package edu.boun.edgecloudsim.research.counterfactual.experiment;

import java.io.File;

/** Aggregates all Stage C reports after counterfactual generation finishes. */
public final class StageCReportAggregationRunner {

    private StageCReportAggregationRunner() {
    }

    public static void main(String[] args) {

        try {
            StageCReportAggregator.aggregate(
                    1,
                    5,
                    new File(
                            "sim_results/stage_c_reports"));

            System.out.println(
                    "Stage C reports successfully aggregated "
                    + "for NORMAL, CONSTRAINED, and SEVERE "
                    + "across Iterations 1 through 5.");
        }
        catch(Exception exception) {
            System.err.println(
                    "Unable to aggregate Stage C reports: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
    }
}
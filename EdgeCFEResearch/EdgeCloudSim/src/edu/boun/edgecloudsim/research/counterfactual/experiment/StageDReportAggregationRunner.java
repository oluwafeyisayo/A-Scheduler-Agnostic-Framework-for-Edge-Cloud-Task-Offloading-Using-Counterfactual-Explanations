package edu.boun.edgecloudsim.research.counterfactual.experiment;

import java.io.File;

public final class StageDReportAggregationRunner {

    private StageDReportAggregationRunner() {
    }

    public static void main(String[] args) {

        try {

            StageDReportAggregator.aggregate(
                    1,
                    5,
                    new File(
                            "sim_results/stage_d_reports"));

            System.out.println(
                    "Stage D reports successfully aggregated "
                    + "for Iterations 1 through 5.");
        }
        catch(Exception exception) {

            System.err.println(
                    "Unable to aggregate Stage D reports: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
    }
}
package edu.boun.edgecloudsim.research.counterfactual.experiment;

import java.io.File;

public final class StageAReportAggregationRunner {

    private StageAReportAggregationRunner() {
    }

    public static void main(String[] args) {

        try {

            StageAReportAggregator.aggregate(
                    1,
                    5,
                    new File(
                            "sim_results/stage_a_reports"));

            System.out.println(
                    "Stage A reports successfully aggregated "
                    + "for Iterations 1 through 5.");
        }
        catch(Exception exception) {

            System.err.println(
                    "Unable to aggregate Stage A reports: "
                    + exception.getMessage());

            exception.printStackTrace();
        }
    }
}
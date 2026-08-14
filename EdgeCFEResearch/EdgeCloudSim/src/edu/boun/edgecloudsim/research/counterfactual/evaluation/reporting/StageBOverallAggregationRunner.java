package edu.boun.edgecloudsim.research.counterfactual.evaluation.reporting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/** Combines all completed Stage B device-profile aggregate files. */
public final class StageBOverallAggregationRunner {

    private static final int[] DEVICE_COUNTS = {100, 250, 500, 750, 1000};

    private static final File STAGE_B_ROOT =
            new File("evaluation_results/stage_b");

    private static final File OUTPUT_DIRECTORY =
            new File(STAGE_B_ROOT, "overall_aggregate");

    private StageBOverallAggregationRunner() {
    }

    public static void main(String[] args) {
        try {
            ensureDirectory(OUTPUT_DIRECTORY);

            combine("all_objective_summaries.csv",
                    "stage_b_all_objective_summaries.csv");
            combine("all_eligibility_summaries.csv",
                    "stage_b_all_eligibility_summaries.csv");
            combine("all_objective_separation_summaries.csv",
                    "stage_b_all_objective_separation_summaries.csv");
            combine("aggregate_statistics.csv",
                    "stage_b_all_aggregate_statistics.csv");
            combine("eligibility_aggregate.csv",
                    "stage_b_all_eligibility_aggregate.csv");
            combine("objective_separation_aggregate.csv",
                    "stage_b_all_objective_separation_aggregate.csv");

            System.out.println("Stage B overall aggregation completed.");
            System.out.println(OUTPUT_DIRECTORY.getAbsolutePath());
        }
        catch(Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
    }

    private static void combine(String sourceName, String outputName)
            throws IOException {

        File outputFile = new File(OUTPUT_DIRECTORY, outputName);
        String expectedHeader = null;
        int dataRows = 0;

        try(BufferedWriter writer =
                new BufferedWriter(new FileWriter(outputFile, false))) {

            for(int deviceCount : DEVICE_COUNTS) {
                File sourceFile = new File(
                        new File(
                                new File(STAGE_B_ROOT,
                                        "STAGE_B_" + deviceCount + "_DEVICES"),
                                "aggregate"),
                        sourceName);

                requireFile(sourceFile);

                try(BufferedReader reader =
                        new BufferedReader(new FileReader(sourceFile))) {

                    String header = reader.readLine();
                    if(header == null || header.trim().isEmpty()) {
                        throw new IOException("Missing CSV header: "
                                + sourceFile.getAbsolutePath());
                    }

                    if(expectedHeader == null) {
                        expectedHeader = header;
                        writer.write(header);
                        writer.newLine();
                    }
                    else if(!expectedHeader.equals(header)) {
                        throw new IOException("CSV header mismatch: "
                                + sourceFile.getAbsolutePath());
                    }

                    String line;
                    while((line = reader.readLine()) != null) {
                        if(line.trim().isEmpty()) {
                            continue;
                        }
                        writer.write(line);
                        writer.newLine();
                        dataRows++;
                    }
                }
            }
        }

        System.out.println(outputName + " -> " + dataRows + " rows");
    }

    private static void requireFile(File file) {
        if(file == null || !file.exists() || !file.isFile()
                || file.length() == 0L) {
            throw new IllegalStateException(
                    "Required Stage B aggregate file is missing: "
                    + (file == null ? "null" : file.getAbsolutePath()));
        }
    }

    private static void ensureDirectory(File directory) throws IOException {
        if(directory.exists()) {
            if(!directory.isDirectory()) {
                throw new IOException("Path is not a directory: "
                        + directory.getAbsolutePath());
            }
            return;
        }
        if(!directory.mkdirs()) {
            throw new IOException("Unable to create directory: "
                    + directory.getAbsolutePath());
        }
    }
}
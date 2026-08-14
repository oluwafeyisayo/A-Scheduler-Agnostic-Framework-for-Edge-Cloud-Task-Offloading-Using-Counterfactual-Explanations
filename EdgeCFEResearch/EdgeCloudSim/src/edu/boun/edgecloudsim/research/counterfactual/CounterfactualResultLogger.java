package edu.boun.edgecloudsim.research.counterfactual;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes batch RG-FCS results to a CSV file.
 *
 * One row represents one original offloading decision and the
 * best counterfactual returned for that decision.
 */
public final class CounterfactualResultLogger
        implements Closeable {

	private static final String CSV_HEADER =
	        "task_id,"
	        + "scheduler_policy,"
	        + "search_status,"
	        + "search_successful,"
	        + "adaptive_generated_candidate_count,"
	        + "adaptive_evaluated_candidate_count,"
	        + "adaptive_accepted_candidate_count,"
	        + "neighborhood_generated_candidate_count,"
	        + "neighborhood_evaluated_candidate_count,"
	        + "neighborhood_accepted_candidate_count,"
	        + "overall_generated_candidate_count,"
	        + "overall_evaluated_candidate_count,"
	        + "overall_accepted_candidate_count,"
	        + "improved_by_neighborhood_refinement,"
	        + "best_candidate_source,"
	        + "candidate_id,"
	        + "changed_feature_count,"
	        + "changed_features,"
	        + "proximity_score,"
	        + "sparsity_score,"
	        + "effort_score,"
	        + "network_penalty,"
	        + "total_score,"
	        + "candidate_accepted,"
	        + "result_reason";

    private final File outputFile;
    private final BufferedWriter writer;

    private boolean closed;

    public CounterfactualResultLogger(
            File outputFile)
            throws IOException {

        if(outputFile == null) {

            throw new IllegalArgumentException(
                    "Output file cannot be null.");
        }

        this.outputFile =
                outputFile;

        File parentDirectory =
                outputFile.getParentFile();

        if(parentDirectory != null
                && !parentDirectory.exists()
                && !parentDirectory.mkdirs()) {

            throw new IOException(
                    "Could not create counterfactual result directory: "
                    + parentDirectory.getAbsolutePath());
        }

        boolean writeHeader =
                !outputFile.exists()
                || outputFile.length() == 0;

        this.writer =
                new BufferedWriter(
                        new FileWriter(
                                outputFile,
                                true));

        if(writeHeader) {

            writer.write(
                    CSV_HEADER);

            writer.newLine();
            writer.flush();
        }
    }

    public File getOutputFile() {

        return outputFile;
    }

    /**
     * Writes a preformatted result record.
     *
     * Keeping the CSV serialization in a separate record object
     * prevents the logger from depending directly on every search,
     * candidate, scheduler, and verification class.
     */
    public synchronized void log(
            CounterfactualResultRecord record)
            throws IOException {

        ensureOpen();

        if(record == null) {

            throw new IllegalArgumentException(
                    "Counterfactual result record cannot be null.");
        }

        writer.write(
                record.toCsvRow());

        writer.newLine();
        writer.flush();
    }

    private void ensureOpen() {

        if(closed) {

            throw new IllegalStateException(
                    "CounterfactualResultLogger is already closed.");
        }
    }

    @Override
    public synchronized void close()
            throws IOException {

        if(closed) {

            return;
        }

        writer.flush();
        writer.close();

        closed =
                true;
    }
}
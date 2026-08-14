package edu.boun.edgecloudsim.research.experiment;

import java.io.File;
import java.io.IOException;

/**
 * Creates and exposes the directory structure for one experiment run.
 *
 * The legacy decision-log location is preserved during this phase so
 * existing replay tests continue to work.
 */
public final class ExperimentDirectoryManager {

    private final File experimentRoot;
    private final File manifestDirectory;
    private final File runtimeDirectory;
    private final File summaryDirectory;

    public ExperimentDirectoryManager(
            File outputRoot,
            String experimentId,
            String runId)
            throws IOException {

        if(outputRoot == null) {

            throw new IllegalArgumentException(
                    "Output root cannot be null.");
        }

        this.experimentRoot =
                new File(
                        new File(
                                outputRoot,
                                "experiments"),
                        sanitize(experimentId)
                        + File.separator
                        + sanitize(runId));

        this.manifestDirectory =
                new File(
                        experimentRoot,
                        "manifest");

        this.runtimeDirectory =
                new File(
                        experimentRoot,
                        "runtime");

        this.summaryDirectory =
                new File(
                        experimentRoot,
                        "summaries");

        createDirectory(
                experimentRoot);

        createDirectory(
                manifestDirectory);

        createDirectory(
                runtimeDirectory);

        createDirectory(
                summaryDirectory);
    }

    public File getExperimentRoot() {
        return experimentRoot;
    }

    public File getManifestDirectory() {
        return manifestDirectory;
    }

    public File getRuntimeDirectory() {
        return runtimeDirectory;
    }

    public File getSummaryDirectory() {
        return summaryDirectory;
    }

    public File getManifestFile() {

        return new File(
                manifestDirectory,
                "experiment_manifest.csv");
    }

    public File getRuntimeFile() {

        return new File(
                runtimeDirectory,
                "runtime_metrics.csv");
    }

    private static void createDirectory(
            File directory)
            throws IOException {

        if(directory.exists()) {

            if(!directory.isDirectory()) {

                throw new IOException(
                        "Path exists but is not a directory: "
                        + directory.getAbsolutePath());
            }

            return;
        }

        if(!directory.mkdirs()) {

            throw new IOException(
                    "Unable to create experiment directory: "
                    + directory.getAbsolutePath());
        }
    }

    private static String sanitize(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return "UNKNOWN";
        }

        return value.trim()
                .replaceAll(
                        "[^A-Za-z0-9._-]",
                        "_");
    }
}
package edu.boun.edgecloudsim.research.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import edu.boun.edgecloudsim.network.DestinationNetworkState;

/**
 * Writes task-level decisions and candidate VM provenance.
 */
public final class DecisionLogger {

    private static final String CANDIDATE_FILE_SUFFIX =
            "_candidates.csv";

    private static DecisionLogger instance;

    private PrintWriter decisionWriter;
    private PrintWriter candidateWriter;
    private boolean initialized;

    private String decisionOutputFilePath;
    private String candidateOutputFilePath;

    private DecisionLogger() {
    }

    public static synchronized DecisionLogger getInstance() {
        if(instance == null) {
            instance = new DecisionLogger();
        }
        return instance;
    }

    public synchronized void initialize(
            String outputFolder,
            String fileName) {

        close();

        if(outputFolder == null
                || outputFolder.trim().isEmpty()
                || fileName == null
                || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Output folder and file name are required.");
        }

        File folder = new File(outputFolder);
        if(!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create log folder: "
                    + folder.getAbsolutePath());
        }

        String candidateName =
                createCandidateFileName(fileName);

        File decisionFile = new File(folder, fileName);
        File candidateFile = new File(folder, candidateName);

        decisionOutputFilePath =
                decisionFile.getAbsolutePath();
        candidateOutputFilePath =
                candidateFile.getAbsolutePath();

        try {
            decisionWriter = new PrintWriter(decisionFile);
            candidateWriter = new PrintWriter(candidateFile);
        }
        catch(FileNotFoundException exception) {
            close();
            throw new IllegalStateException(
                    "Could not create decision log files.",
                    exception);
        }

        writeDecisionHeader();
        writeCandidateHeader();
        initialized = true;

        System.out.println(
                "Decision logger initialized: "
                + decisionOutputFilePath);
        System.out.println(
                "Candidate VM logger initialized: "
                + candidateOutputFilePath);
    }

    private String createCandidateFileName(String fileName) {
        String trimmed = fileName.trim();
        if(trimmed.toLowerCase(Locale.US).endsWith(".csv")) {
            return trimmed.substring(0, trimmed.length() - 4)
                    + CANDIDATE_FILE_SUFFIX;
        }
        return trimmed + CANDIDATE_FILE_SUFFIX;
    }

    private void writeDecisionHeader() {
        decisionWriter.println(
                "simulation_time,"
                + "task_id,"
                + "mobile_device_id,"
                + "application_type,"
                + "orchestrator_policy,"
                + "decision_outcome,"
                + "selected_destination,"
                + "attempted_destination,"
                + "selected_vm_id,"
                + "selected_host_id,"
                + "selected_datacenter_id,"
                + "selected_candidate_index,"
                + "task_length_mi,"
                + "required_cores,"
                + "upload_size_kb,"
                + "download_size_kb,"

                + "edge_serving_wlan_id,"
                + "edge_configured_bandwidth_mbps,"
                + "edge_available_bandwidth_mbps,"
                + "edge_network_latency_ms,"
                + "edge_propagation_delay_ms,"
                + "edge_network_upload_delay_ms,"
                + "edge_connected_device_count,"
                + "edge_network_feasible,"
                + "edge_network_infeasibility_reason,"

                + "cloud_serving_wlan_id,"
                + "cloud_configured_bandwidth_mbps,"
                + "cloud_available_bandwidth_mbps,"
                + "cloud_network_latency_ms,"
                + "cloud_propagation_delay_ms,"
                + "cloud_network_upload_delay_ms,"
                + "cloud_connected_device_count,"
                + "cloud_network_feasible,"
                + "cloud_network_infeasibility_reason,"

                + "required_cpu_percent,"
                + "selected_vm_mips,"
                + "selected_vm_core_count,"
                + "candidate_vm_count,"
                + "feasible_vm_count,"
                + "selected_available_cpu_percent,"
                + "best_available_cpu_percent,"
                + "selection_reason,"
                + "round_robin_starting_candidate_index,"
                + "random_sampled_candidate_index");
        decisionWriter.flush();
    }

    private void writeCandidateHeader() {
        candidateWriter.println(
                "simulation_time,"
                + "task_id,"
                + "orchestrator_policy,"
                + "candidate_index,"
                + "vm_id,"
                + "host_id,"
                + "datacenter_id,"
                + "destination,"
                + "vm_mips,"
                + "vm_core_count,"
                + "available_cpu_percent,"
                + "feasible,"
                + "infeasibility_reason,"
                + "selected_candidate");
        candidateWriter.flush();
    }

    public synchronized void logDecision(
            DecisionRecord record) {

        ensureInitialized();

        if(record == null) {
            throw new IllegalArgumentException(
                    "Decision record cannot be null.");
        }

        writeDecisionRecord(record);
        writeCandidateRecords(record);

        decisionWriter.flush();
        candidateWriter.flush();
    }

    private void writeDecisionRecord(
            DecisionRecord record) {

        StringBuilder row = new StringBuilder();

        append(row, record.getSimulationTime());
        append(row, record.getTaskId());
        append(row, record.getMobileDeviceId());
        append(row, escapeCsv(record.getApplicationType()));
        append(row, escapeCsv(record.getOrchestratorPolicy()));
        append(row, escapeCsv(record.getDecisionOutcome()));
        append(row, escapeCsv(record.getSelectedDestination()));
        append(row, escapeCsv(record.getAttemptedDestination()));
        append(row, record.getSelectedVmId());
        append(row, record.getSelectedHostId());
        append(row, record.getSelectedDatacenterId());
        append(row, record.getSelectedCandidateIndex());
        append(row, record.getTaskLengthMi());
        append(row, record.getRequiredCores());
        append(row, record.getUploadSizeKb());
        append(row, record.getDownloadSizeKb());

        appendNetworkState(
                row,
                record.getEdgeNetworkState());

        appendNetworkState(
                row,
                record.getCloudNetworkState());

        append(row, record.getRequiredCpuPercent());
        append(row, record.getSelectedVmMips());
        append(row, record.getSelectedVmCoreCount());
        append(row, record.getCandidateVmCount());
        append(row, record.getFeasibleVmCount());
        append(row, record.getSelectedAvailableCpuPercent());
        append(row, record.getBestAvailableCpuPercent());
        append(row, escapeCsv(record.getSelectionReason()));
        append(row, record.getRoundRobinStartingCandidateIndex());
        append(row, record.getRandomSampledCandidateIndex());

        decisionWriter.println(row.toString());
    }

    private void appendNetworkState(
            StringBuilder row,
            DestinationNetworkState state) {

        append(row, state.getServingWlanId());
        append(row, state.getConfiguredBandwidthMbps());
        append(row, state.getAvailableBandwidthMbps());

        /*
         * The current network instrumentation exposes total modeled
         * upload delay and propagation delay. The total upload delay is
         * also retained as the tier-specific latency value for backward
         * compatibility with the existing replay equations.
         */
        append(row, state.getUploadDelayMilliseconds());
        append(row, state.getPropagationDelayMilliseconds());
        append(row, state.getUploadDelayMilliseconds());
        append(row, state.getConnectedDeviceCount());
        append(row, Boolean.toString(state.isNetworkFeasible()));
        append(row, escapeCsv(state.getInfeasibilityReason()));
    }

    private void writeCandidateRecords(
            DecisionRecord record) {

        List<CandidateVmRecord> candidates =
                record.getCandidateVmRecords();

        if(candidates == null || candidates.isEmpty()) {
            return;
        }

        for(CandidateVmRecord candidate : candidates) {
            boolean selected =
                    candidate.getCandidateIndex()
                    == record.getSelectedCandidateIndex();

            candidateWriter.printf(
                    Locale.US,
                    "%.4f,%d,%s,%d,%d,%d,%d,%s,"
                    + "%.4f,%d,%.4f,%s,%s,%s%n",
                    record.getSimulationTime(),
                    record.getTaskId(),
                    escapeCsv(record.getOrchestratorPolicy()),
                    candidate.getCandidateIndex(),
                    candidate.getVmId(),
                    candidate.getHostId(),
                    candidate.getDatacenterId(),
                    escapeCsv(candidate.getDestination()),
                    candidate.getVmMips(),
                    candidate.getVmCoreCount(),
                    candidate.getAvailableCpuPercent(),
                    Boolean.toString(candidate.isFeasible()),
                    escapeCsv(candidate.getInfeasibilityReason()),
                    Boolean.toString(selected));
        }
    }

    private void append(StringBuilder row, Object value) {
        if(row.length() > 0) {
            row.append(',');
        }
        row.append(value == null ? "UNKNOWN" : value);
    }

    private void ensureInitialized() {
        if(!initialized
                || decisionWriter == null
                || candidateWriter == null) {
            throw new IllegalStateException(
                    "DecisionLogger must be initialized before use.");
        }
    }

    public synchronized void close() {
        if(decisionWriter != null) {
            decisionWriter.flush();
            decisionWriter.close();
            decisionWriter = null;
        }

        if(candidateWriter != null) {
            candidateWriter.flush();
            candidateWriter.close();
            candidateWriter = null;
        }

        initialized = false;
        decisionOutputFilePath = null;
        candidateOutputFilePath = null;
    }
    

    public synchronized boolean isInitialized() {
        return initialized
                && decisionWriter != null
                && candidateWriter != null;
    }

    public synchronized String getOutputFilePath() {
        return decisionOutputFilePath;
    }

    public synchronized String getDecisionOutputFilePath() {
        return decisionOutputFilePath;
    }

    public synchronized String getCandidateOutputFilePath() {
        return candidateOutputFilePath;
    }

    private String escapeCsv(String value) {
        if(value == null) {
            return "UNKNOWN";
        }

        boolean quote =
                value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");

        if(!quote) {
            return value;
        }

        return "\""
                + value.replace("\"", "\"\"")
                + "\"";
    }
}
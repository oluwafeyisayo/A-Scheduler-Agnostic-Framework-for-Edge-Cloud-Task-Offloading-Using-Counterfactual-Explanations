package edu.boun.edgecloudsim.research.counterfactual;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.boun.edgecloudsim.network.DestinationNetworkState;

/**
 * Reads the tier-specific decision CSV and reconstructs DecisionState.
 */
public final class DecisionReplayEngine {

    private static final int EXPECTED_COLUMN_COUNT =
            44;

    public List<DecisionState> loadDecisions(
            String csvFilePath)
            throws IOException {

        validateFilePath(csvFilePath);

        File csvFile = new File(csvFilePath);

        if(!csvFile.exists() || !csvFile.isFile()) {
            throw new IOException(
                    "Decision CSV does not exist or is not a file: "
                    + csvFile.getAbsolutePath());
        }

        List<DecisionState> decisions =
                new ArrayList<DecisionState>();

        int skipped = 0;

        try(BufferedReader reader =
                new BufferedReader(
                        new FileReader(csvFile))) {

            String headerLine = reader.readLine();

            if(headerLine == null) {
                throw new IOException(
                        "Decision CSV is empty: "
                        + csvFile.getAbsolutePath());
            }

            validateHeader(headerLine);

            String line;
            int lineNumber = 1;

            while((line = reader.readLine()) != null) {
                lineNumber++;

                if(line.trim().isEmpty()) {
                    continue;
                }

                try {
                    decisions.add(
                            parseDecisionState(
                                    line,
                                    lineNumber));
                }
                catch(RuntimeException exception) {
                    skipped++;
                    System.err.println(
                            "[DecisionReplayEngine] Skipping invalid "
                            + "CSV row "
                            + lineNumber
                            + ": "
                            + exception.getMessage());
                }
            }
        }

        if(skipped > 0) {
            System.err.println(
                    "[DecisionReplayEngine] Loaded "
                    + decisions.size()
                    + " valid decisions and skipped "
                    + skipped
                    + " invalid rows.");
        }

        return Collections.unmodifiableList(decisions);
    }

    public List<DecisionState> loadSelectedDecisions(
            String csvFilePath)
            throws IOException {

        List<DecisionState> selected =
                new ArrayList<DecisionState>();

        for(DecisionState decision : loadDecisions(csvFilePath)) {
            if(decision.isSelectedDecision()) {
                selected.add(decision);
            }
        }

        return Collections.unmodifiableList(selected);
    }

    public List<DecisionState> loadUnsuccessfulDecisions(
            String csvFilePath)
            throws IOException {

        List<DecisionState> unsuccessful =
                new ArrayList<DecisionState>();

        for(DecisionState decision : loadDecisions(csvFilePath)) {
            if(!decision.isSelectedDecision()) {
                unsuccessful.add(decision);
            }
        }

        return Collections.unmodifiableList(unsuccessful);
    }

    public List<DecisionState> loadUnsuccessfulDecisionsByPolicy(
            String csvFilePath,
            String policy)
            throws IOException {

        validatePolicy(policy);

        List<DecisionState> matching =
                new ArrayList<DecisionState>();

        for(DecisionState decision
                : loadUnsuccessfulDecisions(csvFilePath)) {
            if(policy.equalsIgnoreCase(
                    decision.getOrchestratorPolicy())) {
                matching.add(decision);
            }
        }

        return Collections.unmodifiableList(matching);
    }

    public List<DecisionState> loadSelectedDecisionsByPolicy(
            String csvFilePath,
            String policy)
            throws IOException {

        validatePolicy(policy);

        List<DecisionState> matching =
                new ArrayList<DecisionState>();

        for(DecisionState decision
                : loadSelectedDecisions(csvFilePath)) {
            if(policy.equalsIgnoreCase(
                    decision.getOrchestratorPolicy())) {
                matching.add(decision);
            }
        }

        return Collections.unmodifiableList(matching);
    }

    public DecisionState findByTaskId(
            String csvFilePath,
            int taskId)
            throws IOException {

        for(DecisionState decision : loadDecisions(csvFilePath)) {
            if(decision.getTaskId() == taskId) {
                return decision;
            }
        }

        return null;
    }

    private DecisionState parseDecisionState(
            String line,
            int lineNumber) {

        List<String> c = parseCsvLine(line);

        if(c.size() != EXPECTED_COLUMN_COUNT) {
            throw new IllegalArgumentException(
                    "Expected "
                    + EXPECTED_COLUMN_COUNT
                    + " columns but found "
                    + c.size()
                    + " at CSV line "
                    + lineNumber
                    + ".");
        }

        DestinationNetworkState edgeState =
                new DestinationNetworkState(
                        "EDGE",
                        parseInt(c.get(16), "edge_serving_wlan_id"),
                        parseInt(
                                c.get(22),
                                "edge_connected_device_count"),
                        parseDouble(
                                c.get(17),
                                "edge_configured_bandwidth_mbps"),
                        parseDouble(
                                c.get(18),
                                "edge_available_bandwidth_mbps"),
                        millisecondsToSeconds(
                                parseDouble(
                                        c.get(20),
                                        "edge_propagation_delay_ms")),
                        millisecondsToSeconds(
                                parseDouble(
                                        c.get(21),
                                        "edge_network_upload_delay_ms")),
                        parseBoolean(
                                c.get(23),
                                "edge_network_feasible"),
                        c.get(24));

        DestinationNetworkState cloudState =
                new DestinationNetworkState(
                        "CLOUD",
                        parseInt(c.get(25), "cloud_serving_wlan_id"),
                        parseInt(
                                c.get(31),
                                "cloud_connected_device_count"),
                        parseDouble(
                                c.get(26),
                                "cloud_configured_bandwidth_mbps"),
                        parseDouble(
                                c.get(27),
                                "cloud_available_bandwidth_mbps"),
                        millisecondsToSeconds(
                                parseDouble(
                                        c.get(29),
                                        "cloud_propagation_delay_ms")),
                        millisecondsToSeconds(
                                parseDouble(
                                        c.get(30),
                                        "cloud_network_upload_delay_ms")),
                        parseBoolean(
                                c.get(32),
                                "cloud_network_feasible"),
                        c.get(33));

        double requiredCpuPercent =
                parseDouble(
                        c.get(34),
                        "required_cpu_percent");

        double selectedAvailableCpuPercent =
                parseDouble(
                        c.get(39),
                        "selected_available_cpu_percent");

        double bestAvailableCpuPercent =
                parseDouble(
                        c.get(40),
                        "best_available_cpu_percent");

        validatePercentage(
                requiredCpuPercent,
                false,
                "required_cpu_percent",
                lineNumber);

        validatePercentage(
                selectedAvailableCpuPercent,
                true,
                "selected_available_cpu_percent",
                lineNumber);

        validatePercentage(
                bestAvailableCpuPercent,
                true,
                "best_available_cpu_percent",
                lineNumber);

        return new DecisionState(
                parseDouble(c.get(0), "simulation_time"),
                parseInt(c.get(1), "task_id"),
                parseInt(c.get(2), "mobile_device_id"),
                c.get(3),
                c.get(4),
                c.get(5),
                c.get(6),
                c.get(7),
                parseInt(c.get(8), "selected_vm_id"),
                parseInt(c.get(9), "selected_host_id"),
                parseInt(c.get(10), "selected_datacenter_id"),
                parseDouble(c.get(12), "task_length_mi"),
                parseInt(c.get(13), "required_cores"),
                parseDouble(c.get(14), "upload_size_kb"),
                parseDouble(c.get(15), "download_size_kb"),
                requiredCpuPercent,
                edgeState,
                cloudState,
                selectedAvailableCpuPercent,
                parseDouble(c.get(35), "selected_vm_mips"),
                parseInt(c.get(36), "selected_vm_core_count"),
                parseInt(c.get(37), "candidate_vm_count"),
                parseInt(c.get(38), "feasible_vm_count"),
                selectedAvailableCpuPercent,
                bestAvailableCpuPercent,
                c.get(41),
                parseInt(
                        c.get(42),
                        "round_robin_starting_candidate_index"),
                parseInt(
                        c.get(43),
                        "random_sampled_candidate_index"));
    }

    private void validateHeader(
            String headerLine)
            throws IOException {

        List<String> actual =
                parseCsvLine(headerLine);

        String[] expected = {
                "simulation_time",
                "task_id",
                "mobile_device_id",
                "application_type",
                "orchestrator_policy",
                "decision_outcome",
                "selected_destination",
                "attempted_destination",
                "selected_vm_id",
                "selected_host_id",
                "selected_datacenter_id",
                "selected_candidate_index",
                "task_length_mi",
                "required_cores",
                "upload_size_kb",
                "download_size_kb",

                "edge_serving_wlan_id",
                "edge_configured_bandwidth_mbps",
                "edge_available_bandwidth_mbps",
                "edge_network_latency_ms",
                "edge_propagation_delay_ms",
                "edge_network_upload_delay_ms",
                "edge_connected_device_count",
                "edge_network_feasible",
                "edge_network_infeasibility_reason",

                "cloud_serving_wlan_id",
                "cloud_configured_bandwidth_mbps",
                "cloud_available_bandwidth_mbps",
                "cloud_network_latency_ms",
                "cloud_propagation_delay_ms",
                "cloud_network_upload_delay_ms",
                "cloud_connected_device_count",
                "cloud_network_feasible",
                "cloud_network_infeasibility_reason",

                "required_cpu_percent",
                "selected_vm_mips",
                "selected_vm_core_count",
                "candidate_vm_count",
                "feasible_vm_count",
                "selected_available_cpu_percent",
                "best_available_cpu_percent",
                "selection_reason",
                "round_robin_starting_candidate_index",
                "random_sampled_candidate_index"
        };

        if(actual.size() != expected.length) {
            throw new IOException(
                    "Unexpected decision CSV header. Expected "
                    + expected.length
                    + " columns but found "
                    + actual.size()
                    + ".");
        }

        for(int i = 0; i < expected.length; i++) {
            String normalized =
                    normalizeHeader(actual.get(i));

            if(!expected[i].equals(normalized)) {
                throw new IOException(
                        "Unexpected header at index "
                        + i
                        + ". Expected '"
                        + expected[i]
                        + "' but found '"
                        + normalized
                        + "'.");
            }
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for(int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if(ch == '"') {
                if(insideQuotes
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                }
                else {
                    insideQuotes = !insideQuotes;
                }
            }
            else if(ch == ',' && !insideQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            }
            else {
                current.append(ch);
            }
        }

        if(insideQuotes) {
            throw new IllegalArgumentException(
                    "Unclosed quoted CSV field.");
        }

        values.add(current.toString().trim());
        return values;
    }

    private double millisecondsToSeconds(double milliseconds) {
        return milliseconds < 0.0
                ? -1.0
                : milliseconds / 1000.0;
    }

    private void validatePercentage(
            double value,
            boolean optional,
            String columnName,
            int lineNumber) {

        if(optional && value == -1.0) {
            return;
        }

        if(Double.isNaN(value)
                || Double.isInfinite(value)
                || value < 0.0
                || value > 100.0) {
            throw new IllegalArgumentException(
                    "Invalid "
                    + columnName
                    + "="
                    + value
                    + " at CSV line "
                    + lineNumber
                    + ".");
        }
    }

    private void validateFilePath(String path) {
        if(path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Decision CSV path cannot be empty.");
        }
    }

    private void validatePolicy(String policy) {
        if(policy == null || policy.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Scheduler policy cannot be empty.");
        }
    }

    private String normalizeHeader(String value) {
        if(value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase();

        if(normalized.startsWith("\uFEFF")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    private int parseInt(String value, String columnName) {
        try {
            return Integer.parseInt(value);
        }
        catch(NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid integer in column "
                    + columnName
                    + ": "
                    + value,
                    exception);
        }
    }

    private double parseDouble(String value, String columnName) {
        try {
            return Double.parseDouble(value);
        }
        catch(NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid decimal in column "
                    + columnName
                    + ": "
                    + value,
                    exception);
        }
    }

    private boolean parseBoolean(
            String value,
            String columnName) {

        if("true".equalsIgnoreCase(value)) {
            return true;
        }

        if("false".equalsIgnoreCase(value)) {
            return false;
        }

        throw new IllegalArgumentException(
                "Invalid boolean in column "
                + columnName
                + ": "
                + value);
    }
}
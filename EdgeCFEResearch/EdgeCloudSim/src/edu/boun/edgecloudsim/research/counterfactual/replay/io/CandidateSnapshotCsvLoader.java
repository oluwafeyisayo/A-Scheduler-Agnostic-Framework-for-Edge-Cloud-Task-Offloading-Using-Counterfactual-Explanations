package edu.boun.edgecloudsim.research.counterfactual.replay.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.boun.edgecloudsim.research.counterfactual.replay
        .VmCandidateSnapshot;

/**
 * Loads scheduler candidate snapshots from the candidate CSV.
 *
 * Candidate rows are joined to decision rows by task_id because
 * task-level compute requirements are stored in the decision CSV.
 */
public final class CandidateSnapshotCsvLoader {

    private static final double EPSILON =
            0.000001;

    /**
     * Task-level compute requirements loaded from the decision CSV.
     */
    private static final class TaskComputeRequirements {

        private final double requiredCpuPercent;
        private final int requiredCores;

        private TaskComputeRequirements(
                double requiredCpuPercent,
                int requiredCores) {

            this.requiredCpuPercent =
                    requiredCpuPercent;

            this.requiredCores =
                    requiredCores;
        }
    }

    public Map<Integer, List<VmCandidateSnapshot>> load(
            File candidateCsvFile,
            File decisionCsvFile)
            throws IOException {

        validateReadableFile(
                candidateCsvFile,
                "Candidate CSV");

        validateReadableFile(
                decisionCsvFile,
                "Decision CSV");

        Map<Integer, TaskComputeRequirements>
                requirementsByTaskId =
                loadTaskComputeRequirements(
                        decisionCsvFile);

        Map<Integer, List<VmCandidateSnapshot>>
                candidatesByTaskId =
                loadCandidateSnapshots(
                        candidateCsvFile,
                        requirementsByTaskId);

        return makeImmutable(
                candidatesByTaskId);
    }

    public Map<Integer, List<VmCandidateSnapshot>> load(
            String candidateCsvPath,
            String decisionCsvPath)
            throws IOException {

        if(candidateCsvPath == null
                || candidateCsvPath.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Candidate CSV path cannot be empty.");
        }

        if(decisionCsvPath == null
                || decisionCsvPath.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Decision CSV path cannot be empty.");
        }

        return load(
                new File(candidateCsvPath),
                new File(decisionCsvPath));
    }

    private Map<Integer, TaskComputeRequirements>
            loadTaskComputeRequirements(
                    File decisionCsvFile)
                    throws IOException {

        Map<Integer, TaskComputeRequirements>
                requirementsByTaskId =
                new LinkedHashMap<Integer,
                        TaskComputeRequirements>();

        int skippedRowCount =
                0;

        try(BufferedReader reader =
                new BufferedReader(
                        new FileReader(
                                decisionCsvFile))) {

            String headerLine =
                    reader.readLine();

            if(headerLine == null
                    || headerLine.trim().isEmpty()) {

                throw new IOException(
                        "Decision CSV is empty: "
                        + decisionCsvFile
                        .getAbsolutePath());
            }

            List<String> headers =
                    parseCsvLine(
                            headerLine);

            Map<String, Integer> headerIndexes =
                    createHeaderIndex(
                            headers);

            int taskIdColumn =
                    requireColumn(
                            headerIndexes,
                            "task_id",
                            "Decision CSV");

            int requiredCpuColumn =
                    requireColumn(
                            headerIndexes,
                            "required_cpu_percent",
                            "Decision CSV");

            int requiredCoresColumn =
                    requireColumn(
                            headerIndexes,
                            "required_cores",
                            "Decision CSV");

            String line;
            int lineNumber =
                    1;

            while((line = reader.readLine())
                    != null) {

                lineNumber++;

                if(line.trim().isEmpty()) {
                    continue;
                }

                try {

                    List<String> values =
                            parseCsvLine(
                                    line);

                    if(values.size()
                            != headers.size()) {

                        throw new IOException(
                                "Expected "
                                + headers.size()
                                + " columns but found "
                                + values.size()
                                + ".");
                    }

                    int taskId =
                            parseInteger(
                                    getValue(
                                            values,
                                            taskIdColumn,
                                            lineNumber,
                                            "task_id"),
                                    lineNumber,
                                    "task_id");

                    double requiredCpu =
                            parseDouble(
                                    getValue(
                                            values,
                                            requiredCpuColumn,
                                            lineNumber,
                                            "required_cpu_percent"),
                                    lineNumber,
                                    "required_cpu_percent");

                    int requiredCores =
                            parseInteger(
                                    getValue(
                                            values,
                                            requiredCoresColumn,
                                            lineNumber,
                                            "required_cores"),
                                    lineNumber,
                                    "required_cores");

                    if(!isValidPercentage(
                            requiredCpu)) {

                        throw new IOException(
                                "required_cpu_percent must be "
                                + "between 0 and 100.");
                    }

                    if(requiredCores < 0) {

                        throw new IOException(
                                "required_cores cannot be negative.");
                    }

                    TaskComputeRequirements previous =
                            requirementsByTaskId.get(
                                    taskId);

                    if(previous != null) {

                        boolean conflictingCpu =
                                Math.abs(
                                        previous
                                        .requiredCpuPercent
                                        - requiredCpu)
                                        > EPSILON;

                        boolean conflictingCores =
                                previous.requiredCores
                                != requiredCores;

                        if(conflictingCpu
                                || conflictingCores) {

                            throw new IOException(
                                    "Conflicting compute requirements "
                                    + "for task "
                                    + taskId
                                    + ".");
                        }
                    }

                    requirementsByTaskId.put(
                            taskId,
                            new TaskComputeRequirements(
                                    requiredCpu,
                                    requiredCores));
                }
                catch(IOException exception) {

                    skippedRowCount++;

                    System.err.println(
                            "[CandidateSnapshotCsvLoader] "
                            + "Skipping invalid decision CSV row "
                            + lineNumber
                            + ": "
                            + exception.getMessage());
                }
                catch(RuntimeException exception) {

                    skippedRowCount++;

                    System.err.println(
                            "[CandidateSnapshotCsvLoader] "
                            + "Skipping invalid decision CSV row "
                            + lineNumber
                            + ": "
                            + exception.getMessage());
                }
            }
        }

        if(skippedRowCount > 0) {

            System.err.println(
                    "[CandidateSnapshotCsvLoader] Loaded compute "
                    + "requirements for "
                    + requirementsByTaskId.size()
                    + " tasks and skipped "
                    + skippedRowCount
                    + " invalid decision rows.");
        }

        return requirementsByTaskId;
    }

    private Map<Integer, List<VmCandidateSnapshot>>
            loadCandidateSnapshots(
                    File candidateCsvFile,
                    Map<Integer, TaskComputeRequirements>
                            requirementsByTaskId)
                    throws IOException {

        Map<Integer, List<VmCandidateSnapshot>>
                candidatesByTaskId =
                new LinkedHashMap<Integer,
                        List<VmCandidateSnapshot>>();

        int skippedRowCount =
                0;

        try(BufferedReader reader =
                new BufferedReader(
                        new FileReader(
                                candidateCsvFile))) {

            String headerLine =
                    reader.readLine();

            if(headerLine == null
                    || headerLine.trim().isEmpty()) {

                throw new IOException(
                        "Candidate CSV is empty: "
                        + candidateCsvFile
                        .getAbsolutePath());
            }

            List<String> headers =
                    parseCsvLine(
                            headerLine);

            Map<String, Integer> headerIndexes =
                    createHeaderIndex(
                            headers);

            int taskIdColumn =
                    requireColumn(
                            headerIndexes,
                            "task_id",
                            "Candidate CSV");

            int candidateIndexColumn =
                    requireColumn(
                            headerIndexes,
                            "candidate_index",
                            "Candidate CSV");

            int vmIdColumn =
                    requireColumn(
                            headerIndexes,
                            "vm_id",
                            "Candidate CSV");

            int hostIdColumn =
                    requireColumn(
                            headerIndexes,
                            "host_id",
                            "Candidate CSV");

            int datacenterIdColumn =
                    requireColumn(
                            headerIndexes,
                            "datacenter_id",
                            "Candidate CSV");

            int destinationColumn =
                    requireColumn(
                            headerIndexes,
                            "destination",
                            "Candidate CSV");

            int vmMipsColumn =
                    requireColumn(
                            headerIndexes,
                            "vm_mips",
                            "Candidate CSV");

            int vmCoreCountColumn =
                    requireColumn(
                            headerIndexes,
                            "vm_core_count",
                            "Candidate CSV");

            int availableCpuColumn =
                    requireColumn(
                            headerIndexes,
                            "available_cpu_percent",
                            "Candidate CSV");

            /*
             * The existing feasible column is retained as a required
             * column for CSV compatibility. Its value is parsed but is
             * no longer treated as the authoritative compute result.
             */
            int feasibleColumn =
                    requireColumn(
                            headerIndexes,
                            "feasible",
                            "Candidate CSV");

            String line;
            int lineNumber =
                    1;

            while((line = reader.readLine())
                    != null) {

                lineNumber++;

                if(line.trim().isEmpty()) {
                    continue;
                }

                try {

                    List<String> values =
                            parseCsvLine(
                                    line);

                    if(values.size()
                            != headers.size()) {

                        throw new IOException(
                                "Expected "
                                + headers.size()
                                + " columns but found "
                                + values.size()
                                + ".");
                    }

                    int taskId =
                            parseInteger(
                                    getValue(
                                            values,
                                            taskIdColumn,
                                            lineNumber,
                                            "task_id"),
                                    lineNumber,
                                    "task_id");

                    TaskComputeRequirements requirements =
                            requirementsByTaskId.get(
                                    taskId);

                    if(requirements == null) {

                        throw new IOException(
                                "No matching decision record "
                                + "found for task "
                                + taskId
                                + ".");
                    }

                    int candidateIndex =
                            parseInteger(
                                    getValue(
                                            values,
                                            candidateIndexColumn,
                                            lineNumber,
                                            "candidate_index"),
                                    lineNumber,
                                    "candidate_index");

                    int vmId =
                            parseInteger(
                                    getValue(
                                            values,
                                            vmIdColumn,
                                            lineNumber,
                                            "vm_id"),
                                    lineNumber,
                                    "vm_id");

                    int hostId =
                            parseInteger(
                                    getValue(
                                            values,
                                            hostIdColumn,
                                            lineNumber,
                                            "host_id"),
                                    lineNumber,
                                    "host_id");

                    int datacenterId =
                            parseInteger(
                                    getValue(
                                            values,
                                            datacenterIdColumn,
                                            lineNumber,
                                            "datacenter_id"),
                                    lineNumber,
                                    "datacenter_id");

                    String destination =
                            getValue(
                                    values,
                                    destinationColumn,
                                    lineNumber,
                                    "destination");

                    double vmMips =
                            parseDouble(
                                    getValue(
                                            values,
                                            vmMipsColumn,
                                            lineNumber,
                                            "vm_mips"),
                                    lineNumber,
                                    "vm_mips");

                    int vmCoreCount =
                            parseInteger(
                                    getValue(
                                            values,
                                            vmCoreCountColumn,
                                            lineNumber,
                                            "vm_core_count"),
                                    lineNumber,
                                    "vm_core_count");

                    double availableCpu =
                            parseDouble(
                                    getValue(
                                            values,
                                            availableCpuColumn,
                                            lineNumber,
                                            "available_cpu_percent"),
                                    lineNumber,
                                    "available_cpu_percent");

                    if(!isValidPercentage(
                            availableCpu)) {

                        throw new IOException(
                                "available_cpu_percent must be "
                                + "between 0 and 100.");
                    }

                    if(Double.isNaN(vmMips)
                            || Double.isInfinite(vmMips)
                            || vmMips < 0.0) {

                        throw new IOException(
                                "vm_mips must be finite and "
                                + "non-negative.");
                    }

                    if(vmCoreCount < 0) {

                        throw new IOException(
                                "vm_core_count cannot be negative.");
                    }

                    /*
                     * Parse the logged value to validate the CSV, but
                     * recompute CPU feasibility from the reconstructed
                     * task and VM values.
                     */
                    boolean loggedFeasible =
                            parseBoolean(
                                    getValue(
                                            values,
                                            feasibleColumn,
                                            lineNumber,
                                            "feasible"),
                                    lineNumber,
                                    "feasible");

                    boolean cpuFeasible =
                            availableCpu
                            + EPSILON
                            >= requirements
                            .requiredCpuPercent;

                    boolean operationallyAvailable =
                            true;

                    int evaluationOrder =
                            candidateIndex;

                    VmCandidateSnapshot snapshot =
                            new VmCandidateSnapshot(
                                    candidateIndex,
                                    vmId,
                                    hostId,
                                    datacenterId,
                                    destination,
                                    requirements
                                    .requiredCpuPercent,
                                    availableCpu,
                                    requirements
                                    .requiredCores,
                                    vmMips,
                                    vmCoreCount,
                                    cpuFeasible,
                                    operationallyAvailable,
                                    evaluationOrder);

                    List<VmCandidateSnapshot>
                            taskCandidates =
                            candidatesByTaskId.get(
                                    taskId);

                    if(taskCandidates == null) {

                        taskCandidates =
                                new ArrayList
                                <VmCandidateSnapshot>();

                        candidatesByTaskId.put(
                                taskId,
                                taskCandidates);
                    }

                    ensureUniqueCandidateIndex(
                            taskCandidates,
                            snapshot,
                            taskId,
                            lineNumber);

                    taskCandidates.add(
                            snapshot);
                }
                catch(IOException exception) {

                    skippedRowCount++;

                    System.err.println(
                            "[CandidateSnapshotCsvLoader] "
                            + "Skipping invalid candidate CSV row "
                            + lineNumber
                            + ": "
                            + exception.getMessage());
                }
                catch(RuntimeException exception) {

                    skippedRowCount++;

                    System.err.println(
                            "[CandidateSnapshotCsvLoader] "
                            + "Skipping invalid candidate CSV row "
                            + lineNumber
                            + ": "
                            + exception.getMessage());
                }
            }
        }

        if(skippedRowCount > 0) {

            System.err.println(
                    "[CandidateSnapshotCsvLoader] Loaded candidates "
                    + "for "
                    + candidatesByTaskId.size()
                    + " tasks and skipped "
                    + skippedRowCount
                    + " invalid candidate rows.");
        }

        return candidatesByTaskId;
    }

    private static void ensureUniqueCandidateIndex(
            List<VmCandidateSnapshot> existingCandidates,
            VmCandidateSnapshot newCandidate,
            int taskId,
            int lineNumber)
            throws IOException {

        for(VmCandidateSnapshot existing
                : existingCandidates) {

            if(existing.getCandidateIndex()
                    == newCandidate.getCandidateIndex()) {

                throw new IOException(
                        "Duplicate candidate_index "
                        + newCandidate
                        .getCandidateIndex()
                        + " for task "
                        + taskId
                        + " at candidate CSV line "
                        + lineNumber
                        + ".");
            }
        }
    }

    private static Map<Integer,
            List<VmCandidateSnapshot>>
            makeImmutable(
                    Map<Integer,
                    List<VmCandidateSnapshot>>
                    source) {

        Map<Integer, List<VmCandidateSnapshot>>
                immutable =
                new LinkedHashMap<Integer,
                        List<VmCandidateSnapshot>>();

        for(Map.Entry<Integer,
                List<VmCandidateSnapshot>> entry
                : source.entrySet()) {

            immutable.put(
                    entry.getKey(),
                    Collections.unmodifiableList(
                            new ArrayList
                            <VmCandidateSnapshot>(
                                    entry.getValue())));
        }

        return Collections.unmodifiableMap(
                immutable);
    }

    private static void validateReadableFile(
            File file,
            String description) {

        if(file == null) {

            throw new IllegalArgumentException(
                    description
                    + " cannot be null.");
        }

        if(!file.exists()) {

            throw new IllegalArgumentException(
                    description
                    + " does not exist: "
                    + file.getAbsolutePath());
        }

        if(!file.isFile()) {

            throw new IllegalArgumentException(
                    description
                    + " is not a file: "
                    + file.getAbsolutePath());
        }

        if(!file.canRead()) {

            throw new IllegalArgumentException(
                    description
                    + " cannot be read: "
                    + file.getAbsolutePath());
        }
    }

    private static Map<String, Integer>
            createHeaderIndex(
                    List<String> headers)
                    throws IOException {

        Map<String, Integer> indexes =
                new LinkedHashMap<String, Integer>();

        for(int index = 0;
                index < headers.size();
                index++) {

            String normalizedHeader =
                    normalizeHeader(
                            headers.get(index));

            if(normalizedHeader.isEmpty()) {
                continue;
            }

            if(indexes.containsKey(
                    normalizedHeader)) {

                throw new IOException(
                        "Duplicate CSV header: "
                        + normalizedHeader);
            }

            indexes.put(
                    normalizedHeader,
                    index);
        }

        return indexes;
    }

    private static int requireColumn(
            Map<String, Integer> headerIndexes,
            String columnName,
            String fileDescription)
            throws IOException {

        Integer index =
                headerIndexes.get(
                        normalizeHeader(
                                columnName));

        if(index == null) {

            throw new IOException(
                    fileDescription
                    + " is missing required column: "
                    + columnName);
        }

        return index.intValue();
    }

    private static String normalizeHeader(
            String header) {

        if(header == null) {
            return "";
        }

        String normalized =
                header.trim()
                .toLowerCase();

        if(normalized.startsWith("\uFEFF")) {

            normalized =
                    normalized.substring(1);
        }

        return normalized;
    }

    private static String getValue(
            List<String> values,
            int columnIndex,
            int lineNumber,
            String columnName)
            throws IOException {

        if(columnIndex < 0
                || columnIndex >= values.size()) {

            throw new IOException(
                    "Missing value for column "
                    + columnName
                    + " at CSV line "
                    + lineNumber
                    + ".");
        }

        String value =
                values.get(
                        columnIndex).trim();

        if(value.isEmpty()) {

            throw new IOException(
                    "Empty value for column "
                    + columnName
                    + " at CSV line "
                    + lineNumber
                    + ".");
        }

        return value;
    }

    private static int parseInteger(
            String value,
            int lineNumber,
            String columnName)
            throws IOException {

        try {

            return Integer.parseInt(
                    value);
        }
        catch(NumberFormatException exception) {

            throw new IOException(
                    "Invalid integer value '"
                    + value
                    + "' for column "
                    + columnName
                    + " at CSV line "
                    + lineNumber
                    + ".",
                    exception);
        }
    }

    private static double parseDouble(
            String value,
            int lineNumber,
            String columnName)
            throws IOException {

        try {

            return Double.parseDouble(
                    value);
        }
        catch(NumberFormatException exception) {

            throw new IOException(
                    "Invalid decimal value '"
                    + value
                    + "' for column "
                    + columnName
                    + " at CSV line "
                    + lineNumber
                    + ".",
                    exception);
        }
    }

    private static boolean parseBoolean(
            String value,
            int lineNumber,
            String columnName)
            throws IOException {

        if("TRUE".equalsIgnoreCase(
                value)) {

            return true;
        }

        if("FALSE".equalsIgnoreCase(
                value)) {

            return false;
        }

        throw new IOException(
                "Invalid boolean value '"
                + value
                + "' for column "
                + columnName
                + " at CSV line "
                + lineNumber
                + ".");
    }

    private static boolean isValidPercentage(
            double value) {

        return !Double.isNaN(
                value)
                && !Double.isInfinite(
                        value)
                && value >= 0.0
                && value <= 100.0;
    }

    private static List<String> parseCsvLine(
            String line)
            throws IOException {

        List<String> values =
                new ArrayList<String>();

        StringBuilder currentValue =
                new StringBuilder();

        boolean insideQuotes =
                false;

        for(int index = 0;
                index < line.length();
                index++) {

            char currentCharacter =
                    line.charAt(index);

            if(currentCharacter == '"') {

                if(insideQuotes
                        && index + 1 < line.length()
                        && line.charAt(index + 1)
                        == '"') {

                    currentValue.append('"');
                    index++;
                }
                else {

                    insideQuotes =
                            !insideQuotes;
                }
            }
            else if(currentCharacter == ','
                    && !insideQuotes) {

                values.add(
                        currentValue.toString());

                currentValue.setLength(0);
            }
            else {

                currentValue.append(
                        currentCharacter);
            }
        }

        if(insideQuotes) {

            throw new IOException(
                    "Unclosed quoted CSV value: "
                    + line);
        }

        values.add(
                currentValue.toString());

        return values;
    }
}
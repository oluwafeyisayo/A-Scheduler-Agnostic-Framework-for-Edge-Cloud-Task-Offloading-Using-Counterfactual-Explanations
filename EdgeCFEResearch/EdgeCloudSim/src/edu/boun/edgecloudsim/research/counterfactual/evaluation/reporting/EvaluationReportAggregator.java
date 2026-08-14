package edu.boun.edgecloudsim.research.counterfactual.evaluation.reporting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.boun.edgecloudsim.research.counterfactual.evaluation.config
        .EvaluationExperimentConfig;
import edu.boun.edgecloudsim.research.counterfactual.evaluation.directory
        .EvaluationDirectoryManager;

/**
 * Generic report aggregator for configuration-driven evaluations.
 *
 * Stage B groups objective metrics by:
 *
 * - device count
 * - scheduler
 * - counterfactual objective
 *
 * The same implementation can later be extended for Stage C and D by
 * adding the independent variable to the grouping key.
 */
public final class EvaluationReportAggregator {

    private static final String[] OBJECTIVE_METRICS = {
            "submitted",
            "processed",
            "successful",
            "unsuccessful",
            "failed_executions",
            "success_rate",
            "adaptive_generated",
            "adaptive_evaluated",
            "adaptive_accepted",
            "neighborhood_generated",
            "neighborhood_evaluated",
            "neighborhood_accepted",
            "overall_generated",
            "overall_evaluated",
            "overall_accepted",
            "average_adaptive_generated",
            "average_adaptive_evaluated",
            "average_adaptive_accepted",
            "average_neighborhood_generated",
            "average_neighborhood_evaluated",
            "average_neighborhood_accepted",
            "average_overall_generated",
            "average_overall_evaluated",
            "average_overall_accepted",
            "adaptive_best_count",
            "neighborhood_best_count",
            "no_best_count",
            "neighborhood_improved_count",
            "neighborhood_improvement_rate",
            "adaptive_best_rate",
            "neighborhood_best_rate",
            "duration_ms",
            "average_duration_per_processed_ms"
    };

    private EvaluationReportAggregator() {
    }

    public static void aggregate(
            EvaluationExperimentConfig config,
            EvaluationDirectoryManager directoryManager)
            throws IOException {

        if(config == null) {

            throw new IllegalArgumentException(
                    "EvaluationExperimentConfig cannot be null.");
        }

        if(directoryManager == null) {

            throw new IllegalArgumentException(
                    "EvaluationDirectoryManager cannot be null.");
        }

        List<CsvTable> objectiveTables =
                new ArrayList<CsvTable>();

        List<CsvTable> eligibilityTables =
                new ArrayList<CsvTable>();

        List<CsvTable> separationTables =
                new ArrayList<CsvTable>();

        for(Integer deviceCountValue
                : config.getDeviceCounts()) {

            int deviceCount =
                    deviceCountValue.intValue();

            for(int iteration =
                    config.getStartIteration();
                    iteration <=
                    config.getEndIteration();
                    iteration++) {

                objectiveTables.add(
                        CsvTable.read(
                                directoryManager
                                .getObjectiveSummaryFile(
                                        deviceCount,
                                        iteration)));

                eligibilityTables.add(
                        CsvTable.read(
                                directoryManager
                                .getEligibilitySummaryFile(
                                        deviceCount,
                                        iteration)));

                separationTables.add(
                        CsvTable.read(
                                directoryManager
                                .getObjectiveSeparationSummaryFile(
                                        deviceCount,
                                        iteration)));
            }
        }

        CsvTable combinedObjectives =
                CsvTable.combine(
                        objectiveTables);

        CsvTable combinedEligibility =
                CsvTable.combine(
                        eligibilityTables);

        CsvTable combinedSeparation =
                CsvTable.combine(
                        separationTables);

        combinedObjectives.write(
                directoryManager
                .getCombinedObjectiveSummaryFile());

        combinedEligibility.write(
                directoryManager
                .getCombinedEligibilitySummaryFile());

        combinedSeparation.write(
                directoryManager
                .getCombinedObjectiveSeparationFile());

        writeObjectiveAggregate(
                combinedObjectives,
                directoryManager
                .getAggregateStatisticsFile());

        writeEligibilityAggregate(
                combinedEligibility,
                new File(
                        directoryManager
                        .getAggregateDirectory(),
                        "eligibility_aggregate.csv"));

        writeSeparationAggregate(
                combinedSeparation,
                new File(
                        directoryManager
                        .getAggregateDirectory(),
                        "objective_separation_aggregate.csv"));
    }

    private static void writeObjectiveAggregate(
            CsvTable table,
            File outputFile)
            throws IOException {

        int deviceIndex =
                table.requireColumn(
                        "device_count");

        int schedulerIndex =
                table.requireColumn(
                        "scheduler");

        int objectiveIndex =
                table.requireColumn(
                        "objective");

        Map<String, List<List<String>>> groups =
                groupRows(
                        table,
                        deviceIndex,
                        schedulerIndex,
                        objectiveIndex);

        try(BufferedWriter writer =
                new BufferedWriter(
                        new FileWriter(
                                outputFile,
                                false))) {

            writer.write(
                    "device_count,scheduler,objective,"
                    + "iteration_count");

            for(String metric
                    : OBJECTIVE_METRICS) {

                writer.write(
                        ","
                        + metric
                        + "_mean");

                writer.write(
                        ","
                        + metric
                        + "_stddev");

                writer.write(
                        ","
                        + metric
                        + "_minimum");

                writer.write(
                        ","
                        + metric
                        + "_maximum");
            }

            writer.newLine();

            for(Map.Entry<String, List<List<String>>> entry
                    : groups.entrySet()) {

                String[] keyParts =
                        entry.getKey()
                        .split(
                                "\\|",
                                -1);

                writer.write(
                        keyParts[0]);

                writer.write(',');
                writer.write(
                        csv(
                                keyParts[1]));

                writer.write(',');
                writer.write(
                        csv(
                                keyParts[2]));

                writer.write(',');
                writer.write(
                        String.valueOf(
                                entry.getValue()
                                .size()));

                for(String metric
                        : OBJECTIVE_METRICS) {

                    Statistics statistics =
                            calculateStatistics(
                                    entry.getValue(),
                                    table.requireColumn(
                                            metric));

                    writer.write(',');
                    writer.write(
                            String.valueOf(
                                    statistics.mean));

                    writer.write(',');
                    writer.write(
                            String.valueOf(
                                    statistics.standardDeviation));

                    writer.write(',');
                    writer.write(
                            String.valueOf(
                                    statistics.minimum));

                    writer.write(',');
                    writer.write(
                            String.valueOf(
                                    statistics.maximum));
                }

                writer.newLine();
            }
        }
    }

    private static void writeEligibilityAggregate(
            CsvTable table,
            File outputFile)
            throws IOException {

        int deviceIndex =
                table.requireColumn(
                        "device_count");

        int schedulerIndex =
                table.requireColumn(
                        "scheduler");

        String[] metrics = {
                "loaded_decisions",
                "eligible_selected_decisions",
                "eligible_restoration_decisions",
                "selected_sample_size",
                "restoration_sample_size",
                "missing_candidate_snapshots",
                "invalid_scheduler_provenance"
        };

        Map<String, List<List<String>>> groups =
                groupRows(
                        table,
                        deviceIndex,
                        schedulerIndex);

        try(BufferedWriter writer =
                new BufferedWriter(
                        new FileWriter(
                                outputFile,
                                false))) {

            writer.write(
                    "device_count,scheduler,iteration_count");

            for(String metric
                    : metrics) {

                writer.write(
                        ","
                        + metric
                        + "_mean");

                writer.write(
                        ","
                        + metric
                        + "_stddev");

                writer.write(
                        ","
                        + metric
                        + "_minimum");

                writer.write(
                        ","
                        + metric
                        + "_maximum");
            }

            writer.newLine();

            for(Map.Entry<String, List<List<String>>> entry
                    : groups.entrySet()) {

                String[] keyParts =
                        entry.getKey()
                        .split(
                                "\\|",
                                -1);

                writer.write(
                        keyParts[0]);

                writer.write(',');
                writer.write(
                        csv(
                                keyParts[1]));

                writer.write(',');
                writer.write(
                        String.valueOf(
                                entry.getValue()
                                .size()));

                for(String metric
                        : metrics) {

                    Statistics statistics =
                            calculateStatistics(
                                    entry.getValue(),
                                    table.requireColumn(
                                            metric));

                    writer.write(',');
                    writer.write(
                            String.valueOf(
                                    statistics.mean));

                    writer.write(',');
                    writer.write(
                            String.valueOf(
                                    statistics.standardDeviation));

                    writer.write(',');
                    writer.write(
                            String.valueOf(
                                    statistics.minimum));

                    writer.write(',');
                    writer.write(
                            String.valueOf(
                                    statistics.maximum));
                }

                writer.newLine();
            }
        }
    }

    private static void writeSeparationAggregate(
            CsvTable table,
            File outputFile)
            throws IOException {

        int deviceIndex =
                table.requireColumn(
                        "device_count");

        int schedulerIndex =
                table.requireColumn(
                        "scheduler");

        int objectiveIndex =
                table.requireColumn(
                        "objective");

        int successfulIndex =
                table.requireColumn(
                        "successful_results");

        int mismatchIndex =
                table.requireColumn(
                        "objective_mismatches");

        Map<String, List<List<String>>> groups =
                groupRows(
                        table,
                        deviceIndex,
                        schedulerIndex,
                        objectiveIndex);

        try(BufferedWriter writer =
                new BufferedWriter(
                        new FileWriter(
                                outputFile,
                                false))) {

            writer.write(
                    "device_count,scheduler,objective,"
                    + "iteration_count,total_successful_results,"
                    + "total_objective_mismatches,"
                    + "objective_mismatch_rate");

            writer.newLine();

            for(Map.Entry<String, List<List<String>>> entry
                    : groups.entrySet()) {

                long totalSuccessful =
                        0L;

                long totalMismatches =
                        0L;

                for(List<String> row
                        : entry.getValue()) {

                    totalSuccessful +=
                            parseLong(
                                    row.get(
                                            successfulIndex));

                    totalMismatches +=
                            parseLong(
                                    row.get(
                                            mismatchIndex));
                }

                double mismatchRate =
                        totalSuccessful == 0L
                        ? 0.0
                        : (double) totalMismatches
                          / totalSuccessful;

                String[] keyParts =
                        entry.getKey()
                        .split(
                                "\\|",
                                -1);

                writer.write(
                        keyParts[0]);

                writer.write(',');
                writer.write(
                        csv(
                                keyParts[1]));

                writer.write(',');
                writer.write(
                        csv(
                                keyParts[2]));

                writer.write(',');
                writer.write(
                        String.valueOf(
                                entry.getValue()
                                .size()));

                writer.write(',');
                writer.write(
                        String.valueOf(
                                totalSuccessful));

                writer.write(',');
                writer.write(
                        String.valueOf(
                                totalMismatches));

                writer.write(',');
                writer.write(
                        String.valueOf(
                                mismatchRate));

                writer.newLine();
            }
        }
    }

    private static Map<String, List<List<String>>> groupRows(
            CsvTable table,
            int... keyIndexes) {

        Map<String, List<List<String>>> groups =
                new LinkedHashMap<String, List<List<String>>>();

        for(List<String> row
                : table.rows) {

            StringBuilder key =
                    new StringBuilder();

            for(int index = 0;
                    index < keyIndexes.length;
                    index++) {

                if(index > 0) {

                    key.append(
                            '|');
                }

                key.append(
                        row.get(
                                keyIndexes[index]));
            }

            String keyText =
                    key.toString();

            List<List<String>> group =
                    groups.get(
                            keyText);

            if(group == null) {

                group =
                        new ArrayList<List<String>>();

                groups.put(
                        keyText,
                        group);
            }

            group.add(
                    row);
        }

        return groups;
    }

    private static Statistics calculateStatistics(
            List<List<String>> rows,
            int columnIndex) {

        List<Double> values =
                new ArrayList<Double>();

        for(List<String> row
                : rows) {

            values.add(
                    Double.valueOf(
                            parseDouble(
                                    row.get(
                                            columnIndex))));
        }

        if(values.isEmpty()) {

            return new Statistics(
                    0.0,
                    0.0,
                    0.0,
                    0.0);
        }

        double sum =
                0.0;

        double minimum =
                Double.POSITIVE_INFINITY;

        double maximum =
                Double.NEGATIVE_INFINITY;

        for(Double value
                : values) {

            double numericValue =
                    value.doubleValue();

            sum +=
                    numericValue;

            minimum =
                    Math.min(
                            minimum,
                            numericValue);

            maximum =
                    Math.max(
                            maximum,
                            numericValue);
        }

        double mean =
                sum
                / values.size();

        double squaredDifferenceSum =
                0.0;

        for(Double value
                : values) {

            double difference =
                    value.doubleValue()
                    - mean;

            squaredDifferenceSum +=
                    difference
                    * difference;
        }

        double standardDeviation =
                values.size() <= 1
                ? 0.0
                : Math.sqrt(
                        squaredDifferenceSum
                        / (
                                values.size()
                                - 1
                        ));

        return new Statistics(
                mean,
                standardDeviation,
                minimum,
                maximum);
    }

    private static long parseLong(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return 0L;
        }

        return Long.parseLong(
                value.trim());
    }

    private static double parseDouble(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return 0.0;
        }

        return Double.parseDouble(
                value.trim());
    }

    private static String csv(
            String value) {

        String safeValue =
                value == null
                ? ""
                : value;

        return "\""
                + safeValue.replace(
                        "\"",
                        "\"\"")
                + "\"";
    }

    private static final class Statistics {

        private final double mean;
        private final double standardDeviation;
        private final double minimum;
        private final double maximum;

        private Statistics(
                double mean,
                double standardDeviation,
                double minimum,
                double maximum) {

            this.mean =
                    mean;

            this.standardDeviation =
                    standardDeviation;

            this.minimum =
                    minimum;

            this.maximum =
                    maximum;
        }
    }

    private static final class CsvTable {

        private final List<String> header;
        private final List<List<String>> rows;

        private CsvTable(
                List<String> header,
                List<List<String>> rows) {

            this.header =
                    header;

            this.rows =
                    rows;
        }

        private static CsvTable read(
                File file)
                throws IOException {

            if(file == null
                    || !file.exists()
                    || !file.isFile()) {

                throw new IOException(
                        "CSV report does not exist: "
                        + (
                                file == null
                                ? "null"
                                : file.getAbsolutePath()
                        ));
            }

            try(BufferedReader reader =
                    new BufferedReader(
                            new FileReader(
                                    file))) {

                String headerLine =
                        reader.readLine();

                if(headerLine == null) {

                    throw new IOException(
                            "CSV report is empty: "
                            + file.getAbsolutePath());
                }

                List<String> header =
                        parseCsvLine(
                                headerLine);

                List<List<String>> rows =
                        new ArrayList<List<String>>();

                String line;

                while((line =
                        reader.readLine())
                        != null) {

                    if(line.trim().isEmpty()) {

                        continue;
                    }

                    List<String> row =
                            parseCsvLine(
                                    line);

                    if(row.size()
                            != header.size()) {

                        throw new IOException(
                                "CSV row has "
                                + row.size()
                                + " columns, but header has "
                                + header.size()
                                + ": "
                                + file.getAbsolutePath());
                    }

                    rows.add(
                            row);
                }

                return new CsvTable(
                        header,
                        rows);
            }
        }

        private static CsvTable combine(
                List<CsvTable> tables) {

            if(tables == null
                    || tables.isEmpty()) {

                throw new IllegalArgumentException(
                        "At least one CSV table is required.");
            }

            List<String> expectedHeader =
                    tables.get(0)
                    .header;

            List<List<String>> combinedRows =
                    new ArrayList<List<String>>();

            for(CsvTable table
                    : tables) {

                if(!expectedHeader.equals(
                        table.header)) {

                    throw new IllegalArgumentException(
                            "Cannot combine reports with "
                            + "different CSV headers.");
                }

                combinedRows.addAll(
                        table.rows);
            }

            return new CsvTable(
                    new ArrayList<String>(
                            expectedHeader),
                    combinedRows);
        }

        private int requireColumn(
                String columnName) {

            int index =
                    header.indexOf(
                            columnName);

            if(index < 0) {

                throw new IllegalArgumentException(
                        "Required CSV column is missing: "
                        + columnName);
            }

            return index;
        }

        private void write(
                File file)
                throws IOException {

            try(BufferedWriter writer =
                    new BufferedWriter(
                            new FileWriter(
                                    file,
                                    false))) {

                writeRow(
                        writer,
                        header);

                for(List<String> row
                        : rows) {

                    writeRow(
                            writer,
                            row);
                }
            }
        }

        private static void writeRow(
                BufferedWriter writer,
                List<String> values)
                throws IOException {

            for(int index = 0;
                    index < values.size();
                    index++) {

                if(index > 0) {

                    writer.write(',');
                }

                writer.write(
                        csv(
                                values.get(
                                        index)));
            }

            writer.newLine();
        }

        private static List<String> parseCsvLine(
                String line) {

            List<String> values =
                    new ArrayList<String>();

            StringBuilder current =
                    new StringBuilder();

            boolean quoted =
                    false;

            for(int index = 0;
                    index < line.length();
                    index++) {

                char character =
                        line.charAt(
                                index);

                if(character == '"') {

                    if(quoted
                            && index + 1
                            < line.length()
                            && line.charAt(
                                    index + 1)
                            == '"') {

                        current.append(
                                '"');

                        index++;
                    }
                    else {

                        quoted =
                                !quoted;
                    }
                }
                else if(character == ','
                        && !quoted) {

                    values.add(
                            current.toString());

                    current.setLength(
                            0);
                }
                else {

                    current.append(
                            character);
                }
            }

            values.add(
                    current.toString());

            return values;
        }
    }
}
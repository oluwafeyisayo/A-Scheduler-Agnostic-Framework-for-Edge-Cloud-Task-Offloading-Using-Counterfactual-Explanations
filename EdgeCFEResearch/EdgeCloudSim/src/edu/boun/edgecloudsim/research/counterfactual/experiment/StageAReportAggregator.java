package edu.boun.edgecloudsim.research.counterfactual.experiment;

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

/** Combines Stage A iteration reports and computes descriptive statistics. */
public final class StageAReportAggregator {

    private static final String[] METRICS = {
            "submitted",
            "processed",
            "successful",
            "unsuccessful",
            "failed_executions",
            "success_rate",
            "average_adaptive_generated",
            "average_adaptive_evaluated",
            "average_adaptive_accepted",
            "average_neighborhood_generated",
            "average_neighborhood_evaluated",
            "average_neighborhood_accepted",
            "average_overall_generated",
            "average_overall_evaluated",
            "average_overall_accepted",
            "neighborhood_improvement_rate",
            "adaptive_best_rate",
            "neighborhood_best_rate",
            "duration_ms",
            "average_duration_per_processed_ms"
    };

    private StageAReportAggregator() {
    }

    public static void aggregate(
            int startIteration,
            int endIteration,
            File outputDirectory)
            throws IOException {

        ensureDirectory(outputDirectory);

        List<CsvTable> objectiveTables = new ArrayList<CsvTable>();
        List<CsvTable> separationTables = new ArrayList<CsvTable>();
        List<CsvTable> eligibilityTables = new ArrayList<CsvTable>();

        for(int iteration = startIteration;
                iteration <= endIteration;
                iteration++) {

            File reportDirectory = new File(
                    "sim_results/ite"
                    + iteration
                    + "/counterfactual_results/reports");

            objectiveTables.add(CsvTable.read(new File(
                    reportDirectory,
                    "objective_summary.csv")));

            separationTables.add(CsvTable.read(new File(
                    reportDirectory,
                    "objective_separation_summary.csv")));

            eligibilityTables.add(CsvTable.read(new File(
                    reportDirectory,
                    "eligibility_summary.csv")));
        }

        CsvTable objectiveCombined = CsvTable.combine(objectiveTables);
        objectiveCombined.write(new File(
                outputDirectory,
                "stage_a_all_iterations.csv"));

        CsvTable.combine(eligibilityTables).write(new File(
                outputDirectory,
                "stage_a_eligibility_all_iterations.csv"));

        CsvTable separationCombined = CsvTable.combine(separationTables);
        separationCombined.write(new File(
                outputDirectory,
                "stage_a_objective_separation_all_iterations.csv"));

        writeObjectiveAggregate(
                objectiveCombined,
                new File(
                        outputDirectory,
                        "stage_a_aggregate_summary.csv"));

        writeSeparationAggregate(
                separationCombined,
                new File(
                        outputDirectory,
                        "stage_a_separation_aggregate.csv"));
    }

    private static void writeObjectiveAggregate(
            CsvTable table,
            File outputFile)
            throws IOException {

        int schedulerIndex = table.requireColumn("scheduler");
        int objectiveIndex = table.requireColumn("objective");

        Map<String, List<List<String>>> groups = groupRows(
                table,
                schedulerIndex,
                objectiveIndex);

        try(BufferedWriter writer = new BufferedWriter(
                new FileWriter(outputFile, false))) {

            writer.write("scheduler,objective,iteration_count");

            for(String metric : METRICS) {
                writer.write("," + metric + "_mean");
                writer.write("," + metric + "_stddev");
                writer.write("," + metric + "_min");
                writer.write("," + metric + "_max");
            }

            writer.newLine();

            for(Map.Entry<String, List<List<String>>> entry
                    : groups.entrySet()) {

                String[] key = entry.getKey().split("\\|", -1);

                writer.write(csv(key[0]));
                writer.write(',');
                writer.write(csv(key[1]));
                writer.write(',');
                writer.write(String.valueOf(entry.getValue().size()));

                for(String metric : METRICS) {
                    int metricIndex = table.requireColumn(metric);
                    Statistics statistics = calculateStatistics(
                            entry.getValue(),
                            metricIndex);

                    writer.write(',' + String.valueOf(statistics.mean));
                    writer.write(',' + String.valueOf(
                            statistics.standardDeviation));
                    writer.write(',' + String.valueOf(statistics.minimum));
                    writer.write(',' + String.valueOf(statistics.maximum));
                }

                writer.newLine();
            }
        }
    }

    private static void writeSeparationAggregate(
            CsvTable table,
            File outputFile)
            throws IOException {

        int schedulerIndex = table.requireColumn("scheduler");
        int objectiveIndex = table.requireColumn("objective");
        int successfulIndex = table.requireColumn("successful_results");
        int mismatchIndex = table.requireColumn("objective_mismatches");

        Map<String, List<List<String>>> groups = groupRows(
                table,
                schedulerIndex,
                objectiveIndex);

        try(BufferedWriter writer = new BufferedWriter(
                new FileWriter(outputFile, false))) {

            writer.write(
                    "scheduler,objective,iteration_count,"
                    + "total_successful_results,"
                    + "total_objective_mismatches,"
                    + "objective_mismatch_rate");
            writer.newLine();

            for(Map.Entry<String, List<List<String>>> entry
                    : groups.entrySet()) {

                long totalSuccessful = 0L;
                long totalMismatches = 0L;

                for(List<String> row : entry.getValue()) {
                    totalSuccessful += parseLong(row.get(successfulIndex));
                    totalMismatches += parseLong(row.get(mismatchIndex));
                }

                double mismatchRate = totalSuccessful == 0L
                        ? 0.0
                        : (double) totalMismatches / totalSuccessful;

                String[] key = entry.getKey().split("\\|", -1);

                writer.write(csv(key[0]));
                writer.write(',');
                writer.write(csv(key[1]));
                writer.write(',');
                writer.write(String.valueOf(entry.getValue().size()));
                writer.write(',');
                writer.write(String.valueOf(totalSuccessful));
                writer.write(',');
                writer.write(String.valueOf(totalMismatches));
                writer.write(',');
                writer.write(String.valueOf(mismatchRate));
                writer.newLine();
            }
        }
    }

    private static Map<String, List<List<String>>> groupRows(
            CsvTable table,
            int schedulerIndex,
            int objectiveIndex) {

        Map<String, List<List<String>>> groups =
                new LinkedHashMap<String, List<List<String>>>();

        for(List<String> row : table.rows) {
            String key = row.get(schedulerIndex)
                    + "|"
                    + row.get(objectiveIndex);

            List<List<String>> group = groups.get(key);

            if(group == null) {
                group = new ArrayList<List<String>>();
                groups.put(key, group);
            }

            group.add(row);
        }

        return groups;
    }

    private static Statistics calculateStatistics(
            List<List<String>> rows,
            int columnIndex) {

        List<Double> values = new ArrayList<Double>();

        for(List<String> row : rows) {
            values.add(Double.valueOf(
                    parseDouble(row.get(columnIndex))));
        }

        double sum = 0.0;
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;

        for(Double value : values) {
            double numericValue = value.doubleValue();
            sum += numericValue;
            minimum = Math.min(minimum, numericValue);
            maximum = Math.max(maximum, numericValue);
        }

        double mean = values.isEmpty() ? 0.0 : sum / values.size();
        double squaredDifferenceSum = 0.0;

        for(Double value : values) {
            double difference = value.doubleValue() - mean;
            squaredDifferenceSum += difference * difference;
        }

        double standardDeviation = values.size() <= 1
                ? 0.0
                : Math.sqrt(
                        squaredDifferenceSum / (values.size() - 1));

        return new Statistics(
                mean,
                standardDeviation,
                values.isEmpty() ? 0.0 : minimum,
                values.isEmpty() ? 0.0 : maximum);
    }

    private static double parseDouble(String value) {
        if(value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(value.trim());
    }

    private static long parseLong(String value) {
        if(value == null || value.trim().isEmpty()) {
            return 0L;
        }
        return Long.parseLong(value.trim());
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static void ensureDirectory(File directory)
            throws IOException {

        if(directory.exists()) {
            if(!directory.isDirectory()) {
                throw new IOException(
                        "Output path is not a directory: "
                        + directory.getAbsolutePath());
            }
            return;
        }

        if(!directory.mkdirs()) {
            throw new IOException(
                    "Unable to create output directory: "
                    + directory.getAbsolutePath());
        }
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
            this.mean = mean;
            this.standardDeviation = standardDeviation;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }

    private static final class CsvTable {
        private final List<String> header;
        private final List<List<String>> rows;

        private CsvTable(
                List<String> header,
                List<List<String>> rows) {
            this.header = header;
            this.rows = rows;
        }

        private static CsvTable read(File file)
                throws IOException {

            if(file == null || !file.exists() || !file.isFile()) {
                throw new IOException(
                        "CSV report does not exist: "
                        + (file == null
                            ? "null"
                            : file.getAbsolutePath()));
            }

            try(BufferedReader reader = new BufferedReader(
                    new FileReader(file))) {

                String headerLine = reader.readLine();

                if(headerLine == null) {
                    throw new IOException(
                            "CSV report is empty: "
                            + file.getAbsolutePath());
                }

                List<String> header = parseCsvLine(headerLine);
                List<List<String>> rows =
                        new ArrayList<List<String>>();

                String line;
                while((line = reader.readLine()) != null) {
                    if(line.trim().isEmpty()) {
                        continue;
                    }

                    List<String> row = parseCsvLine(line);

                    if(row.size() != header.size()) {
                        throw new IOException(
                                "CSV row has "
                                + row.size()
                                + " columns but header has "
                                + header.size()
                                + ": "
                                + file.getAbsolutePath());
                    }

                    rows.add(row);
                }

                return new CsvTable(header, rows);
            }
        }

        private static CsvTable combine(List<CsvTable> tables) {
            if(tables == null || tables.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one CSV table is required.");
            }

            List<String> expectedHeader = tables.get(0).header;
            List<List<String>> combinedRows =
                    new ArrayList<List<String>>();

            for(CsvTable table : tables) {
                if(!expectedHeader.equals(table.header)) {
                    throw new IllegalArgumentException(
                            "Cannot combine reports with different headers.");
                }
                combinedRows.addAll(table.rows);
            }

            return new CsvTable(
                    new ArrayList<String>(expectedHeader),
                    combinedRows);
        }

        private int requireColumn(String name) {
            int index = header.indexOf(name);
            if(index < 0) {
                throw new IllegalArgumentException(
                        "Required CSV column is missing: " + name);
            }
            return index;
        }

        private void write(File file)
                throws IOException {

            try(BufferedWriter writer = new BufferedWriter(
                    new FileWriter(file, false))) {

                writeRow(writer, header);

                for(List<String> row : rows) {
                    writeRow(writer, row);
                }
            }
        }

        private static void writeRow(
                BufferedWriter writer,
                List<String> values)
                throws IOException {

            for(int index = 0; index < values.size(); index++) {
                if(index > 0) {
                    writer.write(',');
                }
                writer.write(csv(values.get(index)));
            }
            writer.newLine();
        }

        private static List<String> parseCsvLine(String line) {
            List<String> values = new ArrayList<String>();
            StringBuilder current = new StringBuilder();
            boolean quoted = false;

            for(int index = 0; index < line.length(); index++) {
                char character = line.charAt(index);

                if(character == '"') {
                    if(quoted
                            && index + 1 < line.length()
                            && line.charAt(index + 1) == '"') {
                        current.append('"');
                        index++;
                    }
                    else {
                        quoted = !quoted;
                    }
                }
                else if(character == ',' && !quoted) {
                    values.add(current.toString());
                    current.setLength(0);
                }
                else {
                    current.append(character);
                }
            }

            values.add(current.toString());
            return values;
        }
    }
}
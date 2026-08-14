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

/**
 * Aggregates Stage D adaptive and exhaustive reports and writes direct
 * scheduler-objective efficiency comparisons.
 */
public final class StageDReportAggregator {

    private StageDReportAggregator() {
    }

    public static void aggregate(
            int startIteration,
            int endIteration,
            File outputDirectory)
            throws IOException {

        ensureDirectory(outputDirectory);

        CsvTable adaptive =
                readVariant(
                        "ADAPTIVE",
                        "adaptive",
                        startIteration,
                        endIteration);

        CsvTable exhaustive =
                readVariant(
                        "EXHAUSTIVE",
                        "exhaustive",
                        startIteration,
                        endIteration);

        CsvTable combined =
                CsvTable.combine(
                        listOf(
                                adaptive,
                                exhaustive));

        combined.write(
                new File(
                        outputDirectory,
                        "stage_d_all_iterations.csv"));

        writeVariantAggregate(
                combined,
                new File(
                        outputDirectory,
                        "stage_d_variant_aggregate.csv"));

        writeComparison(
                combined,
                new File(
                        outputDirectory,
                        "stage_d_replay_efficiency_comparison.csv"));
    }

    private static CsvTable readVariant(
            String variantLabel,
            String folderName,
            int startIteration,
            int endIteration)
            throws IOException {

        List<CsvTable> tables =
                new ArrayList<CsvTable>();

        for(int iteration = startIteration;
                iteration <= endIteration;
                iteration++) {

            File reportFile =
                    new File(
                            "sim_results/stage_d/"
                            + folderName
                            + "/ite"
                            + iteration
                            + "/counterfactual_results/reports/"
                            + "objective_summary.csv");

            CsvTable source =
                    CsvTable.read(reportFile);

            tables.add(
                    source.withLeadingColumns(
                            "search_variant",
                            variantLabel,
                            "iteration",
                            String.valueOf(iteration)));
        }

        return CsvTable.combine(tables);
    }

    private static void writeVariantAggregate(
            CsvTable table,
            File outputFile)
            throws IOException {

        int variantIndex =
                table.requireColumn(
                        "search_variant");

        int schedulerIndex =
                table.requireColumn(
                        "scheduler");

        int objectiveIndex =
                table.requireColumn(
                        "objective");

        Map<String, List<List<String>>> groups =
                new LinkedHashMap<String,
                        List<List<String>>>();

        for(List<String> row : table.rows) {

            String key =
                    row.get(variantIndex)
                    + "|"
                    + row.get(schedulerIndex)
                    + "|"
                    + row.get(objectiveIndex);

            List<List<String>> group =
                    groups.get(key);

            if(group == null) {

                group =
                        new ArrayList<List<String>>();

                groups.put(
                        key,
                        group);
            }

            group.add(row);
        }

        int submittedIndex =
                table.requireColumn("submitted");

        int processedIndex =
                table.requireColumn("processed");

        int successfulIndex =
                table.requireColumn("successful");

        int overallEvaluatedIndex =
                table.requireColumn(
                        "average_overall_evaluated");

        int durationIndex =
                table.requireColumn("duration_ms");

        int durationPerProcessedIndex =
                table.requireColumn(
                        "average_duration_per_processed_ms");

        try(BufferedWriter writer =
                new BufferedWriter(
                        new FileWriter(
                                outputFile,
                                false))) {

            writer.write(
                    "search_variant,scheduler,objective,"
                    + "iteration_count,total_submitted,"
                    + "total_processed,total_successful,"
                    + "pooled_success_rate,"
                    + "mean_average_overall_evaluated,"
                    + "mean_duration_ms,"
                    + "mean_duration_per_processed_ms");

            writer.newLine();

            for(Map.Entry<String,
                    List<List<String>>> entry
                    : groups.entrySet()) {

                String[] key =
                        entry.getKey()
                        .split("\\|", -1);

                long totalSubmitted = 0L;
                long totalProcessed = 0L;
                long totalSuccessful = 0L;

                double evaluatedSum = 0.0;
                double durationSum = 0.0;
                double durationPerProcessedSum = 0.0;

                for(List<String> row
                        : entry.getValue()) {

                    totalSubmitted +=
                            parseLong(
                                    row.get(
                                            submittedIndex));

                    totalProcessed +=
                            parseLong(
                                    row.get(
                                            processedIndex));

                    totalSuccessful +=
                            parseLong(
                                    row.get(
                                            successfulIndex));

                    evaluatedSum +=
                            parseDouble(
                                    row.get(
                                            overallEvaluatedIndex));

                    durationSum +=
                            parseDouble(
                                    row.get(
                                            durationIndex));

                    durationPerProcessedSum +=
                            parseDouble(
                                    row.get(
                                            durationPerProcessedIndex));
                }

                int count =
                        entry.getValue().size();

                double pooledSuccessRate =
                        totalProcessed == 0L
                        ? 0.0
                        : (double) totalSuccessful
                                / totalProcessed;

                writer.write(csv(key[0]));
                writer.write(',');
                writer.write(csv(key[1]));
                writer.write(',');
                writer.write(csv(key[2]));
                writer.write(',');
                writer.write(String.valueOf(count));
                writer.write(',');
                writer.write(String.valueOf(totalSubmitted));
                writer.write(',');
                writer.write(String.valueOf(totalProcessed));
                writer.write(',');
                writer.write(String.valueOf(totalSuccessful));
                writer.write(',');
                writer.write(String.valueOf(pooledSuccessRate));
                writer.write(',');
                writer.write(String.valueOf(
                        count == 0
                        ? 0.0
                        : evaluatedSum / count));
                writer.write(',');
                writer.write(String.valueOf(
                        count == 0
                        ? 0.0
                        : durationSum / count));
                writer.write(',');
                writer.write(String.valueOf(
                        count == 0
                        ? 0.0
                        : durationPerProcessedSum / count));
                writer.newLine();
            }
        }
    }

    private static void writeComparison(
            CsvTable table,
            File outputFile)
            throws IOException {

        int variantIndex =
                table.requireColumn(
                        "search_variant");

        int schedulerIndex =
                table.requireColumn(
                        "scheduler");

        int objectiveIndex =
                table.requireColumn(
                        "objective");

        int processedIndex =
                table.requireColumn(
                        "processed");

        int successfulIndex =
                table.requireColumn(
                        "successful");

        int evaluatedIndex =
                table.requireColumn(
                        "average_overall_evaluated");

        int durationPerProcessedIndex =
                table.requireColumn(
                        "average_duration_per_processed_ms");

        Map<String, VariantTotals> totals =
                new LinkedHashMap<String,
                        VariantTotals>();

        for(List<String> row : table.rows) {

            String key =
                    row.get(schedulerIndex)
                    + "|"
                    + row.get(objectiveIndex);

            VariantTotals value =
                    totals.get(key);

            if(value == null) {

                value =
                        new VariantTotals();

                totals.put(
                        key,
                        value);
            }

            String variant =
                    row.get(variantIndex);

            MetricAccumulator accumulator =
                    "ADAPTIVE".equalsIgnoreCase(
                            variant)
                    ? value.adaptive
                    : value.exhaustive;

            accumulator.iterationCount++;
            accumulator.totalProcessed +=
                    parseLong(
                            row.get(
                                    processedIndex));

            accumulator.totalSuccessful +=
                    parseLong(
                            row.get(
                                    successfulIndex));

            accumulator.evaluatedSum +=
                    parseDouble(
                            row.get(
                                    evaluatedIndex));

            accumulator.runtimePerProcessedSum +=
                    parseDouble(
                            row.get(
                                    durationPerProcessedIndex));
        }

        try(BufferedWriter writer =
                new BufferedWriter(
                        new FileWriter(
                                outputFile,
                                false))) {

            writer.write(
                    "scheduler,objective,"
                    + "adaptive_success_rate,"
                    + "exhaustive_success_rate,"
                    + "success_rate_difference,"
                    + "adaptive_mean_replays,"
                    + "exhaustive_mean_replays,"
                    + "replay_reduction_rate,"
                    + "adaptive_mean_runtime_per_processed_ms,"
                    + "exhaustive_mean_runtime_per_processed_ms,"
                    + "runtime_reduction_rate");

            writer.newLine();

            for(Map.Entry<String, VariantTotals> entry
                    : totals.entrySet()) {

                String[] key =
                        entry.getKey()
                        .split("\\|", -1);

                MetricAccumulator adaptive =
                        entry.getValue().adaptive;

                MetricAccumulator exhaustive =
                        entry.getValue().exhaustive;

                double adaptiveSuccess =
                        adaptive.totalProcessed == 0L
                        ? 0.0
                        : (double) adaptive.totalSuccessful
                                / adaptive.totalProcessed;

                double exhaustiveSuccess =
                        exhaustive.totalProcessed == 0L
                        ? 0.0
                        : (double) exhaustive.totalSuccessful
                                / exhaustive.totalProcessed;

                double adaptiveReplays =
                        adaptive.meanEvaluated();

                double exhaustiveReplays =
                        exhaustive.meanEvaluated();

                double replayReduction =
                        exhaustiveReplays == 0.0
                        ? 0.0
                        : (exhaustiveReplays
                                - adaptiveReplays)
                                / exhaustiveReplays;

                double adaptiveRuntime =
                        adaptive.meanRuntimePerProcessed();

                double exhaustiveRuntime =
                        exhaustive.meanRuntimePerProcessed();

                double runtimeReduction =
                        exhaustiveRuntime == 0.0
                        ? 0.0
                        : (exhaustiveRuntime
                                - adaptiveRuntime)
                                / exhaustiveRuntime;

                writer.write(csv(key[0]));
                writer.write(',');
                writer.write(csv(key[1]));
                writer.write(',');
                writer.write(String.valueOf(adaptiveSuccess));
                writer.write(',');
                writer.write(String.valueOf(exhaustiveSuccess));
                writer.write(',');
                writer.write(String.valueOf(
                        adaptiveSuccess
                        - exhaustiveSuccess));
                writer.write(',');
                writer.write(String.valueOf(adaptiveReplays));
                writer.write(',');
                writer.write(String.valueOf(exhaustiveReplays));
                writer.write(',');
                writer.write(String.valueOf(replayReduction));
                writer.write(',');
                writer.write(String.valueOf(adaptiveRuntime));
                writer.write(',');
                writer.write(String.valueOf(exhaustiveRuntime));
                writer.write(',');
                writer.write(String.valueOf(runtimeReduction));
                writer.newLine();
            }
        }
    }

    private static List<CsvTable> listOf(
            CsvTable first,
            CsvTable second) {

        List<CsvTable> result =
                new ArrayList<CsvTable>();

        result.add(first);
        result.add(second);

        return result;
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

    private static long parseLong(
            String value) {

        if(value == null
                || value.trim().isEmpty()) {

            return 0L;
        }

        return Long.parseLong(
                value.trim());
    }

    private static String csv(
            String value) {

        String safe =
                value == null
                ? ""
                : value;

        return "\""
                + safe.replace(
                        "\"",
                        "\"\"")
                + "\"";
    }

    private static void ensureDirectory(
            File directory)
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

    private static final class VariantTotals {

        private final MetricAccumulator adaptive =
                new MetricAccumulator();

        private final MetricAccumulator exhaustive =
                new MetricAccumulator();
    }

    private static final class MetricAccumulator {

        private int iterationCount;
        private long totalProcessed;
        private long totalSuccessful;
        private double evaluatedSum;
        private double runtimePerProcessedSum;

        private double meanEvaluated() {

            return iterationCount == 0
                    ? 0.0
                    : evaluatedSum / iterationCount;
        }

        private double meanRuntimePerProcessed() {

            return iterationCount == 0
                    ? 0.0
                    : runtimePerProcessedSum / iterationCount;
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

        private static CsvTable read(
                File file)
                throws IOException {

            if(file == null
                    || !file.exists()
                    || !file.isFile()) {

                throw new IOException(
                        "CSV report does not exist: "
                        + (file == null
                                ? "null"
                                : file.getAbsolutePath()));
            }

            try(BufferedReader reader =
                    new BufferedReader(
                            new FileReader(file))) {

                String headerLine =
                        reader.readLine();

                if(headerLine == null) {

                    throw new IOException(
                            "CSV report is empty: "
                            + file.getAbsolutePath());
                }

                List<String> header =
                        parseCsvLine(headerLine);

                List<List<String>> rows =
                        new ArrayList<List<String>>();

                String line;

                while((line = reader.readLine())
                        != null) {

                    if(line.trim().isEmpty()) {
                        continue;
                    }

                    List<String> row =
                            parseCsvLine(line);

                    if(row.size()
                            != header.size()) {

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

                return new CsvTable(
                        header,
                        rows);
            }
        }

        private CsvTable withLeadingColumns(
                String firstName,
                String firstValue,
                String secondName,
                String secondValue) {

            List<String> newHeader =
                    new ArrayList<String>();

            newHeader.add(firstName);
            newHeader.add(secondName);
            newHeader.addAll(header);

            List<List<String>> newRows =
                    new ArrayList<List<String>>();

            for(List<String> row : rows) {

                List<String> newRow =
                        new ArrayList<String>();

                newRow.add(firstValue);
                newRow.add(secondValue);
                newRow.addAll(row);

                newRows.add(newRow);
            }

            return new CsvTable(
                    newHeader,
                    newRows);
        }

        private static CsvTable combine(
                List<CsvTable> tables) {

            if(tables == null
                    || tables.isEmpty()) {

                throw new IllegalArgumentException(
                        "At least one CSV table is required.");
            }

            List<String> expectedHeader =
                    tables.get(0).header;

            List<List<String>> combinedRows =
                    new ArrayList<List<String>>();

            for(CsvTable table : tables) {

                if(!expectedHeader.equals(
                        table.header)) {

                    throw new IllegalArgumentException(
                            "Cannot combine reports with different headers.");
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
                String name) {

            int index =
                    header.indexOf(name);

            if(index < 0) {

                throw new IllegalArgumentException(
                        "Required CSV column is missing: "
                        + name);
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

                for(List<String> row : rows) {

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
                                values.get(index)));
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
                        line.charAt(index);

                if(character == '"') {

                    if(quoted
                            && index + 1 < line.length()
                            && line.charAt(index + 1) == '"') {

                        current.append('"');
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

                    current.setLength(0);
                }
                else {

                    current.append(character);
                }
            }

            values.add(
                    current.toString());

            return values;
        }
    }
}
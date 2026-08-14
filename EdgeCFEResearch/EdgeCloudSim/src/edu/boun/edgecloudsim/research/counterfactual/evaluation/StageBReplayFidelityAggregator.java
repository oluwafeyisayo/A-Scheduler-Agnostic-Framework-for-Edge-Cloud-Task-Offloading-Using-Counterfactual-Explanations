package edu.boun.edgecloudsim.research.counterfactual.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import edu.boun.edgecloudsim.research.counterfactual.DecisionReplayEngine;
import edu.boun.edgecloudsim.research.counterfactual.DecisionState;
import edu.boun.edgecloudsim.research.counterfactual.ReplayState;
import edu.boun.edgecloudsim.research.counterfactual.SchedulerReplayContextFactory;
import edu.boun.edgecloudsim.research.counterfactual.replay.SchedulerDecision;
import edu.boun.edgecloudsim.research.counterfactual.replay.VmCandidateSnapshot;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.SchedulerReplayContext;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.factory.LeastLoadedReplayContextFactory;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.factory.MultiObjectiveReplayContextFactory;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.factory.PpoReplayContextFactory;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.factory.RandomReplayContextFactory;
import edu.boun.edgecloudsim.research.counterfactual.replay.context.factory.RoundRobinReplayContextFactory;
import edu.boun.edgecloudsim.research.counterfactual.replay.engine.SchedulerReplayEngine;
import edu.boun.edgecloudsim.research.counterfactual.replay.io.CandidateSnapshotCsvLoader;
import edu.boun.edgecloudsim.research.counterfactual.replay.strategy.LeastLoadedReplayStrategy;
import edu.boun.edgecloudsim.research.counterfactual.replay.strategy.MultiObjectiveReplayStrategy;
import edu.boun.edgecloudsim.research.counterfactual.replay.strategy.PpoReplayStrategy;
import edu.boun.edgecloudsim.research.counterfactual.replay.strategy.RandomReplayStrategy;
import edu.boun.edgecloudsim.research.counterfactual.replay.strategy.RoundRobinReplayStrategy;

/**
 * Discovers every Stage B decision/candidate provenance pair, replays each
 * eligible original decision once, and writes one combined fidelity CSV.
 */
public final class StageBReplayFidelityAggregator {

    private static final Pattern FILE_PATTERN = Pattern.compile(
            "^offloading_decisions_(.+)_"
            + "(RANDOM|ROUND_ROBIN|LEAST_LOADED|MULTI_OBJECTIVE|PPO)_"
            + "(\\d+)DEVICES_ITE(\\d+)\\.csv$",
            Pattern.CASE_INSENSITIVE);

    private StageBReplayFidelityAggregator() { }

    public static void main(String[] args) {
        Path root = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("sim_results");

        Path output = args.length > 1
                ? Paths.get(args[1])
                : root.resolve("stage_b_replay_fidelity_all_devices.csv");

        try {
            List<InputPair> pairs = discoverPairs(root);
            if(pairs.isEmpty()) {
                System.err.println("No Stage B provenance pairs found under "
                        + root.toAbsolutePath());
                return;
            }

            DecisionReplayEngine decisionLoader = new DecisionReplayEngine();
            SchedulerReplayEngine replayEngine = createReplayEngine();

            Map<String, Counts> runRows = new TreeMap<String, Counts>();
            Map<String, Counts> deviceSchedulerRows =
                    new TreeMap<String, Counts>();
            Map<String, Counts> deviceRows = new TreeMap<String, Counts>();
            Counts overall = new Counts();

            for(InputPair pair : pairs) {
                Counts counts = calculate(pair, decisionLoader, replayEngine);

                runRows.put(pair.runKey(), counts);
                add(deviceSchedulerRows, pair.deviceSchedulerKey(), counts);
                add(deviceRows, pair.deviceKey(), counts);
                overall.add(counts);

                System.out.println(pair.file.getFileName()
                        + " -> eligible=" + counts.eligible
                        + ", faithful=" + counts.faithful
                        + ", fidelity=" + pct(counts.endToEndFidelity()));
            }

            writeOutput(
                    output,
                    runRows,
                    deviceSchedulerRows,
                    deviceRows,
                    overall);

            System.out.println("Combined CSV: " + output.toAbsolutePath());
        }
        catch(IOException exception) {
            System.err.println("Replay-fidelity aggregation failed: "
                    + exception.getMessage());
            exception.printStackTrace();
        }
    }

    private static SchedulerReplayEngine createReplayEngine() {
        return new SchedulerReplayEngine()
                .register(new RandomReplayStrategy())
                .register(new RoundRobinReplayStrategy())
                .register(new LeastLoadedReplayStrategy())
                .register(new MultiObjectiveReplayStrategy())
                .register(new PpoReplayStrategy());
    }

    private static List<InputPair> discoverPairs(Path root)
            throws IOException {
        if(!Files.exists(root)) {
            throw new IOException("Directory does not exist: " + root);
        }

        List<InputPair> result = new ArrayList<InputPair>();

        try(Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String name = path.getFileName().toString();
                if(name.endsWith("_candidates.csv")) {
                    return;
                }

                Matcher matcher = FILE_PATTERN.matcher(name);
                if(!matcher.matches()) {
                    return;
                }

                String candidateName = name.substring(
                        0,
                        name.length() - ".csv".length())
                        + "_candidates.csv";

                result.add(new InputPair(
                        matcher.group(1).toUpperCase(Locale.US),
                        matcher.group(2).toUpperCase(Locale.US),
                        Integer.parseInt(matcher.group(3)),
                        Integer.parseInt(matcher.group(4)),
                        path,
                        path.resolveSibling(candidateName)));
            });
        }

        Collections.sort(result, new Comparator<InputPair>() {
            @Override
            public int compare(InputPair a, InputPair b) {
                return a.runKey().compareTo(b.runKey());
            }
        });
        return result;
    }

    private static Counts calculate(
            InputPair pair,
            DecisionReplayEngine decisionLoader,
            SchedulerReplayEngine replayEngine) {

        Counts counts = new Counts();
        counts.files = 1;

        if(!Files.exists(pair.candidates)) {
            counts.missingCandidateFiles = 1;
            System.err.println("Missing candidate file: " + pair.candidates);
            return counts;
        }

        SchedulerReplayContextFactory factory =
                factoryFor(pair.scheduler);

        if(factory == null || !replayEngine.supports(pair.scheduler)) {
            counts.unsupportedPolicyFiles = 1;
            return counts;
        }

        try {
            List<DecisionState> decisions =
                    decisionLoader.loadDecisions(pair.file.toString());

            Map<Integer, List<VmCandidateSnapshot>> byTask =
                    new CandidateSnapshotCsvLoader().load(
                            pair.candidates.toString(),
                            pair.file.toString());

            Set<Integer> seen = new HashSet<Integer>();

            for(DecisionState decision : decisions) {
                counts.loadedRows++;

                if(decision == null) {
                    counts.invalidRows++;
                    continue;
                }

                int taskId = decision.getTaskId();
                if(!seen.add(taskId)) {
                    counts.duplicates++;
                    continue;
                }
                counts.unique++;

                List<VmCandidateSnapshot> candidates = byTask.get(taskId);
                if(candidates == null || candidates.isEmpty()) {
                    counts.missingSnapshots++;
                    continue;
                }

                if(isInvalidRoundRobin(pair.scheduler, decision)) {
                    counts.invalidRoundRobin++;
                    continue;
                }

                counts.eligible++;

                try {
                    ReplayState state = ReplayState.from(decision).build();
                    SchedulerReplayContext context =
                            factory.create(state, candidates);
                    SchedulerDecision replay =
                            replayEngine.replayBaseline(context);

                    if(replay == null) {
                        counts.replayFailures++;
                    }
                    else if(replay.wasSuccessfullyReplayed()) {
                        /* replayBaseline only succeeds for an exact match. */
                        counts.faithful++;
                    }
                    else if(isMismatch(replay.getReason())) {
                        counts.mismatches++;
                    }
                    else {
                        counts.replayFailures++;
                    }
                }
                catch(RuntimeException exception) {
                    counts.replayFailures++;
                    if(counts.printedErrors < 5) {
                        counts.printedErrors++;
                        System.err.println("Task " + taskId
                                + " replay error: "
                                + safeMessage(exception));
                    }
                }
            }
        }
        catch(IOException exception) {
            counts.fileReadFailures++;
            System.err.println("Could not read " + pair.file + ": "
                    + exception.getMessage());
        }
        catch(RuntimeException exception) {
            counts.fileReadFailures++;
            System.err.println("Could not process " + pair.file + ": "
                    + safeMessage(exception));
        }

        return counts;
    }

    private static boolean isInvalidRoundRobin(
            String scheduler,
            DecisionState decision) {
        return "ROUND_ROBIN".equalsIgnoreCase(scheduler)
                && "EDGE".equalsIgnoreCase(
                        decision.getAttemptedDestination())
                && decision.isSelectedDecision()
                && decision.getRoundRobinStartingCandidateIndex() < 0;
    }

    private static boolean isMismatch(String reason) {
        return reason != null
                && reason.trim().toUpperCase(Locale.US)
                        .startsWith("BASELINE_REPLAY_MISMATCH");
    }

    private static SchedulerReplayContextFactory factoryFor(String policy) {
        if("RANDOM".equalsIgnoreCase(policy)) {
            return new RandomReplayContextFactory();
        }
        if("ROUND_ROBIN".equalsIgnoreCase(policy)) {
            return new RoundRobinReplayContextFactory();
        }
        if("LEAST_LOADED".equalsIgnoreCase(policy)) {
            return new LeastLoadedReplayContextFactory();
        }
        if("MULTI_OBJECTIVE".equalsIgnoreCase(policy)) {
            return new MultiObjectiveReplayContextFactory();
        }
        if("PPO".equalsIgnoreCase(policy)) {
            return new PpoReplayContextFactory();
        }
        return null;
    }

    private static void add(
            Map<String, Counts> map,
            String key,
            Counts value) {
        Counts aggregate = map.get(key);
        if(aggregate == null) {
            aggregate = new Counts();
            map.put(key, aggregate);
        }
        aggregate.add(value);
    }

    private static void writeOutput(
            Path output,
            Map<String, Counts> runRows,
            Map<String, Counts> deviceSchedulerRows,
            Map<String, Counts> deviceRows,
            Counts overall)
            throws IOException {

        if(output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }

        try(BufferedWriter writer = Files.newBufferedWriter(
                output,
                StandardCharsets.UTF_8)) {

            writer.write(
                    "row_type,scenario,device_count,iteration,scheduler,"
                    + "source_file_count,loaded_decision_rows,"
                    + "unique_decisions,eligible_decisions,"
                    + "faithful_replays,mismatch_replays,replay_failures,"
                    + "missing_candidate_snapshots,"
                    + "invalid_round_robin_provenance,duplicate_task_rows,"
                    + "invalid_decision_rows,missing_candidate_files,"
                    + "unsupported_policy_files,file_read_failures,"
                    + "provenance_coverage_pct,replay_completion_rate_pct,"
                    + "conditional_replay_fidelity_pct,"
                    + "end_to_end_replay_fidelity_pct");
            writer.newLine();

            for(Map.Entry<String, Counts> row : runRows.entrySet()) {
                String[] key = row.getKey().split("\\|", -1);
                writeRow(writer, "RUN", key[0], unpad(key[1]), unpad(key[2]), key[3],
                        row.getValue());
            }

            for(Map.Entry<String, Counts> row
                    : deviceSchedulerRows.entrySet()) {
                String[] key = row.getKey().split("\\|", -1);
                writeRow(writer, "DEVICE_SCHEDULER",
                        key[0], unpad(key[1]), "ALL", key[2], row.getValue());
            }

            for(Map.Entry<String, Counts> row : deviceRows.entrySet()) {
                String[] key = row.getKey().split("\\|", -1);
                writeRow(writer, "DEVICE",
                        key[0], unpad(key[1]), "ALL", "ALL", row.getValue());
            }

            writeRow(writer, "OVERALL",
                    "ALL", "ALL", "ALL", "ALL", overall);
        }
    }

    private static void writeRow(
            BufferedWriter writer,
            String rowType,
            String scenario,
            String devices,
            String iteration,
            String scheduler,
            Counts c)
            throws IOException {

        writer.write(String.join(",",
                rowType,
                scenario,
                devices,
                iteration,
                scheduler,
                Long.toString(c.files),
                Long.toString(c.loadedRows),
                Long.toString(c.unique),
                Long.toString(c.eligible),
                Long.toString(c.faithful),
                Long.toString(c.mismatches),
                Long.toString(c.replayFailures),
                Long.toString(c.missingSnapshots),
                Long.toString(c.invalidRoundRobin),
                Long.toString(c.duplicates),
                Long.toString(c.invalidRows),
                Long.toString(c.missingCandidateFiles),
                Long.toString(c.unsupportedPolicyFiles),
                Long.toString(c.fileReadFailures),
                pct(c.provenanceCoverage()),
                pct(c.completionRate()),
                pct(c.conditionalFidelity()),
                pct(c.endToEndFidelity())));
        writer.newLine();
    }

    private static String unpad(String value) {
        return Integer.toString(Integer.parseInt(value));
    }

    private static String pct(double value) {
        return Double.isNaN(value)
                ? ""
                : String.format(Locale.US, "%.6f", value);
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.trim().isEmpty()
                ? throwable.getClass().getSimpleName()
                : message;
    }

    private static final class InputPair {
        final String scenario;
        final String scheduler;
        final int devices;
        final int iteration;
        final Path file;
        final Path candidates;

        InputPair(
                String scenario,
                String scheduler,
                int devices,
                int iteration,
                Path file,
                Path candidates) {
            this.scenario = scenario;
            this.scheduler = scheduler;
            this.devices = devices;
            this.iteration = iteration;
            this.file = file;
            this.candidates = candidates;
        }

        String runKey() {
            return scenario + "|" + pad(devices) + "|"
                    + pad(iteration) + "|" + scheduler;
        }

        String deviceSchedulerKey() {
            return scenario + "|" + pad(devices) + "|" + scheduler;
        }

        String deviceKey() {
            return scenario + "|" + pad(devices);
        }

        private static String pad(int value) {
            return String.format(Locale.US, "%08d", value);
        }
    }

    private static final class Counts {
        long files;
        long loadedRows;
        long unique;
        long eligible;
        long faithful;
        long mismatches;
        long replayFailures;
        long missingSnapshots;
        long invalidRoundRobin;
        long duplicates;
        long invalidRows;
        long missingCandidateFiles;
        long unsupportedPolicyFiles;
        long fileReadFailures;
        int printedErrors;

        void add(Counts other) {
            files += other.files;
            loadedRows += other.loadedRows;
            unique += other.unique;
            eligible += other.eligible;
            faithful += other.faithful;
            mismatches += other.mismatches;
            replayFailures += other.replayFailures;
            missingSnapshots += other.missingSnapshots;
            invalidRoundRobin += other.invalidRoundRobin;
            duplicates += other.duplicates;
            invalidRows += other.invalidRows;
            missingCandidateFiles += other.missingCandidateFiles;
            unsupportedPolicyFiles += other.unsupportedPolicyFiles;
            fileReadFailures += other.fileReadFailures;
        }

        double provenanceCoverage() {
            return percent(eligible, unique);
        }

        double completionRate() {
            return percent(faithful + mismatches, eligible);
        }

        double conditionalFidelity() {
            return percent(faithful, faithful + mismatches);
        }

        double endToEndFidelity() {
            return percent(faithful, eligible);
        }

        private static double percent(long numerator, long denominator) {
            return denominator == 0
                    ? Double.NaN
                    : 100.0 * numerator / denominator;
        }
    }
}
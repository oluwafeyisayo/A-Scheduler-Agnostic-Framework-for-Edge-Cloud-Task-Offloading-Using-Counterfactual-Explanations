/*
 * Title:        EdgeCloudSim - Main Application
 *
 * Description:  Main application for the counterfactual-offloading
 *               research environment.
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.research.counterfactual;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_orchestrator.ppo.PpoConfig;
import edu.boun.edgecloudsim.research.experiment.ExperimentConfig;
import edu.boun.edgecloudsim.research.experiment
        .ExperimentDirectoryManager;
import edu.boun.edgecloudsim.research.experiment.ExperimentManifest;
import edu.boun.edgecloudsim.research.logging.DecisionLogger;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class MainApp {

    private static final long BASE_SIMULATION_SEED =
            1001L;

    private static final long BASE_SEARCH_SEED =
            2001L;

    public static void main(String[] args) {

        Log.disable();

        SimLogger.enablePrintLog();

        int iterationNumber =
                1;

        String configFile;
        String outputFolder;
        String edgeDevicesFile;
        String applicationsFile;

        if(args.length == 5) {

            configFile =
                    args[0];

            edgeDevicesFile =
                    args[1];

            applicationsFile =
                    args[2];

            outputFolder =
                    args[3];

            iterationNumber =
                    Integer.parseInt(
                            args[4]);
        }
        else {

            SimLogger.printLine(
                    "Simulation setting file, output folder and "
                    + "iteration number are not provided! "
                    + "Using default ones...");

            configFile =
                    "scripts/counterfactual_offloading/config/"
                    + "default_config.properties";

            applicationsFile =
                    "scripts/counterfactual_offloading/config/"
                    + "applications_research.xml";

            edgeDevicesFile =
                    "scripts/counterfactual_offloading/config/"
                    + "edge_devices_research.xml";

            outputFolder =
                    "sim_results/ite"
                    + iterationNumber;
        }

        SimSettings settings =
                SimSettings.getInstance();

        if(!settings.initialize(
                configFile,
                edgeDevicesFile,
                applicationsFile)) {

            SimLogger.printLine(
                    "Cannot initialize simulation settings!");

            System.exit(0);
        }

        if(settings.getFileLoggingEnabled()) {

            SimLogger.enableFileLog();

            SimUtils.cleanOutputFolder(
                    outputFolder);
        }

        DateFormat dateFormat =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm:ss");

        Date simulationStartDate =
                Calendar.getInstance()
                .getTime();

        SimLogger.printLine(
                "Simulation started at "
                + dateFormat.format(
                        simulationStartDate));

        SimLogger.printLine(
                "----------------------------------------------------------------------");

        for(int deviceCount =
                    settings.getMinNumOfMobileDev();
                deviceCount <=
                    settings.getMaxNumOfMobileDev();
                deviceCount +=
                    settings.getMobileDevCounterSize()) {

            for(String simulationScenario
                    : settings.getSimulationScenarios()) {

                for(String orchestratorPolicy
                        : settings.getOrchestratorPolicies()) {

                    runScenario(
                            settings,
                            dateFormat,
                            outputFolder,
                            iterationNumber,
                            deviceCount,
                            simulationScenario,
                            orchestratorPolicy);
                }
            }
        }

        Date simulationEndDate =
                Calendar.getInstance()
                .getTime();

        SimLogger.printLine(
                "Simulation finished at "
                + dateFormat.format(
                        simulationEndDate)
                + ". It took "
                + SimUtils.getTimeDifference(
                        simulationStartDate,
                        simulationEndDate));
    }

    private static void runScenario(
            SimSettings settings,
            DateFormat dateFormat,
            String outputFolder,
            int iterationNumber,
            int deviceCount,
            String simulationScenario,
            String orchestratorPolicy) {

        SimLogger.printLine(
                "RESEARCH SCHEDULER ACTIVE: "
                + orchestratorPolicy);

        Date scenarioStartDate =
                Calendar.getInstance()
                .getTime();

        SimLogger.printLine(
                "Scenario started at "
                + dateFormat.format(
                        scenarioStartDate));

        SimLogger.printLine(
                "Scenario: "
                + simulationScenario
                + " - Policy: "
                + orchestratorPolicy
                + " - #iteration: "
                + iterationNumber);

        SimLogger.printLine(
                "Duration: "
                + settings.getSimulationTime()
                / 3600
                + " hour(s) - Poisson: "
                + settings.getTaskLookUpTable()[0][2]
                + " - #devices: "
                + deviceCount);

        SimLogger.getInstance()
                .simStarted(
                        outputFolder,
                        "SIMRESULT_"
                        + simulationScenario
                        + "_"
                        + orchestratorPolicy
                        + "_"
                        + deviceCount
                        + "DEVICES");

        String experimentId =
                "EDGE_CFE_SIMULATION_ITE"
                + iterationNumber;

        String runId =
                simulationScenario
                + "_"
                + orchestratorPolicy
                + "_"
                + deviceCount
                + "DEVICES";

        ExperimentManifest manifest =
                null;

        try {

            ExperimentDirectoryManager directoryManager =
                    new ExperimentDirectoryManager(
                            new File(
                                    outputFolder),
                            experimentId,
                            runId);

            long simulationSeed =
                    BASE_SIMULATION_SEED
                    + iterationNumber
                    + deviceCount;

            long searchSeed =
                    BASE_SEARCH_SEED
                    + iterationNumber;

            ExperimentConfig experimentConfig =
                    ExperimentConfig.builder()
                    .withExperimentId(
                            experimentId)
                    .withRunId(
                            runId)
                    .withScenario(
                            simulationScenario)
                    .withSchedulerPolicy(
                            orchestratorPolicy)
                    .withDeviceCount(
                            deviceCount)
                    .withIterationNumber(
                            iterationNumber)
                    .withSimulationSeed(
                            simulationSeed)
                    .withSearchSeed(
                            searchSeed)
                    .withSimulationDurationSeconds(
                            settings
                            .getSimulationTime())
                    .withPpoModelId(
                            "PPO".equalsIgnoreCase(
                                    orchestratorPolicy)
                            ? PpoConfig.DEFAULT_MODEL_ID
                            : "NOT_APPLICABLE")
                    .withOutputDirectory(
                            directoryManager
                            .getExperimentRoot()
                            .getPath())
                    .build();

            manifest =
                    new ExperimentManifest(
                            directoryManager
                            .getManifestFile(),
                            experimentConfig);

            /*
             * Record requested seeds as system properties. The current
             * SimUtils implementation does not yet expose a global seed
             * setter, so deterministic seed injection is the next step.
             */
            System.setProperty(
                    "edgecfe.simulation.seed",
                    String.valueOf(
                            simulationSeed));

            System.setProperty(
                    "edgecfe.search.seed",
                    String.valueOf(
                            searchSeed));

            int numberOfUsers =
                    2;

            Calendar calendar =
                    Calendar.getInstance();

            boolean traceFlag =
                    false;

            CloudSim.init(
                    numberOfUsers,
                    calendar,
                    traceFlag,
                    0.01);

            String decisionLogFolder =
                    outputFolder
                    + "/decision_logs";

            String decisionFileName =
                    "offloading_decisions_"
                    + simulationScenario
                    + "_"
                    + orchestratorPolicy
                    + "_"
                    + deviceCount
                    + "DEVICES_ITE"
                    + iterationNumber
                    + ".csv";

            DecisionLogger.getInstance()
                    .initialize(
                            decisionLogFolder,
                            decisionFileName);

            SimLogger.printLine(
                    "Decision log initialized: "
                    + decisionLogFolder
                    + "/"
                    + decisionFileName);

            SimLogger.printLine(
                    "Experiment manifest initialized: "
                    + directoryManager
                    .getManifestFile()
                    .getAbsolutePath());

            ScenarioFactory scenarioFactory =
                    new SampleScenarioFactory(
                            deviceCount,
                            settings.getSimulationTime(),
                            orchestratorPolicy,
                            simulationScenario);

            SimManager manager =
                    new SimManager(
                            scenarioFactory,
                            deviceCount,
                            simulationScenario,
                            orchestratorPolicy);

            manager.startSimulation();

            manifest.markCompleted();
        }
        catch(Exception exception) {

            SimLogger.printLine(
                    "The simulation has been terminated due "
                    + "to an unexpected error.");

            try {

                if(manifest != null) {

                    manifest.markFailed(
                            exception);
                }
            }
            catch(Exception manifestException) {

                manifestException.printStackTrace();
            }

            exception.printStackTrace();

            System.exit(0);
        }
        finally {

            try {

                if(manifest != null) {

                    manifest.close();
                }
            }
            catch(Exception exception) {

                exception.printStackTrace();
            }
        }

        Date scenarioEndDate =
                Calendar.getInstance()
                .getTime();

        SimLogger.printLine(
                "Scenario finished at "
                + dateFormat.format(
                        scenarioEndDate)
                + ". It took "
                + SimUtils.getTimeDifference(
                        scenarioStartDate,
                        scenarioEndDate));

        SimLogger.printLine(
                "----------------------------------------------------------------------");
    }
}
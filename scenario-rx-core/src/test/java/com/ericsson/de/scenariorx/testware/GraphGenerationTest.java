package com.ericsson.de.scenariorx.testware;

import static com.ericsson.de.scenariorx.api.RxApi.flow;
import static com.ericsson.de.scenariorx.api.RxApi.runner;
import static com.ericsson.de.scenariorx.api.RxApi.scenario;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.ericsson.de.scenariorx.api.DebugGraphMode;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.impl.ScenarioTest;
import org.junit.Test;

public class GraphGenerationTest extends ScenarioTest {

    private static final String DESIRED_GRAPH_LOCATION = "target/scenario-visualization/";
    private static final String SVG_EXTENSION = ".svg";
    private static final String GRAPHML_EXTENSION = ".graphml";

    @Test
    public void testSavedGraphLocation() throws Exception {
        final String scenarioName = "GraphLocationVerification";
        RxScenario scenario = scenario(scenarioName)
                .addFlow(flow("print flow")
                        .addTestStep(print("testStep1"))
                )
                .build();

        runScenarioAllGraphs(scenario);
        verifyGraphDirectoryContents(scenarioName);
    }

    @Test
    public void testGraphNameSpaceFiltering() throws Exception {
        final String scenarioNameUnfiltered = "   Graph     name   filtered      ";
        final String scenarioNameFiltered = "Graph_name_filtered";
        RxScenario scenario = scenario(scenarioNameUnfiltered)
                .addFlow(flow("print flow")
                        .addTestStep(print("testStep1"))
                )
                .build();

        runScenarioAllGraphs(scenario);
        verifyGraphDirectoryContents(scenarioNameFiltered);
    }

    private void verifyGraphDirectoryContents(String scenarioName) {
        File graphFolder = new File(DESIRED_GRAPH_LOCATION);
        assertThat(graphFolder.isDirectory());
        List<String> graphDirectoryContents = Arrays.asList(requireNonNull(graphFolder.list()));
        assertThat(graphDirectoryContents).contains(scenarioName + GRAPHML_EXTENSION, scenarioName + SVG_EXTENSION);
    }

    private void runScenarioAllGraphs(RxScenario scenario) {
        runner()
                .withGraphExportMode(DebugGraphMode.ALL)
                .build()
                .run(scenario);
    }
}

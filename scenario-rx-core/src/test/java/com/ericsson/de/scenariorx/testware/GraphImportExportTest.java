package com.ericsson.de.scenariorx.testware;

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.impl.Api.fromIterable;
import static com.ericsson.de.scenariorx.impl.graph.GraphNodeFactory.createTestStepNode;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Named;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;

import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.impl.ScenarioDebugger;
import com.ericsson.de.scenariorx.impl.ScenarioTest;
import com.ericsson.de.scenariorx.impl.graph.GraphMlImporter;
import com.ericsson.de.scenariorx.impl.graph.ScenarioExecutionGraph;
import com.ericsson.de.scenariorx.impl.graph.ScenarioExecutionGraph.GraphNode;
import com.ericsson.de.scenariorx.impl.graph.export.GraphMlExporter;
import com.google.common.io.Resources;
import org.junit.Test;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

public class GraphImportExportTest extends ScenarioTest {

    @Test
    public void testGraphEquals() throws Exception {
        ScenarioExecutionGraph graph1 = graph(
                createTestStepNode("1", "1", 0L, 0L, "dr", "1.2", "1", "SUCCESS", ""),
                createTestStepNode("1", "2", 0L, 0L, "dr", "1.3", "1", "SUCCESS", "")
        );

        ScenarioExecutionGraph graph2 = graph(
                createTestStepNode("1", "1", 0L, 0L, "dr", "1.2", "1", "SUCCESS", ""),
                createTestStepNode("1", "2", 0L, 0L, "dr", "1.3", "1", "SUCCESS", "")
        );

        compareGraphs(graph1, graph2);
    }

    @Test
    public void testGraphNotEquals() throws Exception {
        ScenarioExecutionGraph graph1 = graph(
                createTestStepNode("1", "1", 0L, 0L, "dr", "1.2", "1", "SUCCESS", ""),
                createTestStepNode("1", "2", 0L, 0L, "dr", "BAD", "1", "SUCCESS", "")
        );

        ScenarioExecutionGraph graph2 = graph(
                createTestStepNode("1", "1", 0L, 0L, "dr", "1.2", "1", "SUCCESS", ""),
                createTestStepNode("1", "2", 0L, 0L, "dr", "1.3", "1", "SUCCESS", "")
        );

        assertThat(graph2).isNotEqualTo(graph1);
    }

    /**
     * To use layout we need yEd which is well known tool to work with graphs.
     * yEd uses its own flavour of GraphMl. So we need to able import GraphMl from both jGraphT and yEd
     */
    @Test
    public void testImportYed() throws Exception {
        ScenarioExecutionGraph graph = graph(
                createTestStepNode("1", "1", 0L, 0L, "dr", "1.2", "1", "SUCCESS", ""),
                createTestStepNode("1", "2", 0L, 0L, "dr", "1.3", "1", "SUCCESS", "")
        );

        GraphMlImporter graphMlImporter = new GraphMlImporter();
        URL url = Resources.getResource("graphs/yed.graphml");
        ScenarioExecutionGraph importedGraph = graphMlImporter.importFile(url);

        compareGraphs(graph, importedGraph);
    }

    @Test
    public void testImportExport() throws Exception {
        ScenarioExecutionGraph graph = graph(
                createTestStepNode("1", "1", 0L, 0L, "{\"dr\":1}", "1.2", "1", "SUCCESS", ""),
                createTestStepNode("1", "2", 0L, 0L, "{\"dr\":2}", "1.3", "1", "SUCCESS", "")
        );

        File temp = File.createTempFile("scenario-export", ".graphml");
        GraphMlExporter graphMlExporter = new GraphMlExporter();
        graphMlExporter.export(graph, new FileWriter(temp));

        GraphMlImporter graphMlImporter = new GraphMlImporter();
        ScenarioExecutionGraph importedGraph = graphMlImporter.importFile(temp.toURI().toURL());

        compareGraphs(graph, importedGraph);
    }

    private ScenarioExecutionGraph graph(GraphNode node1, GraphNode node2) {
        ScenarioExecutionGraph graph = new ScenarioExecutionGraph();
        graph.addVertex(node1);
        graph.addVertex(node2);
        graph.addEdge(node1, node2);
        return graph;
    }

    @Test
    public void testImportExportAdvanced() throws Exception {
        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(print("testStep1"))
                        .split(
                                flow("subFlow1")
                                        .addTestStep(print("testStep2"))
                                        .withDataSources(fromIterable("ds2", asList("a", "b"))),
                                flow("subFlow2")
                                        .addTestStep(print("testStep3"))
                                        .withVUsers(3)
                        )
                        .withDataSources(fromIterable("ds1", asList("1", "2", "3")))
                        .withVUsers(3)
                )
                .build();

        ScenarioExecutionGraph graph = ScenarioDebugger.debug(scenario);

        File temp = File.createTempFile("scenario-export-advanced", ".graphml");
        GraphMlExporter graphMlExporter = new GraphMlExporter();
        graphMlExporter.export(graph, new FileWriter(temp));

        GraphMlImporter graphMlImporter = new GraphMlImporter();
        ScenarioExecutionGraph importedGraph = graphMlImporter.importFile(temp.toURI().toURL());

        compareGraphs(graph, importedGraph);
    }

    @Test
    public void sameTestSteps() throws Exception {
        final String duplicate = "duplicate";
        final String datSource = "nodesToCreateIds";

        RxDataSource<String> duplicateSource = fromIterable(datSource, asList(duplicate, duplicate, duplicate));


        RxScenario scenario = scenario()
                .addFlow(
                        flow("flow")
                                .addSubFlow(getSubFlow(duplicateSource))
                                .addSubFlow(getSubFlow(duplicateSource))
                                .withVUsers(2)
                                .withDataSources(duplicateSource)
                )
                .build();

        ScenarioExecutionGraph graph = ScenarioDebugger.debug(scenario);
        compareGraphs(graph, "sameTestSteps.graphml");
    }

    private RxFlow getSubFlow(RxDataSource<String> dataSource) {
        return flow("subFlow")
                .addTestStep(
                        new InlineInvocation() {
                            void producer(@Named("nodesToCreateIds") String fromDatSource) {
                            }
                        }
                )
                .withVUsers(2)
                .withDataSources(dataSource)
                .build();
    }
}
package com.ericsson.de.scenariorx.testware;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import static com.ericsson.de.scenariorx.api.RxApiImpl.contextDataSource;
import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.runner;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.impl.Api.fromDataRecords;
import static com.ericsson.de.scenariorx.impl.Api.fromIterable;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Named;
import java.util.List;
import java.util.Stack;

import com.ericsson.de.scenariorx.Node;
import com.ericsson.de.scenariorx.api.RxApiImpl;
import com.ericsson.de.scenariorx.api.RxBasicDataRecord;
import com.ericsson.de.scenariorx.api.RxContextDataSource;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.impl.ScenarioDebugger;
import com.ericsson.de.scenariorx.impl.ScenarioTest;
import com.ericsson.de.scenariorx.impl.graph.ScenarioExecutionGraph;
import com.google.common.base.Optional;
import org.junit.Test;

public class DataSourceTest extends ScenarioTest {

    @Test
    public void scenarioWithSharedDataSources() throws Exception {
        List<String> dataSource = newArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
        List<String> subFlowDataSource = newArrayList("a", "b", "c", "d", "e", "f");
        List<String> subSubFlowDataSource = newArrayList("x", "o", "+");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("flow1"))
                        .addTestStep(print("flow2"))
                        .addSubFlow(flow()
                                .addTestStep(print("subFlow1"))
                                .addSubFlow(flow()
                                        .addTestStep(print("subSubFlow1"))
                                        .withVUsers(2)
                                        .withDataSources(fromIterable("subSubFlowDataSource", subSubFlowDataSource).shared())
                                )
                                .withVUsers(2)
                                .withDataSources(fromIterable("subFlowDataSource", subFlowDataSource).shared())
                        )
                        .addTestStep(print("flow3"))
                        .withVUsers(3)
                        .withDataSources(fromIterable("dataSource", dataSource).shared())
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "scenarioWithSharedDataSources.graphml");
    }

    @Test
    public void scenarioWithRegularDataSources() throws Exception {
        List dataSource = newArrayList("1", "2");
        List subFlowDataSource = newArrayList("a", "b");
        List subSubFlowDataSource = newArrayList("x", "o");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("flow1"))
                        .addTestStep(print("flow2"))
                        .addSubFlow(flow()
                                .addTestStep(print("subFlow1"))
                                .addSubFlow(flow()
                                        .addTestStep(print("subSubFlow1"))
                                        .withVUsers(2)
                                        .withDataSources(fromIterable("subSubFlowDataSource", subSubFlowDataSource))
                                )
                                .withVUsers(2)
                                .withDataSources(fromIterable("subFlowDataSource", subFlowDataSource))
                        )
                        .addTestStep(print("flow3"))
                        .withVUsers(2)
                        .withDataSources(fromIterable("dataSource", dataSource))
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "scenarioWithRegularDataSources.graphml");
    }

    @Test
    public void sharedDataSourcesTest_ChildIsSmaller() throws Exception {
        List<String> dataSource = newArrayList("1", "2", "3", "4");
        List<String> subFlowDataSource = newArrayList("a", "b", "c");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addSubFlow(flow()
                                .addTestStep(print("subFlow1"))
                                .withVUsers(2)
                                .withDataSources(fromIterable("subFlowDataSource", subFlowDataSource).shared())
                        )
                        .withVUsers(2)
                        .withDataSources(fromIterable("dataSource", dataSource).shared())
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "sharedDataSourcesTest_ChildIsSmaller.graphml");
    }

    @Test
    public void sharedDataSourcesTest_ChildIsLarger() throws Exception {
        List<String> dataSource = newArrayList("1", "2", "3", "4");
        List<String> subFlowDataSource = newArrayList("a", "b", "c", "d", "e", "f");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addSubFlow(flow()
                                .addTestStep(print("subFlow1"))
                                .withVUsers(2)
                                .withDataSources(fromIterable("subFlowDataSource", subFlowDataSource).shared())
                        )
                        .withVUsers(2)
                        .withDataSources(fromIterable("dataSource", dataSource).shared())
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "sharedDataSourcesTest_ChildIsLarger.graphml");
    }

    @Test
    public void combinationOfTwoDataSources() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c");

        List<String> dataSource3 = newArrayList("w", "x", "y", "z");
        List<String> dataSource4 = newArrayList("+", "o", "/");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("flow1"))
                        .addSubFlow(flow()
                                .addTestStep(print("subFlow1"))
                                .withVUsers(2)
                                .withDataSources(
                                        fromIterable("dataSource3", dataSource3),
                                        fromIterable("dataSource4", dataSource4)
                                )
                        )
                        .withVUsers(2)
                        .withDataSources(
                                fromIterable("dataSource1", dataSource1),
                                fromIterable("dataSource2", dataSource2)
                        )
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "combinationOfTwoDataSources.graphml");
    }

    @Test
    public void sharedAndNotSharedDataSource_Flow() throws Exception {
        List<String> sharedDataSource1 = newArrayList("1", "2", "3", "4");
        List<String> copiedDataSource2 = newArrayList("a", "b", "c");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("subFlow1"))
                        .withVUsers(2)
                        .withDataSources(
                                fromIterable("sharedDataSource1", sharedDataSource1).shared(),
                                fromIterable("copiedDataSource2", copiedDataSource2)
                        )
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "sharedAndNotSharedDataSource_Flow.graphml");
    }

    @Test
    public void sharedAndNotSharedDataSource_SubFlow() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c", "d");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("subFlow1"))
                        .withVUsers(2)
                        .withDataSources(
                                fromIterable("dataSource1", dataSource1).shared(),
                                fromIterable("dataSource2", dataSource2)
                        )
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "sharedAndNotSharedDataSource_SubFlow.graphml");
    }

    @Test
    public void moreDataSources() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c");
        List<String> dataSource3 = newArrayList("w", "x", "y", "z");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("flow1"))
                        .withDataSources(
                                fromIterable("dataSource1", dataSource1),
                                fromIterable("dataSource2", dataSource2),
                                fromIterable("dataSource3", dataSource3)
                        )
                        .withVUsers(2)
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "moreDataSources.graphml");
    }

    @Test(timeout = 10000L)
    public void moreDataSourcesCyclic() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c");
        List<String> dataSource3 = newArrayList("w", "x", "y", "z");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("flow1"))
                        .withDataSources(
                                fromIterable("dataSource1", dataSource1),
                                fromIterable("dataSource2", dataSource2).cyclic(),
                                fromIterable("dataSource3", dataSource3)
                        )
                        .withVUsers(2)
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "moreDataSourcesCyclic.graphml");
    }

    @Test
    public void moreDataSourcesShared() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c");
        List<String> dataSource3 = newArrayList("w", "x", "y", "z");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("flow1"))
                        .withDataSources(
                                fromIterable("dataSource1", dataSource1),
                                fromIterable("dataSource2", dataSource2).shared(),
                                fromIterable("dataSource3", dataSource3)
                        )
                        .withVUsers(2)
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "moreDataSourcesShared.graphml");
    }

    @Test
    public void moreDataSourcesAllShared() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c");
        List<String> dataSource3 = newArrayList("w", "x", "y", "z");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("flow1"))
                        .withDataSources(
                                fromIterable("dataSource1", dataSource1).shared(),
                                fromIterable("dataSource2", dataSource2).shared(),
                                fromIterable("dataSource3", dataSource3).shared()
                        )
                        .withVUsers(2)
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "moreDataSourcesAllShared.graphml");
    }

    @Test
    public void testDataRecords() throws Exception {
        final String ds = "DS";
        final String subFlowDS1 = "subFlowDS1";
        final String subFlowDS2 = "subFlowDS2";
        final String subSubFlowDS = "subSubFlowDS";

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addSubFlow(flow()
                                .addTestStep(new InlineInvocation() {
                                    @SuppressWarnings("unused")
                                    public void validateSubFlow1(
                                            @Named("key") String akey,
                                            @Named(subFlowDS1 + ".key") String bkey,
                                            @Named(subFlowDS2 + ".key") String ckey,
                                            @Named(ds + ".key") String dkey) {

                                        assertThat(akey).isEqualTo(subFlowDS1);
                                        assertThat(bkey).isEqualTo(subFlowDS1);
                                        assertThat(ckey).isEqualTo(subFlowDS2);
                                        assertThat(dkey).isEqualTo(ds);
                                    }
                                })
                                .addSubFlow(flow()
                                        .addTestStep(new InlineInvocation() {
                                            @SuppressWarnings("unused")
                                            public void validateSubSubFlow1(
                                                    @Named("key") String akey,
                                                    @Named(subSubFlowDS + ".key") String bkey,
                                                    @Named(subFlowDS1 + ".key") String ckey,
                                                    @Named(subFlowDS2 + ".key") String dkey,
                                                    @Named(ds + ".key") String ekey) {

                                                assertThat(akey).isEqualTo(subSubFlowDS);
                                                assertThat(bkey).isEqualTo(subSubFlowDS);
                                                assertThat(ckey).isEqualTo(subFlowDS1);
                                                assertThat(dkey).isEqualTo(subFlowDS2);
                                                assertThat(ekey).isEqualTo(ds);
                                            }
                                        })
                                        .withDataSources(fromDataRecords(subSubFlowDS, RxBasicDataRecord.fromValues("key", subSubFlowDS)))
                                )
                                .withDataSources(
                                        fromDataRecords(subFlowDS1, RxBasicDataRecord.fromValues("key", subFlowDS1)),
                                        fromDataRecords(subFlowDS2, RxBasicDataRecord.fromValues("key", subFlowDS2))
                                )
                        )
                        .withDataSources(fromDataRecords(ds, RxBasicDataRecord.fromValues("key", ds)))
                )
                .build();

        RxApiImpl.run(scenario);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionOnNonExistingField() throws Exception {
        RxDataSource<Node> dataSource = getNodeDataSource();

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("subFlow1"))
                        .withDataSources(
                                dataSource
                                        .filterField("not_existing_field").equalTo("nope")
                        )
                )
                .build();

        RxApiImpl.run(scenario);
    }

    @Test
    public void filterDataSource() throws Exception {
        final Stack<String> ids = new Stack<>();

        String targetId = "AWSM-MEE";
        RxDataSource<Node> dataSource = fromDataRecords("nodes",
                getNode("SGSN-14B", "LTE01ERB", 20),
                getNode("SGSN-MME", "ERBS", 20),
                getNode(targetId, "ERBS", 20)
        );

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void idToStack(@Named(Node.NETWORK_ELEMENT_ID) String id) {
                                ids.add(id);
                            }
                        })
                        .withDataSources(
                                dataSource
                                        .filterField(Node.PORT).equalTo(20)
                                        .filterField(Node.NETWORK_ELEMENT_ID).contains("MEE")
                                        .filterField(Node.NODE_TYPE).equalToIgnoreCase("eRbS")
                        )
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(ids).containsExactly(targetId);
    }

    @Test
    public void filterSharedDataSourcePredictability() throws Exception {
        RxDataSource<Node> dataSource = fromDataRecords("nodes",
                getNode("SGSN-14B", "LTE01ERB", 20),
                getNode("SGSN-MME", "ERBS", 20),
                getNode("AWSM-MEE", "ERBS", 20),
                getNode("DESP-MME", "ERBS", 20),
                getNode("MEEE-MEE", "ERBS", 20)
        );

        RxScenario scenario = scenario()
                .addFlow(
                        flow("parent")
                                .addSubFlow(flow("subFlow1")
                                        .addTestStep(print("subFlow1"))
                                        .withVUsers(3)
                                        .withDataSources(
                                                dataSource
                                                        .filterField(Node.NODE_TYPE).equalTo("ERBS")
                                                        .shared()
                                        )
                                )
                                .withDataSources(fromIterable("parentDs", asList("1", "2")))
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "filterSharedDataSourcePredictability.graphml");
    }

    @Test
    public void filterContextDataSource() throws Exception {
        final Stack<String> ids = new Stack<>();

        RxContextDataSource<Node> contextDataSource = contextDataSource("name", Node.class);

        final String targetId = "AWSM-MEE";
        RxScenario scenario = scenario()
                .addFlow(
                        flow()
                                .addTestStep(
                                        new InlineInvocation() {
                                            RxDataRecord producer(@Named("nodesToCreateIds") String id) {
                                                return getNode(id, "type", 80);
                                            }
                                        }.collectResultsToDataSource(contextDataSource)
                                )
                                .withDataSources(fromIterable("nodesToCreateIds", asList(targetId, targetId, "SGSN-14B", "SGSN-MME")))
                )
                .addFlow(
                        flow("ds")
                                .addTestStep(new InlineInvocation() {
                                    void idToStack(@Named(Node.NETWORK_ELEMENT_ID) String id) {
                                        ids.add(id);
                                    }
                                })
                                .withVUsers(2)
                                .withDataSources(
                                        contextDataSource
                                                .shared()
                                                .filterField(Node.NETWORK_ELEMENT_ID).equalTo(targetId)
                                )
                )
                .build();

        ScenarioDebugger.debug(scenario);

        assertThat(ids).containsExactly(targetId, targetId);
    }

    @Test(timeout = 10000L)
    public void cyclicDataSource() throws Exception {
        final Stack<String> usernodes = new Stack<>();

        RxDataSource<Node> nodes = fromDataRecords("nodes",
                getNode("node1", "LTE01ERB", 20),
                getNode("node2", "ERBS", 20),
                getNode("node3", "ERBS", 20),
                getNode("node4", "ERBS", 20),
                getNode("node5", "ERBS", 20)
        );

        RxDataSource<String> users = fromIterable("user",
                asList("user1", "user2", "user3")
        );

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void idToStack(@Named(Node.NETWORK_ELEMENT_ID) String id, @Named("user") String user) {
                                usernodes.add(user + "-" + id);
                            }
                        })
                        .withVUsers(3)
                        .withDataSources(
                                nodes.shared(),
                                users.shared().cyclic()
                        )
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(usernodes).containsExactlyInAnyOrder("user1-node1", "user2-node2", "user3-node3", "user1-node4", "user2-node5");
    }

    @Test
    public void filteredDataSourceSize() throws Exception {
        String DS_NAME = "users";

        RxDataSource<String> users = fromIterable(DS_NAME,
                asList("user1", "user2", "user3", "admin1", "admin2")
        ).filterField(DS_NAME).contains("user");

        assertThat(users.getSize()).isEqualTo(3);
        assertThat(users).hasSize(3);
    }

    @Test(timeout = 10000L)
    public void cyclicDataSourceSize() throws Exception {
        RxDataSource<String> users = fromIterable("user",
                asList("user1", "user2", "user3")
        );

        assertThat(users.cyclic().getSize()).isEqualTo(users.getSize());
    }

    @Test
    public void dataRecordTest() throws Exception {
        final Stack<String> ids = new Stack<>();

        RxScenario scenario = scenario()
                .addFlow(
                        flow()
                                .addTestStep(
                                        new InlineInvocation() {
                                            void producer(@Named("nodes") Node node) {
                                                ids.push(node.getNetworkElementId());
                                                ids.push(node.getAllFields().get(Node.NETWORK_ELEMENT_ID).toString());
                                                ids.push(node.getFieldValue(Node.NETWORK_ELEMENT_ID).toString());
                                            }
                                        }
                                )
                                .withDataSources(getNodeDataSource())
                )
                .build();

        runner().build().run(scenario);

        assertThat(ids).containsExactly(
                "SGSN-14B", "SGSN-14B", "SGSN-14B",
                "SGSN-MME", "SGSN-MME", "SGSN-MME");
    }
}

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

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.impl.Api.fromIterable;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.impl.ScenarioDebugger;
import com.ericsson.de.scenariorx.impl.ScenarioTest;
import com.ericsson.de.scenariorx.impl.graph.ScenarioExecutionGraph;
import org.junit.Test;

public class ExceptionHandlingTest extends ScenarioTest {

    @Test
    public void exceptionInSharedDataSources() throws Exception {
        List<String> dataSource = newArrayList("1", "2", "3", "4", "5", "6", "7");
        List<String> subFlowDataSource = newArrayList("a", "b", "c");
        List<String> subSubFlowDataSource = newArrayList("x", "o", "+");

        RxScenario scenario = scenario()
                .withParameter(STORE_V_USERS_IN_CONTEXT, true)
                .addFlow(flow()
                        .addTestStep(print("flowStep1"))
                        .addTestStep(print("flowStep2"))
                        .addSubFlow(flow()
                                .addTestStep(new ThrowException("subFlowDataSource", "b", "1.1.2"))
                                .addTestStep(print("subFlowStepAfterException"))
                                .addTestStep(print("subFlowStepAlwaysRun").alwaysRun())
                                .addSubFlow(flow()
                                        .addTestStep(print("subSubFlow1"))
                                        .withVUsers(2)
                                        .withDataSources(fromIterable("subSubFlowDataSource", subSubFlowDataSource).shared())
                                )
                                .withVUsers(2)
                                .withDataSources(fromIterable("subFlowDataSource", subFlowDataSource).shared())
                        )
                        .addTestStep(print("flowStep3"))
                        .addTestStep(print("subFlowStepAlwaysRun").alwaysRun())
                        .withVUsers(3)
                        .withDataSources(fromIterable("dataSource", dataSource).shared())
                )
                .build();

        ScenarioExecutionGraph graph = ScenarioDebugger.debug(scenario);

        compareGraphs(graph, "exceptionInSharedDataSources.graphml");

    }

    @Test
    public void exceptionInRegularDataSources() throws Exception {
        List<String> dataSource = newArrayList("1", "2");
        List<String> subFlowDataSource = newArrayList("a", "b", "c");

        RxScenario scenario = scenario()
                .withParameter(STORE_V_USERS_IN_CONTEXT, true)
                .addFlow(flow()
                        .addTestStep(print("flowStep1"))
                        .addTestStep(print("flowStep2"))
                        .addSubFlow(flow()
                                .addTestStep(new ThrowException("subFlowDataSource", "b", "1.1.1"))
                                .addTestStep(print("subFlowStepAfterException"))
                                .addTestStep(print("subFlowStepAlwaysRun").alwaysRun())
                                .withVUsers(2)
                                .withDataSources(fromIterable("subFlowDataSource", subFlowDataSource))
                        )
                        .addTestStep(print("flowStep3"))
                        .addTestStep(print("subFlowStepAlwaysRun").alwaysRun())
                        .withVUsers(2)
                        .withDataSources(fromIterable("dataSource", dataSource))
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "exceptionInRegularDataSources.graphml");
    }

    @Test
    public void testStepsSkippedWhenExceptionHandled() throws Exception {
        String flowDSName = "flowDataSource";
        String subFlowDSName = "subFlowDataSource";
        List<String> flowDataSource = newArrayList("1", "2");
        List<String> subFlowDataSource = newArrayList("a", "b", "c", "d");

        RxScenario scenario = scenario()
                .withParameter(STORE_V_USERS_IN_CONTEXT, true)
                .addFlow(flow()
                        .addSubFlow(flow()
                                .addTestStep(new ThrowException(subFlowDSName, "a", "1.1.1"))
                                .addTestStep(new ThrowException(subFlowDSName, "b", "1.1.2"))
                                .addTestStep(new ThrowException(subFlowDSName, "c", "1.2.1"))
                                .addTestStep(new ThrowException(subFlowDSName, "d", "1.2.2"))
                                .withExceptionHandler(RxExceptionHandler.IGNORE)
                                .withDataSources(fromIterable(subFlowDSName, subFlowDataSource).shared())
                                .withVUsers(2)
                        )
                        .withDataSources(fromIterable(flowDSName, flowDataSource).shared())
                        .withVUsers(2)
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "testStepsSkippedWhenExceptionHandled.graphml");
    }
}

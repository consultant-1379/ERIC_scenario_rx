package com.ericsson.de.scenariorx.impl;
/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.List;
import java.util.Set;

import com.ericsson.de.scenariorx.api.DebugGraphMode;
import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.impl.graph.ScenarioExecutionGraph;
import com.ericsson.de.scenariorx.impl.graph.export.GraphExporter;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

/**
 * Class which contains the base functionality for running scenario and creating execution graph
 * @see com.ericsson.de.scenariorx.api.RxScenarioRunner
 * @see com.ericsson.de.scenariorx.impl.ScenarioDebugger
 */
public abstract class ScenarioRunner {

    private final DebugGraphMode debugGraphMode;
    private final List<ScenarioListener> listeners;
    private final ExceptionHandler defaultExceptionHandler;

    protected ScenarioRunner(DebugGraphMode debugGraphMode,
                             List<ScenarioListener> listeners,
                             RxExceptionHandler defaultExceptionHandler) {
        this.debugGraphMode = debugGraphMode;
        this.listeners = listeners;
        this.defaultExceptionHandler = defaultExceptionHandler;
    }

    protected void run(Scenario scenario) {
        ScenarioEventBus eventBus = new ScenarioEventBus(concat(listeners, scenario.listeners));
        FlowExecutionContext context = FlowExecutionContext.createScenarioFlowContext(
                scenario, eventBus, defaultExceptionHandler
        );

        eventBus.scenarioStarted(scenario);
        List<Internals.FlowExecutionResult> results = Implementation.runFlow(context).toList().toBlocking().single();
        eventBus.scenarioFinished(scenario, results);

        Throwable error = composeError(results);
        if (error != null && defaultExceptionHandler.cannotHandle(error)) {
            createGraph(results, DebugGraphMode.SVG, scenario.name);
            Throwables.throwIfUnchecked(error);
            throw new RuntimeException(error);
        } else if (!DebugGraphMode.NONE.equals(debugGraphMode)) {
            createGraph(results, debugGraphMode, scenario.name);
        }
    }

    List<Internals.TestStepResult> runPerformance(PerformanceFlowBuilder builder) {
        RxFlow flow = builder.build();

        ScenarioEventBus eventBus = new ScenarioEventBus(listeners);
        Scenario scenario = new RxScenario(flow.getName(), Maps.<String, Object>newHashMap(), flow, listeners);
        FlowExecutionContext flowExecutionContext = FlowExecutionContext.createScenarioFlowContext(scenario, eventBus, RxExceptionHandler.PROPAGATE);

        eventBus.scenarioStarted(scenario);
        List<Internals.TestStepResult> testStepResults = Implementation.runFlowPerformance(flowExecutionContext, builder.getRampUp());
        eventBus.scenarioFinished(scenario, null);
        return testStepResults;
    }

    private Throwable composeError(List<Internals.FlowExecutionResult> results) {
        Set<Throwable> throwables = newLinkedHashSet();
        for (Internals.FlowExecutionResult result : results) {
            throwables.add(result.error);
        }
        return DataSourceRx.compose(throwables);
    }

    ScenarioExecutionGraph createGraph(List<Internals.FlowExecutionResult> results,
                                       DebugGraphMode mode, String graphName) {
        ScenarioExecutionGraph graph = GraphBuilder.build(results);
        for (GraphExporter exporter : mode.getExporters()) {
            exporter.export(graph, graphName);
        }
        return graph;
    }
}

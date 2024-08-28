package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.impl.FlowExecutionContext.createScenarioFlowContext;
import static com.ericsson.de.scenariorx.impl.Implementation.runFlow;
import static com.google.common.collect.Iterators.forArray;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.ericsson.de.scenariorx.api.DebugGraphMode;
import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.impl.Internals.FlowExecutionResult;
import com.ericsson.de.scenariorx.impl.graph.ScenarioExecutionGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScenarioDebugger extends ScenarioRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioRunner.class);

    protected ScenarioDebugger() {
        super(DebugGraphMode.NONE, Collections.<ScenarioListener>emptyList(), RxExceptionHandler.PROPAGATE);
    }

    /**
     * Debug given scenario.
     * Will not fail Test if Scenario produces exception.
     * Used for creating graph for debugging purposes.
     */
    public static ScenarioExecutionGraph debug(Scenario scenario) {
        return new ScenarioDebugger().doDebug(scenario);
    }

    private ScenarioExecutionGraph doDebug(Scenario scenario) {
        ScenarioEventBus eventBus = mock(ScenarioEventBus.class);
        FlowExecutionContext context = createScenarioFlowContext(scenario, eventBus, RxExceptionHandler.PROPAGATE);
        List<FlowExecutionResult> results = runFlow(context).toList().toBlocking().single();

        for (FlowExecutionResult result : results) {
            if (result.isFailed()) {
                logger.info("Scenario produced errors in debug mode: ", result.error);
            }
        }

        return createGraph(results, DebugGraphMode.ALL, graphName());
    }

    private String graphName() {
        Iterator<StackTraceElement> elements = forArray(new Exception().getStackTrace());
        StackTraceElement element;
        do {
            element = elements.next();
        } while (element.getClassName().equals(ScenarioDebugger.class.getName()));
        return element.getMethodName();
    }
}

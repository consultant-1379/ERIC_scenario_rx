package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.api.RxScenarioRunnerBuilder.DEBUG_GRAPH_MODE;
import static com.ericsson.de.scenariorx.api.RxScenarioRunnerBuilder.DEBUG_LOG_ENABLED;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;

import com.ericsson.de.scenariorx.api.Builder;
import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxFlowBuilder;
import com.ericsson.de.scenariorx.api.RxScenarioBuilderInterfaces;
import com.ericsson.de.scenariorx.api.RxScenarioRunnerBuilder;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.ericsson.de.scenariorx.impl.Internals.Fork;
import com.google.common.collect.Lists;

public abstract class ScenarioBuilder<S extends Scenario, F extends Flow> implements RxScenarioBuilderInterfaces.ScenarioBuilderStates<S, F>, Builder<S> {

    protected static final String DEFAULT_NAME = "Scenario";

    static final String ERROR_DEBUG_LOG_ENABLED = "Parameter '" + DEBUG_LOG_ENABLED +
            "' is reserved, use RxApi.runner().withDebugLogEnabled() instead";
    static final String ERROR_DEBUG_GRAPH_MODE = "Parameter '" + DEBUG_GRAPH_MODE +
            "' is reserved, use RxApi.runner().withGraphExportMode(...) instead";

    private static final String ERROR_UNIQUENESS_TEMPLATE = "%1$ss can not be reused within one scenario. " +
            "Please create new %1$s. You may extract %1$s creation to method if you need identical %1$ss";
    static final String ERROR_TEST_STEP_UNIQUENESS = format(ERROR_UNIQUENESS_TEMPLATE, "Test Step");
    static final String ERROR_FLOW_UNIQUENESS = format(ERROR_UNIQUENESS_TEMPLATE, "Flow");


    private final String name;
    protected final FlowBuilder<F> scenarioFlowBuilder;
    private final Map<String, Object> parameters = newLinkedHashMap();
    protected final List<ScenarioListener> listeners = Lists.newArrayList();

    protected ScenarioBuilder(String name, FlowBuilder<F> scenarioFlowBuilder) {
        this.name = name;
        this.scenarioFlowBuilder = scenarioFlowBuilder;
    }

    /**
     * @see #addFlow(Flow)
     */
    @Override
    public RxScenarioBuilderInterfaces.Flows<S, F> addFlow(Builder<F> flowBuilder) {
        scenarioFlowBuilder.addSubFlow(flowBuilder);
        return this;
    }

    /**
     * Adds test step flow to the current scenario.
     *
     * @param flow flow to add
     * @return builder
     */
    @Override
    public RxScenarioBuilderInterfaces.Flows<S, F> addFlow(F flow) {
        scenarioFlowBuilder.addSubFlow(flow);
        return this;
    }

    /**
     * @see #split(Flow...)
     */
    @Override
    public RxScenarioBuilderInterfaces.Flows<S, F> split(Builder<F>... subFlows) {
        scenarioFlowBuilder.split(subFlows);
        return this;
    }

    /**
     * Execute flows passed in param in parallel
     *
     * @param subFlows to execute in parallel
     * @return builder
     */
    @Override
    public RxScenarioBuilderInterfaces.Flows<S, F> split(F... subFlows) {
        scenarioFlowBuilder.split(subFlows);
        return this;
    }

    /**
     * @see FlowBuilder#alwaysRun()
     */
    @Override
    public RxScenarioBuilderInterfaces.AlwaysRun<S, F> alwaysRun() {
        scenarioFlowBuilder.alwaysRun();
        return this;
    }

    /**
     * Add parameter which will be available to all Test Steps of Scenario if not overridden
     * by {@link RxTestStep#withParameter(String)}
     *
     * @return builder
     */
    @Override
    public RxScenarioBuilderInterfaces.ScenarioStart<S, F> withParameter(String key, Object value) {
        checkNotNull(key, RxTestStep.ERROR_PARAMETER_NULL);
        checkState(!parameters.containsKey(key), RxTestStep.ERROR_PARAMETER_ALREADY_SET, key);
        checkArgument(!key.equals(DEBUG_LOG_ENABLED), ERROR_DEBUG_LOG_ENABLED);
        checkArgument(!key.equals(DEBUG_GRAPH_MODE), ERROR_DEBUG_GRAPH_MODE);
        parameters.put(key, value);
        return this;
    }

    /**
     * Set an exception handler for the Scenario, which will be called on exceptions in Test Steps.
     * If the exception handler does not propagate Exception, scenario flow will continue.
     *
     * @see RxScenarioRunnerBuilder#withDefaultExceptionHandler(RxExceptionHandler)
     * @see RxFlowBuilder#withExceptionHandler(RxExceptionHandler)
     */
    @Override
    public RxScenarioBuilderInterfaces.ExceptionHandler<S> withExceptionHandler(RxExceptionHandler exceptionHandler) {
        scenarioFlowBuilder.withExceptionHandler(exceptionHandler);
        return this;
    }

    /**
     * @return Scenario
     */
    @Override
    public S build() {
        F scenarioFlow = scenarioFlowBuilder.build();
        assignUniqueScenarioIds(scenarioFlow, 1);
        //noinspection unchecked
        return createScenario(name, parameters, scenarioFlow);
    }

    protected abstract S createScenario(String name, Map<String, Object> parameters, F flow);

    private long assignUniqueScenarioIds(Flow flow, long startScenarioId) {
        checkArgument(flow.getId() == null, ERROR_FLOW_UNIQUENESS);
        long scenarioId = startScenarioId;
        flow.id = scenarioId++;

        for (Invocation invocation : concat(flow.getBefore(), flow.testSteps, flow.getAfter())) {
            if (invocation instanceof RxTestStep) {
                checkArgument(invocation.getId() == null, ERROR_TEST_STEP_UNIQUENESS);
                invocation.id = scenarioId++;
            } else if (invocation instanceof Fork) {
                Fork fork = (Fork) invocation;
                for (Flow subFlow : fork.flows) {
                    scenarioId = assignUniqueScenarioIds(subFlow, scenarioId);
                }
            }
        }
        return scenarioId;
    }
}

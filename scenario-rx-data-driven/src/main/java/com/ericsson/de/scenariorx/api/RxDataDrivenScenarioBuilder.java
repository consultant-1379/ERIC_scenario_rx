package com.ericsson.de.scenariorx.api;

import java.util.Map;

import com.ericsson.de.scenariorx.impl.DataDrivenScenarioBuilder;
import com.ericsson.de.scenariorx.impl.FlowBuilder;
import com.ericsson.de.scenariorx.impl.ScenarioBuilder;

public class RxDataDrivenScenarioBuilder extends DataDrivenScenarioBuilder implements RxDataDrivenScenarioBuilderInterfaces.ScenarioBuilderStates<RxScenario, RxFlow> {

    private RxDataDrivenScenarioBuilder(String name) {
        super(name, (FlowBuilder<RxFlow>) RxFlowBuilder.flow(name));
    }

    static RxDataDrivenScenarioBuilderInterfaces.ScenarioStart<RxScenario, RxFlow> scenario() {
        return scenario(ScenarioBuilder.DEFAULT_NAME);
    }

    static RxDataDrivenScenarioBuilderInterfaces.ScenarioStart<RxScenario, RxFlow> scenario(String name) {
        return new RxDataDrivenScenarioBuilder(name);
    }

    @Override
    protected RxScenario createScenario(String name, Map<String, Object> parameters, RxFlow flow) {
        return new RxScenario(name, parameters, flow, listeners);
    }

    @Override
    public RxDataDrivenScenarioBuilderInterfaces.Flows<RxScenario, RxFlow> addFlow(Builder<RxFlow> flowBuilder) {
        super.addFlow(flowBuilder);
        return this;
    }

    @Override
    public RxDataDrivenScenarioBuilderInterfaces.Flows<RxScenario, RxFlow> addFlow(RxFlow flow) {
        super.addFlow(flow);
        return this;
    }

    @Override
    public RxDataDrivenScenarioBuilderInterfaces.Flows<RxScenario, RxFlow> split(Builder<RxFlow>[] subFlows) {
        super.split(subFlows);
        return this;
    }

    @Override
    public RxDataDrivenScenarioBuilderInterfaces.Flows<RxScenario, RxFlow> split(RxFlow[] subFlows) {
        super.split(subFlows);
        return this;
    }

    @Override
    public RxDataDrivenScenarioBuilderInterfaces.AlwaysRun<RxScenario, RxFlow> alwaysRun() {
        super.alwaysRun();
        return this;
    }

    /**
     * Adds Data Sources to scenario. Each Data Record will appear as new Test Case in reporting. At least one data
     * source should contain field with name `testCaseId` which will be used as Test Case Id in reporting.
     *
     * @param dataSources array of data sources to add
     * @return builder
     */
    @Override
    public RxDataDrivenScenarioBuilderInterfaces.ExceptionHandler<RxScenario> withScenarioDataSources(RxDataSource... dataSources) {
        scenarioFlowBuilder.withDataSources(dataSources);
        return this;
    }

    /**
     * Split Data Source (set with {@link #withScenarioDataSources(RxDataSource...)}) between multiple
     * vUsers (threads) and run them in parallel.
     * <p>
     * Used to speed up Data Source processing.
     *
     * @param vUsers number of vUsers (threads) running in parallel
     * @return builder
     *
     * @see RxDataDrivenScenarioBuilder#runParallelAuto()
     */
    @Override
    public RxDataDrivenScenarioBuilderInterfaces.ExceptionHandler<RxScenario> runParallel(int vUsers) {
        scenarioFlowBuilder.withVUsers(vUsers);
        return this;
    }

    /**
     * Split Data Source (set with {@link #withScenarioDataSources(RxDataSource...)}) between multiple
     * vUsers (threads) and run them in parallel. Automatically determine the number of vUsers.
     *
     * @return builder
     */
    @Override
    public RxDataDrivenScenarioBuilderInterfaces.ExceptionHandler<RxScenario> runParallelAuto() {
        scenarioFlowBuilder.withVUsersAuto();
        return this;
    }

    @Override
    public RxDataDrivenScenarioBuilderInterfaces.ExceptionHandler<RxScenario> withExceptionHandler(RxExceptionHandler exceptionHandler) {
        super.withExceptionHandler(exceptionHandler);
        return this;
    }

    @Override
    public RxScenario build() {
        return super.build();
    }

}

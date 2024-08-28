package com.ericsson.de.scenariorx.api;

import com.ericsson.de.scenariorx.impl.Api;

/**
 * Utility class that provides access to all API necessary for Scenario creation and execution.
 * Class can be extended to provide custom Scenario functionality.
 */
public abstract class RxApi extends Api {

    /**
     * Creates Builder for flow
     *
     * @return builder
     */
    public static RxFlowBuilderInterfaces.FlowStart<RxFlow> flow() {
        return RxFlowBuilder.flow();
    }

    /**
     * Creates Builder for flow with given name
     *
     * @return builder
     */
    public static RxFlowBuilderInterfaces.FlowStart<RxFlow> flow(String name) {
        return RxFlowBuilder.flow(name);
    }

    /**
     * Creates builder for Scenario
     *
     * @return builder
     */
    public static RxScenarioBuilderInterfaces.ScenarioStart<RxScenario, RxFlow> scenario() {
        return RxScenarioBuilder.scenario();
    }

    /**
     * Creates builder for Scenario with given name
     *
     * @return builder
     */
    public static RxScenarioBuilderInterfaces.ScenarioStart<RxScenario, RxFlow> scenario(String name) {
        return RxScenarioBuilder.scenario(name);
    }

    /**
     * Creates Data Source that could be populated from Test Step return values
     *
     * @param name name of the Data Source
     * @return definition
     * @see RxTestStep#collectResultsToDataSource(RxContextDataSource)
     */
    public static <T> RxContextDataSource<T> contextDataSource(final String name, Class<T> type) {
        return new RxContextDataSource<>(name, type);
    }

    /**
     * Creates builder for Scenario Runner
     **/
    public static RxScenarioRunnerBuilder runner() {
        return new RxScenarioRunnerBuilder();
    }

    /**
     * Run given scenario.
     * In case you need to configure, use {@link #runner()}
     */
    public static void run(RxScenario scenario) {
        runner().build().run(scenario);
    }


}

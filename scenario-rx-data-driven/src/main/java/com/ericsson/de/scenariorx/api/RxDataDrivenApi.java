package com.ericsson.de.scenariorx.api;


public class RxDataDrivenApi extends RxApi {

    public static final String TEST_CASE_ID = "testCaseId";
    public static final String TEST_SUITE_ID = "testSuiteId";
    public static final String ERROR_MISSING_TEST_ID = "Missing test ID in Data Source";

    /**
     * @see RxDataDrivenApi#dataDrivenScenario(java.lang.String)
     */
    public static RxDataDrivenScenarioBuilderInterfaces.ScenarioStart<RxScenario, RxFlow> dataDrivenScenario() {
        return RxDataDrivenScenarioBuilder.scenario();
    }

    /**
     * Creates builder for Scenario with Data Source, each Data Record will appear in reporting as new Test Case
     *
     * @param name name of Scenario
     * @return builder
     * @see RxDataDrivenScenarioBuilder#withScenarioDataSources
     */
    public static RxDataDrivenScenarioBuilderInterfaces.ScenarioStart<RxScenario, RxFlow> dataDrivenScenario(String name) {
        return RxDataDrivenScenarioBuilder.scenario(name);
    }
}

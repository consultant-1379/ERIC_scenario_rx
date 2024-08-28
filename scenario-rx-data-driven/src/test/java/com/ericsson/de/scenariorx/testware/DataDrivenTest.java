package com.ericsson.de.scenariorx.testware;

import static com.ericsson.cifwk.taf.scenario.ext.exporter.ScenarioExecutionGraphListenerBuilder.TAF_SCENARIO_DEBUG_ENABLED;
import static com.ericsson.de.scenariorx.api.RxApi.flow;
import static com.ericsson.de.scenariorx.api.RxApi.runner;
import static com.ericsson.de.scenariorx.impl.Api.fromDataRecords;
import static com.ericsson.de.scenariorx.testware.DataDrivenScenarioIntegrationTest.PARALLEL_THREADS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.cifwk.taf.annotations.TestSuite;
import com.ericsson.cifwk.taf.configuration.TafConfigurationProvider;
import com.ericsson.de.scenariorx.api.RxBasicDataRecord;
import com.ericsson.de.scenariorx.api.RxDataDrivenApi;
import com.ericsson.de.scenariorx.api.RxDataDrivenScenarioBuilderInterfaces;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.TafRxScenarios;
import com.google.common.collect.Maps;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class DataDrivenTest {

    public static final String TEST_SUITE_1 = "Simple data driven scenario";
    public static final String TEST_SUITE_2 = "Parallel/subflow data driven scenario";
    public static final String TEST_SUITE_3 = "scenario with unhandled exceptions";
    public static final String TEST_SUITE_4 = "scenario with subflow unhandled exceptions";
    public static final String TEST_SUITE_5 = "scenario with parallel unhandled exceptions";
    public static final String TEST_SUITE_6 = "handled exceptions in subflow";
    public static final String TEST_SUITE_7 = "assertions";
    public static final String TEST_SUITE_8 = "data driven graph attachemnts";
    public static final String TEST_SUITE_9 = "allure report generated when incorrect datasource used";
    public static final String TEST_SUITE_10 = "allure report generated when DataSourceDefinition.provideIterator throws exception";

    public static final String TEST_STEP_1 = "ts1";
    public static final String TEST_STEP_2 = "ts2";
    public static final String TEST_STEP_3 = "ts3";
    public static final String TEST_STEP_4 = "ts4";
    public static final String TEST_STEP_THROW_EXCEPTION = "throwException";
    public static final String TEST_STEP_ASSERTION_FAILED = "testStepAssertionFailed";

    public static final String TEST_CASE_1 = "testCase1";
    public static final String TEST_CASE_2 = "testCase2";
    public static final String TEST_CASE_3 = "testCase3";

    public static final String STRING_INPUT = "string";
    private static final String GLOBAL_DATA_SOURCE = "globalDataSource";

    private Integer parallelThreads;
    private RxDataSource<RxDataRecord> globalDataSource;

    @BeforeClass
    public void prepareGlobalDataSource() {
        globalDataSource = fromDataRecords(GLOBAL_DATA_SOURCE,
                RxBasicDataRecord.builder()
                        .setField(RxDataDrivenApi.TEST_CASE_ID, TEST_CASE_1)
                        .setField("integer", 1)
                        .setField(STRING_INPUT, "A")
                        .build(),
                RxBasicDataRecord.builder()
                        .setField(RxDataDrivenApi.TEST_CASE_ID, TEST_CASE_2)
                        .setField("integer", 2)
                        .setField(STRING_INPUT, "B")
                        .build());

        parallelThreads = TafConfigurationProvider.provide().getProperty(PARALLEL_THREADS, 1, Integer.class);

        if (parallelThreads > 1) {
            globalDataSource = globalDataSource.shared();
        }
    }

    @Test
    @TestSuite(TEST_SUITE_1)
    public void simpleScenario() {

        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario()
                .addFlow(flow("A")
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2))
                )
                .withScenarioDataSources(globalDataSource)
                .runParallel(parallelThreads)
                .build();
        runner().build().run(scenario);
    }

    @Test
    @TestSuite(TEST_SUITE_2)
    public void subflowAndParallel() {
        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario()
                .addFlow(flow("A")
                        .addSubFlow(

                                flow("subFlow")
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1)))
                        .split(
                                flow("parallel")
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2)),
                                flow("parallel")
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_3)))

                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_4))
                )
                .withScenarioDataSources(globalDataSource)
                .runParallel(parallelThreads)
                .build();
        runner().build().run(scenario);
    }

    @Test
    @TestSuite(TEST_SUITE_3)
    public void unhandledExceptionInStep() {
        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario()
                .addFlow(flow("A")
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_THROW_EXCEPTION))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_3).alwaysRun())

                )
                .withScenarioDataSources(globalDataSource)
                .runParallel(parallelThreads)
                .build();
        runner().build().run(scenario);
    }

    @Test
    @TestSuite(TEST_SUITE_4)
    public void unhandledExceptionInSubflow() {
        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario()
                .addFlow(flow("A")
                        .addSubFlow(

                                flow("subFlow")
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1))
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_THROW_EXCEPTION))
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2)))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_3)
                        ))
                .withScenarioDataSources(globalDataSource)
                .runParallel(parallelThreads)
                .build();

        runner().build().run(scenario);
    }

    @Test
    @TestSuite(TEST_SUITE_5)
    public void unhandledExceptionInParallel() {
        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario()
                .addFlow(flow("A")
                        .split(
                                flow("parallel")
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1))
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_THROW_EXCEPTION))
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2)),
                                flow("parallel")
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_3)))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_4))
                )
                .withScenarioDataSources(globalDataSource)
                .runParallel(parallelThreads)
                .build();
        runner().build().run(scenario);
    }

    @Test
    @TestSuite(TEST_SUITE_6)
    public void handledExceptionInSubflow() {
        RxDataDrivenScenarioBuilderInterfaces.Flows<RxScenario, RxFlow> rxScenarioRxFlowFlows = RxDataDrivenApi.dataDrivenScenario()
                .addFlow(flow("A")
                        .addSubFlow(
                                flow("subFlow")
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1))
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_THROW_EXCEPTION))
                                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2)))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_3)
                        ));

        RxScenario scenario = rxScenarioRxFlowFlows
                .withScenarioDataSources(globalDataSource.filterField(RxDataDrivenApi.TEST_CASE_ID).equalTo(TEST_CASE_1))
                .runParallel(parallelThreads)
                .build();
        runner()
                .withDefaultExceptionHandler(RxExceptionHandler.IGNORE)
                .build().run(scenario);
    }

    @Test
    @TestSuite(TEST_SUITE_7)
    public void assertionInStep() {

        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario()
                .addFlow(flow("A")
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_ASSERTION_FAILED))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2))
                )
                .withScenarioDataSources(globalDataSource)
                .runParallel(parallelThreads)
                .build();
        runner().build().run(scenario);
    }

    @Test
    @TestSuite(TEST_SUITE_8)
    public void graphAttachments() {
        System.setProperty(TAF_SCENARIO_DEBUG_ENABLED, "true");
        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario("testAttachments")
                .addFlow(flow("A")
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2))
                )
                .withScenarioDataSources(globalDataSource.filterField(RxDataDrivenApi.TEST_CASE_ID).equalTo(TEST_CASE_1))
                .runParallel(parallelThreads)
                .build();
        runner().build().run(scenario);
    }

    @Test
    @TestSuite
    public void shouldAddTestCasesToRootSuiteIfSuiteNameIsNotSet() {
        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario()
                .addFlow(flow("A")
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2))
                )
                .withScenarioDataSources(globalDataSource)
                .runParallel(parallelThreads)
                .build();
        runner().build().run(scenario);
    }

    @Test
    @TestSuite(TEST_SUITE_9)
    public void incorrectDatasource() {
        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario("testIncorrectDatasource")
                .addFlow(flow("A")
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2))
                )
                .withScenarioDataSources(fromDataRecords("incorrectDs",
                        RxBasicDataRecord.builder()
                                .setField(RxDataDrivenApi.TEST_CASE_ID, TEST_CASE_3)
                                .setField("integer", 1)
                                .setField(STRING_INPUT, "A")
                                .build(),
                        new ThrowingDataRecord()))
                .build();
        runner().build().run(scenario);
    }

    @Test
    @TestSuite(TEST_SUITE_10)
    public void provideIteratorTrowsException() {
        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario("testIncorrectDatasource")
                .addFlow(flow("A")
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_1))
                        .addTestStep(TafRxScenarios.annotatedMethod(this, TEST_STEP_2))
                )
                .withScenarioDataSources(fromDataRecords("throwing", new ThrowingDataRecord()))
                .build();

        runner().build().run(scenario);
    }

    @Test
    public void _dummy() {
        // @BeforeClass test step is attached to this test case which is run at the very beginning
    }

    @TestStep(id = TEST_STEP_1)
    public void testStep1(@Input(DataDrivenTest.STRING_INPUT) String string) {
    }

    @TestStep(id = TEST_STEP_2)
    public void testStep2() {
    }

    @TestStep(id = TEST_STEP_3)
    public void testStep3() {
    }

    @TestStep(id = TEST_STEP_4)
    public void testStep4() {
    }

    @TestStep(id = TEST_STEP_THROW_EXCEPTION)
    public void throwException() {
        throw new AllureTestUtils.VerySpecialException();
    }

    @TestStep(id = TEST_STEP_ASSERTION_FAILED)
    public void failAssert() {
        assertThat("using_aspects").isEqualTo("good_idea");
    }


    private class ThrowingDataRecord implements RxDataRecord {
        @Override
        public <T> T getFieldValue(String name) {
            throw new RuntimeException("datasource threw exception");
        }

        @Override
        public Map<String, Object> getAllFields() {
            Map<String, Object> map = Maps.newHashMap();
            map.put(RxDataDrivenApi.TEST_CASE_ID, "tc1");
            return map;
        }
    }
}

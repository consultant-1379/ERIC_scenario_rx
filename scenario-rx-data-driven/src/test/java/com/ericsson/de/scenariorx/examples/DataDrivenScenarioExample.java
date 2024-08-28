package com.ericsson.de.scenariorx.examples;

import static com.ericsson.de.scenariorx.api.RxApi.flow;
import static com.ericsson.de.scenariorx.api.RxApi.runner;
import static com.ericsson.de.scenariorx.impl.Api.fromDataRecords;

import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.cifwk.taf.annotations.TestSuite;
import com.ericsson.de.scenariorx.api.RxBasicDataRecord;
import com.ericsson.de.scenariorx.api.RxDataDrivenApi;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.TafRxScenarios;
import com.ericsson.de.scenariorx.testware.DataDrivenTest;
import org.junit.Before;
import org.junit.Test;


public class DataDrivenScenarioExample {

    private RxDataSource<RxDataRecord> dataSource;

    @Before
    public void setUp() throws Exception {
        dataSource = fromDataRecords("ds",
                RxBasicDataRecord.builder()
                        .setField(RxDataDrivenApi.TEST_CASE_ID, DataDrivenTest.TEST_CASE_1)
                        .setField("integer", 1)
                        .setField(DataDrivenTest.STRING_INPUT, "A")
                        .build(),
                RxBasicDataRecord.builder()
                        .setField(RxDataDrivenApi.TEST_CASE_ID, DataDrivenTest.TEST_CASE_2)
                        .setField("integer", 2)
                        .setField(DataDrivenTest.STRING_INPUT, "B")
                        .build());

    }

    // START SNIPPET: SIMPLE_DATA_DRIVEN_SCENARIO
    @Test
    @TestSuite(DataDrivenTest.TEST_SUITE_1)
    public void simpleScenario() {

        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario("Scenario 1")
                .addFlow(
                        flow("login1")
                                .addTestStep(TafRxScenarios.annotatedMethod(this, DataDrivenTest.TEST_STEP_1))
                                .addTestStep(TafRxScenarios.annotatedMethod(this, DataDrivenTest.TEST_STEP_2)))
                .withScenarioDataSources(dataSource)
                .build();

        runner().build().run(scenario);
    }
    // END SNIPPET: SIMPLE_DATA_DRIVEN_SCENARIO


    @Test
    @TestSuite(DataDrivenTest.TEST_SUITE_2)
    public void multipleFlows() {

        // START SNIPPET: DATA_DRIVEN_MULTIPLE_FLOWS
        RxScenario scenario = RxDataDrivenApi.dataDrivenScenario("Scenario 1")
                .addFlow(
                        flow("login1")
                                .addTestStep(TafRxScenarios.annotatedMethod(this, DataDrivenTest.TEST_STEP_1)))
                .addFlow(
                        flow("login2")
                                .addTestStep(TafRxScenarios.annotatedMethod(this, DataDrivenTest.TEST_STEP_2)))
                .withScenarioDataSources(dataSource)
                .build();
        // END SNIPPET: DATA_DRIVEN_MULTIPLE_FLOWS
    }

    @TestStep(id = DataDrivenTest.TEST_STEP_1)
    public void testStep1(@Input(DataDrivenTest.STRING_INPUT) String string) {
    }

    @TestStep(id = DataDrivenTest.TEST_STEP_2)
    public void testStep2() {
    }
}

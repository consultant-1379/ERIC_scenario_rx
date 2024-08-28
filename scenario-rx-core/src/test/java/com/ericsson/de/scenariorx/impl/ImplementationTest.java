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

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.impl.Api.runnable;
import static com.ericsson.de.scenariorx.impl.FlowExecutionContext.createScenarioFlowContext;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ericsson.de.scenariorx.api.RxBasicDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.ericsson.de.scenariorx.impl.Internals.Chunk;
import com.ericsson.de.scenariorx.impl.Internals.Exec;
import com.ericsson.de.scenariorx.impl.Internals.InternalScenarioContext;
import com.ericsson.de.scenariorx.impl.Internals.TestStepResult;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Test;

public class ImplementationTest extends ScenarioTest {

    private Scenario scenario = scenario().addFlow(flow().addTestStep(nop())).build();
    private FlowExecutionContext flowContext = createScenarioFlowContext(scenario, mock(ScenarioEventBus.class), RxExceptionHandler.PROPAGATE);

    @Test
    public void runInParallel() throws Exception {
        Exec parent = Exec.rootExec(Maps.<String, Object>newHashMap());
        List<Exec> execs = asList(
                parent.child("flow", 1, getDataRecords("ds_name", "a")),
                parent.child("flow", 2, getDataRecords("ds_name", "b")),
                parent.child("flow", 3, getDataRecords("ds_name", "c"))
        );

        Chunk chunk = new Chunk();
        RxTestStep testStep1 = runnable(sleepy("ts1"));
        RxTestStep testStep2 = runnable(sleepy("ts2"));
        RxTestStep testStep3 = runnable(sleepy("ts3"));
        chunk.testSteps.addAll(asList(testStep1, testStep2, testStep3));

        Stopwatch timer = Stopwatch.createStarted();
        Implementation.runInParallel(flowContext, execs).call(chunk);
        long timeElapsed = timer.elapsed(TimeUnit.SECONDS);

        assertThat(timeElapsed).isLessThanOrEqualTo(NAP_TIME * chunk.testSteps.size() + 1);

        for (Exec exec : execs) {
            List<TestStepResult> testSteps = exec.getExecutedTestSteps();
            assertThat(testSteps.get(0).name).isEqualTo(testStep1.getName());
            assertThat(testSteps.get(0).isFailed()).isFalse();
            assertThat(testSteps.get(1).name).isEqualTo(testStep2.getName());
            assertThat(testSteps.get(1).isFailed()).isFalse();
            assertThat(testSteps.get(2).name).isEqualTo(testStep3.getName());
            assertThat(testSteps.get(2).isFailed()).isFalse();
        }
    }

    @Test
    public void runInParallel_exceptions() throws Exception {
        Exec parent = Exec.rootExec(ImmutableMap.<String, Object>of(STORE_V_USERS_IN_CONTEXT, true));
        Exec exec1 = parent.child("flow", 1, getDataRecords("ds_name", "a"));
        Exec exec2 = parent.child("flow", 2, getDataRecords("ds_name", "b"));
        Exec failingExec = parent.child("flow", 3, getDataRecords("ds_name", "c"));

        Chunk chunk = new Chunk();
        String ts1 = "ts1";
        String ts2 = "c";
        String ts3 = "ts3";
        String alwaysRun = "alwaysRun";

        ThrowException throwExceptionTs = new ThrowException("ds_name", ts2, "3");

        chunk.testSteps.addAll(asList(
                print(ts1),
                throwExceptionTs,
                print(ts3),
                print(alwaysRun).alwaysRun()));


        Implementation.runInParallel(flowContext, asList(exec1, exec2, failingExec)).call(chunk);

        for (Exec exec : asList(exec1, exec2)) {
            List<TestStepResult> testSteps = exec.getExecutedTestSteps();
            assertThat(testSteps.get(0).name).isEqualTo(ts1);
            assertThat(testSteps.get(0).isFailed()).isFalse();
            assertThat(testSteps.get(1).name).isEqualTo(throwExceptionTs.getName());
            assertThat(testSteps.get(1).isFailed()).isFalse();
            assertThat(testSteps.get(2).name).isEqualTo(ts3);
            assertThat(testSteps.get(2).isFailed()).isFalse();
            assertThat(testSteps.get(3).name).isEqualTo(alwaysRun);
            assertThat(testSteps.get(3).isFailed()).isFalse();
        }

        List<TestStepResult> testSteps = failingExec.getExecutedTestSteps();
        assertThat(testSteps.get(0).name).isEqualTo(ts1);
        assertThat(testSteps.get(0).isFailed()).isFalse();
        assertThat(testSteps.get(1).name).isEqualTo(throwExceptionTs.getName());
        assertThat(testSteps.get(1).isFailed()).isTrue();
        assertThat(testSteps.get(2).name).isEqualTo(alwaysRun);
        assertThat(testSteps.get(2).isFailed()).isFalse();
    }

    @Test
    public void testScopeOfContext() throws Exception {
        InternalScenarioContext scenarioContext = new InternalScenarioContext(Maps.<String, Object>newHashMap());
        scenarioContext.setFieldValue("scenarioParam", "scenarioParam");

        InternalScenarioContext flowContext = scenarioContext.child();
        flowContext.setFieldValue("flowParam", "beforeSubflowCreated");
        flowContext.setFieldValue("scenarioParam", "flowOverride");

        InternalScenarioContext subFlowContext = flowContext.child();
        subFlowContext.setFieldValues(RxBasicDataRecord.fromValues("subFlowParam1", "subFlowParam1", "subFlowParam2", "subFlowParam2"));

        flowContext.setFieldValue("flowParam", "afterSubflowCreated");

        assertThat(scenarioContext.values).hasSize(1);
        assertThat(scenarioContext.values).containsEntry("scenarioParam", "scenarioParam");

        assertThat(flowContext.values).hasSize(2);
        assertThat(flowContext.values).containsEntry("scenarioParam", "flowOverride");
        assertThat(flowContext.values).containsEntry("flowParam", "afterSubflowCreated");

        assertThat(subFlowContext.values).hasSize(4);
        assertThat(subFlowContext.values).containsEntry("scenarioParam", "flowOverride");
        assertThat(subFlowContext.values).containsEntry("flowParam", "beforeSubflowCreated");
        assertThat(subFlowContext.values).containsEntry("subFlowParam1", "subFlowParam1");
        assertThat(subFlowContext.values).containsEntry("subFlowParam2", "subFlowParam2");
    }

    @Test
    public void parseValueTest() throws Exception {
        InternalScenarioContext context = new InternalScenarioContext(Maps.<String, Object>newHashMap());

        context.parseValues("returnObject", "string");

        RxDataRecord dataRecord = RxBasicDataRecord.fromValues("dataRecord", "dataRecordValue");
        context.parseValues("returnDataRecord", dataRecord);

        List<RxDataRecord> dataRecords = newArrayList(
                RxBasicDataRecord.fromValues("dataRecord2", "dataRecordValue2"),
                RxBasicDataRecord.fromValues("dataRecord3", "dataRecordValue3"));

        context.parseValues("returnDataRecords", dataRecords);

        RxDataRecordWrapper records = getDataRecords("dataSource", "value");

        RxDataRecordWrapper dataRecordAndContext = context.wrapDataRecord(records);

        assertThat(input(dataRecordAndContext, "dataSource")).isEqualTo("value");

        assertThat(input(dataRecordAndContext, "returnObject")).isEqualTo("string");

        assertThat(input(dataRecordAndContext, "dataRecord")).isEqualTo("dataRecordValue");

        assertThat(input(dataRecordAndContext, "returnDataRecord", RxDataRecord.class).getFieldValue("dataRecord")).isEqualTo("dataRecordValue");

        assertThat(input(dataRecordAndContext, "dataRecord2")).isEqualTo("dataRecordValue2");
        assertThat(input(dataRecordAndContext, "dataRecord3")).isEqualTo("dataRecordValue3");
    }
}

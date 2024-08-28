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

import com.ericsson.de.scenariorx.api.RxApiImpl;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.ericsson.de.scenariorx.impl.ScenarioDebugger;
import com.ericsson.de.scenariorx.impl.ScenarioTest;
import com.ericsson.de.scenariorx.impl.graph.ScenarioExecutionGraph;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import org.junit.Ignore;
import org.junit.Test;

import javax.inject.Named;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.impl.Api.fromIterable;
import static com.ericsson.de.scenariorx.impl.Api.runnable;
import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class ScenarioFlowTest extends ScenarioTest {

    final static String REPEAT_DATASOURCE = "Repeat datasource";

    @Test
    public void runWithVUsersOnly() throws Exception {
        Counter counter = new Counter();

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(counter)
                        .withVUsers(3)
                )
                .build();

        RxApiImpl.run(scenario);

        counter.assertEqualTo(3);
    }


    static final String DATA_SOURCE = "dataSource";

    @Test
    @Ignore
    public void testRunInParallel() throws Exception {
        List<String> dataSource = newArrayList("1");
        List<String> subFlow1DataSource = newArrayList("a");
        List<String> subFlow2DataSource = newArrayList("x", "y");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(print("flowStep1"))
                        .split(
                                flow()
                                        .addTestStep(runnable(sleepy("subFlow1Step1")))
                                        .addTestStep(runnable(sleepy("subFlow1Step2")))
                                        .withDataSources(fromIterable("subFlow1DataSource", subFlow1DataSource))
                                ,
                                flow()
                                        .addTestStep(runnable(sleepy("subFlow2Step1")))
                                        .addTestStep(runnable(sleepy("subFlow2Step2")))
                                        .withDataSources(fromIterable("subFlow2DataSource", subFlow2DataSource))
                        )
                        .addTestStep(print("flowStep2"))
                        .withDataSources(fromIterable("dataSource", dataSource))
                )
                .build();

        Stopwatch timer = Stopwatch.createStarted();
        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);
        long timeElapsed = timer.elapsed(TimeUnit.SECONDS);

        int testSteps = 2;
        int dataRecords = subFlow2DataSource.size();
        int margin = 1;
        assertThat(timeElapsed).isLessThanOrEqualTo(NAP_TIME * testSteps * dataRecords + margin);

        compareGraphs(debug, "testRunInParallel.graphml");
    }

    @Test
    public void testRunInParallel_moreVusersAndSharedDs() throws Exception {
        List<String> dataSource = newArrayList("1");
        List<String> subFlow1DataSource = newArrayList("a");
        List<String> subFlow2DataSource = newArrayList("u", "v", "x", "y", "z");

        RxScenario scenario = scenario()
                .withParameter(STORE_V_USERS_IN_CONTEXT, true)
                .addFlow(flow()
                        .addTestStep(print("flowStep1"))
                        .split(
                                flow()
                                        .addTestStep(print("subFlow1Step1"))
                                        .addTestStep(print("subFlow1Step2"))
                                        .withDataSources(fromIterable("subFlow1DataSource", subFlow1DataSource))
                                ,
                                flow()
                                        .withBefore(print("Before Flow Test Step"))
                                        .addTestStep(new ThrowException("subFlow2DataSource", "z", "1.1.2"))
                                        .addTestStep(print("subFlow2Step2"))
                                        .withAfter(print("After Flow Test Step"))
                                        .withVUsers(2)
                                        .withDataSources(fromIterable("subFlow2DataSource", subFlow2DataSource).shared())
                        )
                        .addTestStep(print("flowStep2"))
                        .withDataSources(fromIterable("dataSource", dataSource).shared())
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "testRunInParallel_moreVusersAndSharedDs.graphml");
    }

    @Test
    public void beforeAndAfterInFlowAndSubflows_Graph() throws Exception {
        List<String> dataSource = newArrayList("1", "2");
        List<String> subFlow1DataSource = newArrayList("a");
        List<String> subFlow2DataSource = newArrayList("u", "v");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .withBefore(print("Before Flow"))
                        .addTestStep(print("flowStep1"))
                        .split(
                                flow()
                                        .withBefore(print("Before SubFlow1"))
                                        .addTestStep(print("subFlow1Step1"))
                                        .addTestStep(print("subFlow1Step2"))
                                        .withAfter(print("After SubFlow1"))
                                        .withDataSources(fromIterable("subFlow1DataSource", subFlow1DataSource))
                                ,
                                flow()
                                        .withBefore(print("Before SubFlow2"))
                                        .addTestStep(print("subFlow2Step2"))
                                        .withAfter(print("After SubFlow2"))
                                        .withVUsers(2)
                                        .withDataSources(fromIterable("subFlow2DataSource", subFlow2DataSource))
                        )
                        .addTestStep(print("flowStep2"))
                        .withAfter(print("After Flow"))
                        .withVUsers(3)
                        .withDataSources(fromIterable("dataSource", dataSource))
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "beforeAndAfterInFlowAndSubflows_Graph.graphml");
    }

    @Test
    public void beforeAndAfterInFlowAndSubflowsWithException_Graph() throws Exception {
        List<String> subFlow1DataSource = newArrayList("a");
        List<String> subFlow2DataSource = newArrayList("u", "v");
        RxScenario scenario = scenario()
                .withParameter(STORE_V_USERS_IN_CONTEXT, true)
                .addFlow(flow()
                        .withBefore(print("Flow: Before1"), print("Flow: Before2"))
                        .addTestStep(print("flowStep1"))
                        .split(
                                flow()
                                        .withBefore(print("SubFlow1: Before1"), print("SubFlow1: Before2"))
                                        .addTestStep(print("subFlow1Step1"))
                                        .addTestStep(print("subFlow1Step2"))
                                        .withAfter(print("SubFlow1: After1"), print("SubFlow1: After2"))
                                        .withDataSources(fromIterable("subFlow1DataSource", subFlow1DataSource))
                                ,
                                flow()
                                        .withBefore(print("SubFlow2: Before1"), print("SubFlow2: Before2"))
                                        .addTestStep(new ThrowException("subFlow2DataSource", "u", "1.1.2"))
                                        .addTestStep(print("subFlow2Step2"))
                                        .withAfter(print("SubFlow2: After1"), print("SubFlow2: After2"))
                                        .withVUsers(2)
                                        .withDataSources(fromIterable("subFlow2DataSource", subFlow2DataSource))
                        )
                        .addTestStep(print("flowStep2"))
                        .withAfter(print("Flow: After1"), print("Flow: After2"))
                        .withVUsers(3)
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "beforeAndAfterInFlowAndSubflowsWithException_Graph.graphml");
    }

    @Test
    public void beforeAndAfterInFlowAndSubflows_Counter() throws Exception {

        // scenario parameters - prime numbers
        final int FLOW_V_USERS = 2;
        final int SUB_FLOW_2_VUSERS = 3;
        List<String> flowDataSource = newArrayList("11", "22", "33", "44", "55");
        List<String> subFlow1DataSource = newArrayList("a", "b", "c", "d", "e", "f", "g");
        List<String> subFlow2DataSource = newArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

        // Flow level
        Counter beforeFlowCounter = new Counter("Before Flow");
        Counter flowStepCounter = new Counter("Flow Test Step");
        Counter afterFlowCounter = new Counter("After Flow");

        // Subflow 1 level
        Counter beforeSubflow1Counter = new Counter("Subflow 1: Before");
        Counter subflow1StepCounter = new Counter("Subflow 1: Test Step");
        Counter afterSubflow1Counter = new Counter("Subflow 1: After");

        // Subflow 2 level
        Counter beforeSubflow2Counter = new Counter("Subflow 2: Before");
        Counter subflow2StepCounter = new Counter("Subflow 2: Test Step");
        Counter afterSubflow2Counter = new Counter("Subflow 2: After");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .withBefore(beforeFlowCounter)
                        .split(
                                flow()
                                        .withBefore(beforeSubflow1Counter)
                                        .addTestStep(subflow1StepCounter)
                                        .withAfter(afterSubflow1Counter)
                                        .withDataSources(fromIterable("subFlow1DataSource", subFlow1DataSource))
                                ,
                                flow()
                                        .withBefore(beforeSubflow2Counter)
                                        .addTestStep(subflow2StepCounter)
                                        .withAfter(afterSubflow2Counter)
                                        .withVUsers(SUB_FLOW_2_VUSERS)
                                        .withDataSources(fromIterable("subFlow2DataSource", subFlow2DataSource))
                        )
                        .addTestStep(flowStepCounter)
                        .withAfter(afterFlowCounter)
                        .withVUsers(FLOW_V_USERS)
                        .withDataSources(fromIterable("dataSource", flowDataSource))
                )
                .build();
        RxApiImpl.run(scenario);

        // Flow level
        assertThat(beforeFlowCounter.getCount()).isEqualTo(1);
        assertThat(flowStepCounter.getCount()).isEqualTo(FLOW_V_USERS * flowDataSource.size());
        assertThat(afterFlowCounter.getCount()).isEqualTo(1);

        // Subflow 1 level
        assertThat(beforeSubflow1Counter.getCount()).isEqualTo(flowDataSource.size());
        int expectedTestSteps = flowDataSource.size() * FLOW_V_USERS * subFlow1DataSource.size();
        assertThat(subflow1StepCounter.getCount()).isEqualTo(expectedTestSteps);
        assertThat(afterSubflow1Counter.getCount()).isEqualTo(flowDataSource.size());

        // Subflow 2 level
        assertThat(beforeSubflow2Counter.getCount()).isEqualTo(flowDataSource.size());
        expectedTestSteps = flowDataSource.size() * SUB_FLOW_2_VUSERS * FLOW_V_USERS * subFlow2DataSource.size();
        assertThat(subflow2StepCounter.getCount()).isEqualTo(expectedTestSteps);
        assertThat(afterSubflow2Counter.getCount()).isEqualTo(flowDataSource.size());
    }

    @Test
    public void beforeAndAfterInFlowAndSubflowsWithException_Counter() throws Exception {

        // scenario parameters - prime numbers
        final int FLOW_V_USERS = 2;
        final int SUB_FLOW_2_VUSERS = 3;
        List<String> flowDataSource = newArrayList("11", "22", "33", "44", "55");
        List<String> subFlow1DataSource = newArrayList("a", "b", "c", "d", "e", "f", "g");
        List<String> subFlow2DataSource = newArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

        // Flow level
        Counter beforeFlowCounter = new Counter("Before Flow");
        Counter flowStepCounter = new Counter("Flow Test Step");
        Counter afterFlowCounter = new Counter("After Flow");

        // Subflow 1 level
        Counter beforeSubflow1Counter = new Counter("Subflow 1: Before");
        Counter subflow1StepCounter = new Counter("Subflow 1: Test Step");
        Counter afterSubflow1Counter = new Counter("Subflow 1: After");

        // Subflow 2 level
        Counter beforeSubflow2Counter = new Counter("Subflow 2: Before");
        Counter subflow2StepCounter1 = new Counter("Subflow 2: Test Step 1");
        Counter subflow2StepCounter2 = new Counter("Subflow 2: Test Step 2");
        Counter afterSubflow2Counter = new Counter("Subflow 2: After");

        RxScenario scenario = scenario()
                .withParameter(STORE_V_USERS_IN_CONTEXT, true)
                .addFlow(flow()
                        .withBefore(beforeFlowCounter)
                        .addTestStep(flowStepCounter)
                        .split(
                                flow()
                                        .withBefore(beforeSubflow1Counter)
                                        .addTestStep(subflow1StepCounter)
                                        .withAfter(afterSubflow1Counter)
                                        .withDataSources(fromIterable("subFlow1DataSource", subFlow1DataSource))
                                ,
                                flow()
                                        .withBefore(beforeSubflow2Counter)
                                        .addTestStep(subflow2StepCounter1)
                                        .addTestStep(new ThrowException("subFlow2DataSource", "2", "1.1.2"))
                                        .addTestStep(subflow2StepCounter2)
                                        .withAfter(afterSubflow2Counter)
                                        .withVUsers(SUB_FLOW_2_VUSERS)
                                        .withDataSources(fromIterable("subFlow2DataSource", subFlow2DataSource))
                        )
                        .withAfter(afterFlowCounter)
                        .withVUsers(FLOW_V_USERS)
                        .withDataSources(fromIterable("dataSource", flowDataSource))
                )
                .build();

        ScenarioDebugger.debug(scenario);

        // Flow level
        assertThat(beforeFlowCounter.getCount()).isEqualTo(1);
        // exception prevented next data records
        assertThat(flowStepCounter.getCount()).isEqualTo(FLOW_V_USERS * flowDataSource.size() / flowDataSource.size());
        assertThat(afterFlowCounter.getCount()).isEqualTo(1);

        // Subflow 1 level
        assertThat(beforeSubflow1Counter.getCount()).isEqualTo(flowDataSource.size() / flowDataSource.size());
        int expectedTestSteps = flowDataSource.size() * FLOW_V_USERS * subFlow1DataSource.size() / flowDataSource.size();
        assertThat(subflow1StepCounter.getCount()).isEqualTo(expectedTestSteps);
        assertThat(afterSubflow1Counter.getCount()).isEqualTo(flowDataSource.size() / flowDataSource.size());

        // Subflow 2 level
        assertThat(beforeSubflow2Counter.getCount()).isEqualTo(flowDataSource.size() / flowDataSource.size());
        final int ACTUAL_SUB_FLOW_2_RECORDS = 2;
        expectedTestSteps = flowDataSource.size() * SUB_FLOW_2_VUSERS * FLOW_V_USERS * ACTUAL_SUB_FLOW_2_RECORDS / flowDataSource.size();
        assertThat(subflow2StepCounter1.getCount()).isEqualTo(expectedTestSteps);
        // exception prevents next test steps
        assertThat(subflow2StepCounter2.getCount()).isEqualTo(expectedTestSteps - 1);
        assertThat(afterSubflow2Counter.getCount()).isEqualTo(flowDataSource.size() / flowDataSource.size());
    }

    @Test
    public void exceptionInBefore() throws Exception {
        List<String> dataSource = newArrayList("u", "v", "x", "y", "z");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .withBefore(new ThrowExceptionNow("Exception in Before"))
                        .addTestStep(print("flowStep1"))
                        .addTestStep(print("flowStep2"))
                        .withAfter(print("After Flow Test Step"))
                        .withDataSources(fromIterable("dataSource", dataSource))
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "exceptionInBefore.graphml");
    }

    @Test
    public void exceptionInAfter() throws Exception {
        List<String> dataSource = newArrayList("u", "v", "x", "y", "z");

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .withBefore(print("Before Flow Test Step"))
                        .addTestStep(new SimpleTestStepWithArguments("flowStep1"))
                        .addTestStep(print("flowStep2"))
                        .withAfter(new ThrowExceptionNow("Exception in After"))
                        .withDataSources(fromIterable("dataSource", dataSource))
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "exceptionInAfter.graphml");
    }

    @Test
    public void beforeMethodWithParameters() throws Exception {
        List<String> dataSource = newArrayList("u", "v", "x", "y", "z");
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .withBefore(new SimpleTestStepWithArguments("Before Flow Test Step"))
                        .addTestStep(new SimpleTestStepWithArguments("flowStep1"))
                        .addTestStep(print("flowStep2"))
                        .withAfter(print("After Flow Test Step"))
                        .withDataSources(fromIterable("dataSource", dataSource))
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "beforeMethodWithParameters.graphml");
    }

    @Test
    public void afterMethodWithParameters() throws Exception {
        List<String> dataSource = newArrayList("u", "v", "x", "y", "z");
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .withBefore(print("Before Flow Test Step"))
                        .addTestStep(new SimpleTestStepWithArguments("flowStep1"))
                        .addTestStep(print("flowStep2"))
                        .withAfter(new SimpleTestStepWithArguments("After Flow Test Step"))
                        .withDataSources(fromIterable(DATA_SOURCE, dataSource))
                )
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "afterMethodWithParameters.graphml");
    }

    @Test
    public void vUsersAuto() throws Exception {
        int count = 8;
        Counter counter = new Counter();

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(counter)
                        .addTestStep(runnable(sleepy("sleepy")))
                        .withDataSources(fromIterable("numbers", numbers(count)).shared())
                        .withVUsersAuto()
                )
                .build();

        Stopwatch timer = Stopwatch.createStarted();
        RxApiImpl.run(scenario);
        long timeElapsed = timer.elapsed(TimeUnit.SECONDS);

        counter.assertEqualTo(count);
        assertThat(timeElapsed).isLessThanOrEqualTo(NAP_TIME + 1);
    }

    @Test
    public void runWithRepeatWhile() throws Exception {
        List<String> dataSource = newArrayList("u", "v", "x", "y", "z");

        Counter counter = new Counter();
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(counter)
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void consumer1(@Named(REPEAT_DATASOURCE) String fromDataSource
                            ) {
                                System.out.println(fromDataSource);
                            }
                        })
                        .withDataSources(fromIterable(REPEAT_DATASOURCE, dataSource))
                        .runWhile(new Predicate<RxDataRecordWrapper>() {
                            @Override
                            public boolean apply(RxDataRecordWrapper input) {
                                final Optional<String> fieldValue = input.getFieldValue(REPEAT_DATASOURCE, String.class);
                                assertThat(fieldValue.isPresent()).isTrue();
                                return !fieldValue.get().equals("x");
                            }
                        })
                )
                .build();

        RxApiImpl.run(scenario);
        counter.assertEqualTo(2);
    }

    @Test
    public void runWithRepeatWhileLoopsDataRecords() throws Exception {
        List<String> dataSource = newArrayList("u", "v", "x", "y", "z");

        Counter counter = new Counter();
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(counter)
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void consumer1(@Named(REPEAT_DATASOURCE) String fromDataSource
                            ) {
                                System.out.println(fromDataSource);
                            }
                        })
                        .withDataSources(fromIterable(REPEAT_DATASOURCE, dataSource))
                        .runWhile(new Predicate<RxDataRecordWrapper>() {
                            int count = 0;

                            @Override
                            public boolean apply(RxDataRecordWrapper input) {
                                count++;
                                return count <= 8;
                            }
                        })
                )
                .build();

        RxApiImpl.run(scenario);
        counter.assertEqualTo(8);
    }

    @Test
    public void runRepeatWhileNoDataSource() {
        Counter counter = new Counter();
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(counter)
                        .runWhile(new Predicate<RxDataRecordWrapper>() {
                            int count = 0;

                            @Override
                            public boolean apply(RxDataRecordWrapper input) {
                                count++;
                                return count <= 8;
                            }
                        })
                )
                .build();

        RxApiImpl.run(scenario);
        counter.assertEqualTo(8);
    }

    private static class SimpleTestStepWithArguments extends RxTestStep {

        SimpleTestStepWithArguments(String name) {
            super(name);
        }

        @Override
        protected Optional<Object> doRun(RxDataRecordWrapper dataRecord) throws Exception {

            // checking arguments
            Preconditions.checkNotNull(dataRecord);
            Optional<String> fieldValue = dataRecord.getFieldValue(DATA_SOURCE, String.class);
            Preconditions.checkArgument(fieldValue.isPresent());

            System.out.println("running test step " + getName() + " with data: " + fieldValue.get());
            return Optional.of((Object) dataRecord);
        }

        @Override
        protected RxTestStep copySelf() {
            return new SimpleTestStepWithArguments(name);
        }
    }
}

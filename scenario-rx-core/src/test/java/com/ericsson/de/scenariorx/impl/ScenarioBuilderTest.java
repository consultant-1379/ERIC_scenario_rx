package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.impl.ScenarioBuilder.ERROR_FLOW_UNIQUENESS;
import static com.ericsson.de.scenariorx.impl.ScenarioBuilder.ERROR_TEST_STEP_UNIQUENESS;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.api.RxFlowBuilder;
import com.ericsson.de.scenariorx.api.RxFlowBuilderInterfaces;
import com.ericsson.de.scenariorx.api.RxScenarioRunnerBuilder;
import com.ericsson.de.scenariorx.api.RxTestStep;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ScenarioBuilderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private RxTestStep nopStep = ScenarioTest.nop();
    private RxFlowBuilderInterfaces.Steps<RxFlow> nopFlow = flow().addTestStep(nopStep);

    @Test
    public void addFlow_shouldThrowNullPointerException_whenFlowBuilder_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOW_NULL);

        scenario().addFlow((RxFlowBuilder) null).build();
    }

    @Test
    public void addFlow_shouldThrowNullPointerException_whenFlow_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOW_NULL);

        scenario().addFlow((RxFlow) null).build();
    }

    @Test
    public void split_shouldThrowNullPointerException_whenSubFlowsBuilders_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOWS_NULL);

        scenario().split((RxFlowBuilder[]) null).build();
    }

    @Test
    public void split_shouldThrowNullPointerException_whenOneOfSubFlowsBuilders_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOW_NULL);

        scenario().split(nopFlow, null, nopFlow).build();
    }

    @Test
    public void split_shouldThrowNullPointerException_whenSubFlows_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOWS_NULL);

        scenario().split((RxFlow[]) null).build();
    }

    @Test
    public void split_shouldThrowNullPointerException_whenOneOfSubFlows_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOW_NULL);

        RxFlow flow = nopFlow.build();

        scenario().split(flow, null, flow).build();
    }

    @Test
    public void withParameter_shouldThrowNullPointerException_whenParameterKey_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(RxTestStep.ERROR_PARAMETER_NULL);

        scenario().withParameter(null, 13);
    }

    @Test
    public void withParameter_shouldThrowIllegalArgumentException_whenSameParameterSetTwice() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(format(RxTestStep.ERROR_PARAMETER_ALREADY_SET, "param"));

        scenario().withParameter("param", 42).withParameter("param", 13);
    }

    @Test
    public void withParameter_shouldThrowIllegalArgumentException_whenDebugLogEnabled() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ScenarioBuilder.ERROR_DEBUG_LOG_ENABLED);

        scenario().withParameter(RxScenarioRunnerBuilder.DEBUG_LOG_ENABLED, null);
    }

    @Test
    public void withParameter_shouldThrowIllegalArgumentException_whenDebugGraphMode() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ScenarioBuilder.ERROR_DEBUG_GRAPH_MODE);

        scenario().withParameter(RxScenarioRunnerBuilder.DEBUG_GRAPH_MODE, null);
    }

    @Test
    public void withExceptionHandler_shouldThrowNullPointerException_whenExceptionHandler_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_EXCEPTION_HANDLER_NULL);

        scenario().addFlow(nopFlow).withExceptionHandler(null);
    }

    @Test
    public void withExceptionHandler_shouldThrowIllegalStateException_whenCalledMoreThanOnce() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_EXCEPTION_HANDLER_NOT_ONCE);
        thrown.expectMessage(FlowBuilder.HINT_EXCEPTION_HANDLER);

        ((ScenarioBuilder) scenario().addFlow(nopFlow)
                .withExceptionHandler(RxExceptionHandler.PROPAGATE))
                .withExceptionHandler(RxExceptionHandler.IGNORE);
    }

    @Test
    public void build_exceptionHandler_null_byDefault() throws Exception {
        Scenario scenario = scenario().addFlow(nopFlow).build();

        assertThat(scenario.flow.exceptionHandler).isNull();
    }

    @Test
    public void build_shouldAssignUniqueScenarioIds_toAllTestSteps() throws Exception {
        RxTestStep testStepA = ScenarioTest.print("Test Step A");
        RxTestStep testStepB = ScenarioTest.print("Test Step B");
        RxTestStep testStepC = ScenarioTest.print("Test Step C");
        RxTestStep testStepD = ScenarioTest.print("Test Step D");
        RxTestStep testStepE = ScenarioTest.print("Test Step E");

        scenario() // 1
                .addFlow(flow() // 2
                        .addTestStep(testStepA) // 3
                        .addSubFlow(flow() // 4
                                .addTestStep(testStepB) // 5
                        )
                        .split(flow() // 6
                                .addTestStep(testStepC) // 7
                        )
                )
                .split(
                        flow() // 8
                                .addTestStep(testStepD), // 9
                        flow() // 10
                                .split(
                                        flow() // 11
                                                .addTestStep(testStepE) // 12
                                )
                )
                .build();

        assertThat(testStepA.getId()).isEqualTo(3);
        assertThat(testStepB.getId()).isEqualTo(5);
        assertThat(testStepC.getId()).isEqualTo(7);
        assertThat(testStepD.getId()).isEqualTo(9);
        assertThat(testStepE.getId()).isEqualTo(12);
    }

    /*-------- Test Step Uniqueness Tests --------*/

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inSameFlow() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario()
                .addFlow(flow()
                        .addTestStep(nopStep)
                        .addTestStep(nopStep)
                )
                .build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inDifferentFlows() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario()
                .addFlow(flow().addTestStep(nopStep))
                .addFlow(flow().addTestStep(nopStep))
                .build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inDifferentSubFlows() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario()
                .addFlow(flow()
                        .addSubFlow(flow().addTestStep(nopStep))
                        .addSubFlow(flow().addTestStep(nopStep))
                )
                .build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_onDifferentLevels() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario()
                .addFlow(flow()
                        .addTestStep(nopStep)
                        .addSubFlow(flow()
                                .addTestStep(nopStep)
                        )
                )
                .build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inDifferentSplitFlows() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario()
                .addFlow(flow()
                        .split(
                                flow().addTestStep(nopStep),
                                flow().addTestStep(nopStep)
                        )
                )
                .build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inDifferentSplits() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario()
                .addFlow(flow()
                        .split(flow().addTestStep(nopStep))
                        .split(flow().addTestStep(nopStep))
                )
                .build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inDifferentSplitsSubFlows() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario()
                .addFlow(flow()
                        .split(flow()
                                .addSubFlow(flow()
                                        .addTestStep(nopStep))
                        )
                        .split(flow()
                                .addSubFlow(flow()
                                        .addTestStep(nopStep))
                        )
                )
                .build();
    }

    /*-------- Flow Uniqueness Tests --------*/

    @Test
    public void shouldThrowIllegalArgumentException_whenSameFlowAddedTwice_inSameScenario() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_FLOW_UNIQUENESS);

        RxFlow flow = nopFlow.build();

        scenario()
                .addFlow(flow)
                .addFlow(flow)
                .build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameFlowAddedTwice_inDifferentForks() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_FLOW_UNIQUENESS);

        RxFlow flow = nopFlow.build();

        scenario()
                .addFlow(flow)
                .split(flow)
                .build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameFlowAddedTwice_onDifferentLevels() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_FLOW_UNIQUENESS);

        RxFlow flow = nopFlow.build();

        scenario()
                .addFlow(flow)
                .addFlow(flow()
                        .addSubFlow(flow)
                )
                .build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameFlowAddedTwice_inSameSplit() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_FLOW_UNIQUENESS);

        RxFlow flow = nopFlow.build();

        scenario()
                .split(flow, flow)
                .build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameFlowAddedTwice_inDifferentSplits() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_FLOW_UNIQUENESS);

        RxFlow flow = nopFlow.build();

        scenario()
                .split(flow)
                .split(flow)
                .build();
    }
}
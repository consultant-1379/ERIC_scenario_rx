package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.api.RxApi.flow;
import static com.ericsson.de.scenariorx.api.RxApi.scenario;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.api.RxFlowBuilderInterfaces;
import com.ericsson.de.scenariorx.api.RxScenario;
import org.junit.Test;


public class DataDrivenScenarioTest extends ScenarioTest {
    @Test
    public void shouldContinueOnNextDataRecord() throws Exception {
        RxFlowBuilderInterfaces.Options<RxFlow> flowBuilder = flow("flow")
                .addTestStep(pushToStack("a"))
                .addTestStep(new ThrowExceptionNow("exception"))
                .addTestStep(pushToStack("b"))
                .withDataSources(getNodeDataSource());

        ((FlowBuilder) flowBuilder).exceptionHandler = new ContinueOnNextDataRecord(null);

        RxScenario scenario = scenario("scenario")
                .addFlow(
                        flowBuilder
                ).build();

        ScenarioDebugger.debug(scenario);
        assertThat(stack).containsExactly("a", "a");
    }
}
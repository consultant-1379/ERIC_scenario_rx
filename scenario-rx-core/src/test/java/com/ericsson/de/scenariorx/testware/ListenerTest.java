package com.ericsson.de.scenariorx.testware;

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.runner;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.impl.Api.fromIterable;
import static com.google.common.base.Strings.repeat;
import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Stack;

import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.RxScenarioListener;
import com.ericsson.de.scenariorx.api.RxScenarioRunner;
import com.ericsson.de.scenariorx.api.events.RxFlowEvent.RxFlowFinishedEvent;
import com.ericsson.de.scenariorx.api.events.RxFlowEvent.RxFlowStartedEvent;
import com.ericsson.de.scenariorx.api.events.RxScenarioEvent.RxScenarioFinishedEvent;
import com.ericsson.de.scenariorx.api.events.RxScenarioEvent.RxScenarioStartedEvent;
import com.ericsson.de.scenariorx.api.events.RxTestStepEvent.RxTestStepFinishedEvent;
import com.ericsson.de.scenariorx.api.events.RxTestStepEvent.RxTestStepStartedEvent;
import com.ericsson.de.scenariorx.impl.ScenarioTest;
import org.junit.Before;
import org.junit.Test;

public class ListenerTest {

    private Stack<String> stack;
    private RxScenarioRunner runner;

    @Before
    public void setUp() throws Exception {
        stack = new Stack<>();
        runner = runner()
                .addListener(new StackListener(stack))
                .build();
    }

    @Test
    public void empty() throws Exception {
        RxScenario scenario = scenario("foo")
                .addFlow(flow("bar")
                        .addTestStep(ScenarioTest.named("baz"))
                )
                .build();

        runner.run(scenario);

        assertThat(stack).containsExactly(
                "Scenario started: foo",
                "  Flow started: bar",
                "    Test Step started: baz",
                "    Test Step finished: baz",
                "  Flow finished: bar",
                "Scenario finished: foo"
        );
    }

    @Test
    public void steps() throws Exception {
        RxScenario scenario = scenario("foo")
                .addFlow(flow("bar1")
                        .addTestStep(ScenarioTest.named("baz1"))
                        .addTestStep(ScenarioTest.named("baz2"))
                        .addTestStep(ScenarioTest.named("baz3"))
                )
                .addFlow(flow("bar2")
                        .addTestStep(ScenarioTest.named("qux1"))
                        .addTestStep(ScenarioTest.named("qux2"))
                        .addTestStep(ScenarioTest.named("qux3"))
                )
                .build();

        runner.run(scenario);

        assertThat(stack).containsExactly(
                "Scenario started: foo",
                "  Flow started: bar1",
                "    Test Step started: baz1",
                "    Test Step finished: baz1",
                "    Test Step started: baz2",
                "    Test Step finished: baz2",
                "    Test Step started: baz3",
                "    Test Step finished: baz3",
                "  Flow finished: bar1",
                "  Flow started: bar2",
                "    Test Step started: qux1",
                "    Test Step finished: qux1",
                "    Test Step started: qux2",
                "    Test Step finished: qux2",
                "    Test Step started: qux3",
                "    Test Step finished: qux3",
                "  Flow finished: bar2",
                "Scenario finished: foo"
        );
    }


    @Test
    public void subflows() throws Exception {
        RxScenario scenario = scenario("foo")
                .addFlow(flow("bar")
                        .addTestStep(ScenarioTest.named("bar"))
                        .addSubFlow(flow("baz")
                                .addTestStep(ScenarioTest.named("baz"))
                                .addSubFlow(flow("qux")
                                        .addTestStep(ScenarioTest.named("qux"))
                                )
                        )
                )
                .build();

        runner.run(scenario);

        assertThat(stack).containsExactly(
                "Scenario started: foo",
                "  Flow started: bar",
                "    Test Step started: bar",
                "    Test Step finished: bar",
                "    Flow started: baz",
                "      Test Step started: baz",
                "      Test Step finished: baz",
                "      Flow started: qux",
                "        Test Step started: qux",
                "        Test Step finished: qux",
                "      Flow finished: qux",
                "    Flow finished: baz",
                "  Flow finished: bar",
                "Scenario finished: foo"
        );
    }

    @Test
    public void vUsers() throws Exception {
        List<Integer> numbers = newArrayList(1, 2, 3);

        RxScenario scenario = scenario("foo")
                .addFlow(flow("bar")
                        .addTestStep(ScenarioTest.named("baz"))
                        .withDataSources(fromIterable("numbers", numbers).shared())
                        .withVUsersAuto()
                )
                .build();

        runner.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder(
                "Scenario started: foo",
                "  Flow started: bar",
                "    Test Step started: baz",
                "    Test Step finished: baz",
                "    Test Step started: baz",
                "    Test Step finished: baz",
                "    Test Step started: baz",
                "    Test Step finished: baz",
                "  Flow finished: bar",
                "Scenario finished: foo"
        );
    }

    @Test
    public void dataSource() throws Exception {
        List<Integer> numbers = newArrayList(1, 2, 3);

        RxScenario scenario = scenario("foo")
                .addFlow(flow("bar")
                        .addTestStep(ScenarioTest.named("baz"))
                        .withDataSources(fromIterable("numbers", numbers))
                )
                .build();

        runner.run(scenario);

        assertThat(stack).containsExactly(
                "Scenario started: foo",
                "  Flow started: bar",
                "    Test Step started: baz",
                "    Test Step finished: baz",
                "    Test Step started: baz",
                "    Test Step finished: baz",
                "    Test Step started: baz",
                "    Test Step finished: baz",
                "  Flow finished: bar",
                "Scenario finished: foo"
        );
    }

    @Test
    public void exception() throws Exception {
        RxScenario scenario = scenario("foo")
                .addFlow(flow("bar")
                        .addTestStep(ScenarioTest.named("baz"))
                )
                .build();

        runner = runner()
                .addListener(new RxScenarioListener() {
                    @Override
                    public void onScenarioStarted(RxScenarioStartedEvent event) {
                        throw new ScenarioTest.VeryExpectedException();
                    }
                })
                .addListener(new StackListener(stack))
                .build();

        try {
            runner.run(scenario);
        } catch (Exception ignored) {
        }

        assertThat(stack).containsExactly(
                "Scenario started: foo",
                "  Flow started: bar",
                "    Test Step started: baz",
                "    Test Step finished: baz",
                "  Flow finished: bar",
                "Scenario finished: foo"
        );
    }

    private static class StackListener extends RxScenarioListener {

        private Stack<String> stack;
        private int indent = 0;

        StackListener(Stack<String> stack) {
            this.stack = stack;
        }

        @Override
        public void onScenarioStarted(RxScenarioStartedEvent event) {
            indent++;
            stack.push("Scenario started: " + event.getScenario().getName());
        }

        @Override
        public void onScenarioFinished(RxScenarioFinishedEvent event) {
            indent--;
            stack.push("Scenario finished: " + event.getScenario().getName());
        }

        @Override
        public void onFlowStarted(RxFlowStartedEvent event) {
            stack.push(repeat(" ", 2 * indent++) + "Flow started: " + event.getFlow().getName());
        }

        @Override
        public void onFlowFinished(RxFlowFinishedEvent event) {
            stack.push(repeat(" ", 2 * --indent) + "Flow finished: " + event.getFlow().getName());
        }

        @Override
        public void onTestStepStarted(RxTestStepStartedEvent event) {
            stack.push(repeat(" ", 2 * indent) + "Test Step started: " + event.getName());
        }

        @Override
        public void onTestStepFinished(RxTestStepFinishedEvent event) {
            stack.push(repeat(" ", 2 * indent) + "Test Step finished: " + event.getName());
        }
    }
}

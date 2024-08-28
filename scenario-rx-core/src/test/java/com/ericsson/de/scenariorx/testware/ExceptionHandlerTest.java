package com.ericsson.de.scenariorx.testware;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 */

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.fromIterable;
import static com.ericsson.de.scenariorx.api.RxApiImpl.runnable;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.api.RxExceptionHandler.Outcome.CONTINUE_FLOW;
import static com.ericsson.de.scenariorx.api.RxExceptionHandler.Outcome.PROPAGATE_EXCEPTION;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Stack;

import com.ericsson.de.scenariorx.api.RxApiImpl;
import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.ericsson.de.scenariorx.impl.ScenarioTest;
import org.junit.Test;
import rx.exceptions.CompositeException;

/**
 * Ported from com.ericsson.cifwk.taf.scenario.impl.ExceptionHandlerTest
 */
@SuppressWarnings("unchecked")
public class ExceptionHandlerTest extends ScenarioTest {

    @Test
    public void shouldPropagateNextLevel() throws Exception {
        ExceptionAccumulator subFlowHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator flowHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);
        ExceptionAccumulator scenarioHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(pushToStack("a"))
                        .addSubFlow(flow("subFlow")
                                .addTestStep(pushToStack("b"))
                                .addTestStep(new ThrowExceptionNow("exception"))
                                .addTestStep(pushToStack("c"))
                                .withExceptionHandler(subFlowHandlerPropagate)
                        )
                        .addTestStep(pushToStack("d"))
                        .withExceptionHandler(flowHandlerIgnore)
                )
                .withExceptionHandler(scenarioHandlerIgnore)
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactly("a", "b", "d");
        assertThat(subFlowHandlerPropagate.getExceptions()).containsExactly(VeryExpectedException.class);
        assertThat(flowHandlerIgnore.getExceptions()).containsExactly(VeryExpectedException.class);
        assertThat(scenarioHandlerIgnore.getExceptions()).isEmpty();
    }

    @Test
    public void shouldPropagateThroughLevels() throws Exception {
        ExceptionAccumulator subFlowHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator flowHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator scenarioHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(pushToStack("a"))
                        .addTestStep(pushToStack("b"))
                        .addSubFlow(flow("subFlow")
                                .addTestStep(new ThrowExceptionNow("exception"))
                                .addTestStep(pushToStack("c"))
                                .withExceptionHandler(subFlowHandlerPropagate)
                        )
                        .addTestStep(pushToStack("d"))
                        .withExceptionHandler(flowHandlerPropagate)
                )
                .withExceptionHandler(scenarioHandlerIgnore)
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactly("a", "b");
        assertThat(subFlowHandlerPropagate.getExceptions()).containsExactly(VeryExpectedException.class);
        assertThat(flowHandlerPropagate.getExceptions()).containsExactly(VeryExpectedException.class);
        assertThat(scenarioHandlerIgnore.getExceptions()).containsExactly(VeryExpectedException.class);
    }

    @Test
    public void shouldHandleByParentIfNoChildDefined() throws Exception {
        ExceptionAccumulator flowHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);
        ExceptionAccumulator scenarioHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator runnerHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(pushToStack("a"))
                        .addSubFlow(flow("subFlow")
                                .addTestStep(pushToStack("b"))
                                .addTestStep(new ThrowExceptionNow("exception"))
                                .addTestStep(pushToStack("c"))
                        )
                        .addTestStep(pushToStack("d"))
                        .withExceptionHandler(flowHandlerIgnore)
                )
                .withExceptionHandler(scenarioHandlerPropagate)
                .build();

        RxApiImpl.runner().withDefaultExceptionHandler(runnerHandlerIgnore).build().run(scenario);

        assertThat(stack).containsExactly("a", "b", "c", "d");
        assertThat(flowHandlerIgnore.getExceptions()).isEmpty();
        assertThat(scenarioHandlerPropagate.getExceptions()).isEmpty();
        assertThat(runnerHandlerIgnore.getExceptions()).containsExactly(VeryExpectedException.class);
    }

    @Test
    public void shouldHandleExceptionsInSplitAndVUsers() throws Exception {
        ExceptionAccumulator subflow1HandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator subflow2HandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);
        ExceptionAccumulator flowHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);
        ExceptionAccumulator scenarioHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator runnerHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);

        RxScenario scenario = scenario("scenario")
                .withParameter(ScenarioTest.STORE_V_USERS_IN_CONTEXT, true)
                .addFlow(flow("flow")
                        .addTestStep(pushToStack("a"))
                        .split(
                                flow("subFlow1")
                                        .addTestStep(new ScenarioTest.ThrowException("answer", "42", "1.1.1"))
                                        .addTestStep(pushToStack("b"))
                                        .withExceptionHandler(subflow1HandlerPropagate)
                                        .build(),
                                flow("subFlow2")
                                        .addTestStep(pushToStack("c"))
                                        .withExceptionHandler(subflow2HandlerIgnore)
                                        .build()
                        )
                        .addTestStep(pushToStack("d"))
                        .withDataSources(fromIterable("answer", singletonList(42)))
                        .withVUsers(2)
                        .withExceptionHandler(flowHandlerIgnore)
                )
                .withExceptionHandler(scenarioHandlerPropagate)
                .build();

        RxApiImpl.runner().withDefaultExceptionHandler(runnerHandlerIgnore).build().run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("a", "c", "d", "a", "b", "c", "d");
        assertThat(subflow1HandlerPropagate.getExceptions()).containsExactly(VeryExpectedException.class);
        assertThat(subflow2HandlerIgnore.getExceptions()).isEmpty();
        assertThat(flowHandlerIgnore.getExceptions()).containsExactly(VeryExpectedException.class);
        assertThat(scenarioHandlerPropagate.getExceptions()).isEmpty();
        assertThat(runnerHandlerIgnore.getExceptions()).isEmpty();
    }

    @Test
    public void shouldPropagateThroughAllLevels() throws Exception {
        ExceptionAccumulator subFlowHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator flowHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator scenarioHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator runnerHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(pushToStack("a"))
                        .addTestStep(pushToStack("b"))
                        .addSubFlow(flow("subFlow")
                                .addTestStep(new ThrowExceptionNow("exception"))
                                .addTestStep(pushToStack("c"))
                                .withExceptionHandler(subFlowHandlerPropagate)
                        )
                        .addTestStep(pushToStack("d"))
                        .withExceptionHandler(flowHandlerPropagate)
                )
                .withExceptionHandler(scenarioHandlerPropagate)
                .build();

        RxApiImpl.runner().withDefaultExceptionHandler(runnerHandlerIgnore).build().run(scenario);

        assertThat(stack).containsExactly("a", "b");
        assertThat(subFlowHandlerPropagate.getExceptions()).containsExactly(VeryExpectedException.class);
        assertThat(flowHandlerPropagate.getExceptions()).containsExactly(VeryExpectedException.class);
        assertThat(scenarioHandlerPropagate.getExceptions()).containsExactly(VeryExpectedException.class);
        assertThat(runnerHandlerIgnore.getExceptions()).containsExactly(VeryExpectedException.class);
    }

    @Test(expected = VeryExpectedException.class)
    public void shouldPropagateThroughScenario() throws Exception {
        ExceptionAccumulator subFlowHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator flowHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator scenarioHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator runnerHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(pushToStack("a"))
                        .addTestStep(pushToStack("b"))
                        .addSubFlow(flow("subFlow")
                                .addTestStep(new ThrowExceptionNow("exception"))
                                .addTestStep(pushToStack("c"))
                                .withExceptionHandler(subFlowHandlerPropagate)
                        )
                        .addTestStep(pushToStack("d"))
                        .withExceptionHandler(flowHandlerPropagate)
                )
                .withExceptionHandler(scenarioHandlerPropagate)
                .build();

        RxApiImpl.runner().withDefaultExceptionHandler(runnerHandlerPropagate).build().run(scenario);
    }

    @Test
    public void shouldHandleExceptionsInBeforewithAfter_flowHandler() throws Exception {
        ExceptionAccumulator flowHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .withBefore(throwDifferentException())
                        .addTestStep(pushToStack("a"))
                        .addTestStep(new ThrowExceptionNow("exception"))
                        .addTestStep(pushToStack("b"))
                        .withAfter(throwDifferentException(), pushToStack("c"))
                        .withExceptionHandler(flowHandlerIgnore)
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactly("a", "b", "c");

        assertThat(flowHandlerIgnore.getExceptions()).containsExactly(
                DifferentException.class, VeryExpectedException.class, DifferentException.class
        );
    }

    @Test
    public void shouldHandleExceptionsInBeforewithAfter_scenarioHandler() throws Exception {
        ExceptionAccumulator flowHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);
        ExceptionAccumulator scenarioHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);
        ExceptionAccumulator runnerHandlerPropagate = new ExceptionAccumulator(PROPAGATE_EXCEPTION);

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .withBefore(throwDifferentException())
                        .addTestStep(pushToStack("a"))
                        .addTestStep(new ThrowExceptionNow("exception"))
                        .addTestStep(pushToStack("b"))
                        .withAfter(throwDifferentException(), pushToStack("c"))
                        .withExceptionHandler(flowHandlerPropagate)
                )
                .withExceptionHandler(scenarioHandlerIgnore)
                .build();

        RxApiImpl.runner().withDefaultExceptionHandler(runnerHandlerPropagate).build().run(scenario);

        assertThat(stack).containsExactly("c");

        assertThat(flowHandlerPropagate.getExceptions()).containsExactly(DifferentException.class, DifferentException.class);
        assertThat(scenarioHandlerIgnore.getExceptions()).containsExactly(DifferentException.class, DifferentException.class);
        assertThat(runnerHandlerPropagate.getExceptions()).isEmpty();
    }

    @Test
    public void shouldNotHandleExceptionThrownByHandler() throws Exception {
        RxExceptionHandler subFlowHandlerThrowsException = new RxExceptionHandler() {
            @Override
            public Outcome onException(Throwable e) {
                throw new DifferentException();
            }
        };
        ExceptionAccumulator flowHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);
        ExceptionAccumulator scenarioHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);
        ExceptionAccumulator runnerHandlerIgnore = new ExceptionAccumulator(CONTINUE_FLOW);

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(pushToStack("a"))
                        .addTestStep(pushToStack("b"))
                        .addSubFlow(flow("subFlow")
                                .addTestStep(new ThrowExceptionNow("exception"))
                                .addTestStep(pushToStack("c"))
                                .withExceptionHandler(subFlowHandlerThrowsException)
                        )
                        .addTestStep(pushToStack("d"))
                        .withExceptionHandler(flowHandlerIgnore)
                )
                .withExceptionHandler(scenarioHandlerIgnore)
                .build();

        try {
            RxApiImpl.runner().withDefaultExceptionHandler(runnerHandlerIgnore).build().run(scenario);
            fail("Exception expected");
        } catch (DifferentException e) {
            //expected
        }

        assertThat(stack).containsExactly("a", "b");
        assertThat(flowHandlerIgnore.getExceptions()).isEmpty();
        assertThat(scenarioHandlerIgnore.getExceptions()).isEmpty();
        assertThat(runnerHandlerIgnore.getExceptions()).isEmpty();
    }

    protected static class ExceptionAccumulator extends RxExceptionHandler {

        private final Stack<Class<? extends Throwable>> exceptions = new Stack<>();
        private final Outcome outcome;

        ExceptionAccumulator(Outcome outcome) {
            this.outcome = outcome;
        }

        @Override
        public Outcome onException(Throwable e) {
            if (CompositeException.class.isAssignableFrom(e.getClass())) {
                CompositeException composite = (CompositeException) e;
                for (Throwable throwable : composite.getExceptions()) {
                    exceptions.add(throwable.getClass());
                }
            } else {
                exceptions.add(e.getClass());
            }
            return outcome;
        }

        Stack<Class<? extends Throwable>> getExceptions() {
            return exceptions;
        }
    }

    private static class DifferentException extends RuntimeException {
    }

    private RxTestStep throwDifferentException() {
        return runnable(new Runnable() {
            @Override
            public void run() {
                throw new DifferentException();
            }
        });
    }
}

package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.impl.StackTraceFilter.filterListenerStackTrace;

import java.util.List;

import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.ericsson.de.scenariorx.api.events.RxEvent;
import com.ericsson.de.scenariorx.api.events.RxFlowEvent;
import com.ericsson.de.scenariorx.api.events.RxScenarioEvent;
import com.ericsson.de.scenariorx.api.events.RxTestStepEvent;
import com.ericsson.de.scenariorx.impl.Internals.Exec;
import com.ericsson.de.scenariorx.impl.Internals.TestStepResult;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

/**
 * Currently only used for {@link ScenarioListener}
 */
class ScenarioEventBus {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioEventBus.class);

    private EventBus eventBus;

    ScenarioEventBus(Iterable<ScenarioListener> listeners) {
        eventBus = new EventBus(ScenarioLoggingHandler.INSTANCE);
        for (ScenarioListener listener : listeners) {
            eventBus.register(listener);
        }
    }

    void scenarioStarted(Scenario scenario) {
        post(new RxScenarioEvent.RxScenarioStartedEvent(scenario));
    }

    void scenarioFinished(Scenario scenario, List<Internals.FlowExecutionResult> results) {
        post(new InternalScenarioFinishedEvent(scenario, results));
    }

    void flowStarted(Flow flow, Observable<RxDataRecordWrapper> dataSource) {
        post(new InternalFlowStartedEvent(flow, dataSource));
    }

    void flowFinished(Flow flow, Observable<RxDataRecordWrapper> dataSource) {
        post(new InternalFlowFinishedEvent(flow, dataSource));
    }

    void testStepStarted(RxTestStep testStep, Exec execution) {
        post(new InternalTestStepStartedEvent(testStep, execution));
    }

    void testStepFinished(RxTestStep testStep, Exec execution, TestStepResult result) {
        post(new InternalTestStepFinishedEvent(testStep, execution, result));
    }

    private void post(RxEvent event) {
        eventBus.post(event);
    }

    /*---------------- Internal Events ----------------*/

    static class InternalFlowStartedEvent extends RxFlowEvent.RxFlowStartedEvent {

        private Observable<RxDataRecordWrapper> dataSource;

        private InternalFlowStartedEvent(Flow flow, Observable<RxDataRecordWrapper> dataSource) {
            super(flow);
            this.dataSource = dataSource;
        }

        public Observable<RxDataRecordWrapper> getDataSource() {
            return dataSource;
        }
    }

    static class InternalFlowFinishedEvent extends RxFlowEvent.RxFlowFinishedEvent {

        private Observable<RxDataRecordWrapper> dataSource;

        private InternalFlowFinishedEvent(Flow flow, Observable<RxDataRecordWrapper> dataSource) {
            super(flow);
            this.dataSource = dataSource;
        }

        public Observable<RxDataRecordWrapper> getDataSource() {
            return dataSource;
        }
    }

    static class InternalScenarioFinishedEvent extends RxScenarioEvent.RxScenarioFinishedEvent {
        protected List<Internals.FlowExecutionResult> results;

        public InternalScenarioFinishedEvent(Scenario scenario, List<Internals.FlowExecutionResult> results) {
            super(scenario);
            this.results = results;
        }
    }

    static class InternalTestStepStartedEvent extends RxTestStepEvent.RxTestStepStartedEvent {

        private Exec execution;

        private InternalTestStepStartedEvent(RxTestStep testStep, Exec execution) {
            super(testStep, execution.dataRecord);
            this.execution = execution;
        }

        public Exec getExecution() {
            return execution;
        }
    }

    static class InternalTestStepFinishedEvent extends RxTestStepEvent.RxTestStepFinishedEvent {

        private Exec execution;
        private TestStepResult result;

        private InternalTestStepFinishedEvent(RxTestStep testStep, Exec execution, TestStepResult result) {
            super(testStep, result.status, result.error);
            this.execution = execution;
            this.result = result;
        }

        public TestStepResult getResult() {
            return result;
        }

        public Exec getExecution() {
             return execution;
        }
    }

    /*-------- end --------*/

    private static final class ScenarioLoggingHandler implements SubscriberExceptionHandler {

        static final ScenarioLoggingHandler INSTANCE = new ScenarioLoggingHandler();

        @Override
        public void handleException(Throwable exception, SubscriberExceptionContext context) {
            logger.error("Exception thrown by RxScenarioListener:", filterListenerStackTrace(exception));
        }
    }
}

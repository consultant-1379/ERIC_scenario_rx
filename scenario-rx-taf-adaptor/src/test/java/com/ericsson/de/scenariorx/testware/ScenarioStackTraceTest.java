package com.ericsson.de.scenariorx.testware;

import static com.ericsson.cifwk.taf.datasource.TafDataSources.cyclic;
import static com.ericsson.cifwk.taf.datasource.TafDataSources.fromClass;
import static com.ericsson.cifwk.taf.datasource.TafDataSources.fromTafDataProvider;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.annotatedMethod;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.compositeExceptionHandler;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.contextDataSource;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.dataSource;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.flow;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.runner;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.scenario;
import static com.ericsson.de.scenariorx.impl.Api.runnable;
import static com.ericsson.de.scenariorx.impl.ScenarioTest.nop;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.apache.log4j.Logger.getLogger;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ericsson.cifwk.taf.ServiceRegistry;
import com.ericsson.cifwk.taf.TestContext;
import com.ericsson.cifwk.taf.annotations.DataSource;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.cifwk.taf.datasource.DataRecord;
import com.ericsson.de.scenariorx.TafNode;
import com.ericsson.de.scenariorx.api.RxApi;
import com.ericsson.de.scenariorx.api.RxContextDataSource;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.RxScenarioListener;
import com.ericsson.de.scenariorx.api.RxScenarioRunner;
import com.ericsson.de.scenariorx.api.events.RxFlowEvent;
import com.ericsson.de.scenariorx.api.events.RxScenarioEvent;
import com.ericsson.de.scenariorx.api.events.RxTestStepEvent;
import com.ericsson.de.scenariorx.impl.DataSourceStrategy;
import com.ericsson.de.scenariorx.impl.ExceptionHandler;
import com.ericsson.de.scenariorx.impl.ScenarioTest.ThrowExceptionNow;
import com.ericsson.de.scenariorx.impl.TafDataSource;
import com.ericsson.de.scenariorx.operators.ExternalOperator;
import com.ericsson.de.scenariorx.operators.ExternalOperator.VerySpecialExternalOperatorException;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.junit.Test;
import rx.exceptions.CompositeException;

@SuppressWarnings("DanglingJavadoc")
public class ScenarioStackTraceTest {

    private static final String DUMMY = "dummy";
    private static final String TEST_STEP_THROWING_RUNTIME_EXCEPTION = "testStepThrowingRuntimeException";
    private static final String TEST_STEP_THROWING_CHECKED_EXCEPTION = "testStepThrowingCheckedException";
    private static final String INTERNAL_OPERATOR_THROWING_EXCEPTION = "internalOperatorThrowingException";
    private static final String EXTERNAL_OPERATOR_THROWING_EXCEPTION = "externalOperatorThrowingException";
    public static final int EXCEPTION_THRESHOLD = 25;

    @Test
    public void frameworkException_contextDataSource_notPopulated() throws Exception {
        RxContextDataSource<RxDataRecord> contextDataSource = contextDataSource("name", RxDataRecord.class);

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(annotatedMethod(this, DUMMY))
                        .withDataSources(contextDataSource)
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThat(e).hasMessageContaining("Data Source name was not populated during scenario execution");
        assertThat(e.getStackTrace().length).isLessThan(20);
    }

    @Test
    public void frameworkException_dataSourceEmpty_badFiltering() throws Exception {
        final String dataSourceName = "bad filtering";
        TestContext testContext = ServiceRegistry.getTestContextProvider().get();
        testContext.addDataSource(dataSourceName, fromTafDataProvider("nodes"));

        RxScenario scenario = scenario()
                .addFlow(flow("flow")
                        .addTestStep(annotatedMethod(this, DUMMY))
                        .withDataSources(dataSource(dataSourceName, TafNode.class)
                                .filterField("nodeType").equalTo("non existing node type")
                        )
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThat(e).hasMessageContaining(String.format(DataSourceStrategy.ERROR_DATA_SOURCE_EMPTY, "bad filtering"));
        assertThat(e.getStackTrace().length).isLessThan(20);
    }

    @Test
    public void frameworkException_dataSourceEmpty_customImplementation() throws Exception {
        RxDataSource<RxDataRecord> empty = new RxDataSource<RxDataRecord>("empty", RxDataRecord.class) {
            @Override
            public Iterator<? extends RxDataRecord> getIterator() {
                return Lists.<RxDataRecord>newArrayList().iterator();
            }

            @Override
            protected RxDataSource<RxDataRecord> newDefinition() {
                return this;
            }
        };

        RxScenario scenario = scenario()
                .addFlow(flow("flow")
                        .addTestStep(annotatedMethod(this, DUMMY))
                        .withDataSources(empty)
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThat(e).hasMessageContaining(String.format(DataSourceStrategy.ERROR_DATA_SOURCE_EMPTY, "empty"));
        assertThat(e.getStackTrace().length).isLessThan(20);
    }

    @Test
    public void frameworkException_tafDataSource_notFound() {
        String dataSourceName = "notExisting";
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(annotatedMethod(this, DUMMY))
                        .withDataSources(dataSource(dataSourceName, DataRecord.class))
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThat(e).hasMessageContaining(TafDataSource.ERROR_NOT_FOUND + dataSourceName);
        assertThat(e.getStackTrace().length).isLessThan(20);
    }

    @Test
    public void frameworkException_tafDataSource_empty() {
        String dataSourceName = "empty";
        TestContext testContext = ServiceRegistry.getTestContextProvider().get();
        testContext.addDataSource(dataSourceName, fromClass(EmptyProvider.class));

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(annotatedMethod(this, DUMMY))
                        .withDataSources(dataSource(dataSourceName, DataRecord.class))
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThat(e).hasMessageContaining(TafDataSource.ERROR_EMPTY);
        assertThat(e.getStackTrace().length).isLessThan(20);
    }

    @Test
    public void frameworkException_tafDataSource_implicitCycling() throws Exception {
        String dataSourceName = "implicitCycle";
        TestContext testContext = ServiceRegistry.getTestContextProvider().get();
        testContext.addDataSource(dataSourceName, cyclic(fromTafDataProvider("nodes")));

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(annotatedMethod(this, DUMMY))
                        .withDataSources(dataSource(dataSourceName, DataRecord.class))
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThat(e).hasMessageContaining(TafDataSource.ERROR_IMPLICIT_CYCLING);
        assertThat(e.getStackTrace().length).isLessThan(20);
    }

    @Test
    public void frameworkException_exceptionHandler_returnNull() throws Exception {
        RxExceptionHandler nullHandler = new RxExceptionHandler() {
            @Override
            public Outcome onException(Throwable e) {
                return null;
            }
        };
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(new ThrowExceptionNow("exception"))
                        .withExceptionHandler(nullHandler)
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThat(e).hasMessageContaining(ExceptionHandler.ERROR_EXCEPTION_HANDLER_RETURN_NULL);
        assertThat(e.getStackTrace().length).isLessThan(EXCEPTION_THRESHOLD);
    }

    @Test
    public void frameworkException_compositeExceptionHandler_returnNull() throws Exception {
        RxExceptionHandler nullHandler = new RxExceptionHandler() {
            @Override
            public Outcome onException(Throwable e) {
                return null;
            }
        };
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(new ThrowExceptionNow("exception"))
                        .withExceptionHandler(
                                compositeExceptionHandler()
                                        .setFinalExceptionHandler(nullHandler)
                                        .build()
                        )
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThat(e).hasMessageContaining(ExceptionHandler.ERROR_EXCEPTION_HANDLER_RETURN_NULL);
        assertThat(e.getStackTrace().length).isLessThan(EXCEPTION_THRESHOLD);
    }

    @Test
    public void testwareException_inBaseTestStepInvocation() throws Exception {
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(runnable(new Runnable() {
                            @Override
                            public void run() {
                                throw new VerySpecialRuntimeException();
                            }
                        }))
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThatStackTraceContainsExactly(e, VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest$4.run",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.api.RxApi.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testwareException_inBaseTestStepInvocation"
        );
    }

    @Test
    public void testwareException_inRunnable() throws Exception {
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(runnable(new Runnable() {
                            @Override
                            public void run() {
                                throw new VerySpecialRuntimeException();
                            }
                        }))
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThatStackTraceContainsExactly(e, VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest$5.run",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.api.RxApi.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testwareException_inRunnable"
        );
    }

    @Test
    public void testwareException_inAnnotatedMethod_runtimeException() {
        RxScenario scenario = scenario()
                .addFlow(flow()
                        /** {@link ScenarioStackTraceTest#testStepThrowingRuntimeException()} */
                        .addTestStep(annotatedMethod(this, TEST_STEP_THROWING_RUNTIME_EXCEPTION))
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThatStackTraceContainsExactly(e, VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testStepThrowingRuntimeException",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.api.RxApi.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testwareException_inAnnotatedMethod_runtimeException"
        );
    }

    @Test
    public void testwareException_inAnnotatedMethod_checkedException() {
        RxScenario scenario = scenario()
                .addFlow(flow()
                        /** {@link ScenarioStackTraceTest#testStepThrowingCheckedException()} */
                        .addTestStep(annotatedMethod(this, TEST_STEP_THROWING_CHECKED_EXCEPTION))
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThatWrappedExceptionStackTraceContainsExactly(e, VerySpecialCheckedException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testStepThrowingCheckedException",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.api.RxApi.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testwareException_inAnnotatedMethod_checkedException"
        );
    }

    @Test
    public void testwareException_inAnnotatedMethod_internalOperator() {
        RxScenario scenario = scenario()
                .addFlow(flow()
                        /** {@link ScenarioStackTraceTest#internalOperatorThrowingException()} */
                        .addTestStep(annotatedMethod(this, INTERNAL_OPERATOR_THROWING_EXCEPTION))
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThatStackTraceContainsExactly(e, VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest$InternalOperator.throwException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.internalOperatorThrowingException",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.api.RxApi.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testwareException_inAnnotatedMethod_internalOperator"
        );
    }

    @Test
    public void testwareException_inAnnotatedMethod_externalOperator() {
        RxScenario scenario = scenario()
                .addFlow(flow()
                        /** {@link ScenarioStackTraceTest#internalOperatorThrowingException()} */
                        .addTestStep(annotatedMethod(this, EXTERNAL_OPERATOR_THROWING_EXCEPTION))
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThatStackTraceContainsExactly(e, VerySpecialExternalOperatorException.MESSAGE,
                "com.ericsson.de.scenariorx.operators.ExternalOperator.throwException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.externalOperatorThrowingException",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.api.RxApi.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testwareException_inAnnotatedMethod_externalOperator"
        );
    }

    @Test
    public void testwareException_compositeException_alwaysRun() throws Exception {
        RxScenario scenario = scenario()
                .addFlow(flow("Flow 1")
                        /** {@link ScenarioStackTraceTest#testStepThrowingRuntimeException()} */
                        .addTestStep(annotatedMethod(this, TEST_STEP_THROWING_RUNTIME_EXCEPTION))
                        /** {@link ScenarioStackTraceTest#testStepThrowingCheckedException()} */
                        .addTestStep(annotatedMethod(this, TEST_STEP_THROWING_CHECKED_EXCEPTION))
                        .alwaysRun()
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThat(e).isInstanceOf(CompositeException.class);
        CompositeException composite = (CompositeException) e;
        assertThatStackTraceContainsExactly(composite, "2 exceptions occurred. ");

        List<Throwable> exceptions = composite.getExceptions();
        assertThat(exceptions).hasSize(2);
        assertThatStackTraceContainsExactly(exceptions.get(0), VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testStepThrowingRuntimeException",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.api.RxApi.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testwareException_compositeException_alwaysRun");
        assertThatWrappedExceptionStackTraceContainsExactly(exceptions.get(1), VerySpecialCheckedException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testStepThrowingCheckedException",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.api.RxApi.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testwareException_compositeException_alwaysRun");
    }

    @Test
    public void testwareException_compositeException_split() throws Exception {
        //noinspection unchecked
        RxScenario scenario = scenario()
                .split(
                        flow("Flow 1")
                                /** {@link ScenarioStackTraceTest#testStepThrowingRuntimeException()} */
                                .addTestStep(annotatedMethod(this, TEST_STEP_THROWING_RUNTIME_EXCEPTION)),
                        flow("Flow 2")
                                /** {@link ScenarioStackTraceTest#testStepThrowingCheckedException()} */
                                .addTestStep(annotatedMethod(this, TEST_STEP_THROWING_CHECKED_EXCEPTION))
                )
                .build();

        Throwable e = runExpectingException(scenario);

        assertThat(e).isInstanceOf(CompositeException.class);
        CompositeException composite = (CompositeException) e;
        assertThatStackTraceContainsExactly(composite, "2 exceptions occurred. ");

        List<Throwable> exceptions = composite.getExceptions();
        assertThat(exceptions).hasSize(2);
        assertThatStackTraceContainsExactly(exceptions.get(0), VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testStepThrowingRuntimeException",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "java.util.concurrent.ThreadPoolExecutor.runWorker",
                "java.util.concurrent.ThreadPoolExecutor$Worker.run",
                "java.lang.Thread.run"
        );
        assertThatWrappedExceptionStackTraceContainsExactly(exceptions.get(1), VerySpecialCheckedException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.testStepThrowingCheckedException",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "java.util.concurrent.ThreadPoolExecutor.runWorker",
                "java.util.concurrent.ThreadPoolExecutor$Worker.run",
                "java.lang.Thread.run"
        );
    }

    @Test
    public void listenerException_scenarioStarted() throws Exception {
        RxScenarioListener listener = new RxScenarioListener() {
            @Override
            public void onScenarioStarted(RxScenarioEvent.RxScenarioStartedEvent event) {
                throw new VerySpecialRuntimeException();
            }
        };

        runExpectingListenerException(listener, "Exception thrown by RxScenarioListener:",
                VerySpecialRuntimeException.class.getName() + ": " + VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest$6.onScenarioStarted",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.assertThatErrorLogContainsExactly",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingListenerException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.listenerException_scenarioStarted"
        );
    }

    @Test
    public void listenerException_scenarioFinished() throws Exception {
        RxScenarioListener listener = new RxScenarioListener() {
            @Override
            public void onScenarioFinished(RxScenarioEvent.RxScenarioFinishedEvent event) {
                throw new VerySpecialRuntimeException();
            }
        };

        runExpectingListenerException(listener, "Exception thrown by RxScenarioListener:",
                VerySpecialRuntimeException.class.getName() + ": " + VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest$7.onScenarioFinished",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.assertThatErrorLogContainsExactly",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingListenerException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.listenerException_scenarioFinished"
        );
    }

    @Test
    public void listenerException_flowStarted() throws Exception {
        RxScenarioListener listener = new RxScenarioListener() {
            @Override
            public void onFlowStarted(RxFlowEvent.RxFlowStartedEvent event) {
                throw new VerySpecialRuntimeException();
            }
        };

        runExpectingListenerException(listener, "Exception thrown by RxScenarioListener:",
                VerySpecialRuntimeException.class.getName() + ": " + VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest$8.onFlowStarted",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.assertThatErrorLogContainsExactly",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingListenerException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.listenerException_flowStarted"
        );
    }

    @Test
    public void listenerException_flowFinished() throws Exception {
        RxScenarioListener listener = new RxScenarioListener() {
            @Override
            public void onFlowFinished(RxFlowEvent.RxFlowFinishedEvent event) {
                throw new VerySpecialRuntimeException();
            }
        };

        runExpectingListenerException(listener, "Exception thrown by RxScenarioListener:",
                VerySpecialRuntimeException.class.getName() + ": " + VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest$9.onFlowFinished",
                "com.ericsson.de.scenariorx.api.RxTestStep.run",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.assertThatErrorLogContainsExactly",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingListenerException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.listenerException_flowFinished"
        );
    }

    @Test
    public void listenerException_testStepStarted() throws Exception {
        RxScenarioListener listener = new RxScenarioListener() {
            @Override
            public void onTestStepStarted(RxTestStepEvent.RxTestStepStartedEvent event) {
                throw new VerySpecialRuntimeException();
            }
        };

        runExpectingListenerException(listener, "Exception thrown by RxScenarioListener:",
                VerySpecialRuntimeException.class.getName() + ": " + VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest$10.onTestStepStarted",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.assertThatErrorLogContainsExactly",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingListenerException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.listenerException_testStepStarted"
        );
    }

    @Test
    public void listenerException_testStepFinished() throws Exception {
        RxScenarioListener listener = new RxScenarioListener() {
            @Override
            public void onTestStepFinished(RxTestStepEvent.RxTestStepFinishedEvent event) {
                throw new VerySpecialRuntimeException();
            }
        };

        runExpectingListenerException(listener, "Exception thrown by RxScenarioListener:",
                VerySpecialRuntimeException.class.getName() + ": " + VerySpecialRuntimeException.MESSAGE,
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest$11.onTestStepFinished",
                "com.ericsson.de.scenariorx.api.RxScenarioRunner.run",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.assertThatErrorLogContainsExactly",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.runExpectingListenerException",
                "com.ericsson.de.scenariorx.testware.ScenarioStackTraceTest.listenerException_testStepFinished"
        );
    }

    @TestStep(id = DUMMY)
    @SuppressWarnings("unused")
    public void dummy() {
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    @TestStep(id = TEST_STEP_THROWING_RUNTIME_EXCEPTION)
    public void testStepThrowingRuntimeException() {
        throw new VerySpecialRuntimeException();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    @TestStep(id = TEST_STEP_THROWING_CHECKED_EXCEPTION)
    public void testStepThrowingCheckedException() throws VerySpecialCheckedException {
        throw new VerySpecialCheckedException();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    @TestStep(id = INTERNAL_OPERATOR_THROWING_EXCEPTION)
    public void internalOperatorThrowingException() {
        InternalOperator.throwException();
    }

    @SuppressWarnings("unused")
    @TestStep(id = EXTERNAL_OPERATOR_THROWING_EXCEPTION)
    public void externalOperatorThrowingException() {
        ExternalOperator.throwException();
    }

    private static class InternalOperator {
        static void throwException() {
            throw new VerySpecialRuntimeException();
        }
    }

    private static class VerySpecialRuntimeException extends RuntimeException {

        private static final String MESSAGE = "This is a very special runtime exception";

        VerySpecialRuntimeException() {
            super(MESSAGE);
        }
    }

    private static class VerySpecialCheckedException extends Exception {

        private static final String MESSAGE = "This is a very special checked exception";

        VerySpecialCheckedException() {
            super(MESSAGE);
        }
    }

    public static class EmptyProvider {
        @DataSource
        @SuppressWarnings("unused")
        public List<Map<String, Object>> records() {
            return emptyList();
        }
    }

    private Throwable runExpectingException(RxScenario scenario) {
        try {
            RxApi.run(scenario);
        } catch (Throwable e) {
            return e;
        }
        throw new RuntimeException("This exception should never get thrown");
    }

    private void runExpectingListenerException(RxScenarioListener listener, String... expectedStackTrace) {
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(nop())
                )
                .build();

        RxScenarioRunner runner = runner()
                .addListener(listener)
                .build();

        assertThatErrorLogContainsExactly(runner, scenario, expectedStackTrace);
    }

    private void assertThatWrappedExceptionStackTraceContainsExactly(Throwable e, String errorMessage,
                                                                     String... expectedStackTrace) {
        Throwable cause = e.getCause();

        assertThat(e).isInstanceOf(RuntimeException.class);
        assertThat(cause).isInstanceOf(Exception.class);

        /** @see Throwable#toString()  */
        assertThat(e.getMessage()).isEqualTo(cause.getClass().getName() + ": " + errorMessage);

        assertThatStackTraceContainsExactly(cause, errorMessage, expectedStackTrace);
    }

    private void assertThatStackTraceContainsExactly(Throwable e, String errorMessage,
                                                     String... expectedStackTrace) {
        assertThat(e.getMessage()).isEqualTo(errorMessage);

        List<String> actualStackTrace = newArrayList();
        for (StackTraceElement element : e.getStackTrace()) {
            // omit line numbers to avoid constantly fixing them upon any change in code
            actualStackTrace.add(element.getClassName() + "." + element.getMethodName());
        }
        assertThat(actualStackTrace).containsExactly(expectedStackTrace);
    }

    private void assertThatErrorLogContainsExactly(RxScenarioRunner runner, RxScenario scenario,
                                                   String... expectedStackTrace) {
        StringWriter logOutput = new StringWriter();
        Appender appender = new WriterAppender(new PatternLayout(), logOutput);
        Logger logger = getLogger("com.ericsson.de.scenariorx.impl.ScenarioEventBus");
        logger.addAppender(appender);

        runner.run(scenario);

        String[] logLines = logOutput.toString().split(Layout.LINE_SEP);
        List<String> methodCalls = from(logLines).transform(ToMethodCall.INSTANCE).toList();
        assertThat(methodCalls).containsExactly(expectedStackTrace);
        logger.removeAppender(appender);
    }

    private static final class ToMethodCall implements Function<String, String> {

        static final ToMethodCall INSTANCE = new ToMethodCall();

        private static final Pattern STACK_TRACE_LINE_PATTERN = Pattern.compile("\\s+at (.+?)\\(.+");

        @Override
        public String apply(String logLine) {
            Matcher matcher = STACK_TRACE_LINE_PATTERN.matcher(logLine);
            return matcher.find() ? matcher.group(1) : logLine;
        }
    }
}

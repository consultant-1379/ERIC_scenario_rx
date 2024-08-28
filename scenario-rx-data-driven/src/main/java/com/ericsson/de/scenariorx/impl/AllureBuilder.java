package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.impl.Internals.Exec;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.getLast;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.AllureFacade;
import com.ericsson.cifwk.taf.AllureProvider;
import com.ericsson.cifwk.taf.TestCaseBean;
import com.ericsson.cifwk.taf.datasource.DataRecord;
import com.ericsson.de.scenariorx.api.RxDataDrivenApi;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.impl.Internals.FlowExecutionResult;
import com.ericsson.de.scenariorx.impl.Internals.TestStepResult;
import com.ericsson.de.scenariorx.impl.graph.ScenarioExecutionGraph;
import com.ericsson.de.scenariorx.impl.graph.export.SvgExporter;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.events.StepFailureEvent;
import ru.yandex.qatools.allure.events.StepFinishedEvent;
import ru.yandex.qatools.allure.events.StepStartedEvent;
import ru.yandex.qatools.allure.events.TestCaseFailureEvent;
import ru.yandex.qatools.allure.events.TestCaseFinishedEvent;
import ru.yandex.qatools.allure.model.Attachment;
import ru.yandex.qatools.allure.model.Step;
import ru.yandex.qatools.allure.model.TestCaseResult;
import ru.yandex.qatools.allure.utils.AllureResultsUtils;

class AllureBuilder {
    public static final String TEST_CASE_NOT_STARTED = "TestId not found as test case didn't start successfully, refer to log for details";
    private final Multimap<String, TestStepResult> testCases = ArrayListMultimap.create();
    private final Allure provider = AllureProvider.singletone();

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureBuilder.class);

    private AllureBuilder() {
    }

    static void build(List<FlowExecutionResult> results, RxScenario scenario) {

        AllureBuilder allureBuilder = new AllureBuilder();
        try {
            for (FlowExecutionResult result : results) {
                allureBuilder.parseResults(result);
            }
            allureBuilder.saveReport(results, getAttachmentName(scenario));
        } catch (Exception e) {
            allureBuilder.addDummyTestCase(e);
        }
    }

    private void parseResults(FlowExecutionResult result) {
        for (Exec execution : result.executions) {
            try {
                Optional<String> testCaseIdOpt = execution.getDataRecordAndContext().getFieldValue(RxDataDrivenApi.TEST_CASE_ID, String.class);
                checkState(testCaseIdOpt.isPresent(), RxDataDrivenApi.ERROR_MISSING_TEST_ID);
                String testCaseId = testCaseIdOpt.get();

                for (TestStepResult stepResult : execution.getExecutedTestSteps()) {
                    if (stepResult instanceof FlowExecutionResult) {
                        FlowExecutionResult flowExecutionResult = FlowExecutionResult.class.cast(stepResult);
                        parseResults(flowExecutionResult);
                    } else {
                        testCases.put(testCaseId, stepResult);
                    }
                }
            } catch (Exception e) {
                addDummyTestCase(e);
            }
        }
    }

    private void saveReport(List<FlowExecutionResult> results, String name) {
        checkState(AllureFacade.getCurrentSuiteName() != null, "Test Suite not started");

        Attachment attachment = saveGraphAsAttachment(results, name);

        for (Map.Entry<String, Collection<TestStepResult>> testCase : testCases.asMap().entrySet()) {
            Throwable exception = null;
            allureStartTestCase(testCase.getKey());
            Collection<TestStepResult> testSteps = testCase.getValue();

            for (TestStepResult testStep : testSteps) {
                allureAddTestStep(testStep, attachment);
                if (testStep.isFailed()) {
                    exception = testStep.error;
                }
            }

            ExecutionTime executionTime = new ExecutionTime(
                    getFirst(testSteps, null).startTime,
                    getLast(testSteps).endTime);

            allureStopTestCase(attachment, exception, executionTime);
        }
    }

    private Attachment saveGraphAsAttachment(List<FlowExecutionResult> results, String name) {
        try {
            ScenarioExecutionGraph graph = GraphBuilder.build(results);
            SvgExporter svgExporter = new SvgExporter();
            ByteArrayOutputStream svg = new ByteArrayOutputStream();
            svgExporter.export(graph, new OutputStreamWriter(svg));
            byte[] graphBytes = svg.toByteArray();

            return allureAttachment(graphBytes, name);
        } catch (Exception e) {
            LOGGER.error("Error saving scenario execution graph", e);
        }
        return new Attachment();
    }

    private Attachment allureAttachment(byte[] graphBytes, String name) {
        return AllureResultsUtils.writeAttachmentSafely(graphBytes, name, "image/svg+xml");
    }

    private static String getAttachmentName(RxScenario scenario) {
        String safeScenarioName = scenario.getName().replaceAll("\\W+", "");
        return safeScenarioName + "_" + System.currentTimeMillis();
    }

    private void addDummyTestCase(Exception exception) {
        allureStartTestCase(TEST_CASE_NOT_STARTED);
        provider.fire(new TestCaseFailureEvent().withThrowable(exception));
        provider.fire(new TestCaseFinishedEvent());
    }

    private void allureStartTestCase(final String testCaseId) {
        TestCaseBean testCaseBean = new TestCaseBean(testCaseId, Maps.<String, DataRecord>newLinkedHashMap());
        String testName = AllureFacade.getTestName(testCaseId, testCaseBean.getParameters().values().toArray());
        String suiteName = AllureFacade.getCurrentSuiteName();

        AllureFacade.startTestCase(suiteName, testName, testCaseId, testCaseBean);
    }

    private void allureStopTestCase(Attachment attachment, final Throwable e, ExecutionTime executionTime) {
        if (e != null) {
            provider.fire((new TestCaseFailureEvent()).withThrowable(e));
        }
        provider.fire(new ScenarioTestCaseFinishedEvent(attachment, executionTime));
    }

    private void allureAddTestStep(TestStepResult testStep, Attachment graph) {
        provider.fire(new StepStartedEvent(testStep.name));
        if (testStep.isFailed()) {
            provider.fire(new StepFailureEvent().withThrowable(testStep.error));
        }
        provider.fire(new BackDatingStepFinishedEvent(testStep, graph));
    }

    public static class BackDatingStepFinishedEvent extends StepFinishedEvent {
        private TestStepResult testStep;
        private Attachment graph;

        BackDatingStepFinishedEvent(TestStepResult testStep, Attachment graph) {
            this.testStep = testStep;
            this.graph = graph;
        }

        @Override
        public void process(Step step) {
            super.process(step);
            step.setTitle(testStep.name);
            step.setStart(testStep.startTime);
            step.setStop(testStep.endTime);

            Attachment attachment = new Attachment();
            attachment.setTitle("Read more");
            attachment.setSource(graph.getSource() + "#" + testStep.id);

            step.withAttachments(attachment);
        }
    }

    public static class ScenarioTestCaseFinishedEvent extends TestCaseFinishedEvent {
        private Attachment attachment;
        private ExecutionTime executionTime;

        ScenarioTestCaseFinishedEvent(Attachment attachment, ExecutionTime executionTime) {
            this.attachment = attachment;
            this.executionTime = executionTime;
        }

        @Override
        public void process(TestCaseResult testCase) {
            super.process(testCase);
            testCase.withAttachments(attachment);
            testCase.setStart(executionTime.startTime);
            testCase.setStop(executionTime.stopTime);
        }
    }

    private static class ExecutionTime {
        private long startTime;
        private long stopTime;

        private ExecutionTime(long startTime, long stopTime) {
            this.startTime = startTime;
            this.stopTime = stopTime;
        }
    }
}

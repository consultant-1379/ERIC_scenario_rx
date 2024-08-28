package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.api.RxTestStep.Status.FAILED;
import static com.ericsson.de.scenariorx.api.RxTestStep.Status.SUCCESS;
import static java.util.Arrays.asList;

import java.util.List;

import org.junit.Test;

public class PerformanceReporterTest {
    @Test
    public void name() throws Exception {
        List<Internals.TestStepResult> testStepResults = asList(
                new Internals.TestStepResult("1", "step1", 0, 105, null, null, SUCCESS),
                new Internals.TestStepResult("1", "step1", 0, 101, null, null, SUCCESS),
                new Internals.TestStepResult("1", "step1", 0, 1501, null, null, SUCCESS),
                new Internals.TestStepResult("2", "step2", 0, 4001, null, new RuntimeException(), FAILED)
        );

        new PerformanceReporter().summary("test", testStepResults);
    }
}
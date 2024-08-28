package com.ericsson.de.scenariorx.cucumber;

import java.io.IOException;
import java.util.Arrays;

import com.ericsson.de.scenariorx.api.RxScenarioListener;
import com.ericsson.de.scenariorx.api.events.RxScenarioEvent;
import com.ericsson.de.scenariorx.api.events.RxTestStepEvent;
import com.ericsson.de.scenariorx.impl.ScenarioRuntime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

public class RxCucumberRunner extends Runner {

    private final RuntimeOptions runtimeOptions;

    public RxCucumberRunner(Class clazz) {
        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz);
        runtimeOptions = runtimeOptionsFactory.create();
    }

    @Override
    public Description getDescription() {
        return getSuiteDescription();
    }

    private Description getSuiteDescription() {
        return Description.createSuiteDescription("Rx runner description");
    }

    @Override
    public void run(RunNotifier runNotifier) {
        ScenarioRuntime scenarioRuntime = new ScenarioRuntime();
        try {
            scenarioRuntime.run(runtimeOptions, Arrays.asList((RxScenarioListener) new JUnitListener(runNotifier)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class JUnitListener extends RxScenarioListener {
        private RunNotifier runNotifier;

        public JUnitListener(RunNotifier runNotifier) {
            this.runNotifier = runNotifier;
        }

        @Override
        public void onScenarioStarted(RxScenarioEvent.RxScenarioStartedEvent event) {
            runNotifier.fireTestStarted(Description.createSuiteDescription(event.getScenario().getName()));
        }

        @Override
        public void onTestStepFinished(RxTestStepEvent.RxTestStepFinishedEvent event) {
            if (event.getError()!=null) {
                runNotifier.fireTestFailure(new Failure(Description.createSuiteDescription(event.getName()), event.getError()));
            }
        }

        @Override
        public void onScenarioFinished(RxScenarioEvent.RxScenarioFinishedEvent event) {
            runNotifier.fireTestFinished(Description.createSuiteDescription(event.getScenario().getName()));
        }
    }
}

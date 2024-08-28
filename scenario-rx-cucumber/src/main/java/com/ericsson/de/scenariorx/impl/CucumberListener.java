package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.impl.GherkinAdapter.adapt;
import static com.ericsson.de.scenariorx.impl.GherkinAdapter.step;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxScenarioListener;
import com.ericsson.de.scenariorx.api.events.RxScenarioEvent;
import com.google.common.eventbus.Subscribe;
import gherkin.ast.Feature;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Result;

public class CucumberListener extends RxScenarioListener {
    private Reporter reporter;
    private Formatter formatter;
    Pattern firstField = Pattern.compile("\":\"([^\"]{0,15}).*\"");

    public CucumberListener(Reporter reporter, Formatter formatter) {
        this.reporter = reporter;
        this.formatter = formatter;
    }

    @Subscribe
    public void onScenarioStarted(RxScenarioEvent.RxScenarioStartedEvent event) {
        formatter.scenario(adapt(event.getScenario()));
    }

    @Subscribe
    public void debugTestStep(ScenarioEventBus.InternalTestStepFinishedEvent event) {
        Internals.Exec execution = event.getExecution();
        formatter.step(step(event.getName() + " (usr:" + execution.vUser + "/dr:" + printValue(execution.dataRecord) + "...)"));
        reporter.result(new Result(adapt(event.getStatus()), 0L, event.getError(), null));
    }

    public void write(String text) {
        reporter.write(text);
    }

    public void onFeatureStarted(Feature feature) {
        formatter.feature(adapt(feature));
    }

    public void done() {
        formatter.done();
    }

    private String printValue(RxDataRecordWrapper dataRecord) {
        Matcher matcher = firstField.matcher(dataRecord.toString());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return dataRecord.getIteration();
        }
    }

    public Reporter getReporter() {
        return reporter;
    }
}

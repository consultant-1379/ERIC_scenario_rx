package com.ericsson.de.scenariorx;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;

import com.ericsson.de.scenariorx.impl.ScenarioRuntime;
import cucumber.runtime.RuntimeOptions;

/**
 * Runner which brings the RxScenario specific features to the Gherkin language.
 */
public class ScenarioCucumberRunner {
    public static void main(String[] args) throws IOException {
        RuntimeOptions runtimeOptions = new RuntimeOptions(new ArrayList<>(asList(args)));
        ScenarioRuntime scenarioRuntime = new ScenarioRuntime();
        scenarioRuntime.run(runtimeOptions);
    }
}
package com.ericsson.de.scenariorx.cucumber;

import cucumber.api.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(RxCucumberRunner.class)
@CucumberOptions(features = "classpath:performance_features", plugin = {"pretty"}, glue = {"com.ericsson.de.scenariorx.cucumber"})
public class JunitPerformanceRunnerTest {
}

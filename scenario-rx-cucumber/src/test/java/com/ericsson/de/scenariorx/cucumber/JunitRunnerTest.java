package com.ericsson.de.scenariorx.cucumber;

import cucumber.api.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(RxCucumberRunner.class)
@CucumberOptions(features = "classpath:features", plugin = {"pretty", "html:target/site/cucumber-pretty", "json:target/cucumber.json"}, glue = {"com.ericsson.de.scenariorx.cucumber"})
public class JunitRunnerTest {
}

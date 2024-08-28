package com.ericsson.de.scenariorx.cucumber.extra;

import cucumber.api.java.en.Given;

/**
 * Steps provided for autocompletion
 */
public class ExtraStepDefinitions {
    public static final String REPEATING_FROM_FILE = "^repeating from file (.*)$";
    public static final String REPEATING_FROM = "^repeating from Data Source named (.*)$";
    public static final String REPEATING_FROM_DEFAULT = "^repeating from Data Source with name (.*) or file (.*)$";
    public static final String NUMBER_OF_USERS_EQUALS = "^number of users equals (\\d+)$";
    public static final String RAMPUP_DURING = "^rampup during (\\d+) (.*)$";
    public static final String REPEAT_FOR = "^repeat for (\\d+) (.*)$";
    public static final String PERFORMANCE_TEST = "^performance test$";

    @Given(REPEATING_FROM_FILE)
    public void repeatingFromFile() {
        throw new IllegalStateException(
                "These steps are not intended to run!\n" +
                        "Use it for configuration with Rx Scenario Cucumber runner\n" +
                        "http://todo"
        );
    }

    @Given(REPEATING_FROM)
    public void repeatingFrom()  {
        throw new IllegalStateException(
                "These steps are not intended to run!\n" +
                        "Use it for configuration with Rx Scenario Cucumber runner\n" +
                        "http://todo"
        );
    }

    @Given(REPEATING_FROM_DEFAULT)
    public void repeatingFromDefault() {
        throw new IllegalStateException(
                "These steps are not intended to run!\n" +
                        "Use it for configuration with Rx Scenario Cucumber runner\n" +
                        "http://todo"
        );
    }


    @Given(NUMBER_OF_USERS_EQUALS)
    public void numberOfUsersEquals(int arg1) {
        throw new IllegalStateException(
                "These steps are not intended to run!\n" +
                        "Use it for configuration with Rx Scenario Cucumber runner\n" +
                        "http://todo"
        );
    }

    @Given(PERFORMANCE_TEST)
    public void performance() {
        throw new IllegalStateException(
                "These steps are not intended to run!\n" +
                        "Use it for configuration with Rx Scenario Cucumber runner\n" +
                        "http://todo"
        );
    }

    @Given(RAMPUP_DURING)
    public void rampupDuring(int arg1, String arg2) {
        throw new IllegalStateException(
                "These steps are not intended to run!\n" +
                        "Use it for configuration with Rx Scenario Cucumber runner\n" +
                        "http://todo"
        );
    }

    @Given(REPEAT_FOR)
    public void repeatFor(int arg1, String arg2) {
        throw new IllegalStateException(
                "These steps are not intended to run!\n" +
                        "Use it for configuration with Rx Scenario Cucumber runner\n" +
                        "http://todo"
        );
    }
}

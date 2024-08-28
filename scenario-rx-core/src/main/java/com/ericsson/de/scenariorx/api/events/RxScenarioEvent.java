package com.ericsson.de.scenariorx.api.events;

import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.impl.Scenario;

public class RxScenarioEvent extends RxEvent {

    private final RxScenario scenario;

    private RxScenarioEvent(Scenario scenario) {
        this.scenario = (RxScenario) scenario;
    }

    public RxScenario getScenario() {
        return scenario;
    }

    public static class RxScenarioStartedEvent extends RxScenarioEvent {

        public RxScenarioStartedEvent(Scenario scenario) {
            super(scenario);
        }
    }

    public static class RxScenarioFinishedEvent extends RxScenarioEvent {

        public RxScenarioFinishedEvent(Scenario scenario) {
            super(scenario);
        }
    }
}

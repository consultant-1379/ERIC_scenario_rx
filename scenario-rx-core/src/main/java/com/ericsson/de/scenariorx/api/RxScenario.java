package com.ericsson.de.scenariorx.api;

import java.util.List;
import java.util.Map;

import com.ericsson.de.scenariorx.impl.Scenario;
import com.ericsson.de.scenariorx.impl.ScenarioListener;

public class RxScenario extends Scenario {

    public RxScenario(String name, Map<String, Object> parameters, RxFlow flow, List<ScenarioListener> listeners) {
        super(name, parameters, flow, listeners);
    }
}

package com.ericsson.de.scenariorx.api;

import java.util.Map;

import com.ericsson.de.scenariorx.impl.FlowBuilder;
import com.ericsson.de.scenariorx.impl.ScenarioBuilder;

public class RxScenarioBuilder extends ScenarioBuilder<RxScenario, RxFlow> {

    private RxScenarioBuilder(String name) {
        super(name, (FlowBuilder<RxFlow>) RxFlowBuilder.flow(name));
    }

    static RxScenarioBuilderInterfaces.ScenarioStart<RxScenario, RxFlow> scenario() {
        return scenario(DEFAULT_NAME);
    }

    static RxScenarioBuilderInterfaces.ScenarioStart<RxScenario, RxFlow> scenario(String name) {
        return new RxScenarioBuilder(name);
    }

    @Override
    protected RxScenario createScenario(String name, Map<String, Object> parameters, RxFlow flow) {
        return new RxScenario(name, parameters, flow, listeners);
    }
}

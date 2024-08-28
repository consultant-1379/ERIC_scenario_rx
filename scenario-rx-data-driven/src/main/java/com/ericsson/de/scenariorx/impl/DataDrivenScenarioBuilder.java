package com.ericsson.de.scenariorx.impl;

import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.api.RxScenario;

public abstract class DataDrivenScenarioBuilder extends ScenarioBuilder<RxScenario, RxFlow> {
    protected DataDrivenScenarioBuilder(String name, FlowBuilder<RxFlow> flowBuilder) {
        super(name, flowBuilder);
        listeners.add(new RxDataDrivenListener());
    }

    @Override
    public RxScenario build() {
        scenarioFlowBuilder.exceptionHandler = new ContinueOnNextDataRecord(scenarioFlowBuilder.exceptionHandler);
        return super.build();
    }
}

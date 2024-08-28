package com.ericsson.de.scenariorx.impl;

import java.util.List;

import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.google.common.base.Predicate;

public class PerformanceFlowBuilder extends FlowBuilder<RxFlow> {
    private RxRampUp.StrategyProvider rampUp = RxRampUp.allAtOnce();

    protected PerformanceFlowBuilder(String name) {
        super(name);
    }

    @Override
    protected RxFlow[] createFlowArray(int length) {
        return new RxFlow[0];
    }

    @Override
    protected RxFlow createFlow(String name, DataSourceStrategy dataSource, List<Invocation> testSteps, List<RxTestStep> beforeInvocations, List<RxTestStep> afterInvocations, ExceptionHandler exceptionHandler, Predicate<RxDataRecordWrapper> predicate) {
        return new RxFlow(name, dataSource, testSteps, beforeInvocations, afterInvocations, exceptionHandler, predicate);
    }

    public RxRampUp.StrategyProvider getRampUp() {
        return rampUp;
    }

    public void withRampUp(RxRampUp.StrategyProvider rampUp) {
        this.rampUp = rampUp;
    }
}

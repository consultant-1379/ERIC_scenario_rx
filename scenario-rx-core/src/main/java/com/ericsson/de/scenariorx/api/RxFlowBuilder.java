package com.ericsson.de.scenariorx.api;

import java.util.List;

import com.ericsson.de.scenariorx.impl.DataSourceStrategy;
import com.ericsson.de.scenariorx.impl.ExceptionHandler;
import com.ericsson.de.scenariorx.impl.FlowBuilder;
import com.ericsson.de.scenariorx.impl.Invocation;
import com.google.common.base.Predicate;

public final class RxFlowBuilder extends FlowBuilder<RxFlow> {

    private RxFlowBuilder(String name) {
        super(name);
    }

    static RxFlowBuilderInterfaces.FlowStart<RxFlow> flow() {
        return flow(DEFAULT_NAME);
    }

    static RxFlowBuilderInterfaces.FlowStart<RxFlow> flow(String name) {
        return new RxFlowBuilder(name);
    }

    @Override
    protected RxFlow[] createFlowArray(int length) {
        return new RxFlow[length];
    }

    @Override
    protected RxFlow createFlow(String name, DataSourceStrategy dataSource, List<Invocation> testSteps,
                                List<RxTestStep> beforeInvocations, List<RxTestStep> afterInvocations,
                                ExceptionHandler exceptionHandler, Predicate<RxDataRecordWrapper> predicate) {
        return new RxFlow(name, dataSource, testSteps, beforeInvocations, afterInvocations, exceptionHandler, predicate);
    }
}

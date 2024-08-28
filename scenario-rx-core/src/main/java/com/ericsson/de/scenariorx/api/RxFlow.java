package com.ericsson.de.scenariorx.api;

import java.util.List;

import com.ericsson.de.scenariorx.impl.DataSourceStrategy;
import com.ericsson.de.scenariorx.impl.ExceptionHandler;
import com.ericsson.de.scenariorx.impl.Flow;
import com.ericsson.de.scenariorx.impl.Invocation;
import com.google.common.base.Predicate;

public class RxFlow extends Flow {

    public RxFlow(String name, DataSourceStrategy dataSource, List<Invocation> testSteps,
                  List<RxTestStep> beforeInvocations, List<RxTestStep> afterInvocations,
                  ExceptionHandler exceptionHandler, Predicate<RxDataRecordWrapper> predicate) {
        super(name, dataSource, testSteps, beforeInvocations, afterInvocations, exceptionHandler, predicate);
    }
}

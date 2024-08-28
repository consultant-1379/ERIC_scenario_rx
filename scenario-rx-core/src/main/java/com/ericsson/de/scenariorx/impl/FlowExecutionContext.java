package com.ericsson.de.scenariorx.impl;

import static java.util.Collections.singletonList;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

import java.util.Iterator;
import java.util.List;

import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.google.common.base.Predicate;

import rx.Observable;
import rx.functions.Func1;

public class FlowExecutionContext {

    final Scenario scenario;
    final ScenarioEventBus eventBus;
    private final ExceptionHandler defaultExceptionHandler;

    final Flow flow;
    final int vUsers;
    final Observable<RxDataRecordWrapper> dataSource;
    final DataRecordsToExecutions toExecutions;
    protected final Predicate<RxDataRecordWrapper> predicate;

    private FlowExecutionContext(Scenario scenario, ScenarioEventBus eventBus,
                                 ExceptionHandler defaultExceptionHandler, Flow flow, int vUsers,
                                 final Observable<RxDataRecordWrapper> dataSource, DataRecordsToExecutions toExecutions, Predicate<RxDataRecordWrapper> predicate) {
        this.scenario = scenario;
        this.eventBus = eventBus;
        this.defaultExceptionHandler = defaultExceptionHandler;

        this.flow = flow;
        this.vUsers = vUsers;
        this.dataSource = dataSource;
        this.toExecutions = toExecutions;

        this.predicate = predicate;
    }

    /**
     * Initial flow which is created for scenario
     */
    static FlowExecutionContext createScenarioFlowContext(Scenario scenario,
                                                          ScenarioEventBus eventBus,
                                                          ExceptionHandler defaultExceptionHandler) {
        Flow flow = scenario.flow;
        int vUsers = flow.dataSource.vUsers;
        Observable<RxDataRecordWrapper> dataSource = flow.dataSource.provide();
        List<Internals.Exec> executions = singletonList(Internals.Exec.rootExec(scenario.parameters));
        DataRecordsToExecutions toExecutions = new DataRecordsToExecutions(flow, executions, 0);

        final Predicate<RxDataRecordWrapper> rxDataRecordWrapperPredicate = predicateOrDefault(flow, dataSource);

        return new FlowExecutionContext(scenario, eventBus, defaultExceptionHandler, flow, vUsers, dataSource, toExecutions, rxDataRecordWrapperPredicate);
    }

    /**
     * Forks subFlow from flow
     */
    public FlowExecutionContext subFlow(Flow subFlow, List<Internals.Exec> executions, int vUserOffset) {
        int vUsers = executions.size() * subFlow.dataSource.vUsers;
        Observable<RxDataRecordWrapper> subFlowDataSource = subFlow.dataSource.forkFrom(getDataRecords(executions));
        DataRecordsToExecutions dataRecordsToExecutions = new DataRecordsToExecutions(subFlow, executions, vUserOffset);
        final Predicate<RxDataRecordWrapper> rxDataRecordWrapperPredicate = predicateOrDefault(subFlow, subFlowDataSource);
        return new FlowExecutionContext(scenario, eventBus, defaultExceptionHandler, subFlow, vUsers, subFlowDataSource, dataRecordsToExecutions, rxDataRecordWrapperPredicate);
    }

    private static Predicate predicateOrDefault(Flow flow, final Observable<RxDataRecordWrapper> dataSource) {
        if (flow.predicate == null) {
            return new Predicate<RxDataRecordWrapper>() {
                int dataIteration = 0;
                final Integer dataRecordCount = dataSource.count().toBlocking().single();

                @Override
                public boolean apply(RxDataRecordWrapper input) {
                    dataIteration++;
                    return dataIteration <= dataRecordCount;
                }
            };
        } else {
            return flow.predicate;
        }

    }

    ExceptionHandler exceptionHandler() {
        return firstNonNull(flow.exceptionHandler, defaultExceptionHandler);
    }

    private static Observable<RxDataRecordWrapper> getDataRecords(List<Internals.Exec> executions) {
        return Observable.from(executions)
                .map(new Func1<Internals.Exec, RxDataRecordWrapper>() {
                    @Override
                    public RxDataRecordWrapper call(Internals.Exec exec) {
                        return exec.dataRecord;
                    }
                });
    }

    /**
     * Function Wraps {@code dataRecords} into {@link Internals.Exec} allocating vUsers
     */
    static class DataRecordsToExecutions implements Func1<List<RxDataRecordWrapper>, List<Internals.Exec>> {

        final private Flow flow;
        final private int forkCount;
        final private List<Internals.Exec> parentExecutions;
        final private int vUserOffset;

        DataRecordsToExecutions(Flow flow, List<Internals.Exec> parentExecutions, int vUserOffset) {
            this.flow = flow;
            this.forkCount = flow.dataSource.vUsers;
            this.parentExecutions = parentExecutions;
            this.vUserOffset = vUserOffset;
        }

        @Override
        public List<Internals.Exec> call(List<RxDataRecordWrapper> dataRecords) {
            checkArgument(parentExecutions.size() * forkCount >= dataRecords.size());

            Iterator<Internals.Exec> parentIterator = parentExecutions.iterator();
            Internals.Exec parent = parentIterator.next();
            int childNo = 1;

            List<Internals.Exec> executions = newArrayList();
            for (Iterator<RxDataRecordWrapper> iterator = dataRecords.iterator(); iterator.hasNext(); childNo++) {
                if (childNo == forkCount + 1) {
                    parent = parentIterator.next();
                    childNo = 1;
                }

                executions.add(parent.child(flow.getName(), childNo + vUserOffset, iterator.next()));
            }

            return executions;
        }
    }
}

package com.ericsson.de.scenariorx.impl;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import com.ericsson.de.scenariorx.api.Identifiable;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.ericsson.de.scenariorx.impl.Internals.Chunk;
import com.ericsson.de.scenariorx.impl.Internals.Fork;
import com.google.common.base.Predicate;
import rx.Observable;

/**
 * Sequence of Test Steps
 */
public abstract class Flow implements Identifiable<Long> {

    Long id = null;
    private final String name;
    final DataSourceStrategy dataSource;
    final List<Invocation> testSteps;

    private final List<RxTestStep> beforeInvocations;
    private final List<RxTestStep> afterInvocations;
    final Predicate predicate;

    final ExceptionHandler exceptionHandler;

    protected Flow(String name, DataSourceStrategy dataSource, List<Invocation> testSteps,
                   List<RxTestStep> beforeInvocations, List<RxTestStep> afterInvocations,
                   ExceptionHandler exceptionHandler, Predicate<RxDataRecordWrapper> predicate) {
        this.name = name;
        this.dataSource = dataSource;
        this.testSteps = testSteps;

        this.beforeInvocations = beforeInvocations;
        this.afterInvocations = afterInvocations;

        this.exceptionHandler = exceptionHandler;
        this.predicate = predicate;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * @return name of the Flow.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Chunks of scenario that is safe to execute in parallel
     */
    Observable<Chunk> chunks() {
        List<Chunk> chunks = newArrayList();
        Chunk chunk = new Chunk();
        for (Invocation testStep : testSteps) {
            if (testStep instanceof RxTestStep) {
                chunk.testSteps.add(RxTestStep.class.cast(testStep));
            } else if (testStep instanceof Fork) {
                chunk.fork = Fork.class.cast(testStep);
                chunks.add(chunk);
                chunk = new Chunk();
            }
        }
        chunks.add(chunk);

        return Observable.from(chunks);
    }

    List<RxTestStep> getBefore() {
        return beforeInvocations;
    }

    List<RxTestStep> getAfter() {
        return afterInvocations;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("testSteps", testSteps)
                .add("dataSource", dataSource)
                .toString();
    }
}

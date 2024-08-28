package com.ericsson.de.scenariorx.impl;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import static com.ericsson.de.scenariorx.impl.StackTraceFilter.filterFrameworkStackTrace;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ericsson.de.scenariorx.api.RxBasicDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxDataSource;
import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;

public abstract class DataSourceStrategy {
    public static final String ERROR_DATA_SOURCE_EMPTY = "Data Source `%s` did not produce any Data Records";
    private static final RxDataRecord EMPTY = RxBasicDataRecord.builder().build();

    private final Observable<RxDataRecordWrapper> dataSource;
    private final String definition;
    final int vUsers;

    DataSourceStrategy(Observable<RxDataRecordWrapper> dataSource, String definition, int vUsers) {
        this.dataSource = dataSource;
        this.definition = definition;
        this.vUsers = vUsers;
    }

    /**
     * Provide Data Source Observable
     */
    abstract Observable<RxDataRecordWrapper> provide();

    /**
     * Merge Data Records with {@param parentDataRecords}
     */
    abstract Observable<RxDataRecordWrapper> forkFrom(Observable<RxDataRecordWrapper> parentDataRecords);

    /**
     * @return copy if Data Source is mutable to avoid modification in real time
     */
    Observable<RxDataRecordWrapper> getDataSource() {
        if (dataSource instanceof ReplaySubject) {
            ReplaySubject replaySubject = ReplaySubject.class.cast(dataSource);
            Object[] values = replaySubject.getValues();
            RxDataRecordWrapper[] dataRecords = Arrays.copyOf(values, values.length, RxDataRecordWrapper[].class);
            return Observable.from(dataRecords);
        }

        return dataSource;
    }

    static DataSourceStrategy fromDefinitions(RxDataSource[] definitions, int vUsers) {
        if (definitions.length == 1) {
            return fromDefinition(definitions[0], vUsers);
        } else {
            List<DataSourceStrategy> strategies = new ArrayList<>(definitions.length);
            for (RxDataSource definition : definitions) {
                strategies.add(fromDefinition(definition, vUsers));
            }

            return new Multiple(strategies, vUsers);
        }
    }

    static DataSourceStrategy fromDefinition(RxDataSource<RxDataRecord> definition, int vUsers) {
        Observable<RxDataRecordWrapper> observable = provideObservable(definition);

        return definition.isShared()
                ? new DataSourceStrategy.Shared(observable, definition.toString(), vUsers)
                : new DataSourceStrategy.Copied(observable, definition.toString(), vUsers);
    }

    private static Observable<RxDataRecordWrapper> provideObservable(RxDataSource<RxDataRecord> definition) {
        Observable<RxDataRecordWrapper> observable = Observable
                .from((Iterable<RxDataRecord>) definition)
                .defaultIfEmpty(EMPTY)
                .doOnNext(errorOnEmpty(definition.getName()))
                .map(DataSourceRx.wrapDataRecords(definition.getName(), definition.getDataRecordTransformer()))
                .cache();

        if (definition.isCyclic()) {
            observable = makeCyclic(observable);
        }

        return observable;
    }

    private static Action1<? super RxDataRecord> errorOnEmpty(final String name) {
        return new Action1<RxDataRecord>() {
            @Override
            public void call(RxDataRecord rxDataRecord) {
                if (rxDataRecord == EMPTY) {
                    throw filterFrameworkStackTrace(new IllegalArgumentException(format(ERROR_DATA_SOURCE_EMPTY, name)));
                }
            }
        };
    }


    /**
     * Subscription should be done on separate thread to avoid deadlock in {@link DataSourceRx#merge()} while
     * calling {@link rx.Observable#unsafeSubscribe} on multiple repeating observables
     */
    private static <T> Observable<T> makeCyclic(Observable<T> observable) {
        return observable.observeOn(Schedulers.computation()).repeat();
    }

    static DataSourceStrategy empty(String name, int vUsers) {
        return new Empty(name, vUsers);
    }

    String definition() {
        return definition;
    }

    private static class Multiple extends DataSourceStrategy {
        final private List<DataSourceStrategy> strategies;

        Multiple(List<DataSourceStrategy> strategies, int vUsers) {
            super(null, "multiple Data Sources", vUsers);
            this.strategies = strategies;
        }

        @Override
        Observable<RxDataRecordWrapper> provide() {
            List<Observable<RxDataRecordWrapper>> observables = new ArrayList<>(strategies.size());
            for (DataSourceStrategy strategy : strategies) {
                observables.add(strategy.provide());
            }

            return Observable.zip(observables, DataSourceRx.merge());
        }

        @Override
        Observable<RxDataRecordWrapper> forkFrom(Observable<RxDataRecordWrapper> parentDataRecords) {
            List<Observable<RxDataRecordWrapper>> observables = new ArrayList<>(strategies.size());
            for (DataSourceStrategy strategy : strategies) {
                observables.add(strategy.forkFrom(parentDataRecords));
            }

            return Observable.zip(observables, DataSourceRx.merge());
        }
    }

    private static class Shared extends DataSourceStrategy {
        Shared(Observable<RxDataRecordWrapper> dataSource, String definition, int vUsers) {
            super(dataSource, definition, vUsers);
        }

        @Override
        Observable<RxDataRecordWrapper> provide() {
            return getDataSource();
        }

        @Override
        Observable<RxDataRecordWrapper> forkFrom(Observable<RxDataRecordWrapper> parentDataRecords) {
            Observable<RxDataRecordWrapper> idealRepetitions = DataSourceRx.copy(parentDataRecords, vUsers);

            return Observable.zip(idealRepetitions.repeat(), getDataSource(), DataSourceRx.glue());
        }
    }

    private static class Copied extends DataSourceStrategy {
        Copied(Observable<RxDataRecordWrapper> dataSource, String definition, int vUsers) {
            super(dataSource, definition, vUsers);
        }

        @Override
        Observable<RxDataRecordWrapper> provide() {
            return DataSourceRx.copy(getDataSource(), vUsers);
        }

        @Override
        Observable<RxDataRecordWrapper> forkFrom(Observable<RxDataRecordWrapper> parentDataRecords) {
            return DataSourceRx.multiply(provide(), parentDataRecords);
        }
    }

    static class Empty extends Copied {

        Empty(String name, int vUsers) {
            super(Observable.<RxDataRecordWrapper>just(new DataRecords.Empty(name)), "", vUsers);
        }
    }
}

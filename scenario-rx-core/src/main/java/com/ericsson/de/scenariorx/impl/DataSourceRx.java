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

import static com.ericsson.de.scenariorx.impl.StackTraceFilter.clearStackTrace;
import static com.google.common.collect.Iterables.getLast;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecordTransformer;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import rx.Observable;
import rx.exceptions.CompositeException;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.FuncN;

/**
 * Applies transformations to Data Records, utilizing RxJava
 */
class DataSourceRx {
    /**
     * Combines contents of Data Records
     * E.g. a-b-c + 1-2-3 = a-b-c-1-2-3
     * To append Data Record from Parent Data Source to Data Record from child Data Source
     */
    static Func2<RxDataRecordWrapper, RxDataRecordWrapper, RxDataRecordWrapper> glue() {
        return new Func2<RxDataRecordWrapper, RxDataRecordWrapper, RxDataRecordWrapper>() {
            @Override
            public RxDataRecordWrapper call(RxDataRecordWrapper parent, RxDataRecordWrapper child) {
                return new DataRecords.Parent(child, parent);
            }
        };
    }

    /**
     * Merges contents of Data Records.
     * E.g. a-b-c + a-2-3 = a-b-c-2-3
     * To avoid duplicates in case both Data Records have same parent records.
     */
    static FuncN<RxDataRecordWrapper> merge() {
        return new FuncN<RxDataRecordWrapper>() {
            @Override
            public RxDataRecordWrapper call(Object... args) {
                RxDataRecordWrapper[] wrappers = Arrays.copyOf(args, args.length, RxDataRecordWrapper[].class);
                return new DataRecords.Multiple(wrappers);
            }
        };
    }

    /**
     * Flow Data Records * Subflow Data Records
     */
    static Observable<RxDataRecordWrapper> multiply(Observable<RxDataRecordWrapper> first, final Observable<RxDataRecordWrapper> second) {
        return first.flatMap(
                new Func1<RxDataRecordWrapper, Observable<RxDataRecordWrapper>>() {
                    @Override
                    public Observable<RxDataRecordWrapper> call(final RxDataRecordWrapper firstDataRecords) {
                        return second
                                .map(new Func1<RxDataRecordWrapper, RxDataRecordWrapper>() {
                                    @Override
                                    public RxDataRecordWrapper call(RxDataRecordWrapper secondDataRecords) {
                                        return new DataRecords.Parent(firstDataRecords, secondDataRecords);
                                    }
                                });
                    }
                }
        );
    }

    /**
     * Repeat Data Source multiple times
     */
    static Observable<RxDataRecordWrapper> copy(Observable<RxDataRecordWrapper> dataSource, final int vUsers) {
        return dataSource.flatMap(
                new Func1<RxDataRecordWrapper, Observable<RxDataRecordWrapper>>() {
                    @Override
                    public Observable<RxDataRecordWrapper> call(RxDataRecordWrapper DataRecords) {
                        return Observable
                                .just(DataRecords)
                                .repeat(vUsers);
                    }
                }
        );
    }

    /**
     * Wraps exceptions to CompositeException
     */
    static Throwable compose(Set<Throwable> throwables) {
        throwables.remove(null);
        if (throwables.size() == 1) {
            return throwables.iterator().next();
        } else if (throwables.size() > 1) {
            return clearStackTrace(new CompositeException(throwables));
        }
        return null;
    }

    static Func1<RxDataRecord, RxDataRecordWrapper> wrapDataRecords(final String name, final RxDataRecordTransformer transformer) {
        return new Func1<RxDataRecord, RxDataRecordWrapper>() {
            final AtomicInteger iteration = new AtomicInteger();

            @Override
            public RxDataRecordWrapper call(RxDataRecord testDataRecord) {
                return new DataRecords.Single(name, transformer, iteration.incrementAndGet(), testDataRecord);
            }
        };
    }

    static long startTime(Collection<Internals.Exec> executions) {
        long min = Long.MAX_VALUE;

        for (Internals.Exec execution : executions) {
            if (execution.getExecutedTestSteps().get(0).startTime < min) {
                min = execution.getExecutedTestSteps().get(0).startTime;
            }
        }

        return min;
    }

    public static long endTime(Collection<Internals.Exec> executions) {
        long max = 0;

        for (Internals.Exec execution : executions) {
            if (getLast(execution.getExecutedTestSteps()).endTime > max) {
                max = getLast(execution.getExecutedTestSteps()).endTime;
            }
        }

        return max;
    }
}

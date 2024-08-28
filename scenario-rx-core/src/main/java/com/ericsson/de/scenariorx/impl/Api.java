package com.ericsson.de.scenariorx.impl;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

/**
 * Api functions that uses internals from impl package
 */
public abstract class Api {

    /**
     * Creates Data Source based on Iterable contents.
     * Iterable may contain any type which could be injected using `@Input("dataSourceName") Type name`
     *
     * @param name     name of the Data Source
     * @param iterable source of data
     * @return definition
     */
    public static <T> RxDataSource<T> fromIterable(final String name, final Iterable<T> iterable) {
        checkArgument(iterable.iterator().hasNext(), "Iterable should not be empty!");
        checkArgument(Iterators.size(iterable.iterator()) == Iterators.size(iterable.iterator()), "Iterable should produce consistent records!");

        return new RxObjectDataSource<>(name, iterable);
    }

    /**
     * Creates Data Source from Data Records
     *
     * @param name        name of the Data Source
     * @param dataRecords source of data
     * @return definition
     */
    public static <T extends RxDataRecord> RxDataSource<T> fromDataRecords(final String name, final T... dataRecords) {
        checkArgument(dataRecords.length > 0, "Data Records array should not be empty!");
        return new RxIterableDataSource<>(name, RxDataRecord.class, asList(dataRecords));
    }

    /**
     * Creates Data Source from CSV
     *
     * @param name           name of the Data Source
     * @param location       csv location path
     * @param dataRecordType data source type
     * @return data source definition with defined type
     */
    public static <T extends RxDataRecord> RxDataSource<T> fromCsv(final String name, String location, Class<T> dataRecordType) {
        return new RxIterableDataSource<>(name, dataRecordType, CsvReader.read(location));
    }

    /**
     * Creates a Test Step out of @see{@link java.lang.Runnable} instance.
     * You can not pass data driven attributes in this case.
     *
     * @param runnable runnable
     * @return Test Step
     */
    public static <T extends RxTestStep> T runnable(Runnable runnable) {
        checkNotNull(runnable, "Runnable can't be null");
        boolean anonymousRunnable = runnable.getClass().getSimpleName().isEmpty();
        StackTraceElement element = new Exception().getStackTrace()[1];
        String className = anonymousRunnable ? element.getClassName() : runnable.getClass().getName();
        String methodName = anonymousRunnable ? element.getMethodName() + "(" : "run(referenced from ";
        String name = stripPackageName(className) + "." + methodName + element.getFileName() + ":" + element.getLineNumber() + ")";
        return (T) new Internals.RunnableTestStep(name, runnable);
    }

    /**
     * Predicate to repeat flow for given time period
     * Intended to be used with {@link FlowBuilder#runWhile(Predicate)}
     */
    public static Predicate<RxDataRecordWrapper> during(final Integer runFor, final TimeUnit timeUnit) {
        return new Predicate<RxDataRecordWrapper>() {
            final static int NOT_SET = 0;
            AtomicLong endTime = new AtomicLong(NOT_SET);

            @Override
            public boolean apply(RxDataRecordWrapper input) {
                setEndTimeIfFirstCall();

                return System.currentTimeMillis() < endTime.get();
            }

            private void setEndTimeIfFirstCall() {
                endTime.compareAndSet(NOT_SET, System.currentTimeMillis() + MILLISECONDS.convert(runFor, timeUnit));
            }
        };
    }

    private static String stripPackageName(String className) {
        return className.substring(className.lastIndexOf(".") + 1);
    }
}

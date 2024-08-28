package com.ericsson.de.scenariorx.api;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import com.ericsson.de.scenariorx.impl.DefaultDataRecordTransformer;

/**
 * {@link RxDataSource} could be any type, but its iterator should return subclass of {@link RxDataRecord}.
 * {@link RxDataRecordTransformer} should be able to transform {@link RxDataRecord} to type of DataSource
 * @see DefaultDataRecordTransformer
 */
public interface RxDataRecordTransformer<T> {
    /**
     * Transform {@link RxDataRecord} to given {@code type}
     */
    T transform(final RxDataRecord dataRecord, Class<T> type);

    /**
     * If this DataRecordTransformer able to {@link RxDataRecord} to {@code type}
     */
    boolean canTransformTo(Class type);

    /**
     * Convert value to target type
     */
    <U> U convert(String name, Object value, Class<U> targetType);
}

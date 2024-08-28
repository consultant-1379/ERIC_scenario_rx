/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 */

package com.ericsson.de.scenariorx.impl;

import java.util.Iterator;

import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.google.common.base.Preconditions;

/**
 * {@link RxDataSource} of iterable of {@link RxDataRecord}
 */
class RxIterableDataSource<T> extends RxDataSource<T> {
    private Iterable<? extends RxDataRecord> iterable;

    RxIterableDataSource(String name, Class<?> type, Iterable<? extends RxDataRecord> iterable) {
        super(name, type);
        this.iterable = iterable;
    }

    RxIterableDataSource(String name, Class<?> type) {
        super(name, type);
    }

    @Override
    public Iterator<? extends RxDataRecord> getIterator() {
        Preconditions.checkNotNull(iterable, "Iterator should be passed in constructor or overriden");
        return iterable.iterator();
    }

    @Override
    public RxDataSource<T> newDefinition() {
        return new RxIterableDataSource<>(name, getType(), this);
    }
}

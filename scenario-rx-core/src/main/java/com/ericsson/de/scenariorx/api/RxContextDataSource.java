package com.ericsson.de.scenariorx.api;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ericsson.de.scenariorx.impl.Bridge;

/**
 * Only mutable implementation of Data Source in Rx Scenario.
 * Intended for collecting Test Step results. Allows reuse of collected results in scope of one Scenario.
 */
public class RxContextDataSource<T> extends RxDataSource<T> {

    private static final String ERROR_NOT_POPULATED = "Data Source %s was not populated during scenario execution";

    private ConcurrentLinkedQueue<RxDataRecord> results = new ConcurrentLinkedQueue<>();
    private AtomicBoolean populated = new AtomicBoolean();

    RxContextDataSource(String name, Class<T> type) {
        super(name, type);
    }

    private RxContextDataSource(String name, Class<T> type, AtomicBoolean populated, ConcurrentLinkedQueue<RxDataRecord> results) {
        super(name, type);
        this.results = results;
        this.populated = populated;
    }

    /**
     * @return Iterator of RxDataRecord type.
     */
    @Override
    public Iterator<? extends RxDataRecord> getIterator() {
        Bridge.checkRxState(populated.get(), ERROR_NOT_POPULATED, name);
        return results.iterator();
    }

    @Override
    protected RxDataSource<T> newDefinition() {
        return new RxContextDataSource<>(name, (Class<T>) getType(), populated, results);
    }

    @SuppressWarnings("unchecked")
    void collectFromResult(String name, Object result) {
        populated.set(true);
        if (result instanceof RxDataRecord) {
            RxDataRecord rxDataRecord = RxDataRecord.class.cast(result);
            if (dataRecordIsWrapped(name, rxDataRecord)) {
                collectFromResult(name, rxDataRecord.getFieldValue(name));
            } else {
                results.add(rxDataRecord);
            }
        } else if (Bridge.isCollectionOfDataRecords(result)) {
            results.addAll(Collection.class.cast(result));
        } else {
            RxDataRecord record = RxBasicDataRecord.fromValues(name, result);
            results.add(record);
        }
    }

    /**
     * Test Step result might be wrapped in another RxDataRecord
     * @see TafTestContextBridge#mergeOldContextAndReturnValue
     */
    private boolean dataRecordIsWrapped(String name, RxDataRecord rxDataRecord) {
        return rxDataRecord.getFieldValue(name) != null;
    }
}

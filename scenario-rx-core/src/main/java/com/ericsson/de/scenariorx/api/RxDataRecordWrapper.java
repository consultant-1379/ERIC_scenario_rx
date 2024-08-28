package com.ericsson.de.scenariorx.api;

import com.google.common.base.Optional;

/**
 * Encapsulates multiple Data Records from multiple Data Sources in hierarchical structure, including Context and Test Step Params.
 * Used as input value for {@link RxTestStep}
 * @see com.ericsson.de.scenariorx.impl.DataRecords.Parent
 * @see com.ericsson.de.scenariorx.impl.DataRecords.Single
 * @see com.ericsson.de.scenariorx.impl.DataRecords.Multiple
 */
public interface RxDataRecordWrapper {

    /**
     * Should return value for parameter injection of {@link RxTestStep}
     * @param name of Parameter
     * @param type of Parameter
     * @return Optional of parameter value
     */
    <V> Optional<V> getFieldValue(String name, Class<V> type);

    /**
     * Gets name of the data source this record belongs to
     *
     * @return name of the data source
     */
    String getDataSourceName();

    /**
     * Gets iteration number of Data Record
     * Parent iterations are divided by <code>.</code>
     * Multiple Data Source iterations divided by <code>-</code>
     */
    String getIteration();
}

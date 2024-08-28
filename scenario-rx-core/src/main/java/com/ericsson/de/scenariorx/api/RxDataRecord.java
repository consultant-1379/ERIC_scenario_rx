package com.ericsson.de.scenariorx.api;

import java.util.Map;

/**
 * Represents single record of Data Source.
 */
public interface RxDataRecord {
    /**
     * Returns the Data Source field value
     *
     * @param name  field name
     * @return  field value
     */
    <T> T getFieldValue(String name);

    /**
     * Returns a map of all data source fields
     *
     * @return  a map of all data source fields (mapping field name to field value)
     */
    Map<String, Object> getAllFields();
}
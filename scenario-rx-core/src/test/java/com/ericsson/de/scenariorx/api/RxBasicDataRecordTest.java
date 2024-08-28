package com.ericsson.de.scenariorx.api;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;


public class RxBasicDataRecordTest {
    @Test
    public void copy() throws Exception {
        RxDataRecord immutable = RxBasicDataRecord.fromValues("one", "immutable", "two", "value");

        RxDataRecord copy = RxBasicDataRecord
                .copy(immutable)
                .setField("one", "new")
                .build();

        assertThat(immutable.getFieldValue("one")).isEqualTo("immutable");
        assertThat(copy.getFieldValue("one")).isEqualTo("new");
        assertThat(copy.getFieldValue("two")).isEqualTo("value");
    }
}
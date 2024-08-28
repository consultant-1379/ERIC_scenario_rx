package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.impl.StackTraceFilter.filterFrameworkStackTrace;
import static java.lang.String.format;

import java.util.Collection;
import java.util.Map;

import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;

/**
 * Provide access to some internal functionality located in impl to api package
 */
public final class Bridge {
    private Bridge() {
    }

    public static RxDataRecordWrapper wrapWithParameters(Map<String, Object> parameters, Map<String, String> bindings, RxDataRecordWrapper dataRecord) {
        dataRecord = parameters.isEmpty() ? dataRecord : new DataRecords.Parameter(dataRecord, parameters);
        dataRecord = bindings.isEmpty() ? dataRecord : new DataRecords.Binding(dataRecord, bindings);

        return dataRecord;
    }

    public static boolean isCollectionOfDataRecords(Object returnedValue) {
        return returnedValue instanceof Collection &&
                Collection.class.cast(returnedValue).iterator().hasNext() &&
                RxDataRecord.class.isInstance(Collection.class.cast(returnedValue).iterator().next());
    }

    /**
     * Acts as {@link com.google.common.base.Preconditions#checkState(boolean)} but filters stack trace
     * Should only be used to validate User error
     */
    public static void checkRxState(
            boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
        if (!expression) {
            IllegalStateException illegalStateException = new IllegalStateException(format(errorMessageTemplate, errorMessageArgs));
            throw filterFrameworkStackTrace(illegalStateException);
        }
    }

    /**
    * Acts as {@link com.google.common.base.Preconditions#checkNotNull(Object)}} but filters stack trace
    * Should only be used to validate User error
    */
    public static <T> T checkRxNotNull(T reference, String msg) {
        if (reference == null) {
            NullPointerException nullPointerException = new NullPointerException(msg);
            throw filterFrameworkStackTrace(nullPointerException);
        } else {
            return reference;
        }
    }
}

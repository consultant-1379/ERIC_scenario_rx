package com.ericsson.de.scenariorx.impl;

import com.ericsson.de.scenariorx.api.RxExceptionHandler;

final class ContinueOnNextDataRecord extends ExceptionHandler {
    ExceptionHandler delegate;

    public ContinueOnNextDataRecord(ExceptionHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    boolean continueOnNextDataRecord() {
        return true;
    }

    @Override
    public RxExceptionHandler.Outcome onException(Throwable e) {
        if (delegate == null) {
            return RxExceptionHandler.Outcome.PROPAGATE_EXCEPTION;
        } else {
            return delegate.onException(e);
        }
    }
}

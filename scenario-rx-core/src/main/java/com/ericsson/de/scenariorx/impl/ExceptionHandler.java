package com.ericsson.de.scenariorx.impl;

import com.ericsson.de.scenariorx.api.RxExceptionHandler;

public abstract class ExceptionHandler {

    public static final String ERROR_EXCEPTION_HANDLER_RETURN_NULL = "RxExceptionHandler can't return null, " +
            "please use either Outcome.PROPAGATE_EXCEPTION or Outcome.CONTINUE_FLOW";

    final boolean cannotHandle(Throwable e) {
        return RxExceptionHandler.Outcome.PROPAGATE_EXCEPTION.equals(tryHandle(e));
    }

    final boolean canHandle(Throwable e) {
        return RxExceptionHandler.Outcome.CONTINUE_FLOW.equals(tryHandle(e));
    }

    boolean continueOnNextDataRecord() {
        return false;
    }

    private RxExceptionHandler.Outcome tryHandle(Throwable e) {
        return Bridge.checkRxNotNull(onException(e), ERROR_EXCEPTION_HANDLER_RETURN_NULL);
    }

    public abstract RxExceptionHandler.Outcome onException(Throwable e);

}

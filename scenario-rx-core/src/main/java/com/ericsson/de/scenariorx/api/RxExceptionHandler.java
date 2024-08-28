package com.ericsson.de.scenariorx.api;

import com.ericsson.de.scenariorx.impl.ExceptionHandler;

/**
 * Exception handler determines whether flow propagates or ignores the exception.
 */
public abstract class RxExceptionHandler extends ExceptionHandler {

    public enum Outcome {
        /**
         * Stop Flow execution and propagate exception to next level exception handler (i.e. Sub Flow → Flow → Scenario).
         * If there are no more handlers defined, exception will be propagated to main thread and test will fail.
         */
        PROPAGATE_EXCEPTION,

        /**
         * If handler handles exception and returns this constant, Flow execution will continue, and no other handlers will be called.
         */
        CONTINUE_FLOW,
    }

    /**
     * @see Outcome#PROPAGATE_EXCEPTION
     */
    public static final RxExceptionHandler PROPAGATE = new RxExceptionHandler() {
        @Override
        public Outcome onException(Throwable e) {
            return Outcome.PROPAGATE_EXCEPTION;
        }
    };

    /**
     * @see Outcome#CONTINUE_FLOW
     */
    public static final RxExceptionHandler IGNORE = new RxExceptionHandler() {
        @Override
        public Outcome onException(Throwable e) {
            return Outcome.CONTINUE_FLOW;
        }
    };

    @Override
    public abstract Outcome onException(Throwable e);

}

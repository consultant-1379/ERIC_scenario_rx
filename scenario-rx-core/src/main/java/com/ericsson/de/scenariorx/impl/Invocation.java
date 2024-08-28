package com.ericsson.de.scenariorx.impl;

import com.ericsson.de.scenariorx.api.Identifiable;
import com.ericsson.de.scenariorx.api.RxTestStep;

/**
 * Could be {@link RxTestStep} or {@link Internals.Fork}
 */
public abstract class Invocation implements Identifiable<Long> {
    Long id = null;

    @Override
    public Long getId() {
        return id;
    }

    public abstract Invocation alwaysRun();
}

package com.ericsson.de.scenariorx.api;

/**
 * Generic interface that classes use to implement their specific `build` implementation.
 */
public interface Builder<T> {

    T build();

}

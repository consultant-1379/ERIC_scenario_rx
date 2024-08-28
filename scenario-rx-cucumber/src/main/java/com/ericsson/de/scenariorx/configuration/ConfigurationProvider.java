package com.ericsson.de.scenariorx.configuration;

import com.ericsson.cifwk.taf.configuration.Configuration;

public class ConfigurationProvider implements com.ericsson.cifwk.taf.spi.ConfigurationProvider {
    @Override
    public Configuration get() {
        return new RxConfiguration();
    }
}
